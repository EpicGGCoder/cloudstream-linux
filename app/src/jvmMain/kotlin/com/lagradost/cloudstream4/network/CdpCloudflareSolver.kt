package com.lagradost.cloudstream4.network

import com.google.gson.Gson
import com.google.gson.JsonObject
import com.google.gson.JsonParser
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import okhttp3.Interceptor
import okhttp3.Response
import org.slf4j.LoggerFactory
import java.io.File
import java.net.URI
import java.net.http.HttpClient
import java.net.http.WebSocket
import java.time.Duration
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicInteger

/**
 * Cloudflare bypass using Chrome DevTools Protocol (CDP).
 *
 * Uses the system's installed Chrome/Chromium to solve Cloudflare JS challenges.
 * Same approach as Puppeteer/Playwright.
 *
 * Flow:
 *   1. Find Chrome/Chromium binary
 *   2. Launch headless with --remote-debugging-port
 *   3. Connect via WebSocket to CDP
 *   4. Navigate to the Cloudflare-protected URL
 *   5. Wait for cf_clearance cookie
 *   6. Extract all cookies
 *   7. Return cookies for OkHttp
 */
object CdpCloudflareSolver {
    private val log = LoggerFactory.getLogger(CdpCloudflareSolver::class.java)
    private val gson = Gson()

    private var chromeProcess: Process? = null
    private var debugPort: Int = 0
    private var cdpWebSocket: WebSocket? = null
    private val messageId = AtomicInteger(0)
    private val responseMap = ConcurrentHashMap<Int, String>()
    private val cookieCache = ConcurrentHashMap<String, Map<String, String>>()
    private val cookieCacheTime = ConcurrentHashMap<String, Long>()

    /** OkHttp interceptor — detects Cloudflare and solves via CDP. */
    val interceptor: Interceptor = Interceptor { chain ->
        val req = chain.request()
        val resp = chain.proceed(req)

        if (isCloudflareChallenge(resp)) {
            log.info("Cloudflare challenge detected for ${req.url} — solving via CDP")
            val url = req.url.toString()
            val cookies = runBlocking { solveChallenge(url) }
            if (cookies != null) {
                resp.close()
                val cookieHeader = cookies.entries.joinToString("; ") { "${it.key}=${it.value}" }
                val newReq = req.newBuilder()
                    .header("Cookie", cookieHeader)
                    .build()
                return@Interceptor chain.proceed(newReq)
            }
        }
        resp
    }

    private fun isCloudflareChallenge(resp: Response): Boolean {
        if (resp.code != 403 && resp.code != 503) return false
        val server = resp.header("server") ?: ""
        return server.contains("cloudflare", ignoreCase = true)
    }

    private suspend fun solveChallenge(url: String): Map<String, String>? {
        val cached = cookieCache[url]
        val cachedTime = cookieCacheTime[url] ?: 0
        if (cached != null && System.currentTimeMillis() - cachedTime < 30 * 60 * 1000L) {
            return cached
        }

        return withContext(Dispatchers.IO) {
            try {
                val chrome = ensureChromeRunning() ?: run {
                    log.error("No Chrome/Chromium found")
                    return@withContext null
                }

                val ws = connectWebSocket(chrome.port) ?: return@withContext null

                sendCommand(ws, "Page.enable")
                sendCommand(ws, "Network.enable")
                sendCommand(ws, "Page.navigate", mapOf("url" to url))

                // Poll for cf_clearance cookie (max 30s)
                var cookies: Map<String, String>? = null
                for (i in 0 until 15) {
                    Thread.sleep(2000)
                    val allCookies = sendCommand(ws, "Network.getAllCookies")
                    val cookieMap = parseCookies(allCookies, url)
                    if (cookieMap.containsKey("cf_clearance")) {
                        cookies = cookieMap
                        break
                    }
                }

                if (cookies != null) {
                    cookieCache[url] = cookies
                    cookieCacheTime[url] = System.currentTimeMillis()
                    log.info("Cloudflare solved for $url (${cookies.size} cookies)")
                }
                cookies
            } catch (e: Exception) {
                log.error("CDP solve failed", e)
                null
            }
        }
    }

    private data class ChromeInstance(val process: Process, val port: Int)
    private var chromeInstance: ChromeInstance? = null

    private fun ensureChromeRunning(): ChromeInstance? {
        chromeInstance?.let { if (it.process.isAlive) return it }

        val binary = findChromeBinary() ?: return null
        val port = findFreePort()

        log.info("Launching Chrome: $binary --headless --remote-debugging-port=$port")
        val pb = ProcessBuilder(
            binary, "--headless=new", "--no-sandbox", "--disable-gpu",
            "--disable-dev-shm-usage", "--remote-debugging-port=$port",
            "--user-data-dir=${File(System.getProperty("java.io.tmpdir"), "cs-chrome").absolutePath}",
            "--window-size=1920,1080", "about:blank",
        )
        pb.redirectErrorStream(true)
        val proc = pb.start()
        Thread.sleep(2000)
        if (!proc.isAlive) return null

        val instance = ChromeInstance(proc, port)
        chromeInstance = instance
        Runtime.getRuntime().addShutdownHook(Thread { try { proc.destroy() } catch (_: Exception) {} })
        return instance
    }

    private fun findChromeBinary(): String? {
        val candidates = listOf(
            "/usr/bin/google-chrome", "/usr/bin/google-chrome-stable",
            "/usr/bin/chromium", "/usr/bin/chromium-browser",
            "/snap/bin/chromium", "/usr/bin/brave-browser", "/usr/bin/microsoft-edge",
            System.getenv("CHROME_PATH"),
        )
        return candidates.firstNotNullOfOrNull { p -> p?.takeIf { File(it).exists() && File(it).canExecute() } }
    }

    private fun findFreePort(): Int = java.net.ServerSocket(0).use { it.localPort }

    private suspend fun connectWebSocket(port: Int): WebSocket? {
        if (cdpWebSocket != null) return cdpWebSocket
        return withContext(Dispatchers.IO) {
            try {
                val httpClient = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(5)).build()
                val req = java.net.http.HttpRequest.newBuilder()
                    .uri(URI("http://127.0.0.1:$port/json"))
                    .timeout(Duration.ofSeconds(5)).GET().build()
                val resp = httpClient.send(req, java.net.http.HttpResponse.BodyHandlers.ofString())
                val arr = JsonParser.parseString(resp.body()).asJsonArray
                if (arr.size() == 0) return@withContext null
                val wsUrl = arr[0].asJsonObject.get("webSocketDebuggerUrl").asString

                val wsClient = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(5)).build()
                val ws = wsClient.newWebSocketBuilder()
                    .connectTimeout(Duration.ofSeconds(5))
                    .buildAsync(URI(wsUrl), object : WebSocket.Listener {
                        val buffer = StringBuilder()
                        override fun onText(ws: WebSocket, data: CharSequence, last: Boolean): java.util.concurrent.CompletionStage<*>? {
                            buffer.append(data)
                            if (last) { handleMessage(buffer.toString()); buffer.clear() }
                            return null
                        }
                        override fun onError(ws: WebSocket, error: Throwable) { log.error("CDP WS error", error) }
                        override fun onClose(ws: WebSocket, statusCode: Int, reason: String?): java.util.concurrent.CompletionStage<*>? {
                            log.info("CDP WS closed: $statusCode"); cdpWebSocket = null; return null
                        }
                    }).get(5, java.util.concurrent.TimeUnit.SECONDS)

                cdpWebSocket = ws
                ws
            } catch (e: Exception) { log.error("CDP connect failed", e); null }
        }
    }

    private fun handleMessage(message: String) {
        try {
            val json = JsonParser.parseString(message).asJsonObject
            val id = json.get("id")?.asInt
            if (id != null) responseMap[id] = message
        } catch (e: Exception) { log.debug("Parse failed: ${message.take(100)}") }
    }

    private fun sendCommand(ws: WebSocket, method: String, params: Map<String, Any> = emptyMap()): String {
        val id = messageId.incrementAndGet()
        val paramsJson = params.entries.joinToString(",") { "\"${it.key}\":${gson.toJson(it.value)}" }
        ws.sendText("""{"id":$id,"method":"$method","params":{$paramsJson}}""", true)
        for (i in 0 until 100) {
            val resp = responseMap.remove(id)
            if (resp != null) return resp
            Thread.sleep(50)
        }
        return "{}"
    }

    private fun parseCookies(cdpResponse: String, url: String): Map<String, String> {
        return try {
            val json = JsonParser.parseString(cdpResponse).asJsonObject
            val cookiesArray = json.getAsJsonObject("result")?.getAsJsonArray("cookies") ?: return emptyMap()
            val host = URI(url).host
            val result = mutableMapOf<String, String>()
            for (i in 0 until cookiesArray.size()) {
                val cookie = cookiesArray[i].asJsonObject
                val domain = cookie.get("domain")?.asString ?: ""
                val name = cookie.get("name")?.asString ?: continue
                val value = cookie.get("value")?.asString ?: continue
                val cleanDomain = domain.removePrefix(".")
                if (host.endsWith(cleanDomain) || cleanDomain.endsWith(host)) result[name] = value
            }
            result
        } catch (e: Exception) { emptyMap() }
    }

    fun shutdown() {
        try { cdpWebSocket?.sendClose(WebSocket.NORMAL_CLOSURE, "shutdown") } catch (_: Exception) {}
        try { chromeInstance?.process?.destroy() } catch (_: Exception) {}
        cdpWebSocket = null; chromeInstance = null
    }
}

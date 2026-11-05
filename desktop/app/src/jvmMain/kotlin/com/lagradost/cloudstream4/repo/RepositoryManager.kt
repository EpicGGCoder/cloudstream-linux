package com.lagradost.cloudstream4.repo

import com.lagradost.cloudstream4.plugin.PluginManager
import com.lagradost.nicehttp.Requests
import com.lagradost.nicehttp.RequestBodyTypes
import com.lagradost.nicehttp.ResponseParser
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.OkHttpClient
import okhttp3.RequestBody.Companion.toRequestBody
import org.slf4j.LoggerFactory
import java.io.File
import java.net.URI
import java.security.MessageDigest
import java.util.concurrent.TimeUnit

/**
 * Repository manager — mirrors CS3's `RepositoryManager.kt`.
 *
 * A repository is a JSON manifest at a URL that lists plugin descriptors.
 * Each plugin descriptor points to a `.cs3` file URL with a SHA-256 hash.
 *
 * Repo URL → fetch JSON → list of SitePlugin (url, name, version, fileHash)
 *   → download .cs3 → verify SHA-256 → install via PluginManager
 */
class RepositoryManager(
    private val pluginManager: PluginManager,
) {
    private val log = LoggerFactory.getLogger(RepositoryManager::class.java)
    private val json = Json { ignoreUnknownKeys = true; explicitNulls = false }

    private val client = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .followRedirects(true)
        .build()

    @Serializable
    data class Repository(
        val iconUrl: String? = null,
        val name: String,
        val description: String? = null,
        val manifestVersion: Int = 1,
        val pluginLists: List<String>,
    )

    @Serializable
    data class SitePlugin(
        val url: String,
        val status: Int = 1,         // 0=down, 1=ok, 2=slow, 3=beta_only
        val version: Int = 1,
        val apiVersion: Int = 1,
        val name: String,
        val internalName: String,
        val authors: List<String> = emptyList(),
        val description: String? = null,
        val repositoryUrl: String? = null,
        val tvTypes: List<String>? = null,
        val language: String? = null,
        val iconUrl: String? = null,
        val fileSize: Long? = null,
        val fileHash: String? = null,
    )

    @Serializable
    data class PluginWrapper(
        val repository: Repository,
        val plugin: SitePlugin,
    )

    // ------------------------------------------------------------------
    // Repo URL resolution — handles cloudstreamrepo:// shortlinks
    // ------------------------------------------------------------------

    private fun parseRepoUrl(url: String): String {
        val trimmed = url.trim()
        return when {
            trimmed.startsWith("cloudstreamrepo://") ->
                "https://" + trimmed.removePrefix("cloudstreamrepo://")
            trimmed.startsWith("https://cs.repo/") ->
                "https://" + trimmed.removePrefix("https://cs.repo/")
            trimmed.startsWith("!") && trimmed.length > 1 ->
                "https://py.md/${trimmed.removePrefix("!")}"
            trimmed.matches(Regex("^[a-zA-Z0-9_-]+$")) && trimmed.length in 4..16 ->
                "https://cutt.ly/$trimmed"
            else -> trimmed
        }
    }

    // ------------------------------------------------------------------
    // Fetch + parse repo manifest
    // ------------------------------------------------------------------

    suspend fun fetchRepo(repoUrl: String): Pair<Repository, List<SitePlugin>> = withContext(Dispatchers.IO) {
        val finalUrl = parseRepoUrl(repoUrl)
        log.info("Fetching repo: $finalUrl")

        val repoText = get(finalUrl).text
        val repo = json.decodeFromString<Repository>(repoText)

        val plugins = mutableListOf<SitePlugin>()
        for (listUrl in repo.pluginLists) {
            try {
                val listText = get(listUrl).text
                val list = json.decodeFromString<List<SitePlugin>>(listText)
                plugins.addAll(list)
            } catch (e: Exception) {
                log.error("Failed to fetch plugin list $listUrl", e)
            }
        }
        repo to plugins
    }

    // ------------------------------------------------------------------
    // Download + verify + install a single plugin
    // ------------------------------------------------------------------

    suspend fun downloadAndInstall(plugin: SitePlugin): Boolean = withContext(Dispatchers.IO) {
        try {
            val name = sanitize(plugin.internalName.ifBlank { plugin.name })
            val targetFile = File(pluginManager.pluginsDir(), "$name.cs3")
            if (targetFile.exists() && targetFile.length() == plugin.fileSize) {
                log.info("Plugin ${plugin.name} already downloaded; loading")
                return@withContext pluginManager.install(targetFile, plugin.url, isOnline = true)
            }

            log.info("Downloading plugin ${plugin.name} from ${plugin.url}")
            val bytes = getBytes(plugin.url)
            if (plugin.fileHash != null) {
                val actual = sha256(bytes)
                val expected = plugin.fileHash.removePrefix("sha256-")
                if (!actual.equals(expected, ignoreCase = true)) {
                    log.error(
                        "Hash mismatch for {}: expected={}, got={}",
                        plugin.name, expected, actual
                    )
                    return@withContext false
                }
                log.info("Hash verified for ${plugin.name}")
            }
            targetFile.writeBytes(bytes)
            pluginManager.install(targetFile, plugin.url, isOnline = true)
        } catch (e: Exception) {
            log.error("Failed to download ${plugin.url}", e)
            false
        }
    }

    // ------------------------------------------------------------------
    // HTTP helpers — thin wrapper over okhttp
    // ------------------------------------------------------------------

    private class StringResponse(val text: String)

    private fun get(url: String): StringResponse {
        val req = okhttp3.Request.Builder()
            .url(url)
            .header("User-Agent", USER_AGENT)
            .get()
            .build()
        client.newCall(req).execute().use { r ->
            if (!r.isSuccessful) throw RuntimeException("HTTP ${r.code} for $url")
            return StringResponse(r.body?.string() ?: "")
        }
    }

    private fun getBytes(url: String): ByteArray {
        val req = okhttp3.Request.Builder()
            .url(url)
            .header("User-Agent", USER_AGENT)
            .get()
            .build()
        client.newCall(req).execute().use { r ->
            if (!r.isSuccessful) throw RuntimeException("HTTP ${r.code} for $url")
            return r.body?.bytes() ?: ByteArray(0)
        }
    }

    private fun sanitize(s: String): String =
        s.lowercase().replace(Regex("[^a-z0-9._-]"), "_").take(64)

    private fun sha256(bytes: ByteArray): String {
        val md = MessageDigest.getInstance("SHA-256")
        return md.digest(bytes).joinToString("") { "%02x".format(it) }
    }

    companion object {
        const val USER_AGENT =
            "Mozilla/5.0 (X11; Linux x86_64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/149.0.0.0 Safari/537.36"
    }
}

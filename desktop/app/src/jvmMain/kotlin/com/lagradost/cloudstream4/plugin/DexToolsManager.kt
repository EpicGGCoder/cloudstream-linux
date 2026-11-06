package com.lagradost.cloudstream4.plugin

import org.slf4j.LoggerFactory
import java.io.File
import java.net.URI
import java.net.http.HttpClient
import java.net.http.HttpRequest
import java.net.http.HttpResponse
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.StandardCopyOption
import java.util.zip.ZipInputStream

/**
 * Manages the dex2jar tool — auto-downloads it to ~/.cloudstream/dex-tools/
 * on first run if it's not already available.
 *
 * This ensures the AppImage (which doesn't bundle dex-tools due to size)
 * can still convert .cs3 plugins by downloading dex-tools once and caching it.
 */
object DexToolsManager {
    private val log = LoggerFactory.getLogger(DexToolsManager::class.java)

    private const val DEX_TOOLS_VERSION = "v2.4"
    private const val DEX_TOOLS_URL = "https://github.com/pxb1988/dex2jar/releases/download/v2.4/dex-tools-v2.4.zip"
    private const val DEX_TOOLS_SCRIPT = "d2j-dex2jar.sh"

    private var initialized = false
    private var dexToolsDir: File? = null

    /**
     * Initialize dex-tools — download if not already present.
     * Returns the path to the d2j-dex2jar.sh script, or null if download failed.
     */
    fun init(dataDir: File): File? {
        if (initialized) return dexToolsDir?.let { File(it, DEX_TOOLS_SCRIPT) }?.takeIf { it.exists() }

        initialized = true
        val targetDir = File(dataDir, "dex-tools")
        val scriptFile = File(targetDir, DEX_TOOLS_SCRIPT)

        // Already downloaded?
        if (scriptFile.exists() && scriptFile.canExecute()) {
            log.info("dex-tools already present at ${targetDir.absolutePath}")
            dexToolsDir = targetDir
            return scriptFile
        }

        // Check if dex-tools is available alongside the app (dev mode or AppImage)
        val candidates = listOf(
            File("app/resources/dex-tools/$DEX_TOOLS_SCRIPT"),
            File("resources/dex-tools/$DEX_TOOLS_SCRIPT"),
            File(System.getenv("APPDIR") ?: "", "resources/dex-tools/$DEX_TOOLS_SCRIPT"),
            File("/usr/bin/d2j-dex2jar"),
        )
        for (candidate in candidates) {
            if (candidate.exists() && candidate.canExecute()) {
                log.info("Found system dex-tools at ${candidate.absolutePath}")
                dexToolsDir = candidate.parentFile
                return candidate
            }
        }

        // Download dex-tools
        log.info("Downloading dex-tools $DEX_TOOLS_VERSION to ${targetDir.absolutePath}...")
        targetDir.mkdirs()

        try {
            val client = HttpClient.newBuilder().build()
            val request = HttpRequest.newBuilder()
                .uri(URI(DEX_TOOLS_URL))
                .timeout(java.time.Duration.ofMinutes(2))
                .GET()
                .build()
            val response = client.send(request, HttpResponse.BodyHandlers.ofInputStream())

            if (response.statusCode() != 200) {
                log.error("Failed to download dex-tools: HTTP ${response.statusCode()}")
                return null
            }

            // Extract the ZIP
            ZipInputStream(response.body()).use { zis ->
                var entry = zis.nextEntry
                while (entry != null) {
                    val entryPath = entry.name
                    // Skip the top-level directory (e.g., "dex-tools-v2.4/")
                    val relativePath = entryPath.substringAfter("/", entryPath)
                    if (relativePath.isBlank() || relativePath.endsWith("/")) {
                        entry = zis.nextEntry
                        continue
                    }
                    val outFile = File(targetDir, relativePath)
                    outFile.parentFile?.mkdirs()
                    Files.copy(zis, outFile.toPath(), StandardCopyOption.REPLACE_EXISTING)
                    if (relativePath.endsWith(".sh")) {
                        outFile.setExecutable(true)
                    }
                    entry = zis.nextEntry
                }
            }

            if (scriptFile.exists()) {
                scriptFile.setExecutable(true)
                log.info("dex-tools downloaded and extracted to ${targetDir.absolutePath}")
                dexToolsDir = targetDir
                return scriptFile
            } else {
                log.error("dex-tools download succeeded but script not found at ${scriptFile.absolutePath}")
                // List what was actually extracted
                targetDir.listFiles()?.forEach { log.error("  Found: ${it.name}") }
                return null
            }
        } catch (e: Exception) {
            log.error("Failed to download dex-tools", e)
            return null
        }
    }
}

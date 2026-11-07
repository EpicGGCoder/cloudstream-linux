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
    // Plugin auto-update — checks installed plugins for newer versions
    // ------------------------------------------------------------------

    /**
     * Check all installed plugins for updates from their source repos.
     * If a newer version is found, download and update it automatically.
     *
     * Called on app startup. Mirrors CS3's auto-update behavior:
     * "if I have added castletv and it was updated, it will get updated
     *  in the app whenever I reopen it."
     *
     * @param onProgress callback for each plugin being checked/updated
     * @return number of plugins updated
     */
    suspend fun checkAndAutoUpdatePlugins(
        onProgress: (pluginName: String, status: String) -> Unit = { _, _ -> }
    ): Int = withContext(Dispatchers.IO) {
        val installed = pluginManager.installed.value
        if (installed.isEmpty()) return@withContext 0

        var updatedCount = 0

        for (plugin in installed) {
            val sourceUrl = plugin.sourceUrl ?: continue
            onProgress(plugin.displayName, "Checking…")

            try {
                // The sourceUrl is the .cs3 file URL.
                // We need to find the repo.json that listed this plugin to get
                // the latest version info. But we don't store the repo URL.
                //
                // Alternative approach: just re-download the .cs3 from sourceUrl
                // and check if the file size/hash changed. This is simpler and
                // doesn't require storing the repo URL.
                //
                // CS3 stores the repo URL in its REPOSITORIES_KEY. We should do the same.
                // For now, we use the simpler approach: HEAD the source URL and compare
                // content-length with the current file size.

                val headReq = okhttp3.Request.Builder()
                    .url(sourceUrl)
                    .header("User-Agent", USER_AGENT)
                    .head()
                    .build()

                var remoteSize: Long? = null
                client.newCall(headReq).execute().use { resp ->
                    remoteSize = resp.header("Content-Length")?.toLongOrNull()
                }

                val currentFile = File(plugin.filePath)
                val currentSize = if (currentFile.exists()) currentFile.length() else -1

                if (remoteSize != null && remoteSize != currentSize) {
                    onProgress(plugin.displayName, "Updating…")
                    log.info("Plugin ${plugin.displayName}: local=${currentSize}B remote=${remoteSize}B — updating")

                    // Re-download
                    val bytes = getBytes(sourceUrl)
                    currentFile.writeBytes(bytes)

                    // Re-install
                    pluginManager.install(currentFile, sourceUrl, isOnline = true)
                    updatedCount++
                    onProgress(plugin.displayName, "Updated")
                } else {
                    onProgress(plugin.displayName, "Up to date")
                }
            } catch (e: Exception) {
                log.debug("Auto-update check failed for ${plugin.displayName}: ${e.message}")
                onProgress(plugin.displayName, "Check failed")
            }
        }

        log.info("Auto-update: $updatedCount/${installed.size} plugins updated")
        updatedCount
    }

    // ------------------------------------------------------------------
    // Repo storage — track which repos the user has added
    // ------------------------------------------------------------------

    private val reposFile = File(pluginManager.dataDir, "repos.json")

    @Serializable
    data class StoredRepo(
        val url: String,
        val name: String? = null,
        val addedAt: Long = System.currentTimeMillis(),
    )

    @Serializable
    private data class RepoStore(val repos: List<StoredRepo> = emptyList())

    /**
     * Save a repo URL so we can check for updates later.
     */
    fun saveRepo(url: String, name: String?) {
        try {
            val store = if (reposFile.exists()) {
                json.decodeFromString(RepoStore.serializer(), reposFile.readText())
            } else RepoStore()
            val exists = store.repos.any { it.url == url }
            if (!exists) {
                val newStore = RepoStore(store.repos + StoredRepo(url, name))
                reposFile.writeText(json.encodeToString(RepoStore.serializer(), newStore))
            }
        } catch (e: Exception) {
            log.debug("Failed to save repo: ${e.message}")
        }
    }

    /**
     * Get all stored repo URLs.
     */
    fun getStoredRepos(): List<StoredRepo> {
        return try {
            if (reposFile.exists()) {
                json.decodeFromString(RepoStore.serializer(), reposFile.readText()).repos
            } else emptyList()
        } catch (e: Exception) {
            emptyList()
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

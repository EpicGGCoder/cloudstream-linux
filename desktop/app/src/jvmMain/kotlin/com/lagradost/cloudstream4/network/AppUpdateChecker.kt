package com.lagradost.cloudstream4.network

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import okhttp3.OkHttpClient
import okhttp3.Request
import org.slf4j.LoggerFactory
import java.util.concurrent.TimeUnit

/**
 * Checks GitHub releases for app updates.
 *
 * On startup, fetches the latest release from the GitHub API and compares
 * the tag with the current version. If newer, shows a notification to the user.
 */
object AppUpdateChecker {
    private val log = LoggerFactory.getLogger(AppUpdateChecker::class.java)
    private val json = Json { ignoreUnknownKeys = true }

    private const val REPO_OWNER = "EpicGGCoder"
    private const val REPO_NAME = "cloudstream-linux"
    private const val CURRENT_VERSION = "v0.1.0-dev"

    private val client = OkHttpClient.Builder()
        .connectTimeout(10, TimeUnit.SECONDS)
        .readTimeout(15, TimeUnit.SECONDS)
        .build()

    @Serializable
    data class GitHubRelease(
        val tagName: String? = null,
        val name: String? = null,
        val body: String? = null,
        val htmlUrl: String? = null,
        val prerelease: Boolean = false,
        val assets: List<GitHubAsset>? = null,
        val publishedAt: String? = null,
    )

    @Serializable
    data class GitHubAsset(
        val name: String? = null,
        val size: Long = 0,
        val browserDownloadUrl: String? = null,
        val downloadCount: Int = 0,
    )

    data class UpdateInfo(
        val available: Boolean,
        val latestVersion: String,
        val currentVersion: String,
        val releaseUrl: String?,
        val downloadUrl: String?,
        val releaseNotes: String?,
    )

    /**
     * Check for updates. Returns null if check fails (network error, etc).
     */
    suspend fun checkForUpdates(): UpdateInfo? = withContext(Dispatchers.IO) {
        try {
            val req = Request.Builder()
                .url("https://api.github.com/repos/$REPO_OWNER/$REPO_NAME/releases/latest")
                .header("Accept", "application/vnd.github.v3+json")
                .header("User-Agent", "CloudStream-Desktop")
                .get()
                .build()

            client.newCall(req).execute().use { resp ->
                if (!resp.isSuccessful) {
                    log.debug("GitHub API returned ${resp.code}")
                    return@withContext null
                }

                val body = resp.body?.string() ?: return@withContext null
                val release = json.decodeFromString(GitHubRelease.serializer(), body)
                val latestVersion = release.tagName ?: return@withContext null

                // Find the AppImage download URL
                val appImageAsset = release.assets?.find { it.name?.endsWith(".AppImage") == true }
                val downloadUrl = appImageAsset?.browserDownloadUrl

                val isNewer = isNewerVersion(CURRENT_VERSION, latestVersion)

                UpdateInfo(
                    available = isNewer,
                    latestVersion = latestVersion,
                    currentVersion = CURRENT_VERSION,
                    releaseUrl = release.htmlUrl,
                    downloadUrl = downloadUrl,
                    releaseNotes = release.body,
                )
            }
        } catch (e: Exception) {
            log.debug("Update check failed: ${e.message}")
            null
        }
    }

    /**
     * Simple version comparison: v0.2.0 > v0.1.0
     */
    private fun isNewerVersion(current: String, latest: String): Boolean {
        val cur = current.removePrefix("v").split(".").map { it.toIntOrNull() ?: 0 }
        val lat = latest.removePrefix("v").split(".").map { it.toIntOrNull() ?: 0 }
        for (i in 0 until maxOf(cur.size, lat.size)) {
            val c = cur.getOrElse(i) { 0 }
            val l = lat.getOrElse(i) { 0 }
            if (l > c) return true
            if (l < c) return false
        }
        return false
    }
}

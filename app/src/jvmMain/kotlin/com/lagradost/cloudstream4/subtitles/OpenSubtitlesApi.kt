package com.lagradost.cloudstream4.subtitles

import com.lagradost.cloudstream3.SubtitleFile
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.slf4j.LoggerFactory
import java.util.concurrent.TimeUnit

/**
 * OpenSubtitles API client — searches for subtitles by query and returns
 * [SubtitleFile]s that can be passed to the vlcj player.
 *
 * Uses the OpenSubtitles REST API v1 (https://api.opensubtitles.com).
 * Requires a free API key from https://www.opensubtitles.com/consumers.
 * Set the OS_API_KEY environment variable before running the app.
 *
 * Mirrors CS3's OpenSubtitlesApi (syncproviders/providers/OpenSubtitlesApi.kt).
 */
class OpenSubtitlesApi(
    private val apiKey: String = OPENSUBTITLES_DEFAULT_KEY,
) {
    private val log = LoggerFactory.getLogger(OpenSubtitlesApi::class.java)
    private val json = Json { ignoreUnknownKeys = true }

    private val client = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .build()

    @Serializable
    private data class SearchResponse(val data: List<SubtitleEntry>? = null)

    @Serializable
    private data class SubtitleEntry(
        val id: String? = null,
        val attributes: SubtitleAttributes? = null,
    )

    @Serializable
    private data class SubtitleAttributes(
        val language: String? = null,
        val files: List<SubtitleFileEntry>? = null,
        val release: String? = null,
        val ratings: Double? = null,
    )

    @Serializable
    private data class SubtitleFileEntry(
        val file_id: Long? = null,
        val file_name: String? = null,
    )

    @Serializable
    private data class DownloadResponse(val link: String? = null)

    /**
     * Search for subtitles by query (movie/show name).
     * Returns a list of [SubtitleFile]s with the subtitle URL ready to pass to vlcj.
     */
    suspend fun search(query: String, language: String = "en"): List<SubtitleFile> = withContext(Dispatchers.IO) {
        if (apiKey.isBlank()) {
            log.warn("OpenSubtitles API key not set (set OS_API_KEY env var) — skipping subtitle search")
            return@withContext emptyList()
        }

        try {
            val req = Request.Builder()
                .url("https://api.opensubtitles.com/api/v1/subtitles?query=${java.net.URLEncoder.encode(query, "UTF-8")}&languages=$language")
                .header("Api-Key", apiKey)
                .header("User-Agent", "CloudStream-Desktop")
                .header("Content-Type", "application/json")
                .get()
                .build()

            client.newCall(req).execute().use { resp ->
                if (!resp.isSuccessful) {
                    log.warn("OpenSubtitles search HTTP ${resp.code}")
                    return@withContext emptyList()
                }

                val body = resp.body?.string() ?: return@withContext emptyList()
                val search = json.decodeFromString(SearchResponse.serializer(), body)
                val results = mutableListOf<SubtitleFile>()

                for (entry in search.data ?: emptyList()) {
                    val attrs = entry.attributes ?: continue
                    val lang = attrs.language ?: "en"
                    val fileId = attrs.files?.firstOrNull()?.file_id ?: continue

                    // Request a download link for this subtitle
                    val dlUrl = requestDownloadLink(fileId) ?: continue

                    results.add(SubtitleFile(lang = lang, url = dlUrl))
                }

                log.info("OpenSubtitles: found ${results.size} subtitles for '$query'")
                results
            }
        } catch (e: Exception) {
            log.error("OpenSubtitles search failed", e)
            emptyList()
        }
    }

    /**
     * Request a download link for a specific subtitle file.
     * OpenSubtitles requires a POST to /api/v1/download with the file_id.
     */
    private fun requestDownloadLink(fileId: Long): String? {
        return try {
            val reqBody = """{"file_id":$fileId}""".toRequestBody("application/json".toMediaType())
            val req = Request.Builder()
                .url("https://api.opensubtitles.com/api/v1/download")
                .header("Api-Key", apiKey)
                .header("User-Agent", "CloudStream-Desktop")
                .header("Content-Type", "application/json")
                .post(reqBody)
                .build()

            client.newCall(req).execute().use { resp ->
                if (!resp.isSuccessful) return null
                val body = resp.body?.string() ?: return null
                val dlResp = json.decodeFromString(DownloadResponse.serializer(), body)
                dlResp.link
            }
        } catch (e: Exception) {
            log.debug("OpenSubtitles download link request failed for file $fileId", e)
            null
        }
    }

    companion object {
        /**
         * Default API key — set via OS_API_KEY environment variable.
         * Get a free key at https://www.opensubtitles.com/consumers
         */
        val OPENSUBTITLES_DEFAULT_KEY: String = System.getenv("OS_API_KEY") ?: ""
    }
}

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
 * AniSkip API client — fetches skip timestamps (intro/outro) for anime.
 *
 * Uses the AniSkip API (https://api.aniskip.com) which maps MAL IDs to
 * skip timestamps submitted by the community.
 *
 * Mirrors CS3's AniSkip (utils/videoskip/AniSkip.kt).
 */
object AniSkipApi {
    private val log = LoggerFactory.getLogger(AniSkipApi::class.java)
    private val json = Json { ignoreUnknownKeys = true }

    private val client = OkHttpClient.Builder()
        .connectTimeout(10, TimeUnit.SECONDS)
        .readTimeout(15, TimeUnit.SECONDS)
        .build()

    @Serializable
    data class SkipStamp(
        val interval: SkipInterval,
        val skipType: String, // "op", "ed", "recap", "mixed-op", "mixed-ed"
        val skipName: String,
    )

    @Serializable
    data class SkipInterval(
        val startTime: Double, // seconds
        val endTime: Double,   // seconds
    )

    @Serializable
    data class AniSkipResponse(
        val found: Boolean = false,
        val results: List<AniSkipResult> = emptyList(),
    )

    @Serializable
    data class AniSkipResult(
        val interval: SkipInterval,
        val skipType: String,
        val skipId: String,
        val episodeLength: Double,
        val skipName: String? = null,
    )

    /**
     * Get skip timestamps for an anime episode.
     *
     * @param malId MAL anime ID
     * @param episodeNumber episode number
     * @param episodeDurationMs episode duration in milliseconds (for filtering)
     */
    suspend fun getSkipStamps(malId: Int, episodeNumber: Int, episodeDurationMs: Long = 0): List<SkipStamp> = withContext(Dispatchers.IO) {
        try {
            val url = "https://api.aniskip.com/v2/skip-times/$malId/${episodeNumber}?types[]=op&types[]=ed&types[]=recap&types[]=mixed-op&types[]=mixed-ed&episodeLength=${episodeDurationMs / 1000}"
            val req = Request.Builder()
                .url(url)
                .header("User-Agent", "CloudStream-Desktop")
                .get()
                .build()

            client.newCall(req).execute().use { resp ->
                if (!resp.isSuccessful) {
                    log.debug("AniSkip API HTTP ${resp.code} for malId=$malId ep=$episodeNumber")
                    return@withContext emptyList()
                }
                val body = resp.body?.string() ?: return@withContext emptyList()
                val result = json.decodeFromString(AniSkipResponse.serializer(), body)
                if (!result.found) return@withContext emptyList()

                result.results.map {
                    SkipStamp(
                        interval = it.interval,
                        skipType = it.skipType,
                        skipName = it.skipName ?: it.skipType.uppercase(),
                    )
                }
            }
        } catch (e: Exception) {
            log.debug("AniSkip API failed for malId=$malId ep=$episodeNumber: ${e.message}")
            emptyList()
        }
    }
}

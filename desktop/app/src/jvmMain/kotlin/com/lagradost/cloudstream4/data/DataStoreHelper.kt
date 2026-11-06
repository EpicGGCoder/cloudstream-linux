package com.lagradost.cloudstream4.data

import com.lagradost.cloudstream3.SearchResponse
import com.lagradost.cloudstream3.TvType
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import org.slf4j.LoggerFactory
import java.io.File

/**
 * DataStoreHelper — mirrors CS3's DataStoreHelper.kt.
 *
 * Persists:
 * - Bookmarks (saved titles)
 * - Favorites
 * - Resume watching (last position per media)
 * - Watch history (recently viewed)
 *
 * All stored as JSON files under ~/.cloudstream/data/
 */
class DataStoreHelper(private val dataDir: File) {
    private val log = LoggerFactory.getLogger(DataStoreHelper::class.java)
    private val json = Json { ignoreUnknownKeys = true; prettyPrint = true; encodeDefaults = true }
    private val dataSubDir = File(dataDir, "data").apply { mkdirs() }

    // ------------------------------------------------------------------
    // Data classes — mirror CS3's BookmarkedData / FavoritesData / ResumeWatchingResult
    // ------------------------------------------------------------------

    @Serializable
    data class StoredMedia(
        val name: String,
        val url: String,
        val apiName: String,
        val type: String = "Movie",
        val posterUrl: String? = null,
        val year: Int? = null,
        val plot: String? = null,
        val addedAt: Long = System.currentTimeMillis(),
    )

    @Serializable
    data class WatchProgress(
        val url: String,
        val apiName: String,
        val position: Long = 0,
        val duration: Long = 0,
        val updatedAt: Long = System.currentTimeMillis(),
        val name: String,
        val posterUrl: String? = null,
        val episodeData: String? = null,
        val episodeName: String? = null,
        val season: Int? = null,
        val episode: Int? = null,
    )

    @Serializable
    data class LibraryStore(
        val bookmarks: List<StoredMedia> = emptyList(),
        val favorites: List<StoredMedia> = emptyList(),
        val resumeWatching: List<WatchProgress> = emptyList(),
        val history: List<StoredMedia> = emptyList(),
    )

    // ------------------------------------------------------------------
    // State — observable by Compose
    // ------------------------------------------------------------------

    private val _library = MutableStateFlow(LibraryStore())
    val library: StateFlow<LibraryStore> = _library.asStateFlow()

    init { load() }

    private fun load() {
        try {
            val file = File(dataSubDir, "library.json")
            if (file.exists()) {
                _library.value = json.decodeFromString(LibraryStore.serializer(), file.readText())
                log.info("Library loaded: ${_library.value.bookmarks.size} bookmarks, ${_library.value.favorites.size} favorites, ${_library.value.resumeWatching.size} resume")
            }
        } catch (e: Exception) {
            log.error("Failed to load library", e)
        }
    }

    private fun save() {
        try {
            File(dataSubDir, "library.json").writeText(
                json.encodeToString(LibraryStore.serializer(), _library.value)
            )
        } catch (e: Exception) {
            log.error("Failed to save library", e)
        }
    }

    // ------------------------------------------------------------------
    // Bookmarks
    // ------------------------------------------------------------------

    fun toggleBookmark(item: SearchResponse) {
        val current = _library.value
        val media = StoredMedia(
            name = item.name,
            url = item.url,
            apiName = item.apiName,
            type = item.type?.name ?: "Movie",
            posterUrl = item.posterUrl,
            year = extractYear(item),
        )
        val exists = current.bookmarks.any { it.url == item.url && it.apiName == item.apiName }
        _library.value = current.copy(
            bookmarks = if (exists) current.bookmarks.filterNot { it.url == item.url || it.apiName == item.apiName }
                         else current.bookmarks + media
        )
        save()
    }

    fun isBookmarked(url: String, apiName: String): Boolean =
        _library.value.bookmarks.any { it.url == url && it.apiName == apiName }

    // ------------------------------------------------------------------
    // Favorites
    // ------------------------------------------------------------------

    fun toggleFavorite(item: SearchResponse) {
        val current = _library.value
        val media = StoredMedia(
            name = item.name,
            url = item.url,
            apiName = item.apiName,
            type = item.type?.name ?: "Movie",
            posterUrl = item.posterUrl,
            year = extractYear(item),
        )
        val exists = current.favorites.any { it.url == item.url && it.apiName == item.apiName }
        _library.value = current.copy(
            favorites = if (exists) current.favorites.filterNot { it.url == item.url || it.apiName == item.apiName }
                        else current.favorites + media
        )
        save()
    }

    fun isFavorite(url: String, apiName: String): Boolean =
        _library.value.favorites.any { it.url == url && it.apiName == apiName }

    // ------------------------------------------------------------------
    // Resume watching — called when playback starts/progresses
    // ------------------------------------------------------------------

    fun updateWatchProgress(
        url: String,
        apiName: String,
        name: String,
        posterUrl: String?,
        position: Long,
        duration: Long,
        episodeData: String? = null,
        episodeName: String? = null,
        season: Int? = null,
        episode: Int? = null,
    ) {
        val current = _library.value
        val progress = WatchProgress(
            url = url, apiName = apiName, name = name, posterUrl = posterUrl,
            position = position, duration = duration,
            episodeData = episodeData, episodeName = episodeName,
            season = season, episode = episode,
        )
        val existing = current.resumeWatching.filterNot { it.url == url }
        _library.value = current.copy(
            resumeWatching = (listOf(progress) + existing).take(50)
        )
        save()
    }

    fun clearWatchProgress(url: String) {
        val current = _library.value
        _library.value = current.copy(
            resumeWatching = current.resumeWatching.filterNot { it.url == url }
        )
        save()
    }

    // ------------------------------------------------------------------
    // History — called when a detail page is opened
    // ------------------------------------------------------------------

    fun addToHistory(item: SearchResponse) {
        val current = _library.value
        val media = StoredMedia(
            name = item.name,
            url = item.url,
            apiName = item.apiName,
            type = item.type?.name ?: "Movie",
            posterUrl = item.posterUrl,
            year = extractYear(item),
        )
        val filtered = current.history.filterNot { it.url == item.url || it.apiName == item.apiName }
        _library.value = current.copy(history = (listOf(media) + filtered).take(50))
        save()
    }

    // ------------------------------------------------------------------
    // Helpers
    // ------------------------------------------------------------------

    private fun extractYear(item: SearchResponse): Int? {
        // Try reflection — SearchResponse subtypes have `year` field
        return try {
            val field = item::class.java.getDeclaredField("year")
            field.isAccessible = true
            (field.get(item) as? Int)
        } catch (e: Exception) {
            null
        }
    }

    fun StoredMedia.toSearchResponse(): SearchResponse {
        return object : SearchResponse {
            override val name = this@toSearchResponse.name
            override val url = this@toSearchResponse.url
            override val apiName = this@toSearchResponse.apiName
            override var type: TvType? = runCatching { TvType.valueOf(this@toSearchResponse.type) }.getOrNull()
            override var posterUrl: String? = this@toSearchResponse.posterUrl
            override var posterHeaders: Map<String, String>? = null
            override var id: Int? = null
            override var quality: com.lagradost.cloudstream3.SearchQuality? = null
            override var score: com.lagradost.cloudstream3.Score? = null
        }
    }
}

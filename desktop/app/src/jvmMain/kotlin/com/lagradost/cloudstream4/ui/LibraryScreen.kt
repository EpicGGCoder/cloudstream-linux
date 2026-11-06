package com.lagradost.cloudstream4.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.lagradost.cloudstream3.SearchResponse
import com.lagradost.cloudstream4.data.DataStoreHelper

/**
 * Library screen — shows bookmarks, favorites, continue watching, and history.
 *
 * Mirrors CS3's LibraryFragment with its ViewPager of bookmarked / favorite /
 * subscribed / watching / completed lists.
 */
@Composable
fun LibraryScreen(
    dataStore: DataStoreHelper,
    onOpenDetail: (SearchResponse) -> Unit,
) {
    val library by dataStore.library.collectAsState()

    if (library.bookmarks.isEmpty() && library.favorites.isEmpty() &&
        library.resumeWatching.isEmpty() && library.history.isEmpty()
    ) {
        EmptyState(
            title = "Your library is empty",
            body = "Bookmark titles from the detail page to see them here. " +
                "Your continue-watching, favorites, and history will also appear.",
        )
        return
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(vertical = 16.dp),
    ) {
        // Continue watching
        if (library.resumeWatching.isNotEmpty()) {
            item {
                Text(
                    "Continue Watching",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.padding(start = 16.dp, bottom = 8.dp),
                )
            }
            item {
                LazyRow(
                    contentPadding = PaddingValues(horizontal = 16.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    items(library.resumeWatching) { progress ->
                        Column(modifier = Modifier.width(160.dp).clickable {
                            // Build a SearchResponse from the progress entry
                            val resp = object : SearchResponse {
                                override val name = progress.name
                                override val url = progress.url
                                override val apiName = progress.apiName
                                override var type: com.lagradost.cloudstream3.TvType? = null
                                override var posterUrl: String? = progress.posterUrl
                                override var posterHeaders: Map<String, String>? = null
                                override var id: Int? = null
                                override var quality: com.lagradost.cloudstream3.SearchQuality? = null
                                override var score: com.lagradost.cloudstream3.Score? = null
                            }
                            onOpenDetail(resp)
                        }) {
                            // Progress indicator bar
                            val pct = if (progress.duration > 0)
                                (progress.position.toFloat() / progress.duration.toFloat()).coerceIn(0f, 1f)
                            else 0f
                            PosterProgressCard(
                                name = progress.name,
                                posterUrl = progress.posterUrl,
                                progress = pct,
                                episodeLabel = buildString {
                                    progress.season?.let { append("S${it}") }
                                    progress.episode?.let { append("E${it}") }
                                    progress.episodeName?.let { if (isNotEmpty()) { append(" "); append(it) } }
                                }.ifEmpty { null },
                            )
                        }
                    }
                }
            }
        }

        // Favorites
        if (library.favorites.isNotEmpty()) {
            item {
                Spacer(Modifier.height(16.dp))
                Text(
                    "Favorites (${library.favorites.size})",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.padding(start = 16.dp, bottom = 8.dp),
                )
            }
            item {
                LazyRow(
                    contentPadding = PaddingValues(horizontal = 16.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    items(library.favorites) { media ->
                        val resp = dataStore.run { media.toSearchResponse() }
                        PosterCard(item = resp, onClick = { onOpenDetail(resp) })
                    }
                }
            }
        }

        // Bookmarks
        if (library.bookmarks.isNotEmpty()) {
            item {
                Spacer(Modifier.height(16.dp))
                Text(
                    "Bookmarks (${library.bookmarks.size})",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.padding(start = 16.dp, bottom = 8.dp),
                )
            }
            item {
                LazyRow(
                    contentPadding = PaddingValues(horizontal = 16.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    items(library.bookmarks) { media ->
                        val resp = dataStore.run { media.toSearchResponse() }
                        PosterCard(item = resp, onClick = { onOpenDetail(resp) })
                    }
                }
            }
        }

        // Recently viewed
        if (library.history.isNotEmpty()) {
            item {
                Spacer(Modifier.height(16.dp))
                Text(
                    "Recently Viewed (${library.history.size})",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.padding(start = 16.dp, bottom = 8.dp),
                )
            }
            item {
                LazyRow(
                    contentPadding = PaddingValues(horizontal = 16.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    items(library.history) { media ->
                        val resp = dataStore.run { media.toSearchResponse() }
                        PosterCard(item = resp, onClick = { onOpenDetail(resp) })
                    }
                }
            }
        }
    }
}

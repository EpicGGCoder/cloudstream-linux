package com.lagradost.cloudstream4.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.Movie
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Public
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil3.compose.AsyncImage
import com.lagradost.cloudstream3.APIHolder
import com.lagradost.cloudstream3.ActorData
import com.lagradost.cloudstream3.Episode
import com.lagradost.cloudstream3.LoadResponse
import com.lagradost.cloudstream3.MainAPI
import com.lagradost.cloudstream3.MovieLoadResponse
import com.lagradost.cloudstream3.SearchResponse
import com.lagradost.cloudstream3.TvSeriesLoadResponse
import com.lagradost.cloudstream3.TvType
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.slf4j.LoggerFactory
import java.awt.Desktop
import java.net.URI

private val log = LoggerFactory.getLogger("DetailScreen")

/**
 * Full-screen detail overlay. Shows everything the load() call returns:
 * backdrop, plot, cast, episodes (for series), recommendations, trailers.
 *
 * Click "Play" on a movie → opens [PlayerScreen] with [loadMovieData] = dataUrl.
 * Click an episode → opens [PlayerScreen] with the episode's data string.
 */
@Composable
fun DetailScreen(
    item: SearchResponse,
    onBack: () -> Unit,
    onPlay: (MainAPI, String, String) -> Unit, // (api, data, title) -> open player
) {
    val scope = rememberCoroutineScope()
    var loading by remember { mutableStateOf(true) }
    var loaded by remember { mutableStateOf<LoadResponse?>(null) }
    var api by remember { mutableStateOf<MainAPI?>(null) }
    var error by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(item.url, item.apiName) {
        loading = true
        error = null
        loaded = null
        try {
            val resolvedApi = withContext(Dispatchers.IO) {
                APIHolder.getApiFromNameNull(item.apiName)
            }
            if (resolvedApi == null) {
                error = "Provider '${item.apiName}' is not loaded."
                loading = false
                return@LaunchedEffect
            }
            api = resolvedApi
            val resp = withContext(Dispatchers.IO) { resolvedApi.load(item.url) }
            if (resp == null) {
                error = "Provider returned no data."
            } else {
                loaded = resp
            }
        } catch (e: Exception) {
            log.error("Detail load failed for ${item.url}", e)
            error = e.message ?: e::class.simpleName
        }
        loading = false
    }

    Surface(
        color = MaterialTheme.colorScheme.background,
        modifier = Modifier.fillMaxSize(),
    ) {
        when {
            loading -> DetailSkeleton(onBack = onBack)
            error != null -> ErrorState(
                title = "Failed to load details",
                body = error!!,
                onRetry = onBack,
            )
            loaded != null && api != null -> DetailContent(
                searchItem = item,
                loaded = loaded!!,
                api = api!!,
                onBack = onBack,
                onPlay = onPlay,
                onOpenDetail = { /* could push another detail screen */ },
            )
            else -> EmptyState(
                title = "Nothing to show",
                body = "This title returned no metadata.",
            )
        }
    }
}

@Composable
private fun DetailContent(
    searchItem: SearchResponse,
    loaded: LoadResponse,
    api: MainAPI,
    onBack: () -> Unit,
    onPlay: (MainAPI, String, String) -> Unit,
    onOpenDetail: (SearchResponse) -> Unit,
) {
    val state = rememberLazyListState()
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        state = state,
        contentPadding = PaddingValues(bottom = 80.dp),
    ) {
        item(key = "hero") {
            HeroSection(
                searchItem = searchItem,
                loaded = loaded,
                onBack = onBack,
                onPlay = { playData, playTitle ->
                    onPlay(api, playData, playTitle)
                },
            )
        }
        loaded.plot?.let { plot ->
            item(key = "overview") {
                SectionHeader(title = "Overview")
                Text(
                    text = plot,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 24.dp, vertical = 4.dp),
                )
            }
        }
        if (!loaded.actors.isNullOrEmpty()) {
            val actors = loaded.actors!!
            item(key = "cast") {
                SectionHeader(title = "Cast", subtitle = "${actors.size} actors")
                CastRow(actors = actors)
            }
        }
        if (loaded is TvSeriesLoadResponse) {
            item(key = "episodes-header") {
                SectionHeader(title = "Episodes", subtitle = "${loaded.episodes.size} total")
            }
            items(loaded.episodes.groupedBySeason().let { groups ->
                groups.flatMap { (season, eps) -> listOf(season to eps) }
            }, key = { it.first.toString() + "-group" }) { (season, eps) ->
                SeasonGroup(season = season, episodes = eps) { episode ->
                    val title = episode.name?.let { "${searchItem.name} S${season ?: 0}E${episode.episode ?: 0}: $it" }
                        ?: "${searchItem.name} S${season ?: 0}E${episode.episode ?: 0}"
                    onPlay(api, episode.data, title)
                }
            }
        }
        if (!loaded.recommendations.isNullOrEmpty()) {
            val recs = loaded.recommendations!!
            item(key = "recommendations") {
                SectionHeader(title = "More like this", subtitle = "${recs.size} titles")
                LazyRow(
                    contentPadding = PaddingValues(horizontal = 24.dp),
                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                    modifier = Modifier.padding(bottom = 16.dp),
                ) {
                    items(recs, key = { it.url + it.apiName }) { rec ->
                        PosterCard(item = rec, onClick = { onOpenDetail(rec) })
                    }
                }
            }
        }
        if (loaded.trailers.isNotEmpty()) {
            item(key = "trailers") {
                SectionHeader(title = "Trailers")
                LazyRow(
                    contentPadding = PaddingValues(horizontal = 24.dp),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    items(loaded.trailers.withIndex().toList()) { (i, trailer) ->
                        OutlinedButton(onClick = {
                            runCatching {
                                if (Desktop.isDesktopSupported()) {
                                    Desktop.getDesktop().browse(URI.create(trailer.extractorUrl))
                                }
                            }
                        }) {
                            Icon(Icons.Filled.Public, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(Modifier.width(6.dp))
                            Text("Trailer ${i + 1}")
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun HeroSection(
    searchItem: SearchResponse,
    loaded: LoadResponse,
    onBack: () -> Unit,
    onPlay: (data: String, title: String) -> Unit,
) {
    val backdrop = loaded.backgroundPosterUrl ?: loaded.posterUrl ?: searchItem.posterUrl
    val title = loaded.name.ifBlank { searchItem.name }
    Box(
        Modifier
            .fillMaxWidth()
            .height(460.dp),
    ) {
        if (backdrop != null) {
            AsyncImage(
                model = backdrop,
                contentDescription = title,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize(),
            )
        } else {
            Box(
                Modifier
                    .fillMaxSize()
                    .background(
                        Brush.linearGradient(
                            listOf(
                                MaterialTheme.colorScheme.primaryContainer,
                                MaterialTheme.colorScheme.surfaceVariant,
                            ),
                        ),
                    ),
            )
        }
        Box(
            Modifier
                .fillMaxSize()
                .background(bottomFadeOverlay(MaterialTheme.colorScheme.background)),
        )
        // Back button.
        Box(
            Modifier
                .align(Alignment.TopStart)
                .padding(16.dp),
        ) {
            IconButton(
                onClick = onBack,
                modifier = Modifier
                    .clip(CircleShape)
                    .background(Color.Black.copy(alpha = 0.5f))
                    .size(40.dp),
            ) {
                Icon(
                    Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = "Back",
                    tint = Color.White,
                )
            }
        }
        // Bottom panel.
        Column(
            Modifier
                .align(Alignment.BottomStart)
                .fillMaxWidth()
                .padding(start = 32.dp, end = 32.dp, bottom = 24.dp),
        ) {
            // Logo overrides title when available.
            val logo = loaded.logoUrl
            if (!logo.isNullOrBlank()) {
                AsyncImage(
                    model = logo,
                    contentDescription = title,
                    modifier = Modifier
                        .height(70.dp)
                        .padding(bottom = 6.dp),
                )
            } else {
                Text(
                    text = title,
                    style = MaterialTheme.typography.headlineMedium.copy(
                        fontWeight = FontWeight.Bold,
                        letterSpacing = (-0.5).sp,
                    ),
                    color = Color.White,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            Spacer(Modifier.height(8.dp))
            // Badges.
            Row(verticalAlignment = Alignment.CenterVertically) {
                Chip(text = loaded.type.displayLabel())
                Spacer(Modifier.width(6.dp))
                loaded.year?.let {
                    Chip(text = it.toString())
                    Spacer(Modifier.width(6.dp))
                }
                loaded.score?.displayString()?.let { score ->
                    Chip(text = "$score / 10", content = MaterialTheme.colorScheme.primary)
                    Spacer(Modifier.width(6.dp))
                }
                loaded.duration?.let { mins ->
                    Chip(text = "${mins}m")
                    Spacer(Modifier.width(6.dp))
                }
                if (loaded is TvSeriesLoadResponse) {
                    loaded.showStatus?.name?.let {
                        Chip(text = it)
                    }
                }
            }
            // Tags as chips.
            if (!loaded.tags.isNullOrEmpty()) {
                val tags = loaded.tags!!
                Spacer(Modifier.height(8.dp))
                LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    items(tags.take(8)) { tag ->
                        Chip(
                            text = tag,
                            container = MaterialTheme.colorScheme.primary.copy(alpha = 0.15f),
                            content = MaterialTheme.colorScheme.primary,
                        )
                    }
                }
            }
            Spacer(Modifier.height(14.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                // Movie → Play button. Series → scroll to episodes.
                if (loaded is MovieLoadResponse) {
                    androidx.compose.material3.Button(
                        onClick = { onPlay(loaded.dataUrl, loaded.name) },
                    ) {
                        Icon(Icons.Filled.PlayArrow, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(6.dp))
                        Text("Play")
                    }
                } else if (loaded is TvSeriesLoadResponse && loaded.episodes.isNotEmpty()) {
                    androidx.compose.material3.Button(
                        onClick = { onPlay(loaded.episodes.first().data, "${loaded.name} EP1") },
                    ) {
                        Icon(Icons.Filled.PlayArrow, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(6.dp))
                        Text("Play First Episode")
                    }
                }
                if (loaded is MovieLoadResponse && loaded.comingSoon) {
                    Chip(text = "Coming soon", container = MaterialTheme.colorScheme.errorContainer)
                }
            }
        }
    }
}

@Composable
private fun CastRow(actors: List<ActorData>) {
    LazyRow(
        contentPadding = PaddingValues(horizontal = 24.dp),
        horizontalArrangement = Arrangement.spacedBy(14.dp),
        modifier = Modifier.padding(bottom = 16.dp),
    ) {
        items(actors.take(20), key = { it.actor.name + (it.actor.image ?: "") }) { actorData ->
            Column(
                Modifier.width(96.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Box(
                    Modifier
                        .size(80.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.surfaceVariant),
                ) {
                    val img = actorData.actor.image
                    if (!img.isNullOrBlank()) {
                        AsyncImage(
                            model = img,
                            contentDescription = actorData.actor.name,
                            contentScale = ContentScale.Crop,
                            modifier = Modifier.fillMaxSize(),
                        )
                    } else {
                        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            Icon(
                                Icons.Filled.Movie,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }
                }
                Spacer(Modifier.height(6.dp))
                Text(
                    text = actorData.actor.name,
                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.SemiBold),
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
                actorData.roleString?.let { role ->
                    Text(
                        text = role,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }
        }
    }
}

@Composable
private fun SeasonGroup(
    season: Int?,
    episodes: List<Episode>,
    onPlay: (Episode) -> Unit,
) {
    var expanded by remember(season) { mutableStateOf(true) }
    Column(Modifier.padding(horizontal = 24.dp, vertical = 6.dp)) {
        Row(
            Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(10.dp))
                .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.7f))
                .clickable { expanded = !expanded }
                .padding(horizontal = 14.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = if (season != null) "Season $season" else "Specials",
                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.SemiBold),
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.weight(1f),
            )
            Text(
                text = "${episodes.size} eps",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(Modifier.width(6.dp))
            Icon(
                if (expanded) Icons.Filled.ExpandLess else Icons.Filled.ExpandMore,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        if (expanded) {
            Spacer(Modifier.height(4.dp))
            episodes.forEach { ep ->
                EpisodeRow(episode = ep) { onPlay(ep) }
            }
        }
    }
}

@Composable
private fun EpisodeRow(
    episode: Episode,
    onClick: () -> Unit,
) {
    Row(
        Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        // Thumbnail.
        Box(
            Modifier
                .size(width = 120.dp, height = 68.dp)
                .clip(RoundedCornerShape(6.dp))
                .background(MaterialTheme.colorScheme.surfaceVariant),
        ) {
            val posterUrl = episode.posterUrl
            if (!posterUrl.isNullOrBlank()) {
                AsyncImage(
                    model = posterUrl,
                    contentDescription = episode.name,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize(),
                )
            } else {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text(
                        text = "EP${episode.episode ?: 0}",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = "EP${episode.episode ?: 0}",
                    style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.primary,
                )
                Spacer(Modifier.width(8.dp))
                Text(
                    text = episode.name ?: "Untitled",
                    style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f),
                )
            }
            if (!episode.description.isNullOrBlank()) {
                val description = episode.description!!
                Text(
                    text = description,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.padding(top = 2.dp),
                )
            }
            val meta = buildList {
                episode.date?.let { add(formatEpoch(it)) }
                episode.runTime?.let { add("${it}m") }
            }.joinToString(" • ")
            if (meta.isNotEmpty()) {
                Text(
                    text = meta,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 2.dp),
                )
            }
        }
        Icon(
            Icons.AutoMirrored.Filled.KeyboardArrowRight,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun DetailSkeleton(onBack: () -> Unit) {
    Box(Modifier.fillMaxSize()) {
        Box(
            Modifier
                .align(Alignment.TopStart)
                .padding(16.dp)
                .clip(CircleShape)
                .background(Color.Black.copy(alpha = 0.5f))
                .size(40.dp)
                .clickable(onClick = onBack),
        ) {
            Icon(
                Icons.AutoMirrored.Filled.ArrowBack,
                contentDescription = "Back",
                tint = Color.White,
                modifier = Modifier.align(Alignment.Center),
            )
        }
        Column(Modifier.fillMaxSize()) {
            SkeletonBox(Modifier.fillMaxWidth().height(460.dp), cornerRadius = 0)
            Spacer(Modifier.height(20.dp))
            SkeletonBox(Modifier.padding(horizontal = 24.dp).fillMaxWidth().height(20.dp))
            Spacer(Modifier.height(8.dp))
            SkeletonBox(Modifier.padding(horizontal = 24.dp).fillMaxWidth(0.7f).height(14.dp))
            Spacer(Modifier.height(20.dp))
            SkeletonBox(Modifier.padding(horizontal = 24.dp).fillMaxWidth(0.6f).height(14.dp))
        }
    }
}

private fun List<Episode>.groupedBySeason(): Map<Int?, List<Episode>> {
    val map = LinkedHashMap<Int?, MutableList<Episode>>()
    for (ep in this) {
        val key = ep.season
        val list = map.getOrPut(key) { mutableListOf() }
        list.add(ep)
    }
    // Sort within season by episode number ascending.
    return map.mapValues { (_, list) ->
        list.sortedWith(compareBy({ it.episode ?: Int.MAX_VALUE }, { it.name ?: "" }))
    }
}

private fun formatEpoch(epochMs: Long): String {
    return try {
        val cal = java.util.Calendar.getInstance().apply { timeInMillis = epochMs }
        "%04d-%02d-%02d".format(
            cal.get(java.util.Calendar.YEAR),
            cal.get(java.util.Calendar.MONTH) + 1,
            cal.get(java.util.Calendar.DAY_OF_MONTH),
        )
    } catch (_: Throwable) {
        ""
    }
}

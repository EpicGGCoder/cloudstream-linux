package com.lagradost.cloudstream4.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.AudioFile
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Error
import androidx.compose.material.icons.filled.HighQuality
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Subtitles
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.awt.SwingPanel
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.lagradost.cloudstream3.MainAPI
import com.lagradost.cloudstream3.SubtitleFile
import com.lagradost.cloudstream3.utils.ExtractorLink
import com.lagradost.cloudstream3.utils.ExtractorLinkType
import com.lagradost.cloudstream4.data.DataStoreHelper
import com.lagradost.cloudstream4.player.VlcjPlayer
import com.lagradost.cloudstream4.subtitles.OpenSubtitlesApi
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.slf4j.LoggerFactory
import java.awt.Component

private val log = LoggerFactory.getLogger("PlayerScreen")

/**
 * Full-screen vlcj-backed player overlay.
 *
 * Flow:
 *   1. Show loading spinner while we call [MainAPI.loadLinks] for [data].
 *   2. Collect [ExtractorLink]s and [SubtitleFile]s via the callbacks.
 *   3. Pick the highest-quality non-torrent link and play it via vlcj.
 *   4. Auto-hide title + bottom controls after 3s of no mouse activity.
 *   5. Source/subtitle pickers swap the playback live.
 */
@Composable
fun PlayerScreen(
    api: MainAPI,
    data: String,
    title: String,
    dataStore: DataStoreHelper? = null,
    onClose: () -> Unit,
) {
    val scope = rememberCoroutineScope()
    val player = remember { VlcjPlayer(scope) }

    // Link/subs are populated by the loadLinks callbacks.
    var links by remember { mutableStateOf<List<ExtractorLink>>(emptyList()) }
    var subtitles by remember { mutableStateOf<List<SubtitleFile>>(emptyList()) }
    var loadState by remember { mutableStateOf(LoadState.LOADING) }
    var errorMsg by remember { mutableStateOf<String?>(null) }
    var currentLink by remember { mutableStateOf<ExtractorLink?>(null) }
    var currentSubtitleIndex by remember { mutableStateOf(-1) }

    LaunchedEffect(data, api.name) {
        loadState = LoadState.LOADING
        errorMsg = null
        links = emptyList()
        subtitles = emptyList()
        currentLink = null
        currentSubtitleIndex = -1
        try {
            val collectedLinks = mutableListOf<ExtractorLink>()
            val collectedSubs = mutableListOf<SubtitleFile>()
            val ok = withContext(Dispatchers.IO) {
                api.loadLinks(
                    data = data,
                    isCasting = false,
                    subtitleCallback = { sub -> collectedSubs.add(sub) },
                    callback = { link -> collectedLinks.add(link) },
                )
            }
            // Some meta-providers (TMDB) return ok=false and no links.
            if (!ok && collectedLinks.isEmpty()) {
                loadState = LoadState.NO_SOURCES
                return@LaunchedEffect
            }
            if (collectedLinks.isEmpty()) {
                loadState = LoadState.NO_SOURCES
                return@LaunchedEffect
            }
            // Filter out torrents (libvlc can't stream them) and pick best quality.
            val playable = collectedLinks
                .filter { it.type != ExtractorLinkType.TORRENT && it.type != ExtractorLinkType.MAGNET }
                .sortedByDescending { it.quality }
            if (playable.isEmpty()) {
                loadState = LoadState.NO_SOURCES
                return@LaunchedEffect
            }
            links = playable
            subtitles = collectedSubs
            currentLink = playable.first()
            loadState = LoadState.PLAYING
            player.load(playable.first(), title, collectedSubs)
        } catch (e: Exception) {
            log.error("loadLinks failed", e)
            errorMsg = e.message ?: e::class.simpleName
            loadState = LoadState.ERROR
        }
    }

    DisposableEffect(Unit) {
        onDispose {
            player.release()
        }
    }

    val playerState by player.state.collectAsState()

    // Save watch progress every 5 seconds while playing (for "Continue Watching").
    if (dataStore != null) {
        LaunchedEffect(playerState.positionMs, playerState.durationMs, playerState.isPlaying) {
            if (playerState.isPlaying && playerState.durationMs > 0) {
                dataStore.updateWatchProgress(
                    url = data,
                    apiName = api.name,
                    name = title,
                    posterUrl = null,
                    position = playerState.positionMs,
                    duration = playerState.durationMs,
                )
            }
        }
    }

    // Update loadState from vlcj events.
    LaunchedEffect(playerState.hasError) {
        if (playerState.hasError && loadState == LoadState.PLAYING) {
            loadState = LoadState.ERROR
            errorMsg = "VLC reported an error playing this source."
        }
    }

    Surface(color = Color.Black, modifier = Modifier.fillMaxSize()) {
        // Use Column layout — SwingPanel renders ON TOP of Compose content,
        // so we MUST put controls in separate areas NOT covered by the video.
        Column(Modifier.fillMaxSize()) {
            // === TOP BAR — title + close button (not behind video) ===
            Row(
                Modifier
                    .fillMaxWidth()
                    .background(Color(0xFF111111))
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                IconButton(
                    onClick = onClose,
                    modifier = Modifier.size(44.dp),
                ) {
                    Icon(Icons.Filled.Close, contentDescription = "Close", tint = Color.White, modifier = Modifier.size(28.dp))
                }
                Spacer(Modifier.width(8.dp))
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
                    color = Color.White,
                    maxLines = 1,
                    overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f),
                )
            }

            // === VIDEO AREA — fills available space ===
            Box(
                Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .background(Color.Black),
                contentAlignment = Alignment.Center,
            ) {
                if (loadState == LoadState.PLAYING || loadState == LoadState.LOADING) {
                    SwingPanel(
                        factory = { player.surface },
                        modifier = Modifier.fillMaxSize(),
                        update = { /* no-op */ },
                    )
                }

                // Loading overlay — shows on top of video area
                if (loadState == LoadState.LOADING || (playerState.isLoading && loadState == LoadState.PLAYING)) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
                        Spacer(Modifier.height(8.dp))
                        Text(
                            text = if (loadState == LoadState.LOADING) "Loading sources…" else "Buffering…",
                            color = Color.White,
                            style = MaterialTheme.typography.bodyMedium,
                        )
                    }
                }

                // Error state
                if (loadState == LoadState.ERROR) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(Icons.Filled.Error, contentDescription = null, tint = MaterialTheme.colorScheme.error, modifier = Modifier.size(48.dp))
                        Spacer(Modifier.height(8.dp))
                        Text("Playback failed", color = Color.White, style = MaterialTheme.typography.titleMedium)
                        Text(errorMsg ?: "Unknown error", color = Color.White.copy(alpha = 0.7f), style = MaterialTheme.typography.bodySmall)
                        Spacer(Modifier.height(16.dp))
                        Button(onClick = onClose) { Text("Close") }
                    }
                }

                // No sources
                if (loadState == LoadState.NO_SOURCES) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("No playable sources", color = Color.White, style = MaterialTheme.typography.titleMedium)
                        Spacer(Modifier.height(8.dp))
                        Text("Try another provider or episode.", color = Color.White.copy(alpha = 0.7f), style = MaterialTheme.typography.bodySmall)
                        Spacer(Modifier.height(16.dp))
                        Button(onClick = onClose) { Text("Close") }
                    }
                }
            }

            // === BOTTOM CONTROLS — always visible, NOT behind video ===
            PlayerBottomBar(
                state = playerState,
                links = links,
                subtitles = subtitles,
                currentLink = currentLink,
                currentSubtitleIndex = currentSubtitleIndex,
                onSeek = { player.seekTo(it.toLong()) },
                onPlayPause = {
                    if (playerState.isPlaying) player.pause() else player.play()
                },
                onVolume = { player.setVolume(it.toInt()) },
                onSelectLink = { link ->
                    currentLink = link
                    player.load(link, title, subtitles)
                },
                onSelectSubtitle = { idx ->
                    currentSubtitleIndex = idx
                    player.selectSubtitle(idx)
                },
                onSearchOnlineSubs = {
                    scope.launch {
                        val api = OpenSubtitlesApi()
                        val results = api.search(title)
                        if (results.isNotEmpty()) {
                            subtitles = (subtitles + results).distinctBy { it.url }
                        }
                    }
                },
                onSelectAudioTrack = { idx -> player.selectAudioTrack(idx) },
                onSelectHlsVariant = { idx -> player.selectHlsVariant(idx) },
            )
        }
    }
}

@Composable
private fun PlayerBottomBar(
    state: VlcjPlayer.PlayerState,
    links: List<ExtractorLink>,
    subtitles: List<SubtitleFile>,
    currentLink: ExtractorLink?,
    currentSubtitleIndex: Int,
    onSeek: (Int) -> Unit,
    onPlayPause: () -> Unit,
    onVolume: (Float) -> Unit,
    onSelectLink: (ExtractorLink) -> Unit,
    onSelectSubtitle: (Int) -> Unit,
    onSearchOnlineSubs: () -> Unit = {},
    onSelectAudioTrack: (Int) -> Unit = {},
    onSelectHlsVariant: (Int) -> Unit = {},
) {
    var sourcesOpen by remember { mutableStateOf(false) }
    var subsOpen by remember { mutableStateOf(false) }
    var audioOpen by remember { mutableStateOf(false) }
    var qualityOpen by remember { mutableStateOf(false) }
    Column(
        Modifier
            .fillMaxWidth()
            .background(
                Brush.verticalGradient(
                    listOf(
                        Color.Transparent,
                        Color.Black.copy(alpha = 0.85f),
                    ),
                ),
            )
            .padding(horizontal = 16.dp, vertical = 12.dp),
    ) {
        // Seek bar.
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = formatTime(state.positionMs),
                color = Color.White,
                style = MaterialTheme.typography.labelSmall,
            )
            Spacer(Modifier.width(8.dp))
            Slider(
                value = state.positionMs.toFloat().coerceIn(0f, state.durationMs.toFloat().coerceAtLeast(1f)),
                onValueChange = { onSeek(it.toInt()) },
                valueRange = 0f..state.durationMs.toFloat().coerceAtLeast(1f),
                modifier = Modifier.weight(1f),
            )
            Spacer(Modifier.width(8.dp))
            Text(
                text = formatTime(state.durationMs),
                color = Color.White,
                style = MaterialTheme.typography.labelSmall,
            )
        }
        Spacer(Modifier.height(4.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onPlayPause) {
                Icon(
                    if (state.isPlaying) Icons.Filled.Pause else Icons.Filled.PlayArrow,
                    contentDescription = if (state.isPlaying) "Pause" else "Play",
                    tint = Color.White,
                )
            }
            Spacer(Modifier.width(12.dp))
            // Volume.
            Icon(Icons.AutoMirrored.Filled.VolumeUp, contentDescription = "Volume", tint = Color.White, modifier = Modifier.size(20.dp))
            Spacer(Modifier.width(4.dp))
            Slider(
                value = state.volume.toFloat(),
                onValueChange = onVolume,
                valueRange = 0f..150f,
                modifier = Modifier.width(120.dp),
            )

            Spacer(Modifier.weight(1f))

            // Source picker.
            Box {
                Row(
                    Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .background(Color.White.copy(alpha = 0.1f))
                        .clickable { sourcesOpen = true }
                        .padding(horizontal = 10.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        text = currentLink?.name ?: "Source",
                        color = Color.White,
                        style = MaterialTheme.typography.labelMedium,
                    )
                    if (currentLink != null) {
                        Spacer(Modifier.width(6.dp))
                        Text(
                            text = "${currentLink.quality}p",
                            color = MaterialTheme.colorScheme.primary,
                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.SemiBold),
                        )
                    }
                    Icon(Icons.Filled.ArrowDropDown, contentDescription = null, tint = Color.White)
                }
                DropdownMenu(
                    expanded = sourcesOpen,
                    onDismissRequest = { sourcesOpen = false },
                ) {
                    links.forEach { link ->
                        DropdownMenuItem(
                            text = {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(link.name, modifier = Modifier.weight(1f))
                                    Spacer(Modifier.width(6.dp))
                                    Text(
                                        text = "${link.quality}p",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.primary,
                                    )
                                }
                            },
                            onClick = {
                                onSelectLink(link)
                                sourcesOpen = false
                            },
                        )
                    }
                }
            }
            Spacer(Modifier.width(8.dp))
            // Subtitle picker.
            Box {
                Row(
                    Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .background(Color.White.copy(alpha = 0.1f))
                        .clickable { subsOpen = true }
                        .padding(horizontal = 10.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Icon(Icons.Filled.Subtitles, contentDescription = "Subtitles", tint = Color.White, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(6.dp))
                    Text(
                        text = if (currentSubtitleIndex >= 0 && currentSubtitleIndex < subtitles.size) {
                            subtitles[currentSubtitleIndex].lang
                        } else "Off",
                        color = Color.White,
                        style = MaterialTheme.typography.labelMedium,
                    )
                    Icon(Icons.Filled.ArrowDropDown, contentDescription = null, tint = Color.White)
                }
                DropdownMenu(
                    expanded = subsOpen,
                    onDismissRequest = { subsOpen = false },
                ) {
                    DropdownMenuItem(
                        text = { Text("Off") },
                        onClick = {
                            onSelectSubtitle(-1)
                            subsOpen = false
                        },
                    )
                    subtitles.forEachIndexed { i, sub ->
                        DropdownMenuItem(
                            text = { Text(sub.lang) },
                            onClick = {
                                onSelectSubtitle(i)
                                subsOpen = false
                            },
                        )
                    }
                    // Search OpenSubtitles online
                    DropdownMenuItem(
                        text = {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    Icons.Filled.Search,
                                    contentDescription = null,
                                    modifier = Modifier.size(16.dp),
                                    tint = MaterialTheme.colorScheme.primary,
                                )
                                Spacer(Modifier.width(8.dp))
                                Text("Search OpenSubtitles…", color = MaterialTheme.colorScheme.primary)
                            }
                        },
                        onClick = {
                            onSearchOnlineSubs()
                            subsOpen = false
                        },
                    )
                }
            }

            // Audio track picker (for multi-language streams)
            if (state.audioTracks.size > 1) {
                Spacer(Modifier.width(8.dp))
                Box {
                    Row(
                        Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .background(Color.White.copy(alpha = 0.1f))
                            .clickable { audioOpen = true }
                            .padding(horizontal = 10.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Icon(Icons.Filled.AudioFile, contentDescription = "Audio", tint = Color.White, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(6.dp))
                        Text(
                            text = state.audioTracks.getOrNull(state.currentAudioTrackIndex)?.description ?: "Audio",
                            color = Color.White,
                            style = MaterialTheme.typography.labelMedium,
                            maxLines = 1,
                            overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis,
                            modifier = Modifier.widthIn(max = 80.dp),
                        )
                        Icon(Icons.Filled.ArrowDropDown, contentDescription = null, tint = Color.White)
                    }
                    DropdownMenu(
                        expanded = audioOpen,
                        onDismissRequest = { audioOpen = false },
                    ) {
                        state.audioTracks.forEach { track ->
                            DropdownMenuItem(
                                text = {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Text(track.description, modifier = Modifier.weight(1f))
                                        if (track.index == state.currentAudioTrackIndex) {
                                            Spacer(Modifier.width(8.dp))
                                            Text("✓", color = MaterialTheme.colorScheme.primary)
                                        }
                                    }
                                },
                                onClick = {
                                    onSelectAudioTrack(track.index)
                                    audioOpen = false
                                },
                            )
                        }
                    }
                }
            }

            // HLS quality variant picker
            if (state.hlsVariants.size > 1) {
                Spacer(Modifier.width(8.dp))
                Box {
                    Row(
                        Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .background(Color.White.copy(alpha = 0.1f))
                            .clickable { qualityOpen = true }
                            .padding(horizontal = 10.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Icon(Icons.Filled.HighQuality, contentDescription = "Quality", tint = Color.White, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(6.dp))
                        Text(
                            text = state.hlsVariants.getOrNull(state.currentHlsVariantIndex)?.resolution ?: "Auto",
                            color = Color.White,
                            style = MaterialTheme.typography.labelMedium,
                        )
                        Icon(Icons.Filled.ArrowDropDown, contentDescription = null, tint = Color.White)
                    }
                    DropdownMenu(
                        expanded = qualityOpen,
                        onDismissRequest = { qualityOpen = false },
                    ) {
                        state.hlsVariants.forEach { variant ->
                            DropdownMenuItem(
                                text = {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Text(variant.resolution, modifier = Modifier.weight(1f))
                                        if (variant.index == state.currentHlsVariantIndex) {
                                            Spacer(Modifier.width(8.dp))
                                            Text("✓", color = MaterialTheme.colorScheme.primary)
                                        }
                                    }
                                },
                                onClick = {
                                    onSelectHlsVariant(variant.index)
                                    qualityOpen = false
                                },
                            )
                        }
                    }
                }
            }
        }
    }
}

private enum class LoadState { LOADING, PLAYING, ERROR, NO_SOURCES }

private fun formatTime(ms: Long): String {
    if (ms <= 0) return "00:00"
    val totalSec = ms / 1000
    val h = totalSec / 3600
    val m = (totalSec % 3600) / 60
    val s = totalSec % 60
    return if (h > 0) "%02d:%02d:%02d".format(h, m, s) else "%02d:%02d".format(m, s)
}

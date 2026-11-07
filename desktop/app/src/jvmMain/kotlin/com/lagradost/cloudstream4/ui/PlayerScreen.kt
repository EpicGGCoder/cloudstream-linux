package com.lagradost.cloudstream4.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.AudioFile
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Error
import androidx.compose.material.icons.filled.Fullscreen
import androidx.compose.material.icons.filled.FullscreenExit
import androidx.compose.material.icons.filled.HighQuality
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Subtitles
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
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
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.lagradost.cloudstream3.MainAPI
import com.lagradost.cloudstream3.SubtitleFile
import com.lagradost.cloudstream3.utils.ExtractorLink
import com.lagradost.cloudstream3.utils.ExtractorLinkType
import com.lagradost.cloudstream4.data.DataStoreHelper
import com.lagradost.cloudstream4.player.VlcjPlayer
import com.lagradost.cloudstream4.subtitles.OpenSubtitlesApi
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.slf4j.LoggerFactory
import java.awt.Component
import java.awt.Frame
import java.awt.event.KeyEvent as AwtKeyEvent
import java.awt.event.KeyListener
import javax.swing.SwingUtilities

private val log = LoggerFactory.getLogger("PlayerScreen")

/** Which picker dialog (if any) is currently open. Rendered as a Compose Dialog
 *  so it appears ABOVE the SwingPanel video surface. */
private enum class PickerKind { SOURCE, QUALITY, SUBTITLE, AUDIO, HLS_VARIANT }

/**
 * Full-screen vlcj-backed player overlay.
 *
 * Flow:
 *   1. Show loading spinner while we call [MainAPI.loadLinks] for [data].
 *   2. Collect [ExtractorLink]s and [SubtitleFile]s via the callbacks.
 *   3. Pick the highest-quality non-torrent link and play it via vlcj.
 *   4. Auto-hide title + bottom controls after 3s of no mouse activity.
 *   5. Source/Quality/Subtitle/Audio pickers swap the playback live.
 *
 * Dropdown menus are rendered as Compose [Dialog]s rather than [DropdownMenu]
 * because AWT/Swing components always paint ABOVE Skia content — meaning
 * DropdownMenu popups would be hidden behind the [SwingPanel] that hosts the
 * vlcj video surface. Dialogs render as separate OS windows and are not
 * subject to that limitation.
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
    // The currently selected source (ExtractorLink.source). Used to group
    // links into Source vs Quality pickers.
    var currentSource by remember { mutableStateOf<String?>(null) }
    var currentSubtitleIndex by remember { mutableStateOf(-1) }
    var pickerOpen by remember { mutableStateOf<PickerKind?>(null) }
    var isFullscreen by remember { mutableStateOf(false) }

    LaunchedEffect(data, api.name) {
        loadState = LoadState.LOADING
        errorMsg = null
        links = emptyList()
        subtitles = emptyList()
        currentLink = null
        currentSource = null
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
            currentSource = playable.first().source
            loadState = LoadState.PLAYING
            player.load(playable.first(), title, collectedSubs)
        } catch (e: Exception) {
            log.error("loadLinks failed", e)
            errorMsg = e.message ?: e::class.simpleName
            loadState = LoadState.ERROR
        }
    }

    val playerState by player.state.collectAsState()

    // === Keyboard handler =====================================================
    //
    // We register an AWT KeyListener directly on the vlcj surface so keys are
    // captured even when the SwingPanel has focus (the usual case once the
    // user clicks the video). We also add a Compose onPreviewKeyEvent so keys
    // work when focus is on a Compose control (e.g. immediately after the
    // source picker closes).
    //
    // Both call into the same handler which reads live player state via
    // `player.state.value` so it always sees the latest values.
    val keyHandler = remember(player) {
        PlayerKeyHandler(
            player = player,
            isFullscreen = { isFullscreen },
            onClose = onClose,
            onToggleFullscreen = {
                toggleFullscreen(player.surface) { newState -> isFullscreen = newState }
            },
        )
    }

    DisposableEffect(Unit) {
        val surface = player.surface
        val listener = object : KeyListener {
            override fun keyTyped(e: AwtKeyEvent) {}
            override fun keyPressed(e: AwtKeyEvent) {
                keyHandler.onPressed(e.keyCode, e.isShiftDown)
            }
            override fun keyReleased(e: AwtKeyEvent) {
                keyHandler.onReleased(e.keyCode)
            }
        }
        surface.addKeyListener(listener)
        onDispose {
            surface.removeKeyListener(listener)
            player.release()
        }
    }

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

    Surface(color = Color.Black, modifier = Modifier
        .fillMaxSize()
        .onPreviewKeyEvent { event ->
            // Only handle KeyDown events so autorepeat doesn't double-trigger
            // for keys like Space.
            if (event.type != KeyEventType.KeyDown) return@onPreviewKeyEvent false
            val keyCode = composeKeyToAwt(event.key) ?: return@onPreviewKeyEvent false
            keyHandler.onPressed(keyCode, isShift = false)
            true
        },
    ) {
        Column(Modifier.fillMaxSize()) {
            // === TOP BAR — title + close button (not behind video) ===
            // Hidden when fullscreen so the video truly takes the whole screen.
            if (!isFullscreen) {
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
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f),
                    )
                }
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

            // === INLINE PICKER PANEL — appears between video and controls ===
            // When a picker is open, show an expanding list panel here (NOT a Dialog)
            // because Dialogs also get hidden behind SwingPanel.
            val activePicker = pickerOpen
            if (activePicker != null) {
                InlinePickerPanel(
                    kind = activePicker,
                    links = links,
                    subtitles = subtitles,
                    state = playerState,
                    currentLink = currentLink,
                    currentSource = currentSource,
                    currentSubtitleIndex = currentSubtitleIndex,
                    onSelectSource = { source ->
                        val candidate = links
                            .filter { it.source == source }
                            .maxByOrNull { it.quality }
                        if (candidate != null) {
                            currentLink = candidate
                            currentSource = source
                            player.load(candidate, title, subtitles)
                        }
                        pickerOpen = null
                    },
                    onSelectLink = { link ->
                        currentLink = link
                        currentSource = link.source
                        player.load(link, title, subtitles)
                        pickerOpen = null
                    },
                    onSelectSubtitle = { idx ->
                        currentSubtitleIndex = idx
                        player.selectSubtitle(idx)
                        pickerOpen = null
                    },
                    onSelectAudioTrack = { idx ->
                        player.selectAudioTrack(idx)
                        pickerOpen = null
                    },
                    onSelectHlsVariant = { idx ->
                        player.selectHlsVariant(idx)
                        pickerOpen = null
                    },
                    onSearchOnlineSubs = {
                        pickerOpen = null
                        scope.launch {
                            val subApi = OpenSubtitlesApi()
                            val results = subApi.search(title)
                            if (results.isNotEmpty()) {
                                subtitles = (subtitles + results).distinctBy { it.url }
                            }
                        }
                    },
                    onDismiss = { pickerOpen = null },
                )
            }

            // === BOTTOM CONTROLS — always visible, NOT behind video ===
            PlayerBottomBar(
                state = playerState,
                links = links,
                subtitles = subtitles,
                currentLink = currentLink,
                currentSource = currentSource,
                currentSubtitleIndex = currentSubtitleIndex,
                isFullscreen = isFullscreen,
                onSeek = { player.seekTo(it.toLong()) },
                onPlayPause = {
                    if (playerState.isPlaying) player.pause() else player.play()
                },
                onVolume = { player.setVolume(it.toInt()) },
                onSelectLink = { link ->
                    currentLink = link
                    currentSource = link.source
                    player.load(link, title, subtitles)
                },
                onSelectSource = { source ->
                    // Pick the best-quality link within this source.
                    val candidate = links
                        .filter { it.source == source }
                        .maxByOrNull { it.quality }
                    if (candidate != null) {
                        currentLink = candidate
                        currentSource = source
                        player.load(candidate, title, subtitles)
                    }
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
                onToggleFullscreen = {
                    toggleFullscreen(player.surface) { newState -> isFullscreen = newState }
                },
                onOpenPicker = { pickerOpen = it },
            )
        }

        // Old Dialog-based pickers removed — now using InlinePickerPanel
        // inside the Column (above), which renders in Compose-managed space
        // and is NOT hidden behind SwingPanel.
    }
}

// ============================================================================
// PlayerKeyHandler — encapsulates keyboard logic so it can be invoked from
// both the AWT KeyListener (when SwingPanel has focus) and Compose's
// onPreviewKeyEvent (when a Compose control has focus).
// ============================================================================

private class PlayerKeyHandler(
    private val player: VlcjPlayer,
    private val isFullscreen: () -> Boolean,
    private val onClose: () -> Unit,
    private val onToggleFullscreen: () -> Unit,
) {
    // Acceleration state for left/right seek keys
    private var seekAccel: Int = 1
    private var lastSeekKey: Int = 0
    private var lastSeekTimeMs: Long = 0L

    fun onPressed(keyCode: Int, isShift: Boolean) {
        val state = player.state.value
        when (keyCode) {
            AwtKeyEvent.VK_LEFT, AwtKeyEvent.VK_RIGHT -> {
                val now = System.currentTimeMillis()
                // If the same direction key was pressed within 400ms, accelerate.
                if (lastSeekKey == keyCode && (now - lastSeekTimeMs) < 400) {
                    seekAccel = (seekAccel * 2).coerceAtMost(16)
                } else {
                    seekAccel = 1
                }
                lastSeekKey = keyCode
                lastSeekTimeMs = now
                val baseSeekMs = 5_000L
                val seekBy = baseSeekMs * seekAccel *
                    if (keyCode == AwtKeyEvent.VK_LEFT) -1 else 1
                val target = (state.positionMs + seekBy)
                    .coerceIn(0L, state.durationMs.coerceAtLeast(0L))
                player.seekTo(target)
            }
            AwtKeyEvent.VK_UP -> {
                // Shift = bigger step (e.g. for fine volume control)
                val step = if (isShift) 25 else 10
                val newVol = (state.volume + step).coerceAtMost(VlcjPlayer.MAX_VOLUME)
                player.setVolume(newVol)
            }
            AwtKeyEvent.VK_DOWN -> {
                val step = if (isShift) 25 else 10
                val newVol = (state.volume - step).coerceAtLeast(0)
                player.setVolume(newVol)
            }
            AwtKeyEvent.VK_SPACE, AwtKeyEvent.VK_P -> {
                if (state.isPlaying) player.pause() else player.play()
            }
            AwtKeyEvent.VK_F -> {
                onToggleFullscreen()
            }
            AwtKeyEvent.VK_ESCAPE -> {
                // If fullscreen, exit fullscreen; otherwise close the player.
                if (isFullscreen()) {
                    onToggleFullscreen()
                } else {
                    onClose()
                }
            }
            AwtKeyEvent.VK_M -> {
                // Mute toggle: store current volume, set to 0; if already 0, restore 100.
                if (state.volume > 0) {
                    player.setVolume(0)
                } else {
                    player.setVolume(100)
                }
            }
        }
    }

    fun onReleased(keyCode: Int) {
        // Reset acceleration when the seek key is released so the next press
        // starts fresh.
        if (keyCode == AwtKeyEvent.VK_LEFT || keyCode == AwtKeyEvent.VK_RIGHT) {
            seekAccel = 1
            lastSeekKey = 0
        }
    }
}

/**
 * Map a Compose Multiplatform [Key] to the equivalent AWT keyCode.
 * Returns null if the key isn't one we handle.
 */
private fun composeKeyToAwt(key: Key): Int? = when (key) {
    Key.DirectionLeft -> AwtKeyEvent.VK_LEFT
    Key.DirectionRight -> AwtKeyEvent.VK_RIGHT
    Key.DirectionUp -> AwtKeyEvent.VK_UP
    Key.DirectionDown -> AwtKeyEvent.VK_DOWN
    Key.Spacebar -> AwtKeyEvent.VK_SPACE
    Key.F -> AwtKeyEvent.VK_F
    Key.Escape -> AwtKeyEvent.VK_ESCAPE
    Key.P -> AwtKeyEvent.VK_P
    Key.M -> AwtKeyEvent.VK_M
    else -> null
}

// ============================================================================
// Fullscreen helper — finds the parent AWT Frame and toggles its
// MAXIMIZED_BOTH state. This works for Compose Desktop windows since the
// Compose Window is backed by a JFrame under the hood.
// ============================================================================

private fun toggleFullscreen(component: Component, onChange: (Boolean) -> Unit) {
    SwingUtilities.invokeLater {
        val window = SwingUtilities.getWindowAncestor(component) ?: return@invokeLater
        if (window is Frame) {
            val isCurrentlyMaximized =
                (window.extendedState and Frame.MAXIMIZED_BOTH) == Frame.MAXIMIZED_BOTH
            window.extendedState = if (isCurrentlyMaximized) {
                Frame.NORMAL
            } else {
                Frame.MAXIMIZED_BOTH
            }
            onChange(!isCurrentlyMaximized)
        } else {
            // For non-Frame windows (rare), at least try to maximize
            window.setSize(window.toolkit.screenSize)
            onChange(true)
        }
    }
}

// ============================================================================
// Bottom control bar
// ============================================================================

@Composable
private fun PlayerBottomBar(
    state: VlcjPlayer.PlayerState,
    links: List<ExtractorLink>,
    subtitles: List<SubtitleFile>,
    currentLink: ExtractorLink?,
    currentSource: String?,
    currentSubtitleIndex: Int,
    isFullscreen: Boolean,
    onSeek: (Int) -> Unit,
    onPlayPause: () -> Unit,
    onVolume: (Float) -> Unit,
    onSelectLink: (ExtractorLink) -> Unit,
    onSelectSource: (String) -> Unit,
    onSelectSubtitle: (Int) -> Unit,
    onSearchOnlineSubs: () -> Unit = {},
    onSelectAudioTrack: (Int) -> Unit = {},
    onSelectHlsVariant: (Int) -> Unit = {},
    onToggleFullscreen: () -> Unit,
    onOpenPicker: (PickerKind) -> Unit,
) {
    // Group all links by their `source` field. The Source picker shows the
    // list of unique source names; the Quality picker shows the links within
    // the current source, sorted by quality (best first).
    val sourcesByQuality = remember(links) {
        links.groupBy { it.source }
            .mapValues { (_, list) -> list.sortedByDescending { it.quality } }
    }
    val uniqueSources = remember(sourcesByQuality) { sourcesByQuality.keys.toList() }
    val linksInCurrentSource = remember(sourcesByQuality, currentSource) {
        currentSource?.let { sourcesByQuality[it] } ?: emptyList()
    }

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
            // Volume — allow boost up to VlcjPlayer.MAX_VOLUME (300%).
            Icon(Icons.AutoMirrored.Filled.VolumeUp, contentDescription = "Volume", tint = Color.White, modifier = Modifier.size(20.dp))
            Spacer(Modifier.width(4.dp))
            Slider(
                value = state.volume.toFloat(),
                onValueChange = onVolume,
                valueRange = 0f..VlcjPlayer.MAX_VOLUME.toFloat(),
                modifier = Modifier.width(120.dp),
            )
            // Show the volume percent (with "boost" indicator above 100%).
            Spacer(Modifier.width(4.dp))
            Text(
                text = "${state.volume}%",
                color = if (state.volume > 100) MaterialTheme.colorScheme.primary else Color.White,
                style = MaterialTheme.typography.labelSmall,
            )

            Spacer(Modifier.weight(1f))

            // === Source picker — switches between providers ===
            if (uniqueSources.isNotEmpty()) {
                BottomBarChipButton(
                    label = "Source",
                    value = currentSource ?: "—",
                    accentText = null,
                    onClick = { onOpenPicker(PickerKind.SOURCE) },
                )
                Spacer(Modifier.width(8.dp))
            }

            // === Quality picker — switches quality variants within the
            // current source (e.g. 480p, 720p, 1080p) ===
            if (linksInCurrentSource.size > 1) {
                BottomBarChipButton(
                    label = "Quality",
                    value = "${currentLink?.quality ?: 0}p",
                    accentText = null,
                    onClick = { onOpenPicker(PickerKind.QUALITY) },
                )
                Spacer(Modifier.width(8.dp))
            }

            // === Subtitle picker ===
            BottomBarChipButton(
                label = "Subtitles",
                value = if (currentSubtitleIndex in subtitles.indices) {
                    subtitles[currentSubtitleIndex].lang
                } else "Off",
                icon = Icons.Filled.Subtitles,
                onClick = { onOpenPicker(PickerKind.SUBTITLE) },
            )
            Spacer(Modifier.width(8.dp))

            // === Audio track picker (multi-language streams) ===
            if (state.audioTracks.size > 1) {
                BottomBarChipButton(
                    label = "Audio",
                    value = state.audioTracks.getOrNull(state.currentAudioTrackIndex)?.description
                        ?: "—",
                    icon = Icons.Filled.AudioFile,
                    onClick = { onOpenPicker(PickerKind.AUDIO) },
                    maxWidth = 110.dp,
                )
                Spacer(Modifier.width(8.dp))
            }

            // === HLS stream quality variant picker (within one .m3u8 link) ===
            if (state.hlsVariants.size > 1) {
                BottomBarChipButton(
                    label = "Quality",
                    value = state.hlsVariants.getOrNull(state.currentHlsVariantIndex)?.resolution
                        ?: "Auto",
                    icon = Icons.Filled.HighQuality,
                    onClick = { onOpenPicker(PickerKind.HLS_VARIANT) },
                )
                Spacer(Modifier.width(8.dp))
            }

            // === Fullscreen toggle ===
            IconButton(onClick = onToggleFullscreen) {
                Icon(
                    if (isFullscreen) Icons.Filled.FullscreenExit else Icons.Filled.Fullscreen,
                    contentDescription = if (isFullscreen) "Exit fullscreen" else "Fullscreen",
                    tint = Color.White,
                )
            }
        }
    }
}

/** A small pill-style button used in the bottom control bar — opens a picker
 *  dialog when clicked. */
@Composable
private fun BottomBarChipButton(
    label: String,
    value: String,
    accentText: String? = null,
    icon: androidx.compose.ui.graphics.vector.ImageVector? = null,
    maxWidth: androidx.compose.ui.unit.Dp = 160.dp,
    onClick: () -> Unit,
) {
    Row(
        Modifier
            .clip(RoundedCornerShape(8.dp))
            .background(Color.White.copy(alpha = 0.1f))
            .clickable(onClick = onClick)
            .padding(horizontal = 10.dp, vertical = 6.dp)
            .widthIn(max = maxWidth),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (icon != null) {
            Icon(icon, contentDescription = null, tint = Color.White, modifier = Modifier.size(18.dp))
            Spacer(Modifier.width(6.dp))
        }
        Column {
            Text(
                text = label,
                color = Color.White.copy(alpha = 0.6f),
                style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.sp),
            )
            Text(
                text = value,
                color = Color.White,
                style = MaterialTheme.typography.labelMedium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
        if (accentText != null) {
            Spacer(Modifier.width(6.dp))
            Text(
                text = accentText,
                color = MaterialTheme.colorScheme.primary,
                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.SemiBold),
            )
        }
        Spacer(Modifier.width(4.dp))
        Icon(Icons.Filled.ArrowDropDown, contentDescription = null, tint = Color.White)
    }
}

// ============================================================================
// Picker dialogs — rendered as Compose Dialog windows so they appear ABOVE
// the SwingPanel video surface.
// ============================================================================

// ============================================================================
// InlinePickerPanel — renders the picker as an expanding panel INSIDE the
// Column layout (between video and bottom controls). This is the ONLY
// reliable way to show picker UI that isn't hidden behind SwingPanel.
// ============================================================================
@Composable
private fun InlinePickerPanel(
    kind: PickerKind,
    links: List<ExtractorLink>,
    subtitles: List<SubtitleFile>,
    state: VlcjPlayer.PlayerState,
    currentLink: ExtractorLink?,
    currentSource: String,
    currentSubtitleIndex: Int,
    onSelectSource: (String) -> Unit,
    onSelectLink: (ExtractorLink) -> Unit,
    onSelectSubtitle: (Int) -> Unit,
    onSelectAudioTrack: (Int) -> Unit,
    onSelectHlsVariant: (Int) -> Unit,
    onSearchOnlineSubs: () -> Unit,
    onDismiss: () -> Unit,
) {
    Surface(
        color = MaterialTheme.colorScheme.surface,
        tonalElevation = 4.dp,
        modifier = Modifier.fillMaxWidth().heightIn(max = 300.dp),
    ) {
        Column(Modifier.fillMaxWidth()) {
            // Header
            Row(
                Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = when (kind) {
                        PickerKind.SOURCE -> "Select Source"
                        PickerKind.QUALITY -> "Select Quality"
                        PickerKind.SUBTITLES -> "Select Subtitles"
                        PickerKind.AUDIO -> "Select Audio Track"
                        PickerKind.HLS_VARIANT -> "Select Stream Quality"
                    },
                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.SemiBold),
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.weight(1f),
                )
                IconButton(onClick = onDismiss, modifier = Modifier.size(32.dp)) {
                    Icon(Icons.Filled.Close, contentDescription = "Close", modifier = Modifier.size(20.dp))
                }
            }

            // Options list — scrollable
            androidx.compose.foundation.lazy.LazyColumn(
                modifier = Modifier.fillMaxWidth().heightIn(max = 240.dp),
            ) {
                when (kind) {
                    PickerKind.SOURCE -> {
                        val sources = links.map { it.source }.distinct()
                        items(sources) { source ->
                            PickerRow(
                                text = source,
                                isSelected = source == currentSource,
                                subtitle = "${links.count { it.source == source }} streams",
                                onClick = { onSelectSource(source) },
                            )
                        }
                    }
                    PickerKind.QUALITY -> {
                        val sourceLinks = links.filter { it.source == currentSource }
                            .sortedByDescending { it.quality }
                        items(sourceLinks) { link ->
                            PickerRow(
                                text = "${link.name} ${if (link.quality > 0) "${link.quality}p" else ""}",
                                isSelected = link == currentLink,
                                subtitle = link.source,
                                onClick = { onSelectLink(link) },
                            )
                        }
                    }
                    PickerKind.SUBTITLES -> {
                        item {
                            PickerRow(
                                text = "Off",
                                isSelected = currentSubtitleIndex == -1,
                                onClick = { onSelectSubtitle(-1) },
                            )
                        }
                        items(subtitles.size) { idx ->
                            PickerRow(
                                text = subtitles[idx].lang,
                                isSelected = idx == currentSubtitleIndex,
                                onClick = { onSelectSubtitle(idx) },
                            )
                        }
                        item {
                            PickerRow(
                                text = "Search OpenSubtitles…",
                                isSelected = false,
                                onClick = onSearchOnlineSubs,
                            )
                        }
                    }
                    PickerKind.AUDIO -> {
                        if (state.audioTracks.isEmpty()) {
                            item { PickerRow(text = "No audio tracks", isSelected = false, onClick = {}) }
                        } else {
                            items(state.audioTracks.size) { idx ->
                                val track = state.audioTracks[idx]
                                PickerRow(
                                    text = track.description,
                                    isSelected = idx == state.currentAudioTrackIndex,
                                    onClick = { onSelectAudioTrack(idx) },
                                )
                            }
                        }
                    }
                    PickerKind.HLS_VARIANT -> {
                        if (state.hlsVariants.isEmpty()) {
                            item { PickerRow(text = "No quality variants available", isSelected = false, onClick = {}) }
                        } else {
                            items(state.hlsVariants.size) { idx ->
                                val variant = state.hlsVariants[idx]
                                PickerRow(
                                    text = variant.resolution,
                                    isSelected = idx == state.currentHlsVariantIndex,
                                    onClick = { onSelectHlsVariant(idx) },
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun PickerRow(
    text: String,
    isSelected: Boolean,
    subtitle: String? = null,
    onClick: () -> Unit,
) {
    Row(
        Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(Modifier.weight(1f)) {
            Text(
                text = text,
                style = MaterialTheme.typography.bodyMedium,
                color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface,
                fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal,
            )
            if (subtitle != null) {
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
        if (isSelected) {
            Icon(Icons.Filled.Check, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(20.dp))
        }
    }
}


@Composable
private fun PickerDialog(
    title: String,
    subtitle: String? = null,
    onDismiss: () -> Unit,
    content: @Composable ColumnScope.() -> Unit,
) {
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(
            dismissOnBackPress = true,
            dismissOnClickOutside = true,
            usePlatformDefaultWidth = false,
        ),
    ) {
        Surface(
            shape = RoundedCornerShape(12.dp),
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 6.dp,
            modifier = Modifier
                .widthIn(min = 280.dp, max = 380.dp)
                .heightIn(max = 540.dp),
        ) {
            Column {
                // Header
                Row(
                    Modifier
                        .fillMaxWidth()
                        .padding(start = 16.dp, end = 8.dp, top = 12.dp, bottom = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Column(Modifier.weight(1f)) {
                        Text(
                            text = title,
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
                            color = MaterialTheme.colorScheme.onSurface,
                        )
                        if (subtitle != null) {
                            Text(
                                text = subtitle,
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }
                    IconButton(onClick = onDismiss, modifier = Modifier.size(32.dp)) {
                        Icon(Icons.Filled.Close, contentDescription = "Close", modifier = Modifier.size(20.dp))
                    }
                }
                // Body — scrollable list of options
                Column(
                    Modifier
                        .verticalScroll(rememberScrollState())
                        .padding(bottom = 8.dp),
                    content = content,
                )
            }
        }
    }
}

@Composable
private fun PickerRow(
    title: String,
    subtitle: String?,
    isSelected: Boolean,
    icon: androidx.compose.ui.graphics.vector.ImageVector? = null,
    accent: Boolean = false,
    onClick: () -> Unit,
) {
    Row(
        Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (icon != null) {
            Icon(
                icon,
                contentDescription = null,
                tint = if (accent) MaterialTheme.colorScheme.primary
                else MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(18.dp),
            )
            Spacer(Modifier.width(10.dp))
        }
        Column(Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.bodyLarge,
                color = if (accent) MaterialTheme.colorScheme.primary
                else MaterialTheme.colorScheme.onSurface,
            )
            if (subtitle != null) {
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
        if (isSelected) {
            Spacer(Modifier.width(8.dp))
            Text("✓", color = MaterialTheme.colorScheme.primary, style = MaterialTheme.typography.titleMedium)
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

package com.lagradost.cloudstream4.player

import com.lagradost.cloudstream3.SubtitleFile
import com.lagradost.cloudstream3.utils.ExtractorLink
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import org.slf4j.LoggerFactory
import uk.co.caprica.vlcj.player.base.MediaPlayer
import uk.co.caprica.vlcj.player.base.MediaPlayerEventAdapter
import uk.co.caprica.vlcj.player.component.EmbeddedMediaPlayerComponent
import uk.co.caprica.vlcj.player.base.TrackDescription
import java.awt.Component
import javax.swing.SwingUtilities

/**
 * VLCJ-based media player — full-featured like the original CS3 player.
 *
 * Features:
 *   - HLS (.m3u8) playback with quality variant selection
 *   - Direct video (.mp4, .mkv, .webm, .avi)
 *   - Multiple audio tracks (language selection)
 *   - Subtitle tracks (provider-supplied + embedded)
 *   - Volume, seek, playback speed
 *   - Hardware-accelerated decoding (VAAPI/VDPAU)
 *
 * Requires libvlc: sudo apt install vlc libvlc-dev
 */
class VlcjPlayer(
    private val scope: CoroutineScope,
) {
    private val log = LoggerFactory.getLogger(VlcjPlayer::class.java)

    private val component: EmbeddedMediaPlayerComponent = EmbeddedMediaPlayerComponent()
    val surface: Component get() = component

    private val _state = MutableStateFlow(PlayerState())
    val state: StateFlow<PlayerState> = _state.asStateFlow()

    private var currentLink: ExtractorLink? = null
    private var currentSubtitles: List<SubtitleFile> = emptyList()
    private var currentSubtitleIndex: Int = -1

    data class AudioTrack(
        val id: String,
        val description: String,
        val index: Int,
    )

    data class PlayerState(
        val isPlaying: Boolean = false,
        val isLoading: Boolean = false,
        val hasError: Boolean = false,
        val errorMessage: String? = null,
        val positionMs: Long = 0L,
        val durationMs: Long = 0L,
        val volume: Int = 100,
        val rate: Float = 1.0f,
        val title: String = "",
        val availableLinks: List<ExtractorLink> = emptyList(),
        val availableSubtitles: List<SubtitleFile> = emptyList(),
        val currentSubtitleLang: String? = null,
        // Audio tracks
        val audioTracks: List<AudioTrack> = emptyList(),
        val currentAudioTrackIndex: Int = -1,
        // HLS quality variants
        val hlsVariants: List<HlsVariant> = emptyList(),
        val currentHlsVariantIndex: Int = -1,
    )

    data class HlsVariant(
        val index: Int,
        val bandwidth: Long,
        val resolution: String,
        val height: Int,
    )

    init {
        component.mediaPlayer().events().addMediaPlayerEventListener(object : MediaPlayerEventAdapter() {
            override fun playing(mediaPlayer: MediaPlayer) {
                _state.value = _state.value.copy(isPlaying = true, isLoading = false)
                // After playback starts, fetch audio tracks and HLS variants
                refreshAudioTracks()
                refreshHlsVariants()
            }

            override fun paused(mediaPlayer: MediaPlayer) {
                _state.value = _state.value.copy(isPlaying = false)
            }

            override fun stopped(mediaPlayer: MediaPlayer) {
                _state.value = _state.value.copy(isPlaying = false)
            }

            override fun buffering(mediaPlayer: MediaPlayer, newCache: Float) {
                _state.value = _state.value.copy(isLoading = newCache < 1.0f)
            }

            override fun error(mediaPlayer: MediaPlayer) {
                log.error("VLC player error")
                _state.value = _state.value.copy(hasError = true, isLoading = false)
            }

            override fun timeChanged(mediaPlayer: MediaPlayer, newTime: Long) {
                _state.value = _state.value.copy(positionMs = newTime)
            }

            override fun lengthChanged(mediaPlayer: MediaPlayer, newLength: Long) {
                _state.value = _state.value.copy(durationMs = newLength)
            }

            override fun opening(mediaPlayer: MediaPlayer) {
                _state.value = _state.value.copy(isLoading = true)
            }

            override fun mediaChanged(mediaPlayer: MediaPlayer, media: uk.co.caprica.vlcj.media.MediaRef) {
                // Media changed — refresh tracks after a short delay
                scope.launch {
                    delay(1000)
                    refreshAudioTracks()
                }
            }
        })
    }

    fun load(link: ExtractorLink, title: String, subtitles: List<SubtitleFile> = emptyList()) {
        currentLink = link
        currentSubtitles = subtitles
        currentSubtitleIndex = -1
        _state.value = _state.value.copy(
            isLoading = true,
            hasError = false,
            title = title,
            availableLinks = listOf(link),
            availableSubtitles = subtitles,
            positionMs = 0,
            durationMs = 0,
            audioTracks = emptyList(),
            currentAudioTrackIndex = -1,
            hlsVariants = emptyList(),
            currentHlsVariantIndex = -1,
        )

        SwingUtilities.invokeLater {
            val media = component.mediaPlayer().media()
            val opts = buildList {
                if (link.referer.isNotEmpty()) {
                    add(":http-referrer=${link.referer}")
                }
                for ((k, v) in link.headers) {
                    add(":http-header=$k: $v")
                }
                // Prefer hardware decoding
                add(":avcodec-hw=vaapi")
            }
            media.play(link.url, *opts.toTypedArray())
        }
    }

    fun play() {
        SwingUtilities.invokeLater { component.mediaPlayer().controls().play() }
    }

    fun pause() {
        SwingUtilities.invokeLater { component.mediaPlayer().controls().pause() }
    }

    fun seekTo(ms: Long) {
        SwingUtilities.invokeLater { component.mediaPlayer().controls().setTime(ms) }
    }

    fun setVolume(vol: Int) {
        // Allow boost up to 300% (libvlc internally supports up to 200 cleanly,
        // higher values amplify via software and may distort, but are useful
        // for quiet sources).
        val clamped = vol.coerceIn(0, MAX_VOLUME)
        component.mediaPlayer().audio().setVolume(clamped)
        _state.value = _state.value.copy(volume = clamped)
    }

    companion object {
        /** Maximum allowed volume (0-300). 100 = original volume. */
        const val MAX_VOLUME: Int = 300
    }

    fun setRate(rate: Float) {
        component.mediaPlayer().controls().setRate(rate)
        _state.value = _state.value.copy(rate = rate)
    }

    // ------------------------------------------------------------------
    // Audio track selection — like VLC's Audio > Track menu
    // ------------------------------------------------------------------

    /**
     * Refresh the list of available audio tracks from the current media.
     * Called automatically when playback starts.
     */
    private fun refreshAudioTracks() {
        try {
            val descriptions = component.mediaPlayer().audio().trackDescriptions()
            if (descriptions != null && descriptions.isNotEmpty()) {
                val tracks = descriptions.mapIndexed { i, desc ->
                    val rawDesc = desc?.description()?.trim().orEmpty()
                    AudioTrack(
                        id = desc?.id()?.toString() ?: i.toString(),
                        description = formatAudioTrackName(rawDesc, i),
                        index = i,
                    )
                }
                val current = component.mediaPlayer().audio().track()
                _state.value = _state.value.copy(
                    audioTracks = tracks,
                    currentAudioTrackIndex = current,
                )
                log.info("Found ${tracks.size} audio tracks: ${tracks.map { it.description }}")
            }
        } catch (e: Exception) {
            log.debug("Failed to get audio tracks: ${e.message}")
        }
    }

    /**
     * Convert vlcj's raw audio track description into a user-friendly label.
     * libvlc usually returns the language name (e.g. "English", "Japanese"),
     * but sometimes returns "Track N" or an empty string. Fall back to
     * "Audio N" so the picker always shows something sensible.
     */
    private fun formatAudioTrackName(raw: String, index: Int): String {
        if (raw.isEmpty()) return "Audio ${index + 1}"
        // libvlc uses "Track N" as a placeholder when it can't detect language
        if (raw.equals("Track", ignoreCase = true) ||
            raw.matches(Regex("^Track\\s*\\d+$", RegexOption.IGNORE_CASE))
        ) {
            return "Audio ${index + 1}"
        }
        return raw
    }

    /**
     * Select an audio track by index (for language selection).
     * This is the key feature CS3's player is missing — separate audio track selection.
     */
    fun selectAudioTrack(index: Int) {
        try {
            val tracks = _state.value.audioTracks
            if (index in tracks.indices) {
                component.mediaPlayer().audio().setTrack(index)
                _state.value = _state.value.copy(currentAudioTrackIndex = index)
                log.info("Selected audio track: ${tracks[index].description}")
            }
        } catch (e: Exception) {
            log.error("Failed to select audio track $index", e)
        }
    }

    // ------------------------------------------------------------------
    // HLS quality variant selection — pick resolution from m3u8
    // ------------------------------------------------------------------

    /**
     * Refresh the list of HLS quality variants.
     * vlcj exposes these via the TrackDescription API for video tracks.
     */
    private fun refreshHlsVariants() {
        try {
            val descriptions = component.mediaPlayer().video().trackDescriptions()
            if (descriptions != null && descriptions.size > 1) {
                val variants = descriptions.mapIndexed { i, desc ->
                    val descStr = desc?.description() ?: "Variant ${i + 1}"
                    val height = extractHeight(descStr)
                    HlsVariant(
                        index = i,
                        bandwidth = 0,
                        resolution = descStr,
                        height = height,
                    )
                }
                val current = component.mediaPlayer().video().track()
                _state.value = _state.value.copy(
                    hlsVariants = variants,
                    currentHlsVariantIndex = current,
                )
                log.info("Found ${variants.size} HLS variants: ${variants.map { it.resolution }}")
            }
        } catch (e: Exception) {
            log.debug("Failed to get HLS variants: ${e.message}")
        }
    }

    /**
     * Select an HLS quality variant by index.
     * This lets the user switch between 480p, 720p, 1080p, etc.
     */
    fun selectHlsVariant(index: Int) {
        try {
            val variants = _state.value.hlsVariants
            if (index in variants.indices) {
                component.mediaPlayer().video().setTrack(index)
                _state.value = _state.value.copy(currentHlsVariantIndex = index)
                log.info("Selected HLS variant: ${variants[index].resolution}")
            }
        } catch (e: Exception) {
            log.error("Failed to select HLS variant $index", e)
        }
    }

    private fun extractHeight(desc: String): Int {
        // Try to parse "1080p", "720p", "480p" etc.
        Regex("(\\d{3,4})p").find(desc)?.let { return it.groupValues[1].toInt() }
        // Try "1920x1080"
        Regex("\\d+x(\\d+)").find(desc)?.let { return it.groupValues[1].toInt() }
        return 0
    }

    // ------------------------------------------------------------------
    // Subtitle selection
    // ------------------------------------------------------------------

    fun selectSubtitle(index: Int) {
        val subs = currentSubtitles
        if (index in subs.indices) {
            val sub = subs[index]
            component.mediaPlayer().subpictures().setSubTitleFile(sub.url)
            currentSubtitleIndex = index
            _state.value = _state.value.copy(currentSubtitleLang = sub.lang)
        } else if (index == -1) {
            component.mediaPlayer().subpictures().setSubTitleFile("")
            currentSubtitleIndex = -1
            _state.value = _state.value.copy(currentSubtitleLang = null)
        }
    }

    fun release() {
        try {
            component.mediaPlayer().controls().stop()
            component.release()
        } catch (e: Exception) {
            log.warn("Error releasing VLC player", e)
        }
    }
}

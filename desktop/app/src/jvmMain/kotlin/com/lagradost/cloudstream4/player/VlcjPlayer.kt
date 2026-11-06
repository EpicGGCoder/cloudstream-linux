package com.lagradost.cloudstream4.player

import com.lagradost.cloudstream3.SubtitleFile
import com.lagradost.cloudstream3.utils.ExtractorLink
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import org.slf4j.LoggerFactory
import uk.co.caprica.vlcj.player.base.MediaPlayer
import uk.co.caprica.vlcj.player.base.MediaPlayerEventAdapter
import uk.co.caprica.vlcj.player.component.EmbeddedMediaPlayerComponent
import java.awt.Component
import javax.swing.SwingUtilities

/**
 * VLCJ-based media player.
 *
 * vlcj wraps libvlc — the same engine that powers VLC. On Linux, this gives us:
 *   - HLS (.m3u8) playback
 *   - Direct video (.mp4, .mkv, .webm, .avi, ...)
 *   - DASH (.mpd)
 *   - Subtitles (.srt, .vtt, embedded)
 *   - Multiple audio tracks
 *   - Hardware-accelerated decoding (VDPAU/VAAPI)
 *
 * Requires libvlc installed on the system (apt install vlc / libvlc-dev).
 *
 * For DRM (Widevine/PlayReady) content: NOT supported by libvlc. Only DRM-free
 * sources will play. Most CS3 plugins serve DRM-free HLS/MP4.
 */
class VlcjPlayer(
    private val scope: CoroutineScope,
) {
    private val log = LoggerFactory.getLogger(VlcjPlayer::class.java)

    /** The actual vlcj media player component — creates an AWT Canvas inside. */
    private val component: EmbeddedMediaPlayerComponent = EmbeddedMediaPlayerComponent()

    /** Surface to embed in Compose Desktop (via ComposePanel bridge). */
    val surface: Component get() = component

    private val _state = MutableStateFlow(PlayerState())
    val state: StateFlow<PlayerState> = _state.asStateFlow()

    private var currentLink: ExtractorLink? = null
    private var currentSubtitles: List<SubtitleFile> = emptyList()
    private var currentSubtitleIndex: Int = -1

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
    )

    init {
        component.mediaPlayer().events().addMediaPlayerEventListener(object : MediaPlayerEventAdapter() {
            override fun playing(mediaPlayer: MediaPlayer) {
                _state.value = _state.value.copy(isPlaying = true, isLoading = false)
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
        )

        SwingUtilities.invokeLater {
            val media = component.mediaPlayer().media()
            media.prepare(link.url)
            // Apply headers if needed (referer etc.)
            val opts = buildList {
                if (link.referer.isNotEmpty()) {
                    add(":http-referrer=${link.referer}")
                }
                for ((k, v) in link.headers) {
                    add(":http-header=$k: $v")
                }
                if (link.quality > 0) {
                    // No direct quality control in libvlc for HLS —
                    // the manifest chooses the variant.
                }
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
        val clamped = vol.coerceIn(0, 150)
        component.mediaPlayer().audio().setVolume(clamped)
        _state.value = _state.value.copy(volume = clamped)
    }

    fun setRate(rate: Float) {
        component.mediaPlayer().controls().setRate(rate)
        _state.value = _state.value.copy(rate = rate)
    }

    /**
     * Select a subtitle track by index (or pass -1 to disable).
     * For .srt/.vtt URLs, vlcj auto-detects them as we add them.
     */
    fun selectSubtitle(index: Int) {
        val subs = currentSubtitles
        if (index in subs.indices) {
            val sub = subs[index]
            val mp = component.mediaPlayer()
            // Add the subtitle URL via libvlc's slave/spu mechanism.
            mp.subpictures().setSubTitleFile(sub.url)
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

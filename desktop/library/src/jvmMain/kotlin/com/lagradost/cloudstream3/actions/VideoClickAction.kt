package com.lagradost.cloudstream3.actions

/**
 * JVM stub for CS3's `com.lagradost.cloudstream3.actions.VideoClickAction`.
 *
 * In the Android app, VideoClickAction represents an external player option
 * (VLC, MPV, Fcast, etc.) shown when the user clicks the "play" button.
 * On JVM desktop we don't use external players — vlcj handles playback.
 * This stub exists so plugin bytecode that registers VideoClickActions links.
 */
open class VideoClickAction {
    var name: String = ""
    var sourcePlugin: String? = null

    open fun playVideo(url: String) {}
}

/** JVM stub for CS3's VideoClickActionHolder — holds registered actions. */
object VideoClickActionHolder {
    val allVideoClickActions = mutableListOf<VideoClickAction>()
}

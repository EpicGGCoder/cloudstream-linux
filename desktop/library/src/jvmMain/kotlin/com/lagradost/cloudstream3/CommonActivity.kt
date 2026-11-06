package com.lagradost.cloudstream3

import android.content.Context

/**
 * JVM stub for CS3's `com.lagradost.cloudstream3.CommonActivity`.
 *
 * In the Android app, `CommonActivity` is the base activity that holds
 * process-wide state (cast session, toast helpers, snackbar). On JVM desktop
 * we don't have Activities — this stub exists only so plugin bytecode can
 * link when plugins extend `CommonActivity` or hold a reference to it.
 *
 * Some CNCVerse plugins expect `CommonActivity.INSTANCE` (a static singleton
 * field) — we provide that via the companion object so the bytecode resolves.
 *
 * Extends our android.content.Context stub so instance checks pass.
 */
open class CommonActivity : Context() {

    /** Stub: in Android this returns the active cast session. */
    fun getCastSession(): Any? = null

    /** Stub: shows a toast. */
    fun showToast(message: String) {
        org.slf4j.LoggerFactory.getLogger("CommonActivity").info("Toast: $message")
    }

    /** Stub: hides the keyboard. */
    fun hideKeyboard() {}

    /** Stub: shows the keyboard. */
    fun showKeyboard() {}

    /** Stub: finishes the activity (no-op on desktop). */
    fun finish() {}

    /**
     * Returns the stub Activity. Some CNCVerse plugins (Cricify, LivXow,
     * M3UPlaylist, PlayFy, PlayZTV, SKTech, Sportzx) call this from their
     * plugin constructor to grab the host activity. On JVM desktop we return
     * a stub Activity instance.
     *
     * Note: this is an INSTANCE method (not @JvmStatic) because the plugins
     * were compiled expecting an instance method signature.
     */
    fun getActivity(): android.app.Activity {
        return ActivityHolder.activity
    }

    companion object {
        /**
         * Static singleton instance — some plugins reference `CommonActivity.INSTANCE`
         * (compiled against a forked version of CS3 that made CommonActivity an object).
         * We provide a stub instance so the bytecode resolves.
         */
        @JvmField
        val INSTANCE: CommonActivity = CommonActivity()
    }

    /** Holds the stub Activity returned by getActivity(). */
    private object ActivityHolder {
        val activity: android.app.Activity = android.app.Activity()
    }
}

package android.app

import android.content.Context

/**
 * JVM stub for Android's [android.app.Activity].
 *
 * Plugins sometimes hold a reference to Activity (the host window). On JVM
 * desktop we don't have Activities — this stub extends Context so instance
 * checks pass and plugin bytecode links.
 */
open class Activity : Context() {
    fun runOnUiThread(action: Runnable) { action.run() }
    fun runOnUiThread(action: () -> Unit) { action() }
    fun startActivity(intent: android.content.Intent) {}
    fun startActivityForResult(intent: android.content.Intent, requestCode: Int) {}
    fun setResult(resultCode: Int) {}
    fun setResult(resultCode: Int, data: android.content.Intent?) {}
    fun finish() {}

    companion object {
        const val RESULT_OK = -1
        const val RESULT_CANCELED = 0
        const val RESULT_FIRST_USER = 1
    }
}

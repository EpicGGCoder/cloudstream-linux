package android.widget

import android.content.Context
import android.view.View

/**
 * JVM stub for Android's [android.widget.FrameLayout].
 *
 * Plugins reference FrameLayout when building in-app UI views. On JVM desktop
 * we don't render Android views — this stub lets bytecode link.
 */
open class FrameLayout : View {
    constructor(context: Context?) : super()
    constructor(context: Context?, attrs: Any?) : super()

    fun addView(child: View?) {}
    fun addView(child: View?, params: LayoutParams) {}
    fun removeAllViews() {}
    fun removeView(view: View?) {}

    open class LayoutParams {
        constructor(width: Int, height: Int)
        constructor(width: Int, height: Int, gravity: Int)
    }

    companion object {
        const val FILL_PARENT = -1
        const val MATCH_PARENT = -1
        const val WRAP_CONTENT = -2
    }
}

package android.view

/**
 * JVM stub for Android's [android.view.View].
 *
 * Plugins rarely instantiate View directly — they reference inner
 * listener interfaces (e.g. View.OnKeyListener, View.OnClickListener).
 * These stubs let bytecode link.
 */
open class View {
    interface OnClickListener {
        fun onClick(v: View?)
    }
    interface OnKeyListener {
        fun onKey(v: View?, keyCode: Int, event: KeyEvent?): Boolean
    }
    interface OnTouchListener {
        fun onTouch(v: View?, event: MotionEvent?): Boolean
    }
    interface OnFocusChangeListener {
        fun onFocusChange(v: View?, hasFocus: Boolean)
    }
}

/** Stub KeyEvent. */
open class KeyEvent

/** Stub MotionEvent. */
open class MotionEvent

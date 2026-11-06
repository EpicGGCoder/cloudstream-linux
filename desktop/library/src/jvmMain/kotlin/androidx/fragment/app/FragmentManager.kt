package androidx.fragment.app

/**
 * JVM stubs for Android's androidx.fragment.app classes.
 *
 * Plugins reference FragmentManager/Fragment to show donation prompts
 * or in-app browsers. On JVM desktop we don't have fragments — these
 * stubs exist only so plugin bytecode can link.
 */
open class Fragment

object FragmentManager {
    fun findFragmentByTag(tag: String?): Fragment? = null
}

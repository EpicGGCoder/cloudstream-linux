package android.content

/**
 * JVM stub for Android's [android.content.ComponentName].
 *
 * Plugins reference this for Intent component resolution. On JVM desktop
 * it's a stub — just wraps a package/class name pair.
 */
class ComponentName(
    private val packageName: String,
    private val className: String,
) {
    fun getPackageName(): String = packageName
    fun getClassName(): String = className
    override fun toString(): String = "$packageName/$className"
}

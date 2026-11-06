package android.content.res

/**
 * JVM stub for Android's [android.content.res.AssetManager].
 *
 * Plugins use this to access bundled assets when `requiresResources = true`.
 * On JVM desktop we don't load Android assets — stub only.
 */
class AssetManager {
    fun open(fileName: String): java.io.InputStream = java.io.ByteArrayInputStream(ByteArray(0))
    fun list(path: String): Array<String> = emptyArray()
}

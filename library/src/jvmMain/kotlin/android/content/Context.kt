package android.content

/**
 * JVM stub for Android's [android.content.Context] class.
 *
 * CloudStream plugins compiled against Android's `:app` module hold a
 * reference to `Context` (used for resources, shared preferences, etc).
 * On JVM desktop we don't have a real Android Context, so we provide this
 * stub in the same package so the plugin bytecode can link.
 *
 * Plugin code that calls real Context methods will use the stubbed
 * implementations here which return sensible no-op defaults so plugins
 * don't crash during load().
 */
open class Context {

    /** Stub: returns self (Android convention). */
    open fun getApplicationContext(): Context = this

    /** Stub: returns an empty in-memory preferences map. */
    open fun getSharedPreferences(name: String?, mode: Int): SharedPreferences {
        return NoopSharedPreferences
    }

    /** Stub: returns empty resources. */
    open fun getResources(): android.content.res.Resources {
        return android.content.res.Resources()
    }

    /** Stub: returns the package name. */
    open fun getPackageName(): String = "com.lagradost.cloudstream4"

    /** Stub: returns empty File. */
    open fun getFilesDir(): java.io.File = java.io.File(System.getProperty("user.home"), ".cloudstream")

    /** Stub: returns empty cache dir. */
    open fun getCacheDir(): java.io.File = java.io.File(System.getProperty("java.io.tmpdir"))

    /** Stub: returns empty external files dir. */
    open fun getExternalFilesDir(type: String?): java.io.File? = getFilesDir()

    /** Stub: returns the asset manager. */
    open fun getAssets(): android.content.res.AssetManager = android.content.res.AssetManager()

    /** Stub: returns the class loader. */
    open fun getClassLoader(): ClassLoader = Context::class.java.classLoader
}

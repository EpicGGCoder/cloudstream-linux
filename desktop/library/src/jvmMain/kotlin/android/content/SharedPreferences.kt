package android.content

/**
 * JVM stub for Android's [android.content.SharedPreferences] interface.
 *
 * Plugins use SharedPreferences to persist small key/value state. On JVM
 * desktop, we provide this stub-backed in-memory implementation so plugin
 * bytecode links and any prefs the plugin reads/writes during load() work
 * (returns default values; nothing persists).
 *
 * For real persistence, the desktop app intercepts at a higher layer
 * (PluginManager + AppSettings) and doesn't rely on plugin-side prefs.
 */
interface SharedPreferences {
    fun getAll(): Map<String, *>
    fun getString(key: String, defValue: String?): String?
    fun getInt(key: String, defValue: Int): Int
    fun getLong(key: String, defValue: Long): Long
    fun getFloat(key: String, defValue: Float): Float
    fun getBoolean(key: String, defValue: Boolean): Boolean
    fun contains(key: String): Boolean

    interface Editor {
        fun putString(key: String, value: String?): Editor
        fun putInt(key: String, value: Int): Editor
        fun putLong(key: String, value: Long): Editor
        fun putFloat(key: String, value: Float): Editor
        fun putBoolean(key: String, value: Boolean): Editor
        fun remove(key: String): Editor
        fun clear(): Editor
        fun commit(): Boolean
        fun apply()
    }

    fun edit(): Editor
}

/** In-memory no-op implementation. */
object NoopSharedPreferences : SharedPreferences {
    private val map = HashMap<String, Any?>()
    override fun getAll(): Map<String, *> = map
    override fun getString(key: String, defValue: String?): String? = (map[key] as? String) ?: defValue
    override fun getInt(key: String, defValue: Int): Int = (map[key] as? Int) ?: defValue
    override fun getLong(key: String, defValue: Long): Long = (map[key] as? Long) ?: defValue
    override fun getFloat(key: String, defValue: Float): Float = (map[key] as? Float) ?: defValue
    override fun getBoolean(key: String, defValue: Boolean): Boolean = (map[key] as? Boolean) ?: defValue
    override fun contains(key: String): Boolean = map.containsKey(key)
    override fun edit(): SharedPreferences.Editor = NoopEditor
}

private object NoopEditor : SharedPreferences.Editor {
    override fun putString(key: String, value: String?) = this
    override fun putInt(key: String, value: Int) = this
    override fun putLong(key: String, value: Long) = this
    override fun putFloat(key: String, value: Float) = this
    override fun putBoolean(key: String, value: Boolean) = this
    override fun remove(key: String) = this
    override fun clear() = this
    override fun commit() = true
    override fun apply() {}
}

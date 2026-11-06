package android.content

/**
 * JVM stub for Android's [android.content.Intent].
 *
 * Plugins use Intent to launch activities / pass data. On JVM desktop we
 * don't have Android Intents — this stub lets bytecode link. Most plugins
 * only reference Intent as a type without actually launching activities
 * during provider registration.
 */
open class Intent {
    private val extras = HashMap<String, Any?>()

    constructor()
    constructor(action: String?)
    constructor(action: String?, uri: android.net.Uri?)
    constructor(packageContext: Context?, cls: Class<*>?)

    fun putExtra(name: String, value: String?): Intent {
        extras[name] = value
        return this
    }
    fun putExtra(name: String, value: Int): Intent {
        extras[name] = value
        return this
    }
    fun putExtra(name: String, value: Boolean): Intent {
        extras[name] = value
        return this
    }
    fun putExtra(name: String, value: Long): Intent {
        extras[name] = value
        return this
    }
    fun putExtra(name: String, value: java.io.Serializable?): Intent {
        extras[name] = value
        return this
    }

    fun getStringExtra(name: String): String? = extras[name] as? String
    fun getIntExtra(name: String, defaultValue: Int): Int = (extras[name] as? Int) ?: defaultValue
    fun getBooleanExtra(name: String, defaultValue: Boolean): Boolean = (extras[name] as? Boolean) ?: defaultValue
    fun getLongExtra(name: String, defaultValue: Long): Long = (extras[name] as? Long) ?: defaultValue

    fun getAction(): String? = null
    fun getData(): android.net.Uri? = null
    fun getType(): String? = null
    fun setAction(action: String?): Intent = this
    fun setType(type: String?): Intent = this
    fun setData(uri: android.net.Uri?): Intent = this
    fun addCategory(category: String?): Intent = this
    fun setClassName(packageName: String?, className: String?): Intent = this
    fun setClass(packageContext: Context?, cls: Class<*>?): Intent = this
    fun addFlags(flags: Int): Intent = this
    fun setFlags(flags: Int): Intent = this
    fun getFlags(): Int = 0

    companion object {
        const val ACTION_VIEW = "android.intent.action.VIEW"
        const val ACTION_SEND = "android.intent.action.SEND"
        const val ACTION_MAIN = "android.intent.action.MAIN"
        const val ACTION_PICK = "android.intent.action.PICK"
        const val EXTRA_TEXT = "android.intent.extra.TEXT"
        const val EXTRA_STREAM = "android.intent.extra.STREAM"
        const val EXTRA_SUBJECT = "android.intent.extra.SUBJECT"
        const val FLAG_ACTIVITY_NEW_TASK = 0x10000000
        const val FLAG_ACTIVITY_CLEAR_TOP = 0x04000000
        const val FLAG_GRANT_READ_URI_PERMISSION = 0x00000001
    }
}

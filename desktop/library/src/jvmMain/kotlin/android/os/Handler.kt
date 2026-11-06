package android.os

/**
 * JVM stub for Android's [android.os.Handler].
 *
 * Plugins use Handler to post runnables to the main thread. On JVM desktop
 * we execute runnables inline (synchronously) — fine for plugin load() use.
 */
open class Handler {
    constructor()
    constructor(looper: Looper?)

    /**
     * Post a runnable to the message queue. On Android this runs async on
     * the Looper's thread. On JVM desktop we just return true WITHOUT
     * running — this prevents Cloudflare-killer WebView code from blocking
     * the HTTP call. The HTTP request proceeds without Cloudflare bypass
     * (will fail on Cloudflare-protected sites — Phase 2 JCEF fixes this).
     */
    fun post(r: Runnable): Boolean {
        return true
    }
    fun postDelayed(r: Runnable, delayMillis: Long): Boolean {
        return true
    }
    fun removeCallbacks(r: Runnable) {}
    fun removeMessages(what: Int) {}

    interface Callback {
        fun handleMessage(msg: Message): Boolean
    }
}

/** JVM stub for [android.os.Message]. */
open class Message {
    var what: Int = 0
    var arg1: Int = 0
    var arg2: Int = 0
    var obj: Any? = null

    companion object {
        @JvmStatic
        fun obtain(): Message = Message()
        @JvmStatic
        fun obtain(what: Int): Message { val m = Message(); m.what = what; return m }
    }
}

/**
 * JVM stub for Android's [android.os.Looper].
 *
 * Plugins (especially OkHttp interceptors) reference Looper.getMainLooper()
 * to post callbacks to the main thread. On JVM desktop we return a stub.
 */
open class Looper {
    companion object {
        @JvmStatic
        fun getMainLooper(): Looper = Looper()
        @JvmStatic
        fun myLooper(): Looper? = getMainLooper()
        @JvmStatic
        fun prepare() {}
        @JvmStatic
        fun loop() {}
    }
}

/** JVM stub for [android.os.Bundle]. */
open class Bundle {
    private val map = HashMap<String, Any?>()

    fun putString(key: String, value: String?): Bundle { map[key] = value; return this }
    fun putInt(key: String, value: Int): Bundle { map[key] = value; return this }
    fun putBoolean(key: String, value: Boolean): Bundle { map[key] = value; return this }
    fun putLong(key: String, value: Long): Bundle { map[key] = value; return this }

    fun getString(key: String): String? = map[key] as? String
    fun getInt(key: String): Int = (map[key] as? Int) ?: 0
    fun getBoolean(key: String): Boolean = (map[key] as? Boolean) ?: false
    fun getLong(key: String): Long = (map[key] as? Long) ?: 0L

    fun containsKey(key: String): Boolean = map.containsKey(key)
    fun keySet(): Set<String> = map.keys
}

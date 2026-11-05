package android.util

/**
 * JVM stub for Android's [android.util.Log] class.
 *
 * CS3 plugins call `Log.i/d/e/w(tag, msg)` everywhere. On JVM, we forward
 * to SLF4J so the logs appear in the desktop app's log output.
 *
 * Methods are @JvmStatic because Android's Log has static methods.
 */
object Log {
    @JvmStatic
    fun i(tag: String, msg: String): Int {
        org.slf4j.LoggerFactory.getLogger(tag).info(msg)
        return 0
    }
    @JvmStatic
    fun d(tag: String, msg: String): Int {
        org.slf4j.LoggerFactory.getLogger(tag).debug(msg)
        return 0
    }
    @JvmStatic
    fun w(tag: String, msg: String): Int {
        org.slf4j.LoggerFactory.getLogger(tag).warn(msg)
        return 0
    }
    @JvmStatic
    fun e(tag: String, msg: String, throwable: Throwable? = null): Int {
        if (throwable != null) {
            org.slf4j.LoggerFactory.getLogger(tag).error(msg, throwable)
        } else {
            org.slf4j.LoggerFactory.getLogger(tag).error(msg)
        }
        return 0
    }
    @JvmStatic
    fun e(tag: String, msg: String): Int {
        org.slf4j.LoggerFactory.getLogger(tag).error(msg)
        return 0
    }
    @JvmStatic
    fun v(tag: String, msg: String): Int {
        org.slf4j.LoggerFactory.getLogger(tag).trace(msg)
        return 0
    }
}

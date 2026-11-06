package android.util

/**
 * JVM stub for Android's [android.util.Base64] class.
 *
 * Plugins use Base64.encode/decode for various encoding tasks.
 * On JVM we delegate to java.util.Base64.
 *
 * Methods must be @JvmStatic because Android's Base64 has static methods
 * and the plugin bytecode was compiled against that signature.
 */
object Base64 {
    @JvmStatic
    fun encodeToString(input: ByteArray, flags: Int = 0): String {
        return java.util.Base64.getEncoder().encodeToString(input)
    }

    @JvmStatic
    fun encode(input: ByteArray, flags: Int = 0): ByteArray {
        return java.util.Base64.getEncoder().encode(input)
    }

    @JvmStatic
    fun decode(input: String, flags: Int = 0): ByteArray {
        return java.util.Base64.getDecoder().decode(input)
    }

    @JvmStatic
    fun decode(input: ByteArray, flags: Int = 0): ByteArray {
        return java.util.Base64.getDecoder().decode(input)
    }
}

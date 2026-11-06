package android.net

import java.net.URI

/**
 * JVM stub for Android's [android.net.Uri].
 *
 * Plugins use Uri.parse() to parse URLs and getQueryParameter() to extract
 * query params. On JVM we delegate to java.net.URI.
 */
class Uri private constructor(private val uri: URI) {

    override fun toString(): String = uri.toString()

    fun getHost(): String? = uri.host
    fun getPath(): String? = uri.path
    fun getQuery(): String? = uri.query
    fun getScheme(): String? = uri.scheme
    fun getPort(): Int = uri.port

    /**
     * Returns the decoded query parameter value for the given key,
     * or null if the parameter isn't present.
     */
    fun getQueryParameter(key: String): String? {
        val q = uri.query ?: return null
        for (pair in q.split("&")) {
            val eq = pair.indexOf("=")
            val k = if (eq > 0) pair.substring(0, eq) else pair
            val v = if (eq > 0) pair.substring(eq + 1) else ""
            if (k == key) return v
        }
        return null
    }

    /**
     * Returns the set of query parameter names.
     */
    fun getQueryParameterNames(): Set<String> {
        val q = uri.query ?: return emptySet()
        val names = LinkedHashSet<String>()
        for (pair in q.split("&")) {
            val eq = pair.indexOf("=")
            val k = if (eq > 0) pair.substring(0, eq) else pair
            if (k.isNotEmpty()) names.add(k)
        }
        return names
    }

    /** Returns the last path segment. */
    fun getLastPathSegment(): String? {
        val p = uri.path ?: return null
        val segments = p.split("/").filter { it.isNotEmpty() }
        return segments.lastOrNull()
    }

    /** Returns the list of path segments. */
    fun getPathSegments(): List<String> {
        val p = uri.path ?: return emptyList()
        return p.split("/").filter { it.isNotEmpty() }
    }

    companion object {
        @JvmStatic
        fun parse(uriString: String): Uri? {
            return try {
                Uri(URI.create(uriString))
            } catch (e: Exception) {
                null
            }
        }
    }
}

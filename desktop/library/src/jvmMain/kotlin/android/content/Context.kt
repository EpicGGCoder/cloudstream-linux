package android.content

/**
 * JVM stub for Android's [android.content.Context] class.
 *
 * CloudStream plugins compiled against Android's `:app` module hold a
 * reference to `Context` (used for resources, shared preferences, etc).
 * On JVM desktop we don't have a real Android Context, so we provide this
 * empty stub in the same package so the plugin bytecode can link.
 *
 * Plugin code that calls real Context methods (e.g. `getSharedPreferences`)
 * will fail at runtime — but most providers only store the Context for
 * later use (e.g. `Companion.setContext(context)`) and never call methods
 * that don't have JVM equivalents.
 *
 * For full Context method coverage, plugins should be patched to use
 * `MainAPI.app` (the cross-platform HTTP client) instead of `Context`.
 */
open class Context

package android.content.res

/**
 * JVM stub for Android's [android.content.res.Resources] class.
 *
 * Plugins use `Resources` to access bundled drawables/strings when
 * `requiresResources = true` in their manifest. On JVM desktop we don't
 * load Android resources — most plugins have `requiresResources = false`
 * and won't touch this.
 *
 * Stub is here only so the bytecode links when a plugin holds a
 * `Resources?` field.
 */
class Resources

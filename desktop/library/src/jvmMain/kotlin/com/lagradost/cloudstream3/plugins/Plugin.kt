package com.lagradost.cloudstream3.plugins

/**
 * JVM-compatible stub for the app-level `Plugin` abstract class.
 *
 * On Android, plugins extend `com.lagradost.cloudstream3.plugins.Plugin`
 * (defined in the `:app` module). That class extends [BasePlugin] and adds
 * Android `Context` / `Resources` plumbing.
 *
 * On JVM, we provide this no-op stub in the same package so that plugins
 * compiled against Android's `Plugin` class can still be loaded via
 * [java.net.URLClassLoader]. The plugin's `load(context)` override will
 * fail (no Android Context) but its no-arg `load()` (called by BasePlugin
 * subclasses) works fine — which is all we need for provider registration.
 *
 * See ARCHITECTURE.md §2 (Plugin Loading).
 */
abstract class Plugin : BasePlugin() {

    /**
     * Called when the plugin is loaded. The Android version takes a Context;
     * on JVM we don't have one, so plugins that override `load(context)`
     * instead of `load()` will need a small patch. Most plugins just
     * override `load()` and call `registerMainAPI(...)`.
     */
    @Throws(Throwable::class)
    open fun load(context: Any? = null) {
        // Fall back to the no-arg load() — most plugins use this.
        load()
    }
}

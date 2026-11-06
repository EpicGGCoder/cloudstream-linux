package com.lagradost.cloudstream3.plugins

/**
 * JVM stub for the app-level `Plugin` abstract class.
 *
 * On Android, plugins extend `com.lagradost.cloudstream3.plugins.Plugin`
 * (defined in the `:app` module). That class extends [BasePlugin] and adds
 * Android `Context` / `Resources` plumbing plus `openSettings` / `resources`
 * fields that some plugins set in their constructor.
 *
 * On JVM, we provide this stub in the same package so that plugins
 * compiled against Android's `Plugin` class can still be loaded via
 * [java.net.URLClassLoader].
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
        load()
    }

    /**
     * Allows plugins to register a settings callback. On Android this opens
     * a settings Activity. On JVM desktop it's a no-op stub — the property
     * exists so plugin bytecode that calls `setOpenSettings(...)` links.
     *
     * NOTE: This is a `var` property, so Kotlin generates both `getOpenSettings`
     * and `setOpenSettings` accessors. Plugins compiled against the Android
     * `Plugin` class expect `setOpenSettings(kotlin.jvm.functions.Function1)`.
     */
    var openSettings: ((context: Any?) -> Unit)? = null

    /** Stub for plugin resources (Android Resources). Not used on JVM. */
    var resources: Any? = null
}

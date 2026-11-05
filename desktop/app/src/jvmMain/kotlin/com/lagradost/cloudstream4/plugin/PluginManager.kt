package com.lagradost.cloudstream4.plugin

import com.lagradost.cloudstream3.APIHolder
import com.lagradost.cloudstream3.MainAPI
import com.lagradost.cloudstream3.plugins.BasePlugin
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import org.slf4j.LoggerFactory
import java.io.File

/**
 * Tracks installed plugins and the loaded [BasePlugin] instances.
 *
 * Mirrors CS3's `PluginManager` (app/src/main/java/.../plugins/PluginManager.kt):
 *   - plugins: Map<String, BasePlugin>     (filepath → instance)
 *   - loadedLocalPlugins / loadedOnlinePlugins flags
 *
 * Persistence: a JSON file at `dataDir/plugins.json` records the list of
 * installed plugin file paths + their source URLs.
 */
class PluginManager(private val dataDir: File) {
    private val log = LoggerFactory.getLogger(PluginManager::class.java)
    private val json = Json { ignoreUnknownKeys = true; prettyPrint = true }

    private val pluginsFile = File(dataDir, "plugins.json").apply { parentFile?.mkdirs() }
    private val pluginsDir = File(dataDir, "plugins").apply { mkdirs() }

    data class InstalledPlugin(
        val internalName: String,
        val displayName: String,
        val filePath: String,
        val sourceUrl: String?,
        val isOnline: Boolean,
        val version: Int = 1,
        val enabled: Boolean = true,
    )

    @Serializable
    private data class PluginStore(val plugins: List<InstalledPluginSerialized> = emptyList())

    @Serializable
    private data class InstalledPluginSerialized(
        val internalName: String,
        val displayName: String,
        val filePath: String,
        val sourceUrl: String? = null,
        val isOnline: Boolean = true,
        val version: Int = 1,
        val enabled: Boolean = true,
    )

    private val _installed = MutableStateFlow<List<InstalledPlugin>>(emptyList())
    val installed: StateFlow<List<InstalledPlugin>> = _installed.asStateFlow()

    private val loadedInstances = mutableMapOf<String, BasePlugin>()

    init {
        PluginLoader.init(dataDir)
        loadStore()
    }

    // ------------------------------------------------------------------
    // Store I/O
    // ------------------------------------------------------------------

    private fun loadStore() {
        try {
            if (!pluginsFile.exists()) return
            val text = pluginsFile.readText()
            val store = json.decodeFromString<PluginStore>(text)
            _installed.value = store.plugins.map {
                InstalledPlugin(
                    internalName = it.internalName,
                    displayName = it.displayName,
                    filePath = it.filePath,
                    sourceUrl = it.sourceUrl,
                    isOnline = it.isOnline,
                    version = it.version,
                    enabled = it.enabled,
                )
            }
        } catch (e: Exception) {
            log.error("Failed to load plugin store", e)
        }
    }

    private fun saveStore() {
        try {
            val store = PluginStore(
                _installed.value.map {
                    InstalledPluginSerialized(
                        internalName = it.internalName,
                        displayName = it.displayName,
                        filePath = it.filePath,
                        sourceUrl = it.sourceUrl,
                        isOnline = it.isOnline,
                        version = it.version,
                        enabled = it.enabled,
                    )
                }
            )
            pluginsFile.writeText(json.encodeToString(PluginStore.serializer(), store))
        } catch (e: Exception) {
            log.error("Failed to save plugin store", e)
        }
    }

    // ------------------------------------------------------------------
    // Install / uninstall
    // ------------------------------------------------------------------

    /**
     * Register an already-downloaded .cs3 file. The plugin is loaded eagerly.
     */
    suspend fun install(file: File, sourceUrl: String? = null, isOnline: Boolean = true): Boolean {
        val plugin = PluginLoader.loadPlugin(file, sourceUrl) ?: return false
        val internalName = plugin::class.simpleName ?: file.nameWithoutExtension
        loadedInstances[file.absolutePath] = plugin

        _installed.value = _installed.value.filterNot { it.filePath == file.absolutePath } + InstalledPlugin(
            internalName = internalName,
            displayName = plugin::class.simpleName ?: file.nameWithoutExtension,
            filePath = file.absolutePath,
            sourceUrl = sourceUrl,
            isOnline = isOnline,
        )
        saveStore()
        log.info("Installed plugin: $internalName")
        return true
    }

    /**
     * Load all plugins saved in the store. Called at app startup.
     */
    suspend fun loadAllSavedPlugins() = withContext(Dispatchers.IO) {
        val saved = _installed.value.toList()
        var ok = 0
        for (entry in saved) {
            val file = File(entry.filePath)
            if (!file.exists()) {
                log.warn("Plugin file missing: ${entry.filePath}; skipping")
                continue
            }
            val plugin = PluginLoader.loadPlugin(file, entry.sourceUrl)
            if (plugin != null) {
                loadedInstances[file.absolutePath] = plugin
                ok++
            }
        }
        log.info("Loaded $ok/${saved.size} saved plugins")
    }

    fun uninstall(filePath: String) {
        val entry = _installed.value.firstOrNull { it.filePath == filePath } ?: return
        loadedInstances.remove(filePath)
        _installed.value = _installed.value.filterNot { it.filePath == filePath }
        saveStore()
        runCatching { File(filePath).delete() }
        log.info("Uninstalled plugin: ${entry.internalName}")
    }

    fun pluginsDir(): File = pluginsDir

    /**
     * All currently-loaded MainAPI providers (from all loaded plugins).
     * Equivalent to CS3's `APIHolder.allProviders`.
     */
    fun providers(): List<MainAPI> = APIHolder.allProviders.toList()
}

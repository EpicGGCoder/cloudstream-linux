package com.lagradost.cloudstream4.data

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import org.slf4j.LoggerFactory
import java.io.File

/**
 * App settings — persisted as JSON at `~/.cloudstream/settings.json`.
 *
 * This is a minimal version of CS3's `DevicePreferenceStore` (see ARCHITECTURE.md §9.4).
 * We only model what's needed for the desktop port right now; we can grow this
 * as we add features.
 */
@Serializable
data class SettingsData(
    val theme: String = "dark",                // "dark", "light", "amoled", "dracula"
    val preferredMediaTypes: List<String> = listOf("Movie", "TvSeries", "Anime"),
    val extensionLanguages: List<String> = listOf("en"),
    val displaySubDub: Boolean = true,
    val filterQuality: List<String> = emptyList(),
    val enableAdult: Boolean = false,
    val parallelDownloads: Int = 3,
    val downloadPath: String = "~/Downloads/CloudStream",
    val playerSpeed: Float = 1.0f,
    val softwareDecoding: Boolean = false,
    val swipeGestures: Boolean = true,
    val doubleTapSeek: Boolean = true,
    val previewBar: Boolean = true,
    val primaryColor: String = "#B388FF",
    val locale: String = "en-US",
    val confirmExit: Boolean = false,
    val autoUpdatePlugins: Boolean = true,
    val autoDownloadPlugins: Boolean = false,
    val showAppUpdates: Boolean = true,
    val skipUpdate: Int = 0,
)

class AppSettings(private val dataDir: File) {
    private val log = LoggerFactory.getLogger(AppSettings::class.java)
    private val json = Json { ignoreUnknownKeys = true; prettyPrint = true; encodeDefaults = true }
    private val file = File(dataDir, "settings.json")

    @Volatile
    var data: SettingsData = SettingsData()
        private set

    init {
        load()
    }

    private fun load() {
        try {
            if (file.exists()) {
                data = json.decodeFromString(SettingsData.serializer(), file.readText())
                log.info("Settings loaded from ${file.absolutePath}")
            }
        } catch (e: Exception) {
            log.error("Failed to load settings", e)
        }
    }

    fun update(transform: (SettingsData) -> SettingsData) {
        data = transform(data)
        save()
    }

    private fun save() {
        try {
            file.writeText(json.encodeToString(SettingsData.serializer(), data))
        } catch (e: Exception) {
            log.error("Failed to save settings", e)
        }
    }
}

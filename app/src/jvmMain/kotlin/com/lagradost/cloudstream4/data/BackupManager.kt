package com.lagradost.cloudstream4.data

import com.lagradost.cloudstream3.SearchResponse
import com.lagradost.cloudstream3.TvType
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import org.slf4j.LoggerFactory
import java.io.File

/**
 * Backup/restore — mirrors CS3's BackupUtils.
 *
 * Exports all user data (settings, library, plugin list) to a single JSON file.
 * Can be imported on another machine.
 */
class BackupManager(
    private val dataDir: File,
    private val settings: AppSettings,
    private val dataStore: DataStoreHelper,
) {
    private val log = LoggerFactory.getLogger(BackupManager::class.java)
    private val json = Json { ignoreUnknownKeys = true; prettyPrint = true; encodeDefaults = true }

    @Serializable
    data class BackupFile(
        val version: Int = 1,
        val createdAt: Long = System.currentTimeMillis(),
        val settings: SettingsBackup = SettingsBackup(),
        val library: LibraryBackup = LibraryBackup(),
    )

    @Serializable
    data class SettingsBackup(val values: Map<String, String> = emptyMap())

    @Serializable
    data class LibraryBackup(
        val bookmarks: List<DataStoreHelper.StoredMedia> = emptyList(),
        val favorites: List<DataStoreHelper.StoredMedia> = emptyList(),
        val resumeWatching: List<DataStoreHelper.WatchProgress> = emptyList(),
        val history: List<DataStoreHelper.StoredMedia> = emptyList(),
    )

    /** Export all data to a JSON file. */
    fun export(targetFile: File): Boolean {
        return try {
            val library = dataStore.library.value
            val backup = BackupFile(
                settings = SettingsBackup(
                    values = mapOf(
                        "theme" to settings.data.theme,
                        "locale" to settings.data.locale,
                        "preferredMediaTypes" to settings.data.preferredMediaTypes.joinToString(","),
                        "extensionLanguages" to settings.data.extensionLanguages.joinToString(","),
                        "playerSpeed" to settings.data.playerSpeed.toString(),
                        "primaryColor" to settings.data.primaryColor,
                        "autoUpdatePlugins" to settings.data.autoUpdatePlugins.toString(),
                    )
                ),
                library = LibraryBackup(
                    bookmarks = library.bookmarks,
                    favorites = library.favorites,
                    resumeWatching = library.resumeWatching,
                    history = library.history,
                ),
            )
            targetFile.writeText(json.encodeToString(BackupFile.serializer(), backup))
            log.info("Backup exported to ${targetFile.absolutePath} (${targetFile.length()} bytes)")
            true
        } catch (e: Exception) {
            log.error("Backup export failed", e)
            false
        }
    }

    /** Import data from a JSON file. */
    fun import(sourceFile: File): Boolean {
        return try {
            val text = sourceFile.readText()
            val backup = json.decodeFromString(BackupFile.serializer(), text)

            // Restore settings
            backup.settings.values["theme"]?.let { themeValue ->
                settings.update { it.copy(theme = themeValue) }
            }
            backup.settings.values["locale"]?.let { locale ->
                settings.update { it.copy(locale = locale) }
            }
            backup.settings.values["primaryColor"]?.let { color ->
                settings.update { it.copy(primaryColor = color) }
            }

            // Restore library
            val currentLibrary = dataStore.library.value
            val merged = currentLibrary.copy(
                bookmarks = (currentLibrary.bookmarks + backup.library.bookmarks).distinctBy { it.url },
                favorites = (currentLibrary.favorites + backup.library.favorites).distinctBy { it.url },
                resumeWatching = (currentLibrary.resumeWatching + backup.library.resumeWatching).distinctBy { it.url },
                history = (currentLibrary.history + backup.library.history).distinctBy { it.url },
            )
            // Write merged library directly
            File(dataDir, "data/library.json").apply { parentFile?.mkdirs() }
                .writeText(json.encodeToString(DataStoreHelper.LibraryStore.serializer(),
                    DataStoreHelper.LibraryStore(
                        bookmarks = merged.bookmarks,
                        favorites = merged.favorites,
                        resumeWatching = merged.resumeWatching,
                        history = merged.history,
                    )
                ))

            log.info("Backup imported from ${sourceFile.absolutePath}")
            true
        } catch (e: Exception) {
            log.error("Backup import failed", e)
            false
        }
    }
}

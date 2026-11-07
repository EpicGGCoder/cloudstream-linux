package com.lagradost.cloudstream4

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.SystemUpdate
import androidx.compose.material.icons.filled.VideoLibrary
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import com.lagradost.cloudstream3.MainAPI
import com.lagradost.cloudstream3.SearchResponse
import com.lagradost.cloudstream4.data.AppSettings
import com.lagradost.cloudstream4.data.DataStoreHelper
import com.lagradost.cloudstream4.network.AppUpdateChecker
import com.lagradost.cloudstream4.network.WebViewResolverJvm
import com.lagradost.cloudstream4.plugin.DexToolsManager
import com.lagradost.cloudstream4.plugin.PluginManager
import com.lagradost.cloudstream4.repo.RepositoryManager
import com.lagradost.cloudstream4.ui.DetailScreen
import com.lagradost.cloudstream4.ui.HomeScreen
import com.lagradost.cloudstream4.ui.LibraryScreen
import com.lagradost.cloudstream4.ui.PlayerScreen
import com.lagradost.cloudstream4.ui.PluginsScreen
import com.lagradost.cloudstream4.ui.SearchScreen
import com.lagradost.cloudstream4.ui.SettingsScreen
import org.slf4j.LoggerFactory
import java.io.File

private val log = LoggerFactory.getLogger("CloudStreamMain")

/**
 * CloudStream Linux Desktop — main entry point.
 *
 * This is a REAL native Compose Desktop application (not a website). When
 * packaged with `./gradlew :app:packageAppImage`, it produces a portable
 * CloudStream.AppImage that runs on any Linux distro with libvlc + glibc.
 *
 * Architecture:
 *   - UI: Compose Multiplatform (the same framework CS3 is migrating to upstream).
 *   - Plugin loading: real .cs3 plugins via dex2jar (see PluginLoader).
 *   - WebView: JavaFX WebView (for Cloudflare-protected sites).
 *   - Player: vlcj (libvlc bindings) — supports HLS, MP4, MKV, subtitles, DRM-free.
 *   - Persistence: JSON files under ~/.cloudstream/
 */
fun main() = application {
    val dataDir = File(System.getProperty("user.home"), ".cloudstream").apply { mkdirs() }
    log.info("CloudStream data directory: ${dataDir.absolutePath}")

    val settings = remember { AppSettings(dataDir) }
    val dataStore = remember { DataStoreHelper(dataDir) }
    val pluginManager = remember { PluginManager(dataDir) }
    val repoManager = remember { RepositoryManager(pluginManager) }

    // All init happens in background — never blocks or crashes the UI
    var appUpdateInfo by remember { mutableStateOf<AppUpdateChecker.UpdateInfo?>(null) }
    var pluginUpdateCount by remember { mutableStateOf(0) }

    LaunchedEffect(Unit) {
        // Step 1: WebView resolver (non-blocking, errors ignored)
        try { WebViewResolverJvm.init() } catch (e: Exception) { log.warn("WebView init: ${e.message}") }

        // Step 2: Ensure dex-tools is available
        try { DexToolsManager.init(dataDir) } catch (e: Exception) { log.warn("DexTools init: ${e.message}") }

        // Step 3: Load saved plugins
        try { pluginManager.loadAllSavedPlugins() } catch (e: Exception) { log.warn("Plugin load: ${e.message}") }

        // Step 4: Auto-update installed plugins
        try {
            val updated = repoManager.checkAndAutoUpdatePlugins { name, status ->
                log.info("Plugin update: $name → $status")
            }
            pluginUpdateCount = updated
        } catch (e: Exception) { log.warn("Plugin auto-update: ${e.message}") }

        // Step 5: Check for app updates
        try {
            val update = AppUpdateChecker.checkForUpdates()
            if (update != null && update.available) {
                appUpdateInfo = update
                log.info("App update available: ${update.latestVersion}")
            }
        } catch (e: Exception) { log.warn("App update check: ${e.message}") }
    }

    val state = rememberWindowState()

    Window(
        onCloseRequest = ::exitApplication,
        title = "CloudStream",
        state = state,
    ) {
        CloudStreamTheme {
            CompositionLocalProvider {
                Scaffold(
                    containerColor = MaterialTheme.colorScheme.background,
                    contentColor = MaterialTheme.colorScheme.onBackground,
                ) { padding ->
                    Box(Modifier.fillMaxSize().padding(padding)) {
                        try {
                            AppShell(pluginManager, repoManager, settings, dataStore)
                        } catch (e: Exception) {
                            log.error("UI error", e)
                            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Text("Something went wrong", style = MaterialTheme.typography.titleLarge)
                                    Spacer(Modifier.height(8.dp))
                                    Text(e.message ?: "Unknown error", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                            }
                        }

                        // App update notification (top-right corner)
                        appUpdateInfo?.let { info ->
                            Box(Modifier.align(Alignment.TopEnd)) {
                                UpdateNotification(
                                    info = info,
                                    onDismiss = { appUpdateInfo = null },
                                )
                            }
                        }

                        // Plugin update toast (bottom-right, above nav)
                        if (pluginUpdateCount > 0) {
                            Box(Modifier.align(Alignment.BottomEnd)) {
                                PluginUpdateToast(count = pluginUpdateCount)
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun CloudStreamTheme(content: @Composable () -> Unit) {
    val scheme = darkColorScheme(
        primary = Color(0xFFB388FF),
        onPrimary = Color(0xFF1A0D1F),
        primaryContainer = Color(0xFF4A2E70),
        onPrimaryContainer = Color(0xFFEADDFF),
        secondary = Color(0xFFCCC2DC),
        onSecondary = Color(0xFF332D41),
        background = Color(0xFF0E0A12),
        onBackground = Color(0xFFE6E1E9),
        surface = Color(0xFF15101A),
        onSurface = Color(0xFFE6E1E9),
        surfaceVariant = Color(0xFF221B2C),
        onSurfaceVariant = Color(0xFFCAC4D0),
        error = Color(0xFFF2B8B5),
        onError = Color(0xFF601410),
        errorContainer = Color(0xFF8C1D18),
        onErrorContainer = Color(0xFFF9DEDC),
    )
    MaterialTheme(colorScheme = scheme, content = content)
}

private enum class Tab { Home, Search, Library, Plugins, Settings }

private sealed class Screen {
    data class TabScreen(val tab: Tab) : Screen()
    data class Detail(val item: SearchResponse) : Screen()
    data class Player(val api: MainAPI, val data: String, val title: String) : Screen()
}

@Composable
private fun AppShell(
    pluginManager: PluginManager,
    repoManager: RepositoryManager,
    settings: AppSettings,
    dataStore: DataStoreHelper,
) {
    var screen by remember { mutableStateOf<Screen>(Screen.TabScreen(Tab.Home)) }

    Box(Modifier.fillMaxSize()) {
        // Underlying tab content (always present so state is preserved).
        when (val s = screen) {
            is Screen.TabScreen -> when (s.tab) {
                Tab.Home -> HomeScreen(
                    pluginManager = pluginManager,
                    onOpenDetail = { screen = Screen.Detail(it) },
                    onGoToPlugins = { screen = Screen.TabScreen(Tab.Plugins) },
                )
                Tab.Search -> SearchScreen(
                    pluginManager = pluginManager,
                    settings = settings,
                    onOpenDetail = { screen = Screen.Detail(it) },
                )
                Tab.Library -> LibraryScreen(
                    dataStore = dataStore,
                    onOpenDetail = { screen = Screen.Detail(it) },
                )
                Tab.Plugins -> PluginsScreen(pluginManager, repoManager)
                Tab.Settings -> SettingsScreen(settings)
            }
            else -> {
                HomeScreen(
                    pluginManager = pluginManager,
                    onOpenDetail = { screen = Screen.Detail(it) },
                    onGoToPlugins = { screen = Screen.TabScreen(Tab.Plugins) },
                )
            }
        }

        // Overlays (Detail / Player) — render on top of the tab content.
        when (val s = screen) {
            is Screen.Detail -> DetailScreen(
                item = s.item,
                dataStore = dataStore,
                onBack = { screen = Screen.TabScreen(currentTab(screen) ?: Tab.Home) },
                onPlay = { api, data, title ->
                    screen = Screen.Player(api, data, title)
                },
            )
            is Screen.Player -> PlayerScreen(
                api = s.api,
                data = s.data,
                title = s.title,
                dataStore = dataStore,
                onClose = { screen = Screen.TabScreen(currentTab(screen) ?: Tab.Home) },
            )
            else -> Unit
        }

        // Bottom navigation — hide when a full-screen overlay is active.
        if (screen is Screen.TabScreen) {
            Column(Modifier.fillMaxSize()) {
                Box(Modifier.weight(1f).fillMaxWidth())
                NavigationBar(
                    containerColor = MaterialTheme.colorScheme.surface,
                    contentColor = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.height(64.dp),
                ) {
                    val current = (screen as? Screen.TabScreen)?.tab ?: Tab.Home
                    val tabs = listOf(
                        Triple(Tab.Home, Icons.Filled.Home, "Home"),
                        Triple(Tab.Search, Icons.Filled.Search, "Search"),
                        Triple(Tab.Library, Icons.Filled.VideoLibrary, "Library"),
                        Triple(Tab.Plugins, Icons.Filled.PlayArrow, "Plugins"),
                        Triple(Tab.Settings, Icons.Filled.Settings, "Settings"),
                    )
                    tabs.forEach { (tab, icon, label) ->
                        NavigationBarItem(
                            selected = current == tab,
                            onClick = { screen = Screen.TabScreen(tab) },
                            icon = { Icon(icon, contentDescription = label, modifier = Modifier.size(22.dp)) },
                            label = { Text(label) },
                        )
                    }
                }
            }
        }
    }
}

private fun currentTab(screen: Screen): Tab? = (screen as? Screen.TabScreen)?.tab

// Compose window state helper (avoid pulling additional imports).
@Composable
private fun rememberWindowState(): androidx.compose.ui.window.WindowState =
    androidx.compose.ui.window.rememberWindowState(
        width = 1400.dp,
        height = 900.dp,
    )

// ============================================================================
// Update notification UI
// ============================================================================

@Composable
private fun UpdateNotification(
    info: AppUpdateChecker.UpdateInfo,
    onDismiss: () -> Unit,
) {
    var showDetails by remember { mutableStateOf(false) }

    Card(
        modifier = Modifier
            .padding(16.dp)
            .widthIn(max = 360.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer,
        ),
    ) {
        Column(Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    Icons.Filled.SystemUpdate,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(24.dp),
                )
                Spacer(Modifier.width(12.dp))
                Column(Modifier.weight(1f)) {
                    Text(
                        text = "Update Available!",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                    )
                    Text(
                        text = "${info.currentVersion} → ${info.latestVersion}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                    )
                }
                IconButton(onClick = onDismiss) {
                    Icon(
                        Icons.Filled.Close,
                        contentDescription = "Dismiss",
                        tint = MaterialTheme.colorScheme.onPrimaryContainer,
                        modifier = Modifier.size(18.dp),
                    )
                }
            }
            Spacer(Modifier.height(8.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                TextButton(onClick = {
                    showDetails = !showDetails
                }) { Text(if (showDetails) "Hide details" else "View details") }
                if (info.downloadUrl != null) {
                    Button(onClick = {
                        runCatching {
                            if (java.awt.Desktop.isDesktopSupported()) {
                                java.awt.Desktop.getDesktop().browse(java.net.URI(info.downloadUrl))
                            }
                        }
                    }) { Text("Download") }
                } else if (info.releaseUrl != null) {
                    Button(onClick = {
                        runCatching {
                            if (java.awt.Desktop.isDesktopSupported()) {
                                java.awt.Desktop.getDesktop().browse(java.net.URI(info.releaseUrl))
                            }
                        }
                    }) { Text("Open") }
                }
            }
            if (showDetails && info.releaseNotes != null) {
                Spacer(Modifier.height(8.dp))
                Text(
                    text = info.releaseNotes.take(300) + if (info.releaseNotes.length > 300) "…" else "",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                )
            }
        }
    }
}

@Composable
private fun PluginUpdateToast(count: Int) {
    Card(
        modifier = Modifier
            .padding(bottom = 80.dp, end = 16.dp)
            .widthIn(max = 300.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant,
        ),
    ) {
        Row(
            Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                Icons.Filled.Refresh,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(20.dp),
            )
            Spacer(Modifier.width(8.dp))
            Text(
                text = "$count plugin${if (count > 1) "s" else ""} updated",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface,
            )
        }
    }
}

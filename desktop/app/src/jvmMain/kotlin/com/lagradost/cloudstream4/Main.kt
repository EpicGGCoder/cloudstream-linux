package com.lagradost.cloudstream4

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.VideoLibrary
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import com.lagradost.cloudstream3.MainAPI
import com.lagradost.cloudstream3.SearchResponse
import com.lagradost.cloudstream4.data.AppSettings
import com.lagradost.cloudstream4.network.WebViewResolverJvm
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
    val pluginManager = remember { PluginManager(dataDir) }
    val repoManager = remember { RepositoryManager(pluginManager) }

    WebViewResolverJvm.init()

    LaunchedEffect(Unit) {
        pluginManager.loadAllSavedPlugins()
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
                        AppShell(pluginManager, repoManager, settings)
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
                Tab.Library -> LibraryScreen()
                Tab.Plugins -> PluginsScreen(pluginManager, repoManager)
                Tab.Settings -> SettingsScreen(settings)
            }
            else -> {
                // While a detail/player overlay is open, show the previously
                // active tab underneath (so the back button returns to it).
                // The overlay covers it visually.
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
                onBack = { screen = Screen.TabScreen(currentTab(screen) ?: Tab.Home) },
                onPlay = { api, data, title ->
                    screen = Screen.Player(api, data, title)
                },
            )
            is Screen.Player -> PlayerScreen(
                api = s.api,
                data = s.data,
                title = s.title,
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

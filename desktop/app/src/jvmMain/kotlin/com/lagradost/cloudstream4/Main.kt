package com.lagradost.cloudstream4

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
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
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import com.lagradost.cloudstream4.data.AppSettings
import com.lagradost.cloudstream4.network.WebViewResolverJvm
import com.lagradost.cloudstream4.player.VlcjPlayer
import com.lagradost.cloudstream4.plugin.PluginManager
import com.lagradost.cloudstream4.repo.RepositoryManager
import com.lagradost.cloudstream4.ui.HomeScreen
import com.lagradost.cloudstream4.ui.LibraryScreen
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
    // Resolve the user data directory: ~/.cloudstream on Linux.
    val dataDir = File(System.getProperty("user.home"), ".cloudstream").apply { mkdirs() }
    log.info("CloudStream data directory: ${dataDir.absolutePath}")

    val settings = remember { AppSettings(dataDir) }
    val pluginManager = remember { PluginManager(dataDir) }
    val repoManager = remember { RepositoryManager(pluginManager) }

    // Initialize the WebView resolver (JavaFX). Must happen on AWT thread.
    WebViewResolverJvm.init()

    val scope = rememberCoroutineScope()

    // Load all previously-installed plugins on startup.
    LaunchedEffect(Unit) {
        pluginManager.loadAllSavedPlugins()
    }

    val state = rememberWindowState()

    Window(
        onCloseRequest = ::exitApplication,
        title = "CloudStream",
        // icon = painterResource("icon-linux.png"),
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
    // CloudStream signature deep-purple accent on near-black.
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
    )
    MaterialTheme(colorScheme = scheme, content = content)
}

private enum class Tab(val label: String) {
    Home("Home"),
    Search("Search"),
    Library("Library"),
    Plugins("Plugins"),
    Settings("Settings"),
}

@Composable
private fun AppShell(
    pluginManager: PluginManager,
    repoManager: RepositoryManager,
    settings: AppSettings,
) {
    var activeTab by remember { mutableStateOf(Tab.Home) }

    Column(Modifier.fillMaxSize()) {
        Box(Modifier.weight(1f).fillMaxWidth()) {
            when (activeTab) {
                Tab.Home -> HomeScreen(pluginManager)
                Tab.Search -> SearchScreen(pluginManager)
                Tab.Library -> LibraryScreen()
                Tab.Plugins -> PluginsScreen(pluginManager, repoManager)
                Tab.Settings -> SettingsScreen(settings)
            }
        }

        // Bottom navigation — sticky to the bottom of the window.
        NavigationBar(
            containerColor = MaterialTheme.colorScheme.surface,
            contentColor = MaterialTheme.colorScheme.primary,
            modifier = Modifier.height(64.dp),
        ) {
            val tabs = listOf(
                Triple(Tab.Home, Icons.Filled.Home, "Home"),
                Triple(Tab.Search, Icons.Filled.Search, "Search"),
                Triple(Tab.Library, Icons.Filled.VideoLibrary, "Library"),
                Triple(Tab.Plugins, Icons.Filled.PlayArrow, "Plugins"),
                Triple(Tab.Settings, Icons.Filled.Settings, "Settings"),
            )
            tabs.forEach { (tab, icon, label) ->
                NavigationBarItem(
                    selected = activeTab == tab,
                    onClick = { activeTab = tab },
                    icon = { Icon(icon, contentDescription = label, modifier = Modifier.size(22.dp)) },
                    label = { Text(label) },
                )
            }
        }
    }
}

// Compose window state helper (avoid pulling additional imports).
@Composable
private fun rememberWindowState(): androidx.compose.ui.window.WindowState =
    androidx.compose.ui.window.rememberWindowState(
        width = 1400.dp,
        height = 900.dp,
    )

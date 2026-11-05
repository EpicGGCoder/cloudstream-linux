package com.lagradost.cloudstream4.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.lagradost.cloudstream4.plugin.PluginManager
import com.lagradost.cloudstream4.repo.RepositoryManager
import kotlinx.coroutines.launch

@Composable
fun PluginsScreen(pluginManager: PluginManager, repoManager: RepositoryManager) {
    val installed by pluginManager.installed.collectAsState()
    val scope = rememberCoroutineScope()
    var repoUrl by remember { mutableStateOf("") }
    var fetchedRepo by remember { mutableStateOf<RepositoryManager.Repository?>(null) }
    var fetchedPlugins by remember { mutableStateOf<List<RepositoryManager.SitePlugin>>(emptyList()) }
    var error by remember { mutableStateOf<String?>(null) }
    var loading by remember { mutableStateOf(false) }

    Column(Modifier.fillMaxSize().padding(16.dp)) {
        Text("Repositories & Plugins", style = MaterialTheme.typography.headlineSmall)
        Spacer(Modifier.height(12.dp))

        Row(verticalAlignment = Alignment.CenterVertically) {
            OutlinedTextField(
                value = repoUrl,
                onValueChange = { repoUrl = it },
                label = { Text("Repository URL") },
                modifier = Modifier.weight(1f),
                placeholder = { Text("https://raw.githubusercontent.com/.../repo.json") },
            )
            Spacer(Modifier.height(0.dp))
            Button(
                onClick = {
                    scope.launch {
                        loading = true; error = null
                        try {
                            val (repo, plugins) = repoManager.fetchRepo(repoUrl)
                            fetchedRepo = repo
                            fetchedPlugins = plugins
                        } catch (e: Exception) {
                            error = e.message
                        }
                        loading = false
                    }
                },
                enabled = repoUrl.isNotBlank() && !loading,
                modifier = Modifier.padding(start = 8.dp),
            ) {
                Text(if (loading) "Fetching…" else "Fetch")
            }
        }

        error?.let {
            Text(it, color = MaterialTheme.colorScheme.error, modifier = Modifier.padding(top = 8.dp))
        }

        Spacer(Modifier.height(20.dp))

        Text("Installed plugins (${installed.size})", style = MaterialTheme.typography.titleMedium)
        Spacer(Modifier.height(8.dp))
        LazyColumn(Modifier.weight(1f).fillMaxWidth()) {
            items(installed) { plugin ->
                Card(Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
                    Column(Modifier.padding(12.dp)) {
                        Text(plugin.displayName, style = MaterialTheme.typography.bodyLarge)
                        Text(
                            "Internal: ${plugin.internalName} • Version ${plugin.version} • ${
                                if (plugin.isOnline) "Online" else "Local"
                            }",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }
        }

        if (fetchedRepo != null) {
            Spacer(Modifier.height(16.dp))
            Text(
                "Plugins in ${fetchedRepo!!.name} (${fetchedPlugins.size})",
                style = MaterialTheme.typography.titleMedium,
            )
            Spacer(Modifier.height(8.dp))
            LazyColumn(Modifier.fillMaxWidth().height(220.dp)) {
                items(fetchedPlugins) { plugin ->
                    Card(Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
                        Row(
                            Modifier.padding(12.dp).fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Column(Modifier.weight(1f)) {
                                Text(plugin.name, style = MaterialTheme.typography.bodyMedium)
                                Text(
                                    "${plugin.language ?: "?"} • v${plugin.version} • ${
                                        plugin.tvTypes?.joinToString(", ") ?: ""
                                    }",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            }
                            Button(
                                onClick = {
                                    scope.launch {
                                        val ok = repoManager.downloadAndInstall(plugin)
                                        if (!ok) error = "Failed to install ${plugin.name}"
                                    }
                                },
                            ) { Text("Install") }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun SettingsScreen(settings: com.lagradost.cloudstream4.data.AppSettings) {
    val data by remember { mutableStateOf(settings.data) }
    Column(Modifier.fillMaxSize().padding(16.dp)) {
        Text("Settings", style = MaterialTheme.typography.headlineSmall)
        Spacer(Modifier.height(12.dp))
        Text("Theme: ${data.theme}", style = MaterialTheme.typography.bodyMedium)
        Text("Locale: ${data.locale}", style = MaterialTheme.typography.bodyMedium)
        Text("Player speed: ${data.playerSpeed}x", style = MaterialTheme.typography.bodyMedium)
        Text("Auto-update plugins: ${data.autoUpdatePlugins}", style = MaterialTheme.typography.bodyMedium)
        Spacer(Modifier.height(20.dp))
        Text(
            "CloudStream Linux Port — Phase 1 foundation. " +
                "Real plugin support via dex2jar is wired in PluginsScreen. " +
                "Build with ./gradlew :app:packageAppImage to produce the .AppImage.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

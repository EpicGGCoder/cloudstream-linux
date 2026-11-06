package com.lagradost.cloudstream4.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.OpenInNew
import androidx.compose.material.icons.filled.CloudDownload
import androidx.compose.material.icons.filled.Extension
import androidx.compose.material.icons.filled.Info
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.lagradost.cloudstream4.plugin.PluginManager
import com.lagradost.cloudstream4.repo.RepositoryManager
import kotlinx.coroutines.launch
import java.awt.Desktop
import java.net.URI

private val PREBUILT_REPOS = listOf(
    PrebuiltRepo(
        name = "CNCVerse",
        url = "https://raw.githubusercontent.com/CNCVerse/cloudstream/main/repo.json",
        description = "Largest curated CS3 plugin collection.",
    ),
    PrebuiltRepo(
        name = "recloudstream/extensions",
        url = "https://raw.githubusercontent.com/recloudstream/extensions/main/repo.json",
        description = "Official reCloudStream repository.",
    ),
    PrebuiltRepo(
        name = "phisher98/plugins",
        url = "https://raw.githubusercontent.com/phisher98/plugins/main/repo.json",
        description = "phisher98 maintained plugin set.",
    ),
    PrebuiltRepo(
        name = "Luna712/LunaStream",
        url = "https://raw.githubusercontent.com/Luna712/LunaStream/main/repo.json",
        description = "Luna712 curated plugins.",
    ),
    PrebuiltRepo(
        name = "OctoView/plugins",
        url = "https://raw.githubusercontent.com/OctoView/plugins/main/repo.json",
        description = "OctoView curated plugins.",
    ),
    PrebuiltRepo(
        name = "KeyWorker/KeyPlugins",
        url = "https://raw.githubusercontent.com/KeyWorker/KeyPlugins/main/repo.json",
        description = "KeyWorker maintained plugins.",
    ),
    PrebuiltRepo(
        name = "Hexated/cloudstream",
        url = "https://raw.githubusercontent.com/Hexated/cloudstream-extensions-hexated/main/repo.json",
        description = "Hexated's popular extensions fork.",
    ),
    PrebuiltRepo(
        name = "Margalit/cloudstream",
        url = "https://raw.githubusercontent.com/Margalit/KnightCrawler/main/repo.json",
        description = "Margalit KnightCrawler.",
    ),
    PrebuiltRepo(
        name = "soup3000/plugins",
        url = "https://raw.githubusercontent.com/soup3000/plugins/main/repo.json",
        description = "soup3000 maintained plugins.",
    ),
    PrebuiltRepo(
        name = "sauravsidoshiya/plugins",
        url = "https://raw.githubusercontent.com/sauravsidoshiya/plugins/main/repo.json",
        description = "sauravsidoshiya plugins.",
    ),
)

private data class PrebuiltRepo(
    val name: String,
    val url: String,
    val description: String,
)

@Composable
fun PluginsScreen(
    pluginManager: PluginManager,
    repoManager: RepositoryManager,
) {
    val installed by pluginManager.installed.collectAsState()
    val scope = rememberCoroutineScope()
    var repoUrl by remember { mutableStateOf("") }
    var fetchedRepo by remember { mutableStateOf<RepositoryManager.Repository?>(null) }
    var fetchedPlugins by remember { mutableStateOf<List<RepositoryManager.SitePlugin>>(emptyList()) }
    var error by remember { mutableStateOf<String?>(null) }
    var loading by remember { mutableStateOf(false) }
    var detailFor by remember { mutableStateOf<PluginManager.InstalledPlugin?>(null) }

    LazyColumn(
        Modifier.fillMaxSize(),
        contentPadding = PaddingValues(bottom = 32.dp),
    ) {
        item {
            Column(Modifier.padding(24.dp, 24.dp, 24.dp, 8.dp)) {
                Text(
                    text = "Repositories & Plugins",
                    style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold),
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    text = "Install .cs3 plugin repositories. Loaded ${installed.size} plugins " +
                        "(${com.lagradost.cloudstream3.APIHolder.allProviders.size} providers).",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
        // Add repo URL input + Fetch button.
        item {
            Row(
                Modifier.padding(horizontal = 24.dp).fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                OutlinedTextField(
                    value = repoUrl,
                    onValueChange = { repoUrl = it },
                    label = { Text("Repository URL") },
                    placeholder = { Text("https://raw.githubusercontent.com/.../repo.json") },
                    modifier = Modifier.weight(1f),
                    singleLine = true,
                )
                Spacer(Modifier.width(8.dp))
                Button(
                    onClick = {
                        scope.launch {
                            loading = true; error = null
                            try {
                                val (repo, plugins) = repoManager.fetchRepo(repoUrl)
                                fetchedRepo = repo
                                fetchedPlugins = plugins
                            } catch (e: Exception) {
                                error = e.message ?: e::class.simpleName
                            }
                            loading = false
                        }
                    },
                    enabled = repoUrl.isNotBlank() && !loading,
                ) { Text(if (loading) "Fetching…" else "Fetch") }
            }
            error?.let {
                Text(
                    text = it,
                    color = MaterialTheme.colorScheme.error,
                    modifier = Modifier.padding(horizontal = 24.dp, vertical = 8.dp),
                )
            }
        }
        // Pre-built repos.
        item {
            Text(
                text = "Popular repositories",
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
                modifier = Modifier.padding(horizontal = 24.dp, vertical = 12.dp),
            )
            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp)
                    .height(220.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                items(PREBUILT_REPOS, key = { it.url }) { repo ->
                    PrebuiltRepoCard(
                        repo = repo,
                        onAdd = { repoUrl = repo.url },
                        onOpen = {
                            runCatching {
                                if (Desktop.isDesktopSupported()) {
                                    Desktop.getDesktop().browse(URI.create(repo.url))
                                }
                            }
                        },
                    )
                }
            }
        }
        // Installed plugins.
        item {
            Spacer(Modifier.height(20.dp))
            Text(
                text = "Installed plugins (${installed.size})",
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
                modifier = Modifier.padding(horizontal = 24.dp, vertical = 8.dp),
            )
        }
        items(installed, key = { it.filePath }) { plugin ->
            InstalledPluginCard(
                plugin = plugin,
                onClick = { detailFor = plugin },
            )
        }
        // Fetched plugins (after a Fetch).
        if (fetchedRepo != null) {
            item {
                Spacer(Modifier.height(20.dp))
                Text(
                    text = "Plugins in ${fetchedRepo!!.name} (${fetchedPlugins.size})",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
                    modifier = Modifier.padding(horizontal = 24.dp, vertical = 8.dp),
                )
            }
            items(fetchedPlugins, key = { it.url }) { plugin ->
                FetchedPluginCard(
                    plugin = plugin,
                    onInstall = {
                        scope.launch {
                            val ok = repoManager.downloadAndInstall(plugin)
                            if (!ok) error = "Failed to install ${plugin.name}"
                        }
                    },
                )
            }
        }
    }

    // Plugin detail dialog.
    detailFor?.let { plugin ->
        AlertDialog(
            onDismissRequest = { detailFor = null },
            title = { Text(plugin.displayName) },
            text = {
                Column {
                    PluginDetailRow("Internal name", plugin.internalName)
                    PluginDetailRow("Version", plugin.version.toString())
                    PluginDetailRow("Source", if (plugin.isOnline) "Online repo" else "Local file")
                    PluginDetailRow("File path", plugin.filePath)
                    plugin.sourceUrl?.let { PluginDetailRow("URL", it) }
                }
            },
            confirmButton = {
                Row {
                    plugin.sourceUrl?.let {
                        TextButton(onClick = {
                            runCatching {
                                if (Desktop.isDesktopSupported()) {
                                    Desktop.getDesktop().browse(URI.create(it))
                                }
                            }
                        }) { Text("Open URL") }
                    }
                    Spacer(Modifier.width(8.dp))
                    TextButton(onClick = {
                        pluginManager.uninstall(plugin.filePath)
                        detailFor = null
                    }) { Text("Uninstall", color = MaterialTheme.colorScheme.error) }
                    Spacer(Modifier.width(8.dp))
                    TextButton(onClick = { detailFor = null }) { Text("Close") }
                }
            },
        )
    }
}

@Composable
private fun PluginDetailRow(label: String, value: String) {
    Row(Modifier.padding(vertical = 4.dp)) {
        Text(
            text = "$label: ",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.width(100.dp),
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurface,
        )
    }
}

@Composable
private fun PrebuiltRepoCard(
    repo: PrebuiltRepo,
    onAdd: () -> Unit,
    onOpen: () -> Unit,
) {
    Card(
        Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
    ) {
        Row(
            Modifier.padding(14.dp).fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                Modifier
                    .size(36.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)),
                contentAlignment = Alignment.Center,
            ) {
                Icon(Icons.Filled.CloudDownload, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
            }
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(
                    text = repo.name,
                    style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.SemiBold),
                )
                Text(
                    text = repo.description,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                )
            }
            IconButton(onClick = onOpen) {
                Icon(Icons.AutoMirrored.Filled.OpenInNew, contentDescription = "Open URL", tint = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Button(
                onClick = onAdd,
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary,
                ),
            ) { Text("Use") }
        }
    }
}

@Composable
private fun InstalledPluginCard(
    plugin: PluginManager.InstalledPlugin,
    onClick: () -> Unit,
) {
    Card(
        Modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp, vertical = 4.dp)
            .clickable(onClick = onClick),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
    ) {
        Row(
            Modifier.padding(14.dp).fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                Modifier
                    .size(36.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f)),
                contentAlignment = Alignment.Center,
            ) {
                Icon(Icons.Filled.Extension, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(
                    text = plugin.displayName,
                    style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.SemiBold),
                )
                Text(
                    text = "v${plugin.version} • ${if (plugin.isOnline) "Online" else "Local"} • ${plugin.sourceUrl ?: "—"}",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                )
            }
            if (plugin.sourceUrl != null) {
                IconButton(onClick = onClick) {
                    Icon(Icons.Filled.Info, contentDescription = "Details", tint = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }
    }
}

@Composable
private fun FetchedPluginCard(
    plugin: RepositoryManager.SitePlugin,
    onInstall: () -> Unit,
) {
    Card(
        Modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp, vertical = 4.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
    ) {
        Row(
            Modifier.padding(14.dp).fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(Modifier.weight(1f)) {
                Text(
                    text = plugin.name,
                    style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
                )
                Text(
                    text = "${plugin.language ?: "?"} • v${plugin.version} • " +
                        (plugin.tvTypes?.joinToString(", ") ?: "—"),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Button(onClick = onInstall) { Text("Install") }
        }
    }
}

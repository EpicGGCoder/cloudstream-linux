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
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.OpenInNew
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CloudDownload
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Extension
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
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

// Correct, verified repo URLs from the official cs-repos database
private val PREBUILT_REPOS = listOf(
    PrebuiltRepo(
        name = "CNCVerse (42 plugins)",
        url = "https://raw.githubusercontent.com/NivinCNC/CNCVerse-Cloud-Stream-Extension/refs/heads/builds/CNC.json",
        description = "Largest curated CS3 plugin collection — movies, TV, live sports, anime.",
    ),
    PrebuiltRepo(
        name = "recloudstream/extensions (official)",
        url = "https://raw.githubusercontent.com/recloudstream/extensions/master/repo.json",
        description = "Official reCloudStream repository — Dailymotion, Internet Archive, YouTube, Twitch, Invidious.",
    ),
    PrebuiltRepo(
        name = "phisher98/plugins",
        url = "https://raw.githubusercontent.com/phisher98/cloudstream-extensions-phisher/refs/heads/builds/repo.json",
        description = "phisher98 maintained plugin set — AllMovieLand, Anichi, Anikage, etc.",
    ),
    PrebuiltRepo(
        name = "Luna712 (anime)",
        url = "https://raw.githubusercontent.com/Luna712/Luna712-CloudStream-Extensions/master/repo.json",
        description = "Luna712 curated anime plugins.",
    ),
    PrebuiltRepo(
        name = "CakesTwix (Ukrainian)",
        url = "https://raw.githubusercontent.com/CakesTwix/cloudstream-extensions-uk/master/repo.json",
        description = "Ukrainian anime/movie providers.",
    ),
    PrebuiltRepo(
        name = "Gian-Fr (Italian)",
        url = "https://raw.githubusercontent.com/Gian-Fr/ItalianProvider/builds/repo.json",
        description = "Italian movie/TV providers.",
    ),
    PrebuiltRepo(
        name = "Bnyro (German)",
        url = "https://raw.githubusercontent.com/Bnyro/GermanProviders/refs/heads/master/repo.json",
        description = "German providers — ARD, Arte, Aniworld, C3TV.",
    ),
    PrebuiltRepo(
        name = "doGior (Italian)",
        url = "https://raw.githubusercontent.com/doGior/doGiorsHadEnough/refs/heads/builds/repo.json",
        description = "Italian — AltaDefinizione, AnimeUnity, AnimeWorld, Arte, CalcioStreaming.",
    ),
    PrebuiltRepo(
        name = "redowan99 (BDIX)",
        url = "https://raw.githubusercontent.com/redowan99/Redowan-CloudStream/master/repo.json",
        description = "Bangladeshi BDIX providers.",
    ),
    PrebuiltRepo(
        name = "KSHITIJ8473/raghav",
        url = "https://raw.githubusercontent.com/KSHITIJ8473/raghav/builds/repo.json",
        description = "AniChan, Anidap, AnimeInWeb, AnimeTH, etc.",
    ),
    PrebuiltRepo(
        name = "aslafy-z (French)",
        url = "https://github.com/aslafy-z/cloudstream-plugins/releases/download/catalog-stable/repo.json",
        description = "French — ARTE, FranceTV, M6Plus, Novo19, RMCPlus.",
    ),
    PrebuiltRepo(
        name = "Eclipsia",
        url = "https://cs.eclipsia.dpdns.org/repo.json",
        description = "Eclipsia multi-language — Eclipsia, EclipsiaAnime, EclipsiaBDIX, LiveSports, NetMirror.",
    ),
    PrebuiltRepo(
        name = "saimuelbr (Portuguese)",
        url = "https://raw.githubusercontent.com/saimuelbr/saimuelrepo/refs/heads/main/builds/repo.json",
        description = "Portuguese — AnimesCloud, MegaFlix, NetCine, PobreFlix, TopFilmes.",
    ),
    PrebuiltRepo(
        name = "mouradchaouche (French)",
        url = "https://raw.githubusercontent.com/mouradchaouche/cloudstream-frenchrepo/main/repo.json",
        description = "French — Wiflix, FrenchAnime, FsMirrorLol, Coflix, 1Jour1Film.",
    ),
    PrebuiltRepo(
        name = "yorik100",
        url = "https://raw.githubusercontent.com/yorik100/Cloudstream/refs/heads/builds/repo.json",
        description = "Frembed, Xalaflix, AfterDark.",
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

    // Track install state per plugin URL — "idle", "installing", "installed", "failed"
    val installStates = remember { mutableStateMapOf<String, InstallState>() }

    // Check which fetched plugins are already installed
    val installedUrls by remember(installed) {
        derivedStateOf { installed.map { it.sourceUrl }.toSet() }
    }

    // Auto-scroll to fetched plugins when they appear
    val listState = rememberLazyListState()
    LaunchedEffect(fetchedRepo) {
        if (fetchedRepo != null && fetchedPlugins.isNotEmpty()) {
            val targetIndex = 4 + installed.size + 1
            kotlinx.coroutines.delay(300) // wait for list to update
            listState.animateScrollToItem(targetIndex)
        }
    }

    fun doFetch(url: String) {
        if (url.isBlank()) return
        scope.launch {
            loading = true
            error = null
            try {
                val (repo, plugins) = repoManager.fetchRepo(url)
                fetchedRepo = repo
                fetchedPlugins = plugins
            } catch (e: Exception) {
                error = e.message ?: e::class.simpleName ?: "Unknown error"
            }
            loading = false
        }
    }

    LazyColumn(
        Modifier.fillMaxSize(),
        state = listState,
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
                    text = "Install .cs3 plugin repositories. ${installed.size} plugins loaded, " +
                        "${com.lagradost.cloudstream3.APIHolder.allProviders.size} providers active.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }

        // Repo URL input + Fetch button
        item {
            Row(
                Modifier.padding(horizontal = 24.dp).fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                OutlinedTextField(
                    value = repoUrl,
                    onValueChange = { repoUrl = it },
                    label = { Text("Repository URL or shortcode") },
                    placeholder = { Text("e.g. cncv or full repo.json URL") },
                    modifier = Modifier.weight(1f),
                    singleLine = true,
                )
                Spacer(Modifier.width(8.dp))
                Button(
                    onClick = { doFetch(repoUrl) },
                    enabled = repoUrl.isNotBlank() && !loading,
                ) {
                    if (loading) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(16.dp),
                            strokeWidth = 2.dp,
                            color = MaterialTheme.colorScheme.onPrimary,
                        )
                    } else {
                        Text("Fetch")
                    }
                }
            }
            error?.let {
                Text(
                    text = it,
                    color = MaterialTheme.colorScheme.error,
                    modifier = Modifier.padding(horizontal = 24.dp, vertical = 8.dp),
                )
            }
        }

        // Pre-built repos — "Use" auto-fetches immediately
        item {
            Text(
                text = "Popular repositories",
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
                modifier = Modifier.padding(horizontal = 24.dp, vertical = 12.dp),
            )
        }
        items(PREBUILT_REPOS, key = { it.url }) { repo ->
            PrebuiltRepoCard(
                repo = repo,
                onUse = {
                    repoUrl = repo.url
                    doFetch(repo.url)
                },
                onOpen = {
                    runCatching {
                        if (Desktop.isDesktopSupported()) {
                            Desktop.getDesktop().browse(URI.create(repo.url))
                        }
                    }
                },
            )
        }

        // Installed plugins
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

        // Fetched plugins (after a Fetch)
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
                val state = installStates[plugin.url] ?: InstallState.IDLE
                val isAlreadyInstalled = plugin.url in installedUrls

                FetchedPluginCard(
                    plugin = plugin,
                    installState = if (isAlreadyInstalled && state != InstallState.INSTALLING) InstallState.INSTALLED else state,
                    onInstall = {
                        installStates[plugin.url] = InstallState.INSTALLING
                        scope.launch {
                            val ok = repoManager.downloadAndInstall(plugin)
                            installStates[plugin.url] = if (ok) InstallState.INSTALLED else InstallState.FAILED
                            if (!ok) error = "Failed to install ${plugin.name}"
                        }
                    },
                )
            }
        }
    }

    // Plugin detail dialog
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

enum class InstallState { IDLE, INSTALLING, INSTALLED, FAILED }

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
    onUse: () -> Unit,
    onOpen: () -> Unit,
) {
    Card(
        Modifier.fillMaxWidth().padding(horizontal = 24.dp, vertical = 4.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
    ) {
        Row(
            Modifier.padding(14.dp).fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                Modifier.size(36.dp).clip(RoundedCornerShape(8.dp))
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
                    maxLines = 2,
                    overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis,
                )
            }
            IconButton(onClick = onOpen) {
                Icon(Icons.AutoMirrored.Filled.OpenInNew, contentDescription = "Open URL", tint = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Button(
                onClick = onUse,
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
        Modifier.fillMaxWidth().padding(horizontal = 24.dp, vertical = 4.dp).clickable(onClick = onClick),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
    ) {
        Row(
            Modifier.padding(14.dp).fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                Modifier.size(36.dp).clip(RoundedCornerShape(8.dp))
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
                    overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis,
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
    installState: InstallState,
    onInstall: () -> Unit,
) {
    Card(
        Modifier.fillMaxWidth().padding(horizontal = 24.dp, vertical = 4.dp),
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
                    maxLines = 1,
                    overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis,
                )
            }
            when (installState) {
                InstallState.IDLE -> Button(onClick = onInstall) {
                    Icon(Icons.Filled.Download, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(6.dp))
                    Text("Install")
                }
                InstallState.INSTALLING -> Button(onClick = {}, enabled = false) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(16.dp),
                        strokeWidth = 2.dp,
                        color = MaterialTheme.colorScheme.onPrimary,
                    )
                    Spacer(Modifier.width(6.dp))
                    Text("Installing…")
                }
                InstallState.INSTALLED -> Button(onClick = {}, enabled = false,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.3f),
                    )
                ) {
                    Icon(Icons.Filled.Check, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(6.dp))
                    Text("Installed")
                }
                InstallState.FAILED -> Button(onClick = onInstall,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.error,
                    )
                ) {
                    Icon(Icons.Filled.Refresh, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(6.dp))
                    Text("Retry")
                }
            }
        }
    }
}

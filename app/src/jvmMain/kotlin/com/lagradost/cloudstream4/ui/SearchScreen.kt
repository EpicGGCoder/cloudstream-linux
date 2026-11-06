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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.lagradost.cloudstream3.APIHolder
import com.lagradost.cloudstream3.MainAPI
import com.lagradost.cloudstream3.SearchResponse
import com.lagradost.cloudstream4.data.AppSettings
import com.lagradost.cloudstream4.plugin.PluginManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import org.slf4j.LoggerFactory

private val log = LoggerFactory.getLogger("SearchScreen")

@Composable
fun SearchScreen(
    pluginManager: PluginManager,
    settings: AppSettings,
    onOpenDetail: (SearchResponse) -> Unit,
) {
    var query by remember { mutableStateOf("") }
    var loading by remember { mutableStateOf(false) }
    var results by remember { mutableStateOf<List<ProviderResults>>(emptyList()) }
    var error by remember { mutableStateOf<String?>(null) }

    // Debounced search — LaunchedEffect re-runs (and cancels prior) every time query changes.
    LaunchedEffect(query) {
        if (query.isBlank()) {
            results = emptyList()
            loading = false
            error = null
            return@LaunchedEffect
        }
        delay(400)
        loading = true
        error = null
        try {
            val rs = withContext(Dispatchers.IO) { runSearch(query) }
            results = rs
            // Persist into history (most recent first, deduped, capped at 10).
            settings.update { data ->
                val updated = (listOf(query) + data.searchHistory.filter { it != query }).take(10)
                data.copy(searchHistory = updated)
            }
        } catch (e: Exception) {
            log.error("Search failed", e)
            error = e.message ?: e::class.simpleName
        }
        loading = false
    }

    val focus = remember { FocusRequester() }
    LaunchedEffect(Unit) { runCatching { focus.requestFocus() } }

    Column(Modifier.fillMaxSize()) {
        // Sticky search input.
        Box(
            Modifier
                .fillMaxWidth()
                .background(MaterialTheme.colorScheme.surface)
                .padding(horizontal = 24.dp, vertical = 16.dp),
        ) {
            OutlinedTextField(
                value = query,
                onValueChange = { query = it },
                placeholder = { Text("Search movies, shows, anime…") },
                leadingIcon = { Icon(Icons.Filled.Search, contentDescription = null) },
                trailingIcon = {
                    if (query.isNotEmpty()) {
                        IconButton(onClick = { query = "" }) {
                            Icon(Icons.Filled.Close, contentDescription = "Clear search")
                        }
                    }
                },
                singleLine = true,
                modifier = Modifier
                    .fillMaxWidth()
                    .focusRequester(focus),
                colors = TextFieldDefaults.colors(
                    focusedContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                    unfocusedContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f),
                    focusedIndicatorColor = MaterialTheme.colorScheme.primary,
                    unfocusedIndicatorColor = Color.Transparent,
                ),
                shape = RoundedCornerShape(14.dp),
            )
        }

        // Body.
        when {
            query.isBlank() -> SearchEmptyState(settings = settings, onPick = { query = it })
            loading -> SearchLoadingGrid()
            error != null -> ErrorState(
                title = "Search failed",
                body = error!!,
                onRetry = { loading = true },
            )
            results.isEmpty() -> EmptyState(
                title = "No results",
                body = "Try a different title or install more plugins for wider coverage.",
            )
            else -> SearchResultsList(results = results, onOpenDetail = onOpenDetail)
        }
    }
}

@Composable
private fun SearchResultsList(
    results: List<ProviderResults>,
    onOpenDetail: (SearchResponse) -> Unit,
) {
    LazyColumn(
        Modifier.fillMaxSize(),
        contentPadding = PaddingValues(bottom = 32.dp),
    ) {
        results.forEach { section ->
            item(key = "header-${section.providerName}") {
                Row(
                    Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 24.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    Text(
                        text = section.providerName,
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
                        color = MaterialTheme.colorScheme.onBackground,
                    )
                    Text(
                        text = "${section.items.size}",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
            item(key = "row-${section.providerName}") {
                LazyRow(
                    contentPadding = PaddingValues(horizontal = 24.dp),
                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                    modifier = Modifier.padding(bottom = 16.dp),
                ) {
                    items(section.items, key = { it.url + it.apiName }) { item ->
                        PosterCard(item = item, onClick = { onOpenDetail(item) })
                    }
                }
            }
        }
    }
}

@Composable
private fun SearchLoadingGrid() {
    LazyVerticalGrid(
        columns = GridCells.Adaptive(minSize = 160.dp),
        contentPadding = PaddingValues(horizontal = 24.dp, vertical = 16.dp),
        horizontalArrangement = Arrangement.spacedBy(16.dp),
        verticalArrangement = Arrangement.spacedBy(20.dp),
    ) {
        items(15) { PosterSkeleton() }
    }
}

@Composable
private fun SearchEmptyState(
    settings: AppSettings,
    onPick: (String) -> Unit,
) {
    val history = remember { settings.data.searchHistory }
    if (history.isEmpty()) {
        EmptyState(
            title = "Find what to watch",
            body = "Search for movies, shows, anime, Asian dramas, and live streams across all your installed providers.",
        )
        return
    }
    Column(Modifier.fillMaxSize().padding(24.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Filled.History, contentDescription = null, modifier = Modifier.size(18.dp))
            Spacer(Modifier.width(8.dp))
            Text(
                text = "Recent searches",
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
                color = MaterialTheme.colorScheme.onBackground,
            )
        }
        Spacer(Modifier.height(12.dp))
        LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            items(history) { q ->
                Box(
                    Modifier
                        .clip(RoundedCornerShape(50))
                        .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                        .clickable { onPick(q) }
                        .padding(horizontal = 14.dp, vertical = 8.dp),
                ) {
                    Text(
                        text = q,
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.onSurface,
                    )
                }
            }
        }
    }
}

data class ProviderResults(
    val providerName: String,
    val items: List<SearchResponse>,
)

private suspend fun runSearch(query: String): List<ProviderResults> = coroutineScope {
    val providers = APIHolder.allProviders.withLock { APIHolder.allProviders.toList() }
    log.info("Searching '$query' across ${providers.size} providers")
    val deferreds = providers.map { api ->
        async {
            try {
                val list = withContext(Dispatchers.IO) { api.search(query) }
                if (!list.isNullOrEmpty()) ProviderResults(api.name, list) else null
            } catch (e: Exception) {
                log.warn("Provider ${api.name} search failed: ${e.message}")
                null
            }
        }
    }
    val results = deferreds.awaitAll().filterNotNull()
    results.sortedByDescending { it.items.size }
}

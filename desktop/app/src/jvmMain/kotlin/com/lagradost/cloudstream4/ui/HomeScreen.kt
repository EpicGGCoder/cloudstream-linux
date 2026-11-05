package com.lagradost.cloudstream4.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import com.lagradost.cloudstream3.APIHolder
import com.lagradost.cloudstream3.MainPageRequest
import com.lagradost.cloudstream3.MovieSearchResponse
import com.lagradost.cloudstream3.SearchResponse
import com.lagradost.cloudstream3.TvSeriesSearchResponse
import com.lagradost.cloudstream4.plugin.PluginManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.slf4j.LoggerFactory

/**
 * Home screen — renders the aggregated feed from all loaded providers'
 * getMainPage() responses. Real poster images load via Coil 3.
 *
 * This is what populates when the user installs plugins from the Plugins tab.
 */
@Composable
fun HomeScreen(pluginManager: PluginManager) {
    var loading by remember { mutableStateOf(true) }
    var rows by remember { mutableStateOf<List<HomeRow>>(emptyList()) }
    var error by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(Unit) {
        loading = true
        error = null
        try {
            rows = withContext(Dispatchers.IO) { loadHomeFeed() }
        } catch (e: Exception) {
            error = e.message
        }
        loading = false
    }

    when {
        loading -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
        }

        error != null -> Column(Modifier.fillMaxSize().padding(32.dp)) {
            Text("Failed to load home feed", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.error)
            Spacer(Modifier.height(8.dp))
            Text(error!!, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }

        rows.isEmpty() -> Column(Modifier.fillMaxSize().padding(32.dp)) {
            Text("No providers loaded.", style = MaterialTheme.typography.titleMedium)
            Spacer(Modifier.height(8.dp))
            Text(
                "Install plugins from the Plugins tab to populate your home feed.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }

        else -> LazyColumn(Modifier.fillMaxSize()) {
            items(rows) { row ->
                Text(
                    row.title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.padding(start = 16.dp, top = 16.dp, bottom = 8.dp),
                )
                LazyRow(
                    contentPadding = PaddingValues(horizontal = 16.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier.padding(bottom = 8.dp),
                ) {
                    items(row.items) { item -> PosterCard(item) }
                }
            }
        }
    }
}

private data class HomeRow(val title: String, val providerName: String, val items: List<SearchResponse>)

private suspend fun loadHomeFeed(): List<HomeRow> = withContext(Dispatchers.IO) {
    val log = LoggerFactory.getLogger("HomeScreen")
    val rows = mutableListOf<HomeRow>()
    val providers = APIHolder.allProviders.filter { it.hasMainPage }
    log.info("Loading home feed from ${providers.size} providers")
    for (api in providers) {
        for (section in api.mainPage) {
            try {
                val resp = api.getMainPage(
                    page = 1,
                    request = MainPageRequest(section.name, section.data, section.horizontalImages),
                )
                if (resp != null) {
                    for (list in resp.items) {
                        rows.add(HomeRow(list.name, api.name, list.list))
                    }
                    log.info("Provider ${api.name}: ${resp.items.sumOf { it.list.size }} items across ${resp.items.size} lists")
                }
            } catch (e: Exception) {
                log.error("Provider ${api.name} home page failed", e)
            }
        }
    }
    rows
}

private fun SearchResponse.yearOrUnknown(): String? = when (this) {
    is MovieSearchResponse -> year?.toString()
    is TvSeriesSearchResponse -> year?.toString()
    else -> null
}

@Composable
private fun PosterCard(item: SearchResponse) {
    val year = item.yearOrUnknown()
    Column(modifier = Modifier.width(140.dp)) {
        Card(
            modifier = Modifier
                .width(140.dp)
                .height(210.dp)
                .clip(RoundedCornerShape(8.dp))
                .clickable { /* TODO: open detail — wired in Phase 2 */ },
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
        ) {
            if (item.posterUrl != null) {
                AsyncImage(
                    model = item.posterUrl,
                    contentDescription = item.name,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize(),
                )
            } else {
                Box(
                    Modifier.fillMaxSize().background(
                        Brush.verticalGradient(
                            listOf(
                                MaterialTheme.colorScheme.primary.copy(alpha = 0.4f),
                                MaterialTheme.colorScheme.surfaceVariant,
                            )
                        )
                    ),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(item.name.take(2), color = Color.White, fontWeight = FontWeight.Bold)
                }
            }
        }
        Spacer(Modifier.height(6.dp))
        Text(item.name, style = MaterialTheme.typography.bodySmall, maxLines = 1, overflow = TextOverflow.Ellipsis)
        if (year != null) {
            Text(year, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

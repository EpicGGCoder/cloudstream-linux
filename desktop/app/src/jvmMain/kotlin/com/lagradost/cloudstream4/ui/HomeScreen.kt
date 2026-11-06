package com.lagradost.cloudstream4.ui

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
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
import androidx.compose.ui.unit.sp
import coil3.compose.AsyncImage
import com.lagradost.cloudstream3.APIHolder
import com.lagradost.cloudstream3.MainPageRequest
import com.lagradost.cloudstream3.SearchResponse
import com.lagradost.cloudstream4.plugin.PluginManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import org.slf4j.LoggerFactory

private val log = LoggerFactory.getLogger("HomeScreen")

@Composable
fun HomeScreen(
    pluginManager: PluginManager,
    onOpenDetail: (SearchResponse) -> Unit,
    onGoToPlugins: () -> Unit,
) {
    var loading by remember { mutableStateOf(true) }
    var rows by remember { mutableStateOf<List<HomeRow>>(emptyList()) }
    var error by remember { mutableStateOf<String?>(null) }

    // Reload when provider count changes (e.g. user installs a plugin).
    val providerCount = pluginManager.installed.value.size
    LaunchedEffect(providerCount) {
        loading = true
        error = null
        try {
            rows = withContext(Dispatchers.IO) { loadHomeFeed() }
        } catch (e: Exception) {
            log.error("Home feed failed", e)
            error = e.message ?: e::class.simpleName
        }
        loading = false
    }

    when {
        loading -> HomeSkeleton()
        error != null -> ErrorState(
            title = "Failed to load home feed",
            body = error!!,
            onRetry = { loading = true },
        )
        rows.isEmpty() -> EmptyState(
            title = "No providers loaded",
            body = "Install plugins from the Plugins tab to populate your home feed.",
            action = {
                OutlinedButton(onClick = onGoToPlugins) { Text("Open Plugins") }
            },
        )
        else -> HomeContent(rows = rows, onOpenDetail = onOpenDetail)
    }
}

@Composable
private fun HomeContent(
    rows: List<HomeRow>,
    onOpenDetail: (SearchResponse) -> Unit,
) {
    val heroItems = remember(rows) {
        rows.firstOrNull()?.items?.take(5) ?: emptyList()
    }
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(bottom = 32.dp),
    ) {
        if (heroItems.isNotEmpty()) {
            item(key = "hero") {
                HeroCarousel(items = heroItems, onOpenDetail = onOpenDetail)
                Spacer(Modifier.height(12.dp))
            }
        }
        items(rows, key = { it.id }) { row ->
            SectionHeader(title = row.title, subtitle = "${row.items.size} items • ${row.providerName}")
            LazyRow(
                contentPadding = PaddingValues(horizontal = 24.dp),
                horizontalArrangement = Arrangement.spacedBy(16.dp),
                modifier = Modifier.padding(bottom = 12.dp),
            ) {
                items(row.items, key = { it.url + it.apiName }) { item ->
                    PosterCard(item = item, onClick = { onOpenDetail(item) })
                }
            }
        }
    }
}

@Composable
private fun HeroCarousel(
    items: List<SearchResponse>,
    onOpenDetail: (SearchResponse) -> Unit,
) {
    var index by remember { mutableStateOf(0) }
    val total = items.size
    LaunchedEffect(total) {
        while (total > 1) {
            delay(8000)
            index = (index + 1) % total
        }
    }
    Box(
        Modifier
            .fillMaxWidth()
            .height(420.dp)
            .background(MaterialTheme.colorScheme.surface),
    ) {
        AnimatedContent(
            targetState = index,
            transitionSpec = {
                val direction = if (targetState > initialState) 1 else -1
                (slideInHorizontally(tween(700)) { it * direction } + fadeIn(tween(700))) togetherWith
                    (slideOutHorizontally(tween(700)) { -it * direction } + fadeOut(tween(700)))
            },
            label = "hero",
        ) { current ->
            val item = items.getOrNull(current) ?: return@AnimatedContent
            HeroSlide(item = item, onPlay = { onOpenDetail(item) }, onMore = { onOpenDetail(item) })
        }
        // Position indicators.
        Row(
            Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 18.dp),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            repeat(total) { i ->
                val isActive = i == index
                Box(
                    Modifier
                        .height(4.dp)
                        .width(if (isActive) 24.dp else 8.dp)
                        .clip(RoundedCornerShape(2.dp))
                        .background(
                            if (isActive) MaterialTheme.colorScheme.primary
                            else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f),
                        )
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null,
                        ) { index = i },
                )
            }
        }
    }
}

@Composable
private fun HeroSlide(
    item: SearchResponse,
    onPlay: () -> Unit,
    onMore: () -> Unit,
) {
    val backdrop = item.posterUrl
    Box(Modifier.fillMaxSize()) {
        if (backdrop != null) {
            AsyncImage(
                model = backdrop,
                contentDescription = item.name,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize(),
            )
        } else {
            Box(
                Modifier
                    .fillMaxSize()
                    .background(
                        Brush.linearGradient(
                            listOf(
                                MaterialTheme.colorScheme.primaryContainer,
                                MaterialTheme.colorScheme.surfaceVariant,
                            ),
                        ),
                    ),
            )
        }
        // Gradient overlay.
        Box(
            Modifier
                .fillMaxSize()
                .background(bottomFadeOverlay(MaterialTheme.colorScheme.background)),
        )
        // Text panel at bottom-left.
        Column(
            Modifier
                .align(Alignment.BottomStart)
                .padding(start = 32.dp, end = 32.dp, bottom = 38.dp)
                .fillMaxWidth(0.6f),
        ) {
            Text(
                text = item.name,
                style = MaterialTheme.typography.headlineMedium.copy(
                    fontWeight = FontWeight.Bold,
                    letterSpacing = (-0.5).sp,
                ),
                color = Color.White,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
            Spacer(Modifier.height(8.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                item.type?.let { Chip(text = it.displayLabel()) }
                Spacer(Modifier.width(6.dp))
                item.yearOrNull()?.let {
                    Chip(text = it.toString())
                    Spacer(Modifier.width(6.dp))
                }
                item.score?.displayString()?.let { score ->
                    Chip(text = "$score / 10", content = MaterialTheme.colorScheme.primary)
                }
            }
            Spacer(Modifier.height(12.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Button(
                    onClick = onPlay,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primary,
                        contentColor = MaterialTheme.colorScheme.onPrimary,
                    ),
                ) {
                    Icon(Icons.Filled.PlayArrow, contentDescription = null, modifier = Modifier.size(20.dp))
                    Spacer(Modifier.width(6.dp))
                    Text("Play")
                }
                OutlinedButton(onClick = onMore) {
                    Icon(Icons.Filled.Info, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(6.dp))
                    Text("More Info")
                }
            }
        }
    }
}

@Composable
private fun HomeSkeleton() {
    LazyColumn(Modifier.fillMaxSize()) {
        item {
            Box(
                Modifier
                    .fillMaxWidth()
                    .height(420.dp)
                    .padding(0.dp),
            ) { SkeletonBox(Modifier.fillMaxSize(), cornerRadius = 0) }
        }
        items(3) {
            SectionHeader(title = "Loading…")
            LazyRow(
                contentPadding = PaddingValues(horizontal = 24.dp),
                horizontalArrangement = Arrangement.spacedBy(16.dp),
                modifier = Modifier.padding(bottom = 24.dp),
            ) {
                items(6) { PosterSkeleton() }
            }
        }
    }
}

private data class HomeRow(
    val title: String,
    val providerName: String,
    val items: List<SearchResponse>,
) {
    val id: String get() = "$providerName::$title"
}

private suspend fun loadHomeFeed(): List<HomeRow> = withContext(Dispatchers.IO) {
    val rows = mutableListOf<HomeRow>()
    // Take a snapshot under the atomic list's lock to avoid CME.
    val providers = APIHolder.allProviders.withLock { APIHolder.allProviders.toList() }
        .filter { it.hasMainPage }
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
                        if (list.list.isNotEmpty()) {
                            rows.add(HomeRow(list.name, api.name, list.list))
                        }
                    }
                    log.info("Provider ${api.name}: ${resp.items.sumOf { it.list.size }} items")
                }
            } catch (e: Exception) {
                log.warn("Provider ${api.name} home page failed: ${e.message}")
            }
        }
    }
    rows
}

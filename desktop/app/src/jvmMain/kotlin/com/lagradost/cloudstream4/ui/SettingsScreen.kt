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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Movie
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.PlayCircle
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Update
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Slider
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.lagradost.cloudstream4.data.AppSettings

/**
 * Settings screen — every setting is editable (writes immediately to
 * [AppSettings] disk JSON).
 */
@Composable
fun SettingsScreen(settings: AppSettings) {
    val data = remember { settings.data }
    val current = remember { mutableStateOf(data) }

    fun update(transform: (com.lagradost.cloudstream4.data.SettingsData) -> com.lagradost.cloudstream4.data.SettingsData) {
        settings.update(transform)
        current.value = settings.data
    }

    LazyColumn(
        Modifier.fillMaxSize(),
        contentPadding = PaddingValues(24.dp, 24.dp, 24.dp, 48.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        item {
            Text(
                text = "Settings",
                style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold),
            )
            Spacer(Modifier.height(4.dp))
            Text(
                text = "All changes are saved to ~/.cloudstream/settings.json.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        item {
            SettingsGroup(icon = Icons.Filled.Settings, title = "General") {
                DropdownSetting(
                    label = "Theme",
                    value = current.value.theme,
                    options = listOf("dark", "amoled", "light", "dracula"),
                    onSelected = { v -> update { it.copy(theme = v) } },
                )
                DropdownSetting(
                    label = "Locale",
                    value = current.value.locale,
                    options = listOf("en-US", "es-ES", "fr-FR", "de-DE", "it-IT", "pt-BR", "ru-RU", "ja-JP", "ko-KR", "zh-CN", "hi-IN", "ta-IN", "te-IN", "ml-IN"),
                    onSelected = { v -> update { it.copy(locale = v) } },
                )
                SliderSetting(
                    label = "Parallel downloads",
                    value = current.value.parallelDownloads.toFloat(),
                    range = 1f..10f,
                    steps = 8,
                    onChange = { v -> update { it.copy(parallelDownloads = v.toInt()) } },
                    display = "${current.value.parallelDownloads}",
                )
                StringSetting(
                    label = "DNS override",
                    value = current.value.dnsOverride,
                    placeholder = "Leave blank for system DNS",
                    onChange = { v -> update { it.copy(dnsOverride = v) } },
                )
            }
        }
        item {
            SettingsGroup(icon = Icons.Filled.PlayCircle, title = "Player") {
                SliderSetting(
                    label = "Default playback speed",
                    value = current.value.playerSpeed,
                    range = 0.25f..3.0f,
                    steps = 10,
                    onChange = { v -> update { it.copy(playerSpeed = v) } },
                    display = "%.2fx".format(current.value.playerSpeed),
                )
                DropdownSetting(
                    label = "Resize mode",
                    value = current.value.resizeMode,
                    options = listOf("Fit", "Fill", "Stretch", "Original"),
                    onSelected = { v -> update { it.copy(resizeMode = v) } },
                )
                SwitchSetting(
                    label = "Software decoding (slower, more compatible)",
                    value = current.value.softwareDecoding,
                    onChange = { v -> update { it.copy(softwareDecoding = v) } },
                )
                SwitchSetting(
                    label = "Double-tap to seek",
                    value = current.value.doubleTapSeek,
                    onChange = { v -> update { it.copy(doubleTapSeek = v) } },
                )
                SwitchSetting(
                    label = "Swipe gestures (brightness/volume)",
                    value = current.value.swipeGestures,
                    onChange = { v -> update { it.copy(swipeGestures = v) } },
                )
                SliderSetting(
                    label = "Buffer length",
                    value = current.value.bufferMs.toFloat(),
                    range = 5_000f..120_000f,
                    steps = 10,
                    onChange = { v -> update { it.copy(bufferMs = v.toInt()) } },
                    display = "${current.value.bufferMs / 1000}s",
                )
            }
        }
        item {
            SettingsGroup(icon = Icons.Filled.Palette, title = "UI") {
                StringSetting(
                    label = "Primary color",
                    value = current.value.primaryColor,
                    placeholder = "#B388FF",
                    onChange = { v -> update { it.copy(primaryColor = v) } },
                )
                DropdownSetting(
                    label = "Poster size",
                    value = current.value.posterSize,
                    options = listOf("Small", "Medium", "Large"),
                    onSelected = { v -> update { it.copy(posterSize = v) } },
                )
                SwitchSetting(
                    label = "Display sub/dub badges",
                    value = current.value.displaySubDub,
                    onChange = { v -> update { it.copy(displaySubDub = v) } },
                )
                SwitchSetting(
                    label = "Show ratings on posters",
                    value = current.value.ratingsDisplay,
                    onChange = { v -> update { it.copy(ratingsDisplay = v) } },
                )
            }
        }
        item {
            SettingsGroup(icon = Icons.Filled.Movie, title = "Providers") {
                TagListSetting(
                    label = "Preferred media types",
                    values = current.value.preferredMediaTypes,
                    available = listOf("Movie", "TvSeries", "Anime", "AsianDrama", "Documentary", "Live", "Cartoon", "AnimeMovie", "OVA"),
                    onToggle = { tag, on ->
                        update { d ->
                            val list = d.preferredMediaTypes.toMutableList()
                            if (on) list.add(tag) else list.remove(tag)
                            d.copy(preferredMediaTypes = list.distinct())
                        }
                    },
                )
                TagListSetting(
                    label = "Extension languages",
                    values = current.value.extensionLanguages,
                    available = listOf("en", "es", "fr", "de", "it", "pt", "ru", "ja", "ko", "zh", "hi", "ta", "te", "ml"),
                    onToggle = { tag, on ->
                        update { d ->
                            val list = d.extensionLanguages.toMutableList()
                            if (on) list.add(tag) else list.remove(tag)
                            d.copy(extensionLanguages = list.distinct())
                        }
                    },
                )
                SwitchSetting(
                    label = "Enable adult content (18+)",
                    value = current.value.enableAdult,
                    onChange = { v -> update { it.copy(enableAdult = v) } },
                )
            }
        }
        item {
            SettingsGroup(icon = Icons.Filled.Update, title = "Updates") {
                SwitchSetting(
                    label = "Auto-update plugins on launch",
                    value = current.value.autoUpdatePlugins,
                    onChange = { v -> update { it.copy(autoUpdatePlugins = v) } },
                )
                SwitchSetting(
                    label = "Auto-download new plugins from added repos",
                    value = current.value.autoDownloadPlugins,
                    onChange = { v -> update { it.copy(autoDownloadPlugins = v) } },
                )
                SwitchSetting(
                    label = "Show app update notifications",
                    value = current.value.showAppUpdates,
                    onChange = { v -> update { it.copy(showAppUpdates = v) } },
                )
            }
        }
    }
}

@Composable
private fun SettingsGroup(
    icon: ImageVector,
    title: String,
    content: @Composable () -> Unit,
) {
    Card(
        Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
    ) {
        Column(Modifier.padding(20.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                Spacer(Modifier.width(10.dp))
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
                    color = MaterialTheme.colorScheme.onBackground,
                )
            }
            Spacer(Modifier.height(12.dp))
            content()
        }
    }
}

@Composable
private fun SwitchSetting(
    label: String,
    value: Boolean,
    onChange: (Boolean) -> Unit,
) {
    Row(
        Modifier.fillMaxWidth().padding(vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            modifier = Modifier.weight(1f),
        )
        Switch(checked = value, onCheckedChange = onChange)
    }
}

@Composable
private fun SliderSetting(
    label: String,
    value: Float,
    range: ClosedFloatingPointRange<Float>,
    steps: Int,
    onChange: (Float) -> Unit,
    display: String,
) {
    Column(Modifier.fillMaxWidth().padding(vertical = 6.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = label,
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.weight(1f),
            )
            Text(
                text = display,
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.primary,
            )
        }
        Slider(
            value = value,
            onValueChange = onChange,
            valueRange = range,
            steps = steps,
        )
    }
}

@Composable
private fun DropdownSetting(
    label: String,
    value: String,
    options: List<String>,
    onSelected: (String) -> Unit,
) {
    var expanded by remember { mutableStateOf(false) }
    Row(
        Modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp)
            .clickable { expanded = true },
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            modifier = Modifier.weight(1f),
        )
        Box(
            Modifier
                .clip(RoundedCornerShape(8.dp))
                .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f))
                .padding(horizontal = 12.dp, vertical = 6.dp),
        ) {
            Text(
                text = value,
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.primary,
            )
            DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
                options.forEach { opt ->
                    DropdownMenuItem(
                        text = { Text(opt) },
                        onClick = {
                            onSelected(opt)
                            expanded = false
                        },
                    )
                }
            }
        }
    }
}

@Composable
private fun StringSetting(
    label: String,
    value: String,
    placeholder: String,
    onChange: (String) -> Unit,
) {
    Column(Modifier.fillMaxWidth().padding(vertical = 6.dp)) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
        )
        Spacer(Modifier.height(4.dp))
        OutlinedTextField(
            value = value,
            onValueChange = onChange,
            placeholder = { Text(placeholder) },
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
        )
    }
}

@Composable
private fun TagListSetting(
    label: String,
    values: List<String>,
    available: List<String>,
    onToggle: (String, Boolean) -> Unit,
) {
    Column(Modifier.fillMaxWidth().padding(vertical = 6.dp)) {
        Text(label, style = MaterialTheme.typography.bodyMedium)
        Spacer(Modifier.height(6.dp))
        // Wrap-style chip layout via Row of rows (simple approximation).
        val rows = available.chunked(4)
        rows.forEach { rowTags ->
            Row(
                Modifier.padding(vertical = 4.dp),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                rowTags.forEach { tag ->
                    val on = tag in values
                    Box(
                        Modifier
                            .clip(RoundedCornerShape(50))
                            .background(
                                if (on) MaterialTheme.colorScheme.primary
                                else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                            )
                            .clickable { onToggle(tag, !on) }
                            .padding(horizontal = 12.dp, vertical = 6.dp),
                    ) {
                        Text(
                            text = tag,
                            style = MaterialTheme.typography.labelMedium,
                            color = if (on) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }
        }
    }
}

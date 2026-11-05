package com.lagradost.cloudstream4.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.lagradost.cloudstream4.plugin.PluginManager

@Composable
fun SearchScreen(pluginManager: PluginManager) {
    var query by remember { mutableStateOf("") }
    Column(Modifier.fillMaxSize().padding(16.dp)) {
        OutlinedTextField(
            value = query,
            onValueChange = { query = it },
            label = { Text("Search movies, shows, anime…") },
            modifier = Modifier.fillMaxWidth(),
        )
        // TODO: results grid — see ARCHITECTURE.md §4 for the search-result groupings
        Box(Modifier.fillMaxSize(), contentAlignment = androidx.compose.ui.Alignment.Center) {
            Text("Search will be wired to providers when you start typing.")
        }
    }
}

@Composable
fun LibraryScreen() {
    Box(Modifier.fillMaxSize(), contentAlignment = androidx.compose.ui.Alignment.Center) {
        Text("Your library is empty. Bookmark titles to see them here.")
    }
}

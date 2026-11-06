package com.lagradost.cloudstream4.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier

/**
 * Library screen — empty for Phase 1. Phase 2 will add favorites grid,
 * continue-watching row, and subscription list (see LibraryViewModel in
 * upstream CS3).
 */
@Composable
fun LibraryScreen() {
    EmptyState(
        title = "Your library is empty",
        body = "Bookmark titles to see them here. Favorites, continue-watching, " +
            "and subscriptions will appear in this view in a future update.",
    )
}

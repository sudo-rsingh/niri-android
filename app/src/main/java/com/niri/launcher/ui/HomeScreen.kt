package com.niri.launcher.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.snapping.rememberSnapFlingBehavior
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.niri.launcher.LauncherViewModel

// Each column fills ~85% of screen width so the next column peeks in — mirrors niri's window layout.
private const val COLUMN_WIDTH_FRACTION = 0.85f
private val COLUMN_GAP = 16.dp

@Composable
fun HomeScreen(viewModel: LauncherViewModel = viewModel()) {
    val apps by viewModel.apps.collectAsState()
    val context = LocalContext.current
    val columns = remember(apps) { apps.chunkedIntoColumns() }

    val screenWidth = LocalConfiguration.current.screenWidthDp.dp
    val columnWidth = screenWidth * COLUMN_WIDTH_FRACTION

    val listState = rememberLazyListState()
    val snapBehavior = rememberSnapFlingBehavior(listState)

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xCC000000)),
    ) {
        LazyRow(
            state = listState,
            flingBehavior = snapBehavior,
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(horizontal = (screenWidth - columnWidth) / 2),
            horizontalArrangement = Arrangement.spacedBy(COLUMN_GAP),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            items(
                items = columns,
                key = { col -> col.firstOrNull()?.packageName ?: col.hashCode() },
            ) { columnApps ->
                AppColumn(
                    apps = columnApps,
                    columnWidth = columnWidth,
                    onAppClick = { app -> viewModel.launchApp(context, app) },
                )
            }
        }
    }
}

package com.niri.launcher.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.niri.launcher.data.AppInfo

private const val APPS_PER_COLUMN = 5

@Composable
fun AppColumn(
    apps: List<AppInfo>,
    columnWidth: Dp,
    onAppClick: (AppInfo) -> Unit,
) {
    Column(
        modifier = Modifier
            .width(columnWidth)
            .fillMaxHeight()
            .padding(vertical = 64.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp, Alignment.CenterVertically),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        apps.forEach { app ->
            AppIconItem(app = app, onClick = { onAppClick(app) })
        }
    }
}

fun <T> List<T>.chunkedIntoColumns(): List<List<T>> = chunked(APPS_PER_COLUMN)

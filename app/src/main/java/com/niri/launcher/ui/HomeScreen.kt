package com.niri.launcher.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.niri.launcher.LauncherViewModel

@Composable
fun HomeScreen(
    openPicker: Boolean = false,
    viewModel: LauncherViewModel = viewModel(),
) {
    val context = LocalContext.current
    val tiles by viewModel.tiles.collectAsState()
    val focused by viewModel.focusedPackage.collectAsState()
    val apps by viewModel.allApps.collectAsState()

    var showPicker by remember(openPicker) { mutableStateOf(openPicker) }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xCC000000)),
    ) {
        if (tiles.isEmpty() && !showPicker) {
            // Empty state
            Column(
                modifier = Modifier.align(Alignment.Center),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Text("No apps open", color = Color(0x88FFFFFF), fontSize = 14.sp)
                androidx.compose.foundation.layout.Spacer(Modifier.height(12.dp))
                androidx.compose.material3.FilledTonalButton(onClick = { showPicker = true }) {
                    Text("+ Open an app")
                }
            }
        } else if (!showPicker) {
            // Large strip centred on screen
            NiriStrip(
                tiles = tiles,
                focusedPackage = focused,
                compact = false,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(140.dp)
                    .align(Alignment.Center)
                    .windowInsetsPadding(WindowInsets.statusBars),
                onTileClick = { tile -> viewModel.launchTile(context, tile) },
                onTileClose = { tile -> viewModel.removeTile(tile.id) },
                onAddClick = { showPicker = true },
            )
        }

        if (showPicker) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color(0xF0050A10)),
            ) {
                AppPickerScreen(
                    apps = apps,
                    onAppSelected = { app ->
                        showPicker = false
                        viewModel.addAndLaunch(context, app)
                    },
                )
            }
        }
    }
}

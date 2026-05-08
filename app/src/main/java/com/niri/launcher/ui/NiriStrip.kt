package com.niri.launcher.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.snapping.rememberSnapFlingBehavior
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.niri.launcher.data.NiriTile

@Composable
fun NiriStrip(
    tiles: List<NiriTile>,
    focusedPackage: String?,
    compact: Boolean,
    modifier: Modifier = Modifier,
    onTileClick: (NiriTile) -> Unit,
    onTileClose: (NiriTile) -> Unit,
    onAddClick: () -> Unit,
) {
    val iconSize: Dp = if (compact) 28.dp else 52.dp
    val labelWidth: Dp = if (compact) 56.dp else 80.dp
    val hPad: Dp = if (compact) 8.dp else 16.dp

    val listState = rememberLazyListState()
    val snap = rememberSnapFlingBehavior(listState)

    // Auto-scroll so the focused tile is always visible.
    val focusedIndex = tiles.indexOfFirst { it.packageName == focusedPackage }
    LaunchedEffect(focusedIndex) {
        if (focusedIndex >= 0) listState.animateScrollToItem(focusedIndex)
    }

    LazyRow(
        state = listState,
        flingBehavior = snap,
        modifier = modifier
            .fillMaxWidth()
            .fillMaxHeight()
            .background(Color(0xE6050A10)),
        contentPadding = PaddingValues(horizontal = hPad),
        horizontalArrangement = Arrangement.spacedBy(if (compact) 6.dp else 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        items(tiles, key = { it.id }) { tile ->
            TileCard(
                tile = tile,
                isFocused = tile.packageName == focusedPackage,
                iconSize = iconSize,
                labelWidth = labelWidth,
                compact = compact,
                onClick = { onTileClick(tile) },
                onClose = { onTileClose(tile) },
            )
        }

        // "+" add button
        item {
            Box(
                modifier = Modifier
                    .size(if (compact) 36.dp else 56.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(Color(0x33FFFFFF))
                    .clickable { onAddClick() },
                contentAlignment = Alignment.Center,
            ) {
                if (compact) {
                    Text("+", color = Color.White, fontSize = 18.sp)
                } else {
                    Icon(
                        painter = painterResource(android.R.drawable.ic_input_add),
                        contentDescription = "Open app",
                        tint = Color.White,
                        modifier = Modifier.size(28.dp).padding(2.dp),
                    )
                }
            }
        }
    }
}

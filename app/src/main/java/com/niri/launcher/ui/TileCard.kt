package com.niri.launcher.ui

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.graphics.drawable.toBitmap
import com.niri.launcher.R
import com.niri.launcher.data.NiriTile

@Composable
fun TileCard(
    tile: NiriTile,
    isFocused: Boolean,
    iconSize: Dp,
    labelWidth: Dp,
    compact: Boolean,
    onClick: () -> Unit,
    onClose: () -> Unit,
) {
    var showClose by remember { mutableStateOf(false) }

    val bg by animateColorAsState(
        if (isFocused) Color(0xFF1E2A3A) else Color(0x33FFFFFF),
        label = "tile-bg",
    )
    val indicator = if (isFocused) Color(0xFF9ECAFF) else Color.Transparent

    val iconBitmap = remember(tile.packageName) {
        tile.icon.toBitmap(192, 192).asImageBitmap()
    }

    Box(contentAlignment = Alignment.TopEnd) {
        Column(
            modifier = Modifier
                .clip(RoundedCornerShape(10.dp))
                .background(bg)
                .pointerInput(Unit) {
                    detectTapGestures(
                        onTap = {
                            showClose = false
                            onClick()
                        },
                        onLongPress = { showClose = !showClose },
                    )
                }
                .padding(horizontal = if (compact) 10.dp else 14.dp,
                         vertical   = if (compact) 6.dp  else 10.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(if (compact) 2.dp else 6.dp),
        ) {
            // Active indicator bar (niri-style blue line above focused tile)
            Box(
                modifier = Modifier
                    .width(labelWidth)
                    .size(width = labelWidth, height = 2.dp)
                    .background(indicator, RoundedCornerShape(1.dp))
            )
            Image(
                bitmap = iconBitmap,
                contentDescription = tile.label,
                modifier = Modifier.size(iconSize),
            )
            if (!compact) {
                Text(
                    text = tile.label,
                    color = Color.White,
                    fontSize = 11.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.width(labelWidth),
                )
            }
        }

        if (showClose) {
            IconButton(
                onClick = {
                    showClose = false
                    onClose()
                },
                modifier = Modifier
                    .size(20.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(Color(0xFFEF5350)),
            ) {
                Icon(
                    painter = painterResource(android.R.drawable.ic_menu_close_clear_cancel),
                    contentDescription = "Close",
                    tint = Color.White,
                    modifier = Modifier.size(12.dp),
                )
            }
        }
    }
}

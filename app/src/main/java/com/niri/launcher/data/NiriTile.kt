package com.niri.launcher.data

import android.graphics.drawable.Drawable
import java.util.UUID

data class NiriTile(
    val id: String = UUID.randomUUID().toString(),
    val packageName: String,
    val activityName: String,
    val label: String,
    val icon: Drawable,
)

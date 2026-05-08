package com.niri.launcher.data

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager

class AppRepository(private val context: Context) {

    fun getInstalledApps(): List<AppInfo> {
        val pm = context.packageManager
        val intent = Intent(Intent.ACTION_MAIN).apply {
            addCategory(Intent.CATEGORY_LAUNCHER)
        }
        @Suppress("DEPRECATION")
        return pm.queryIntentActivities(intent, PackageManager.GET_META_DATA)
            .map { info ->
                AppInfo(
                    label = info.loadLabel(pm).toString(),
                    packageName = info.activityInfo.packageName,
                    activityName = info.activityInfo.name,
                    icon = info.loadIcon(pm),
                )
            }
            .filter { it.packageName != context.packageName }
            .sortedBy { it.label.lowercase() }
    }
}

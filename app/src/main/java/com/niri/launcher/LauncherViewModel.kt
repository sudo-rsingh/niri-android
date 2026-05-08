package com.niri.launcher

import android.app.Application
import android.content.Context
import android.content.Intent
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.niri.launcher.data.AppInfo
import com.niri.launcher.data.AppRepository
import com.niri.launcher.data.NiriTile
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class LauncherViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = AppRepository(application)
    private val niriState = (application as NiriApplication).niriState

    val tiles: StateFlow<List<com.niri.launcher.data.NiriTile>> = niriState.tiles
    val focusedPackage: StateFlow<String?> = niriState.focusedPackage

    private val _allApps = kotlinx.coroutines.flow.MutableStateFlow<List<AppInfo>>(emptyList())
    val allApps: StateFlow<List<AppInfo>> = _allApps

    init {
        viewModelScope.launch(Dispatchers.IO) { _allApps.value = repository.getInstalledApps() }
    }

    fun addAndLaunch(context: Context, app: AppInfo) {
        val tile = NiriTile(
            packageName = app.packageName,
            activityName = app.activityName,
            label = app.label,
            icon = app.icon,
        )
        niriState.addTile(tile)
        launchTile(context, tile)
    }

    fun launchTile(context: Context, tile: NiriTile) {
        val intent = (context.packageManager.getLaunchIntentForPackage(tile.packageName)
            ?: Intent().setClassName(tile.packageName, tile.activityName)).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_REORDER_TO_FRONT)
        }
        // Ask the service for proper bounds so the app opens below the strip.
        val svc = NiriOverlayService()
        runCatching {
            val opts = android.app.ActivityOptions.makeBasic().apply {
                setLaunchBounds(getBounds(context))
            }
            context.startActivity(intent, opts.toBundle())
        }.onFailure { context.startActivity(intent) }
    }

    fun removeTile(id: String) = niriState.removeTile(id)

    private fun getBounds(context: Context): android.graphics.Rect {
        val wm = context.getSystemService(android.view.WindowManager::class.java)
        val screen = wm.currentWindowMetrics.bounds
        val res = context.resources
        val sbId = res.getIdentifier("status_bar_height", "dimen", "android")
        val sbH = if (sbId > 0) res.getDimensionPixelSize(sbId) else (24 * res.displayMetrics.density).toInt()
        val stripH = (52 * res.displayMetrics.density).toInt()
        return android.graphics.Rect(0, sbH + stripH, screen.width(), screen.height())
    }
}

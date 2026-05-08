package com.niri.launcher

import android.app.ActivityOptions
import android.app.usage.UsageEvents
import android.app.usage.UsageStatsManager
import android.content.Context
import android.content.Intent
import android.graphics.PixelFormat
import android.graphics.Rect
import android.view.Gravity
import android.view.View
import android.view.WindowManager
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.core.app.NotificationCompat
import androidx.lifecycle.LifecycleService
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.setViewTreeLifecycleOwner
import androidx.lifecycle.setViewTreeViewModelStoreOwner
import androidx.savedstate.setViewTreeSavedStateRegistryOwner
import com.niri.launcher.data.AppInfo
import com.niri.launcher.data.AppRepository
import com.niri.launcher.data.NiriTile
import com.niri.launcher.ui.AppPickerScreen
import com.niri.launcher.ui.NiriStrip
import com.niri.launcher.ui.theme.NiriLauncherTheme
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class NiriOverlayService : LifecycleService() {

    private lateinit var windowManager: WindowManager
    private lateinit var usageStatsManager: UsageStatsManager
    private lateinit var niriState: NiriStateHolder

    private val composeOwner = ComposeServiceOwner()
    private var overlayView: ComposeView? = null
    private var lastForeground: String? = null

    private val allAppsFlow = MutableStateFlow<List<AppInfo>>(emptyList())

    override fun onCreate() {
        super.onCreate()
        niriState = (applicationContext as NiriApplication).niriState
        windowManager = getSystemService(WindowManager::class.java)
        usageStatsManager = getSystemService(UsageStatsManager::class.java)

        startForeground(
            OVERLAY_NOTIF_ID,
            NotificationCompat.Builder(this, OVERLAY_NOTIF_CHANNEL)
                .setContentTitle("Niri strip active")
                .setSmallIcon(R.mipmap.ic_launcher)
                .setPriority(NotificationCompat.PRIORITY_LOW)
                .setOngoing(true)
                .build(),
        )

        composeOwner.onCreate()
        composeOwner.onStart()
        composeOwner.onResume()

        overlayView = buildOverlayView()
        windowManager.addView(overlayView, stripParams())

        // Load all installed apps for the picker.
        lifecycleScope.launch(Dispatchers.IO) {
            allAppsFlow.value = AppRepository(this@NiriOverlayService).getInstalledApps()
        }

        lifecycleScope.launch { pollForegroundApp() }

        // When picker opens/closes, resize the overlay window.
        lifecycleScope.launch {
            niriState.isPickerOpen.collectLatest { open ->
                withContext(Dispatchers.Main) {
                    overlayView?.let {
                        windowManager.updateViewLayout(it, if (open) pickerParams() else stripParams())
                    }
                }
            }
        }

        // Hide strip when no tiles or launcher is foreground.
        lifecycleScope.launch {
            niriState.tiles.collectLatest { tiles ->
                val isUs = lastForeground == packageName
                if (tiles.isEmpty() && !niriState.isPickerOpen.value) {
                    overlayView?.visibility = View.GONE
                } else if (!isUs) {
                    overlayView?.visibility = View.VISIBLE
                }
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        overlayView?.let { windowManager.removeView(it) }
        composeOwner.onDestroy()
    }

    // ------------------------------------------------------------------
    // Overlay view — switches between compact strip and full-screen picker
    // ------------------------------------------------------------------

    private fun buildOverlayView(): ComposeView = ComposeView(this).apply {
        setViewTreeLifecycleOwner(composeOwner)
        setViewTreeSavedStateRegistryOwner(composeOwner)
        setViewTreeViewModelStoreOwner(composeOwner)
        setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed)
        setContent {
            val tiles by niriState.tiles.collectAsState()
            val focused by niriState.focusedPackage.collectAsState()
            val pickerOpen by niriState.isPickerOpen.collectAsState()
            val apps by allAppsFlow.collectAsState()

            NiriLauncherTheme {
                if (pickerOpen) {
                    AppPickerScreen(
                        apps = apps,
                        onDismiss = { niriState.closePicker() },
                        onAppSelected = { app ->
                            niriState.closePicker()
                            addAndLaunch(app)
                        },
                    )
                } else {
                    NiriStrip(
                        tiles = tiles,
                        focusedPackage = focused,
                        compact = true,
                        onTileClick = { bringToFront(it) },
                        onTileClose = { niriState.removeTile(it.id) },
                        onAddClick = { niriState.openPicker() },
                    )
                }
            }
        }
    }

    /** Compact strip at the top — non-focusable so keyboard stays with the running app. */
    private fun stripParams() = WindowManager.LayoutParams(
        WindowManager.LayoutParams.MATCH_PARENT,
        statusBarHeightPx() + dpToPx(52),
        WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
        WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL or
                WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN,
        PixelFormat.TRANSLUCENT,
    ).apply { gravity = Gravity.TOP }

    /** Full-screen picker — focusable so the search field can receive keyboard input. */
    private fun pickerParams() = WindowManager.LayoutParams(
        WindowManager.LayoutParams.MATCH_PARENT,
        WindowManager.LayoutParams.MATCH_PARENT,
        WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
        WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN,
        PixelFormat.TRANSLUCENT,
    ).apply { gravity = Gravity.TOP }

    // ------------------------------------------------------------------
    // Foreground-app detection
    // ------------------------------------------------------------------

    private suspend fun pollForegroundApp() {
        lastForeground = queryLastForeground(10_000L)
        while (true) {
            delay(500L)
            // Skip polling while picker is open — we own the screen.
            if (niriState.isPickerOpen.value) continue
            val pkg = queryLastForeground(1_500L) ?: continue
            if (pkg == lastForeground) continue
            lastForeground = pkg
            niriState.setFocusedPackage(pkg)
            val isUs = pkg == packageName
            val hasTiles = niriState.tiles.value.isNotEmpty()
            withContext(Dispatchers.Main) {
                overlayView?.visibility =
                    if (isUs || !hasTiles) View.GONE else View.VISIBLE
            }
        }
    }

    private fun queryLastForeground(windowMs: Long): String? {
        val now = System.currentTimeMillis()
        val events = usageStatsManager.queryEvents(now - windowMs, now)
        var last: String? = null
        val ev = UsageEvents.Event()
        while (events.hasNextEvent()) {
            events.getNextEvent(ev)
            if (ev.eventType == UsageEvents.Event.MOVE_TO_FOREGROUND ||
                ev.eventType == UsageEvents.Event.ACTIVITY_RESUMED
            ) last = ev.packageName
        }
        return last
    }

    // ------------------------------------------------------------------
    // Actions
    // ------------------------------------------------------------------

    private fun bringToFront(tile: NiriTile) {
        val intent = (packageManager.getLaunchIntentForPackage(tile.packageName)
            ?: Intent().setClassName(tile.packageName, tile.activityName)).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_REORDER_TO_FRONT)
        }
        val opts = ActivityOptions.makeBasic().apply { setLaunchBounds(appBounds()) }
        startActivity(intent, opts.toBundle())
    }

    private fun addAndLaunch(app: AppInfo) {
        val tile = NiriTile(
            packageName = app.packageName,
            activityName = app.activityName,
            label = app.label,
            icon = app.icon,
        )
        niriState.addTile(tile)
        bringToFront(tile)
    }

    fun appBounds(): Rect {
        val screen = windowManager.currentWindowMetrics.bounds
        return Rect(0, statusBarHeightPx() + dpToPx(52), screen.width(), screen.height())
    }

    private fun statusBarHeightPx(): Int {
        val id = resources.getIdentifier("status_bar_height", "dimen", "android")
        return if (id > 0) resources.getDimensionPixelSize(id) else dpToPx(24)
    }

    private fun dpToPx(dp: Int) = (dp * resources.displayMetrics.density).toInt()

    companion object {
        fun start(context: Context) =
            context.startForegroundService(Intent(context, NiriOverlayService::class.java))
    }
}

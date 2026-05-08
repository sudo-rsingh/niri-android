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
import com.niri.launcher.data.NiriTile
import com.niri.launcher.ui.NiriStrip
import com.niri.launcher.ui.theme.NiriLauncherTheme
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

class NiriOverlayService : LifecycleService() {

    private lateinit var windowManager: WindowManager
    private lateinit var usageStatsManager: UsageStatsManager
    private lateinit var niriState: NiriStateHolder

    private val composeOwner = ComposeServiceOwner()
    private var overlayView: ComposeView? = null

    // Kept across polls so the strip stays on whichever app was last seen.
    private var lastForeground: String? = null

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
        windowManager.addView(overlayView, makeParams())

        lifecycleScope.launch { pollForegroundApp() }

        // Hide the strip when there are no tiles.
        lifecycleScope.launch {
            niriState.tiles.collectLatest { tiles ->
                val isUs = lastForeground == packageName
                overlayView?.visibility =
                    if (tiles.isEmpty() || isUs) View.GONE else View.VISIBLE
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        overlayView?.let { windowManager.removeView(it) }
        composeOwner.onDestroy()
    }

    // ------------------------------------------------------------------
    // Overlay
    // ------------------------------------------------------------------

    private fun buildOverlayView(): ComposeView = ComposeView(this).apply {
        setViewTreeLifecycleOwner(composeOwner)
        setViewTreeSavedStateRegistryOwner(composeOwner)
        setViewTreeViewModelStoreOwner(composeOwner)
        setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed)
        setContent {
            val tiles by niriState.tiles.collectAsState()
            val focused by niriState.focusedPackage.collectAsState()
            NiriLauncherTheme {
                NiriStrip(
                    tiles = tiles,
                    focusedPackage = focused,
                    compact = true,
                    onTileClick = { bringToFront(it) },
                    onTileClose = { niriState.removeTile(it.id) },
                    onAddClick = { openPicker() },
                )
            }
        }
    }

    private fun makeParams(): WindowManager.LayoutParams {
        val h = statusBarHeightPx() + dpToPx(52)
        return WindowManager.LayoutParams(
            WindowManager.LayoutParams.MATCH_PARENT,
            h,
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                    WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL or
                    WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN,
            PixelFormat.TRANSLUCENT,
        ).apply { gravity = Gravity.TOP }
    }

    // ------------------------------------------------------------------
    // Foreground-app detection
    // ------------------------------------------------------------------

    private suspend fun pollForegroundApp() {
        lastForeground = queryLastForeground(10_000L)
        while (true) {
            delay(500L)
            val pkg = queryLastForeground(1_500L) ?: continue
            if (pkg == lastForeground) continue
            lastForeground = pkg
            niriState.setFocusedPackage(pkg)
            val isUs = pkg == packageName
            val hasTiles = niriState.tiles.value.isNotEmpty()
            overlayView?.visibility =
                if (isUs || !hasTiles) View.GONE else View.VISIBLE
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

    fun bringToFront(tile: NiriTile) {
        val intent = (packageManager.getLaunchIntentForPackage(tile.packageName)
            ?: Intent().setClassName(tile.packageName, tile.activityName)).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_REORDER_TO_FRONT)
        }
        val opts = ActivityOptions.makeBasic().apply { setLaunchBounds(appBounds()) }
        startActivity(intent, opts.toBundle())
    }

    private fun openPicker() {
        startActivity(
            Intent(this, MainActivity::class.java).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_REORDER_TO_FRONT)
                putExtra(MainActivity.EXTRA_OPEN_PICKER, true)
            }
        )
    }

    /** Screen area below the strip where apps should open. */
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

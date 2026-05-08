package com.niri.launcher

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.provider.Settings
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.niri.launcher.ui.HomeScreen
import com.niri.launcher.ui.theme.NiriLauncherTheme

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.addFlags(WindowManager.LayoutParams.FLAG_SHOW_WALLPAPER)
        enableEdgeToEdge()

        val openPicker = intent.getBooleanExtra(EXTRA_OPEN_PICKER, false)

        if (!Settings.canDrawOverlays(this)) {
            showOverlayPermissionUi(openPicker)
            return
        }

        NiriOverlayService.start(this)

        setContent {
            NiriLauncherTheme {
                Surface(modifier = Modifier.fillMaxSize(), color = Color.Transparent) {
                    BackHandler(enabled = true) { /* swallow — already home */ }
                    HomeScreen(openPicker = openPicker)
                }
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        if (intent.getBooleanExtra(EXTRA_OPEN_PICKER, false)) {
            // Recreate so HomeScreen opens with picker pre-shown.
            setIntent(intent)
            recreate()
        }
    }

    private fun showOverlayPermissionUi(openPicker: Boolean) {
        setContent {
            NiriLauncherTheme {
                var dismissed by remember { mutableStateOf(false) }
                if (!dismissed) {
                    AlertDialog(
                        onDismissRequest = {},
                        title = { Text("Permission needed") },
                        text = {
                            Text(
                                "Run this ADB command to grant the overlay permission, " +
                                "then relaunch the app:\n\n" +
                                "adb shell appops set com.niri.launcher SYSTEM_ALERT_WINDOW allow\n\n" +
                                "Also grant usage-stats access:\n\n" +
                                "adb shell appops set com.niri.launcher GET_USAGE_STATS allow"
                            )
                        },
                        confirmButton = {
                            TextButton(onClick = {
                                startActivity(
                                    Intent(
                                        Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                                        Uri.parse("package:$packageName"),
                                    )
                                )
                            }) { Text("Open settings") }
                        },
                        dismissButton = {
                            TextButton(onClick = { dismissed = true }) { Text("Dismiss") }
                        },
                    )
                }
            }
        }
    }

    companion object {
        const val EXTRA_OPEN_PICKER = "open_picker"
    }
}

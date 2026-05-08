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
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.ui.graphics.Color
import com.niri.launcher.ui.HomeScreen
import com.niri.launcher.ui.theme.NiriLauncherTheme

class MainActivity : ComponentActivity() {

    // Survives onNewIntent without recreate().
    private val shouldOpenPicker = mutableStateOf(false)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.addFlags(WindowManager.LayoutParams.FLAG_SHOW_WALLPAPER)
        enableEdgeToEdge()

        shouldOpenPicker.value = intent.getBooleanExtra(EXTRA_OPEN_PICKER, false)

        if (!Settings.canDrawOverlays(this)) {
            showPermissionDialog()
            return
        }

        NiriOverlayService.start(this)

        setContent {
            NiriLauncherTheme {
                Surface(modifier = Modifier.fillMaxSize(), color = Color.Transparent) {
                    BackHandler(enabled = true) { /* swallow */ }
                    HomeScreen(openPicker = shouldOpenPicker.value,
                               onPickerClosed = { shouldOpenPicker.value = false })
                }
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        if (intent.getBooleanExtra(EXTRA_OPEN_PICKER, false)) {
            shouldOpenPicker.value = true
        }
    }

    private fun showPermissionDialog() {
        setContent {
            NiriLauncherTheme {
                var dismissed by remember { mutableStateOf(false) }
                if (!dismissed) {
                    AlertDialog(
                        onDismissRequest = {},
                        title = { Text("Permission needed") },
                        text = {
                            Text(
                                "Grant the overlay and usage-stats permissions via ADB:\n\n" +
                                "adb shell appops set com.niri.launcher SYSTEM_ALERT_WINDOW allow\n\n" +
                                "adb shell appops set com.niri.launcher GET_USAGE_STATS allow"
                            )
                        },
                        confirmButton = {
                            TextButton(onClick = {
                                startActivity(Intent(
                                    Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                                    Uri.parse("package:$packageName"),
                                ))
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

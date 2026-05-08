package com.niri.launcher

import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager

const val OVERLAY_NOTIF_CHANNEL = "niri_overlay"
const val OVERLAY_NOTIF_ID = 1

class NiriApplication : Application() {

    lateinit var niriState: NiriStateHolder
        private set

    override fun onCreate() {
        super.onCreate()
        niriState = NiriStateHolder(this)
        createNotificationChannel()
    }

    private fun createNotificationChannel() {
        val channel = NotificationChannel(
            OVERLAY_NOTIF_CHANNEL,
            "Niri strip overlay",
            NotificationManager.IMPORTANCE_LOW,
        ).apply { setShowBadge(false) }
        getSystemService(NotificationManager::class.java).createNotificationChannel(channel)
    }
}

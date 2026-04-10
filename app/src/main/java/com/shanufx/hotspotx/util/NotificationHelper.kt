package com.shanufx.hotspotx.util

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationCompat
import com.shanufx.hotspotx.MainActivity
import com.shanufx.hotspotx.R
import com.shanufx.hotspotx.service.HotspotService

object NotificationHelper {

    const val CHANNEL_SERVICE = "hotspot_service"
    const val CHANNEL_ALERTS  = "hotspot_alerts"
    const val NOTIF_ID_SERVICE = 1001
    const val NOTIF_ID_DATA_LIMIT = 1002
    const val NOTIF_ID_SCHEDULE = 1003

    fun createChannels(context: Context) {
        val nm = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        nm.createNotificationChannel(
            NotificationChannel(
                CHANNEL_SERVICE,
                context.getString(R.string.hotspot_service_channel),
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = context.getString(R.string.hotspot_service_channel_desc)
                setShowBadge(false)
            }
        )

        nm.createNotificationChannel(
            NotificationChannel(
                CHANNEL_ALERTS,
                "Hotspot Alerts",
                NotificationManager.IMPORTANCE_DEFAULT
            ).apply {
                description = "Data limit warnings and schedule notifications"
            }
        )
    }

    fun buildServiceNotification(
        context: Context,
        isActive: Boolean,
        connectedDevices: Int,
        uploadSpeed: String,
        downloadSpeed: String
    ): Notification {
        val dashboardIntent = PendingIntent.getActivity(
            context, 0,
            Intent(context, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_SINGLE_TOP
            },
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val stopIntent = PendingIntent.getService(
            context, 1,
            Intent(context, HotspotService::class.java).apply {
                action = HotspotService.ACTION_STOP
            },
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val statusText = if (isActive) {
            "Devices: $connectedDevices  ↑ $uploadSpeed  ↓ $downloadSpeed"
        } else {
            "Hotspot is stopped"
        }

        return NotificationCompat.Builder(context, CHANNEL_SERVICE)
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentTitle(
                if (isActive) context.getString(R.string.notification_title_active)
                else context.getString(R.string.notification_title_inactive)
            )
            .setContentText(statusText)
            .setOngoing(isActive)
            .setContentIntent(dashboardIntent)
            .addAction(
                android.R.drawable.ic_menu_close_clear_cancel,
                context.getString(R.string.stop_hotspot),
                stopIntent
            )
            .addAction(
                android.R.drawable.ic_menu_view,
                context.getString(R.string.open_dashboard),
                dashboardIntent
            )
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setForegroundServiceBehavior(NotificationCompat.FOREGROUND_SERVICE_IMMEDIATE)
            .build()
    }

    fun buildDataLimitNotification(context: Context, percent: Int): Notification =
        NotificationCompat.Builder(context, CHANNEL_ALERTS)
            .setSmallIcon(android.R.drawable.ic_dialog_alert)
            .setContentTitle("Data Limit Warning")
            .setContentText("You have used $percent% of your monthly data cap.")
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setAutoCancel(true)
            .build()
}

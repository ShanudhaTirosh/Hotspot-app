package com.shanufx.hotspotx.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log
import com.shanufx.hotspotx.service.HotspotService

class ScheduleReceiver : BroadcastReceiver() {

    companion object {
        const val ACTION_SCHEDULE_START = "com.shanufx.hotspotx.SCHEDULE_START"
        const val ACTION_SCHEDULE_STOP  = "com.shanufx.hotspotx.SCHEDULE_STOP"
        const val EXTRA_SCHEDULE_ID     = "schedule_id"
    }

    override fun onReceive(context: Context, intent: Intent) {
        val scheduleId = intent.getLongExtra(EXTRA_SCHEDULE_ID, -1L)
        when (intent.action) {
            ACTION_SCHEDULE_START -> {
                Log.i("ScheduleReceiver", "Schedule $scheduleId: starting hotspot")
                val serviceIntent = Intent(context, HotspotService::class.java).apply {
                    action = HotspotService.ACTION_START
                }
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    context.startForegroundService(serviceIntent)
                } else {
                    context.startService(serviceIntent)
                }
            }
            ACTION_SCHEDULE_STOP -> {
                Log.i("ScheduleReceiver", "Schedule $scheduleId: stopping hotspot")
                val serviceIntent = Intent(context, HotspotService::class.java).apply {
                    action = HotspotService.ACTION_STOP
                }
                context.startService(serviceIntent)
            }
        }
    }
}

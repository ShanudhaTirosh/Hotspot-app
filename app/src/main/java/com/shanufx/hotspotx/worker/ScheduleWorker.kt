package com.shanufx.hotspotx.worker

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log
import androidx.hilt.work.HiltWorker
import androidx.work.*
import com.shanufx.hotspotx.data.db.dao.ScheduleDao
import com.shanufx.hotspotx.data.db.entity.ScheduleEntity
import com.shanufx.hotspotx.receiver.ScheduleReceiver
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import java.util.*
import java.util.concurrent.TimeUnit

@HiltWorker
class ScheduleWorker @AssistedInject constructor(
    @Assisted appContext: Context,
    @Assisted workerParams: WorkerParameters,
    private val scheduleDao: ScheduleDao
) : CoroutineWorker(appContext, workerParams) {

    override suspend fun doWork(): Result {
        return try {
            val schedules = mutableListOf<ScheduleEntity>()
            scheduleDao.observeAll().collect { list ->
                schedules.addAll(list.filter { it.isEnabled })
                return@collect
            }
            val alarmManager = applicationContext.getSystemService(Context.ALARM_SERVICE) as AlarmManager
            schedules.forEach { schedule ->
                scheduleSingleAlarm(alarmManager, schedule)
            }
            Result.success()
        } catch (e: Exception) {
            Log.e("ScheduleWorker", "Failed: ${e.message}")
            Result.retry()
        }
    }

    private fun scheduleSingleAlarm(alarmManager: AlarmManager, schedule: ScheduleEntity) {
        val cal = Calendar.getInstance()
        val dayMask = schedule.daysOfWeekMask
        // Find the next occurrence for each enabled day
        for (dayOfWeek in Calendar.SUNDAY..Calendar.SATURDAY) {
            val bit = 1 shl (dayOfWeek - 1)
            if (dayMask and bit == 0) continue

            val startCal = Calendar.getInstance().apply {
                set(Calendar.DAY_OF_WEEK, dayOfWeek)
                set(Calendar.HOUR_OF_DAY, schedule.startMinutes / 60)
                set(Calendar.MINUTE, schedule.startMinutes % 60)
                set(Calendar.SECOND, 0)
                if (timeInMillis <= System.currentTimeMillis()) add(Calendar.WEEK_OF_YEAR, 1)
            }
            val stopCal = Calendar.getInstance().apply {
                set(Calendar.DAY_OF_WEEK, dayOfWeek)
                set(Calendar.HOUR_OF_DAY, schedule.endMinutes / 60)
                set(Calendar.MINUTE, schedule.endMinutes % 60)
                set(Calendar.SECOND, 0)
                if (timeInMillis <= System.currentTimeMillis()) add(Calendar.WEEK_OF_YEAR, 1)
            }

            setRepeatingAlarm(alarmManager, schedule.id, startCal.timeInMillis,
                ScheduleReceiver.ACTION_SCHEDULE_START)
            setRepeatingAlarm(alarmManager, schedule.id + 100_000, stopCal.timeInMillis,
                ScheduleReceiver.ACTION_SCHEDULE_STOP)
        }
    }

    private fun setRepeatingAlarm(alarmManager: AlarmManager, requestCode: Long,
                                   triggerAtMs: Long, action: String) {
        val intent = Intent(applicationContext, ScheduleReceiver::class.java).apply {
            this.action = action
            putExtra(ScheduleReceiver.EXTRA_SCHEDULE_ID, requestCode)
        }
        val pi = PendingIntent.getBroadcast(
            applicationContext,
            requestCode.toInt(),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAtMs, pi)
        } else {
            alarmManager.setExact(AlarmManager.RTC_WAKEUP, triggerAtMs, pi)
        }
    }

    companion object {
        const val WORK_NAME = "hotspot_schedule_sync"

        fun enqueue(context: Context) {
            WorkManager.getInstance(context).enqueueUniqueWork(
                WORK_NAME,
                ExistingWorkPolicy.REPLACE,
                OneTimeWorkRequestBuilder<ScheduleWorker>().build()
            )
        }
    }
}

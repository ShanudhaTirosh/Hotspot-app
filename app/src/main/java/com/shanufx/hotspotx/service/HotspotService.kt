package com.shanufx.hotspotx.service

import android.app.Service
import android.content.Intent
import android.os.IBinder
import android.util.Log
import androidx.core.app.NotificationManagerCompat
import com.shanufx.hotspotx.data.db.dao.SessionDao
import com.shanufx.hotspotx.data.db.dao.UsageSnapshotDao
import com.shanufx.hotspotx.data.db.entity.SessionEntity
import com.shanufx.hotspotx.data.db.entity.UsageSnapshotEntity
import com.shanufx.hotspotx.data.repository.DeviceRepository
import com.shanufx.hotspotx.data.repository.HotspotState
import com.shanufx.hotspotx.data.repository.SpeedSnapshot
import com.shanufx.hotspotx.data.repository.StatsRepository
import com.shanufx.hotspotx.data.repository.SettingsRepository
import com.shanufx.hotspotx.data.repository.TetheringRepository
import com.shanufx.hotspotx.util.AutoOffManager
import com.shanufx.hotspotx.util.FormatUtils
import com.shanufx.hotspotx.util.NotificationHelper
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import javax.inject.Inject

@AndroidEntryPoint
class HotspotService : Service() {

    companion object {
        const val ACTION_STOP  = "com.shanufx.hotspotx.ACTION_STOP"
        const val ACTION_START = "com.shanufx.hotspotx.ACTION_START"

        private val _speedFlow = MutableStateFlow(SpeedSnapshot(0L, 0L))
        val speedFlow: StateFlow<SpeedSnapshot> = _speedFlow.asStateFlow()

        private val _isRunning = MutableStateFlow(false)
        val isRunning: StateFlow<Boolean> = _isRunning.asStateFlow()
    }

    @Inject lateinit var tetheringRepository: TetheringRepository
    @Inject lateinit var statsRepository: StatsRepository
    @Inject lateinit var deviceRepository: DeviceRepository
    @Inject lateinit var settingsRepository: SettingsRepository
    @Inject lateinit var sessionDao: SessionDao
    @Inject lateinit var usageSnapshotDao: UsageSnapshotDao
    @Inject lateinit var autoOffManager: AutoOffManager

    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private var currentSessionId: Long = -1L
    private var peakSpeedBps = 0L
    private var maxConnectedDevices = 0

    override fun onCreate() {
        super.onCreate()
        NotificationHelper.createChannels(this)
        startForeground(
            NotificationHelper.NOTIF_ID_SERVICE,
            NotificationHelper.buildServiceNotification(this, false, 0, "0 B/s", "0 B/s")
        )
        _isRunning.value = true
        startCoroutines()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_STOP -> {
                tetheringRepository.stopHotspot()
                autoOffManager.stop()
                stopSelf()
            }
        }
        return START_STICKY
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onDestroy() {
        super.onDestroy()
        autoOffManager.stop()
        serviceScope.cancel()
        _isRunning.value = false
        closeCurrentSession()
    }

    private fun startCoroutines() {
        // ARP scan every 3 seconds
        serviceScope.launch { deviceRepository.startScanning(3000L) }

        // Real-time speed polling every 1 second
        serviceScope.launch {
            statsRepository.observeRealtimeSpeed(1000L).collect { snapshot ->
                _speedFlow.value = snapshot
                val combined = snapshot.uploadBps + snapshot.downloadBps
                if (combined > peakSpeedBps) peakSpeedBps = combined
                val devCount = deviceRepository.connectedCount()
                if (devCount > maxConnectedDevices) maxConnectedDevices = devCount
                updateNotification(snapshot, devCount)
            }
        }

        // Persist usage snapshots every 30 seconds
        serviceScope.launch {
            while (true) {
                delay(30_000L)
                val snap = _speedFlow.value
                usageSnapshotDao.insert(
                    UsageSnapshotEntity(
                        uploadBytes   = snap.uploadBps * 30,
                        downloadBytes = snap.downloadBps * 30,
                        deviceMac     = null
                    )
                )
                usageSnapshotDao.pruneOlderThan(System.currentTimeMillis() - 90L * 24 * 3600 * 1000)
            }
        }

        // Watch hotspot state → open/close sessions + auto-off
        serviceScope.launch {
            tetheringRepository.hotspotState.collect { state ->
                when (state) {
                    is HotspotState.Active -> {
                        openSession()
                        val settings = settingsRepository.settings.first()
                        autoOffManager.start(serviceScope, settings.autoOffMinutes)
                    }
                    is HotspotState.Idle, is HotspotState.Error -> {
                        autoOffManager.stop()
                        closeCurrentSession()
                    }
                    else -> {}
                }
            }
        }

        // Watch device count changes to reset auto-off idle timer
        serviceScope.launch {
            deviceRepository.connectedDevices.collect { devices ->
                if (devices.isNotEmpty()) autoOffManager.resetIdleTimer()
            }
        }

        // Monitor data cap
        serviceScope.launch {
            while (true) {
                delay(60_000L)
                checkDataCap()
            }
        }
    }

    private suspend fun checkDataCap() {
        val settings = settingsRepository.settings.first()
        if (settings.monthlyDataCapBytes <= 0L) return
        val used = usageSnapshotDao.totalBytesSince(FormatUtils.startOfMonthMs()) ?: 0L
        val pct = (used.toDouble() / settings.monthlyDataCapBytes * 100).toInt()

        if (settings.notifyOnDataLimit) {
            if (pct >= 100) {
                tetheringRepository.stopHotspot()
                Log.i("HotspotService", "Monthly data cap reached — hotspot stopped")
            } else if (pct >= settings.dataCapWarningPercent) {
                try {
                    NotificationManagerCompat.from(this)
                        .notify(
                            NotificationHelper.NOTIF_ID_DATA_LIMIT,
                            NotificationHelper.buildDataLimitNotification(this, pct)
                        )
                } catch (_: SecurityException) {}
            }
        }
    }

    private fun updateNotification(speed: SpeedSnapshot, deviceCount: Int) {
        val isActive = tetheringRepository.isHotspotActive()
        val notif = NotificationHelper.buildServiceNotification(
            context        = this,
            isActive       = isActive,
            connectedDevices = deviceCount,
            uploadSpeed    = FormatUtils.formatSpeed(speed.uploadBps),
            downloadSpeed  = FormatUtils.formatSpeed(speed.downloadBps)
        )
        try {
            NotificationManagerCompat.from(this)
                .notify(NotificationHelper.NOTIF_ID_SERVICE, notif)
        } catch (_: SecurityException) {}
    }

    private fun openSession() {
        if (currentSessionId != -1L) return
        serviceScope.launch {
            currentSessionId = sessionDao.insert(SessionEntity(startTime = System.currentTimeMillis()))
        }
    }

    private fun closeCurrentSession() {
        if (currentSessionId == -1L) return
        val id = currentSessionId
        currentSessionId = -1L
        serviceScope.launch {
            sessionDao.close(
                id     = id,
                end    = System.currentTimeMillis(),
                up     = _speedFlow.value.uploadBps,
                down   = _speedFlow.value.downloadBps,
                peak   = peakSpeedBps,
                maxDev = maxConnectedDevices
            )
            peakSpeedBps = 0L
            maxConnectedDevices = 0
        }
    }
}

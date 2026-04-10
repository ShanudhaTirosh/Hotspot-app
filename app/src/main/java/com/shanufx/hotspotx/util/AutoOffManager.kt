package com.shanufx.hotspotx.util

import android.util.Log
import com.shanufx.hotspotx.data.repository.DeviceRepository
import com.shanufx.hotspotx.data.repository.TetheringRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.inject.Singleton

private const val TAG = "AutoOffManager"
private const val CHECK_INTERVAL_MS = 30_000L // check every 30 seconds

/**
 * Monitors connected device count and stops the hotspot when no devices have
 * been connected for [autoOffMinutes] consecutive minutes.
 *
 * Injected into [HotspotService] and activated only when autoOffMinutes > 0.
 */
@Singleton
class AutoOffManager @Inject constructor(
    private val tetheringRepository: TetheringRepository,
    private val deviceRepository: DeviceRepository
) {
    private var monitorJob: Job? = null
    private var idleSinceMs: Long = 0L

    fun start(scope: CoroutineScope, autoOffMinutes: Int) {
        if (autoOffMinutes <= 0) return
        val thresholdMs = autoOffMinutes * 60_000L
        monitorJob?.cancel()
        idleSinceMs = System.currentTimeMillis()

        monitorJob = scope.launch {
            Log.d(TAG, "Auto-off monitoring started: threshold=${autoOffMinutes}min")
            while (true) {
                delay(CHECK_INTERVAL_MS)
                val deviceCount = deviceRepository.connectedCount()
                val now = System.currentTimeMillis()

                if (deviceCount > 0) {
                    // Devices connected — reset idle timer
                    idleSinceMs = now
                } else {
                    val idleMs = now - idleSinceMs
                    if (idleMs >= thresholdMs) {
                        Log.i(TAG, "Auto-off triggered after ${idleMs / 60_000}min idle")
                        tetheringRepository.stopHotspot()
                        stop()
                        return@launch
                    }
                }
            }
        }
    }

    fun resetIdleTimer() {
        idleSinceMs = System.currentTimeMillis()
    }

    fun stop() {
        monitorJob?.cancel()
        monitorJob = null
    }
}

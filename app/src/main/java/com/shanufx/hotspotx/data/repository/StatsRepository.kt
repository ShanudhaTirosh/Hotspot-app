package com.shanufx.hotspotx.data.repository

import android.app.usage.NetworkStats
import android.app.usage.NetworkStatsManager
import android.content.Context
import android.net.ConnectivityManager
import android.net.TrafficStats
import android.util.Log
import com.shanufx.hotspotx.util.FormatUtils
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

private const val TAG = "StatsRepo"

data class SpeedSnapshot(
    val uploadBps: Long,
    val downloadBps: Long,
    val timestampMs: Long = System.currentTimeMillis()
)

data class UsageSummary(
    val totalTxBytes: Long,
    val totalRxBytes: Long
)

@Singleton
class StatsRepository @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private var lastTxBytes = -1L
    private var lastRxBytes = -1L
    private var lastTimestampMs = 0L

    /**
     * Emits real-time upload/download speed every [intervalMs] milliseconds.
     * Uses TrafficStats — no special permissions required.
     */
    fun observeRealtimeSpeed(intervalMs: Long = 1000L): Flow<SpeedSnapshot> = flow {
        lastTxBytes    = TrafficStats.getTotalTxBytes()
        lastRxBytes    = TrafficStats.getTotalRxBytes()
        lastTimestampMs = System.currentTimeMillis()

        while (true) {
            delay(intervalMs)
            val nowTx  = TrafficStats.getTotalTxBytes()
            val nowRx  = TrafficStats.getTotalRxBytes()
            val now    = System.currentTimeMillis()
            val elapsedSec = (now - lastTimestampMs) / 1000.0

            val uploadBps   = if (lastTxBytes >= 0 && elapsedSec > 0)
                ((nowTx - lastTxBytes) / elapsedSec).toLong().coerceAtLeast(0L) else 0L
            val downloadBps = if (lastRxBytes >= 0 && elapsedSec > 0)
                ((nowRx - lastRxBytes) / elapsedSec).toLong().coerceAtLeast(0L) else 0L

            emit(SpeedSnapshot(uploadBps, downloadBps, now))

            lastTxBytes     = nowTx
            lastRxBytes     = nowRx
            lastTimestampMs = now
        }
    }

    /**
     * Returns aggregated Wi-Fi + Mobile data usage for [startMs]–[endMs].
     * Requires READ_PHONE_STATE on API 23+.
     */
    suspend fun getUsageSummary(startMs: Long, endMs: Long): UsageSummary =
        withContext(Dispatchers.IO) {
            var tx = 0L; var rx = 0L
            try {
                val nsm = context.getSystemService(Context.NETWORK_STATS_SERVICE) as NetworkStatsManager
                for (type in listOf(ConnectivityManager.TYPE_WIFI, ConnectivityManager.TYPE_MOBILE)) {
                    try {
                        val bucket = NetworkStats.Bucket()
                        val stats  = nsm.querySummary(type, null, startMs, endMs)
                        while (stats.hasNextBucket()) {
                            stats.getNextBucket(bucket)
                            tx += bucket.txBytes
                            rx += bucket.rxBytes
                        }
                        stats.close()
                    } catch (e: Exception) {
                        Log.w(TAG, "querySummary type=$type: ${e.message}")
                    }
                }
            } catch (e: SecurityException) {
                Log.w(TAG, "READ_PHONE_STATE not granted: ${e.message}")
            } catch (e: Exception) {
                Log.e(TAG, "getUsageSummary: ${e.message}")
            }
            UsageSummary(tx, rx)
        }

    /** Periodic daily usage observer (refreshes every [intervalMs]). */
    fun observeDailyUsage(intervalMs: Long = 60_000L): Flow<UsageSummary> = flow {
        val dayStart = FormatUtils.startOfDayMs()
        while (true) {
            emit(getUsageSummary(dayStart, System.currentTimeMillis()))
            delay(intervalMs)
        }
    }
}

package com.shanufx.hotspotx.ui.usage

import android.content.Context
import android.content.Intent
import androidx.core.content.FileProvider
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.shanufx.hotspotx.data.db.dao.*
import com.shanufx.hotspotx.data.db.entity.SessionEntity
import com.shanufx.hotspotx.data.repository.SpeedSnapshot
import com.shanufx.hotspotx.data.repository.StatsRepository
import com.shanufx.hotspotx.service.HotspotService
import com.shanufx.hotspotx.util.FormatUtils
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.io.File
import java.text.SimpleDateFormat
import java.util.*
import javax.inject.Inject

data class UsageUiState(
    val realtimeUpload: List<Float> = emptyList(),
    val realtimeDownload: List<Float> = emptyList(),
    val hourlyLabels: List<String> = emptyList(),
    val hourlyUpload: List<Float> = emptyList(),
    val hourlyDownload: List<Float> = emptyList(),
    val dailyLabels: List<String> = emptyList(),
    val dailyUpload: List<Float> = emptyList(),
    val dailyDownload: List<Float> = emptyList(),
    val perDeviceMacs: List<String> = emptyList(),
    val perDeviceBytes: List<Float> = emptyList(),
    val todayTotalText: String = "0 B",
    val weekTotalText: String = "0 B",
    val monthTotalText: String = "0 B",
    val peakSpeedText: String = "0 B/s",
    val avgSessionText: String = "0s",
    val totalSessionsText: String = "0",
    val exportUri: android.net.Uri? = null
)

@HiltViewModel
class UsageViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    private val statsRepository: StatsRepository,
    private val usageSnapshotDao: UsageSnapshotDao,
    private val sessionDao: SessionDao
) : ViewModel() {

    private val _uiState = MutableStateFlow(UsageUiState())
    val uiState: StateFlow<UsageUiState> = _uiState.asStateFlow()

    private val realtimeUp = ArrayDeque<Float>(300)
    private val realtimeDown = ArrayDeque<Float>(300)

    init {
        observeRealtime()
        observeHourly()
        observeDaily()
        observeStats()
    }

    private fun observeRealtime() = viewModelScope.launch {
        HotspotService.speedFlow.collect { snap ->
            if (realtimeUp.size >= 300) { realtimeUp.removeFirst(); realtimeDown.removeFirst() }
            realtimeUp.addLast(snap.uploadBps.toFloat())
            realtimeDown.addLast(snap.downloadBps.toFloat())
            _uiState.update { it.copy(realtimeUpload = realtimeUp.toList(), realtimeDownload = realtimeDown.toList()) }
        }
    }

    private fun observeHourly() = viewModelScope.launch {
        val dayStart = FormatUtils.startOfDayMs()
        usageSnapshotDao.observeHourlyToday(dayStart).collect { rows ->
            val labels = (0..23).map { "%02d".format(it) }
            val upMap = rows.associate { it.hour to it.upload.toFloat() }
            val dnMap = rows.associate { it.hour to it.download.toFloat() }
            _uiState.update { it.copy(
                hourlyLabels = labels,
                hourlyUpload = labels.map { h -> upMap[h] ?: 0f },
                hourlyDownload = labels.map { h -> dnMap[h] ?: 0f }
            )}
        }
    }

    private fun observeDaily() = viewModelScope.launch {
        val monthStart = FormatUtils.startOfMonthMs()
        usageSnapshotDao.observeDaily(monthStart).collect { rows ->
            val fmt = SimpleDateFormat("MMM dd", Locale.getDefault())
            _uiState.update { it.copy(
                dailyLabels = rows.map { r -> r.day },
                dailyUpload = rows.map { r -> r.upload.toFloat() },
                dailyDownload = rows.map { r -> r.download.toFloat() }
            )}
        }
    }

    private fun observeStats() = viewModelScope.launch {
        val todayStart = FormatUtils.startOfDayMs()
        val weekStart = FormatUtils.startOfWeekMs()
        val monthStart = FormatUtils.startOfMonthMs()

        val todayBytes = usageSnapshotDao.totalBytesSince(todayStart) ?: 0L
        val weekBytes = usageSnapshotDao.totalBytesSince(weekStart) ?: 0L
        val monthBytes = usageSnapshotDao.totalBytesSince(monthStart) ?: 0L
        val peak = sessionDao.maxPeakSpeed() ?: 0L
        val avgMs = sessionDao.avgDurationMs() ?: 0.0
        val count = sessionDao.totalCount()

        _uiState.update { it.copy(
            todayTotalText = FormatUtils.formatBytes(todayBytes),
            weekTotalText = FormatUtils.formatBytes(weekBytes),
            monthTotalText = FormatUtils.formatBytes(monthBytes),
            peakSpeedText = FormatUtils.formatSpeed(peak),
            avgSessionText = FormatUtils.formatDuration(avgMs.toLong()),
            totalSessionsText = count.toString()
        )}
    }

    fun exportCsv() = viewModelScope.launch {
        try {
            val file = File(context.cacheDir, "hotspotx_usage_${System.currentTimeMillis()}.csv")
            file.bufferedWriter().use { w ->
                w.write("timestamp,upload_bytes,download_bytes,device_mac\n")
                val dayStart = FormatUtils.startOfDayMs() - 30L * 24 * 3600 * 1000
                usageSnapshotDao.observeSince(dayStart).firstOrNull()?.forEach { snap ->
                    w.write("${snap.timestamp},${snap.uploadBytes},${snap.downloadBytes},${snap.deviceMac ?: ""}\n")
                }
            }
            val uri = FileProvider.getUriForFile(context, "${context.packageName}.provider", file)
            _uiState.update { it.copy(exportUri = uri) }
        } catch (e: Exception) {
            // Silently fail — caller can show snackbar
        }
    }

    fun clearExportUri() { _uiState.update { it.copy(exportUri = null) } }
}

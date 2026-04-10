package com.shanufx.hotspotx.ui.dashboard

import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.shanufx.hotspotx.data.repository.*
import com.shanufx.hotspotx.service.HotspotService
import com.shanufx.hotspotx.util.FormatUtils
import com.shanufx.hotspotx.util.QrCodeGenerator
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

data class DashboardUiState(
    val isHotspotActive: Boolean  = false,
    val isLoading: Boolean        = false,
    val ssid: String              = "",
    val password: String          = "",
    val passwordVisible: Boolean  = false,
    val connectedDevices: Int     = 0,
    val uploadSpeedText: String   = "0 B/s",
    val downloadSpeedText: String = "0 B/s",
    val uploadBps: Long           = 0L,
    val downloadBps: Long         = 0L,
    val totalTodayText: String    = "0 B",
    val uploadHistory: List<Float>   = emptyList(),
    val downloadHistory: List<Float> = emptyList(),
    val errorMessage: String?     = null
)

@HiltViewModel
class DashboardViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    private val tetheringRepository: TetheringRepository,
    private val statsRepository: StatsRepository,
    private val deviceRepository: DeviceRepository,
    private val settingsRepository: SettingsRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(DashboardUiState())
    val uiState: StateFlow<DashboardUiState> = _uiState.asStateFlow()

    private val uploadHistory   = ArrayDeque<Float>(60)
    private val downloadHistory = ArrayDeque<Float>(60)

    private val _qrBitmap = MutableStateFlow<android.graphics.Bitmap?>(null)
    val qrBitmap: StateFlow<android.graphics.Bitmap?> = _qrBitmap.asStateFlow()

    init {
        observeHotspotState()
        observeSpeed()
        observeDailyUsage()
        observeDevices()
        observeSettings()
    }

    private fun observeHotspotState() = viewModelScope.launch {
        tetheringRepository.hotspotState.collect { state ->
            _uiState.update {
                when (state) {
                    is HotspotState.Active  -> it.copy(isHotspotActive = true,  isLoading = false,
                        ssid = state.ssid, password = state.password, errorMessage = null)
                    is HotspotState.Starting -> it.copy(isLoading = true, errorMessage = null)
                    is HotspotState.Error   -> it.copy(isLoading = false, isHotspotActive = false,
                        errorMessage = state.message)
                    is HotspotState.Idle    -> it.copy(isHotspotActive = false, isLoading = false)
                }
            }
        }
    }

    private fun observeSpeed() = viewModelScope.launch {
        HotspotService.speedFlow.collect { snap ->
            if (uploadHistory.size   >= 60) uploadHistory.removeFirst()
            if (downloadHistory.size >= 60) downloadHistory.removeFirst()
            uploadHistory.addLast(snap.uploadBps.toFloat())
            downloadHistory.addLast(snap.downloadBps.toFloat())
            _uiState.update {
                it.copy(
                    uploadBps         = snap.uploadBps,
                    downloadBps       = snap.downloadBps,
                    uploadSpeedText   = FormatUtils.formatSpeed(snap.uploadBps),
                    downloadSpeedText = FormatUtils.formatSpeed(snap.downloadBps),
                    uploadHistory     = uploadHistory.toList(),
                    downloadHistory   = downloadHistory.toList()
                )
            }
        }
    }

    private fun observeDailyUsage() = viewModelScope.launch {
        statsRepository.observeDailyUsage(60_000L).collect { summary ->
            _uiState.update {
                it.copy(totalTodayText = FormatUtils.formatBytes(summary.totalTxBytes + summary.totalRxBytes))
            }
        }
    }

    private fun observeDevices() = viewModelScope.launch {
        deviceRepository.connectedDevices.collect { devices ->
            _uiState.update { it.copy(connectedDevices = devices.size) }
        }
    }

    private fun observeSettings() = viewModelScope.launch {
        settingsRepository.settings.collect { settings ->
            if (!tetheringRepository.isHotspotActive()) {
                _uiState.update { it.copy(ssid = settings.hotspotSsid, password = settings.hotspotPassword) }
            }
        }
    }

    fun toggleHotspot() = viewModelScope.launch {
        if (tetheringRepository.isHotspotActive()) {
            tetheringRepository.stopHotspot()
            stopService()
        } else {
            val settings = settingsRepository.settings.first()
            val config = HotspotConfig(
                ssid       = settings.hotspotSsid,
                password   = settings.hotspotPassword,
                maxClients = settings.maxClients,
                isHidden   = settings.hiddenNetwork
            )
            startService()
            tetheringRepository.startHotspot(config)
        }
    }

    private fun startService() {
        val intent = Intent(context, HotspotService::class.java).apply { action = HotspotService.ACTION_START }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) context.startForegroundService(intent)
        else context.startService(intent)
    }

    private fun stopService() {
        context.stopService(Intent(context, HotspotService::class.java))
    }

    fun togglePasswordVisibility() = _uiState.update { it.copy(passwordVisible = !it.passwordVisible) }

    fun generateQrCode() = viewModelScope.launch {
        val ui = _uiState.value
        _qrBitmap.value = QrCodeGenerator.generateWifiQr(ui.ssid, ui.password)
    }

    fun dismissQr()    { _qrBitmap.value = null }
    fun dismissError() = _uiState.update { it.copy(errorMessage = null) }
    fun refreshDevices() = viewModelScope.launch { deviceRepository.refreshArp() }
}

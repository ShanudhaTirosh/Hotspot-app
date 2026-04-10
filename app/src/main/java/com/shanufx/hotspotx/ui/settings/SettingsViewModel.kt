package com.shanufx.hotspotx.ui.settings

import android.content.Context
import android.content.Intent
import androidx.core.content.FileProvider
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.gson.Gson
import com.shanufx.hotspotx.data.db.dao.UsageSnapshotDao
import com.shanufx.hotspotx.data.repository.AppSettings
import com.shanufx.hotspotx.data.repository.SettingsRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.io.File
import javax.inject.Inject

data class SettingsUiState(
    val settings: AppSettings = AppSettings(),
    val exportedUri: android.net.Uri? = null,
    val showResetConfirm: Boolean = false,
    val snackMessage: String? = null
)

@HiltViewModel
class SettingsViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    private val settingsRepository: SettingsRepository,
    private val usageSnapshotDao: UsageSnapshotDao
) : ViewModel() {

    private val _uiState = MutableStateFlow(SettingsUiState())
    val uiState: StateFlow<SettingsUiState> = _uiState.asStateFlow()

    private val gson = Gson()

    init {
        viewModelScope.launch {
            settingsRepository.settings.collect { s -> _uiState.update { it.copy(settings = s) } }
        }
    }

    fun updateTheme(mode: String) = viewModelScope.launch { settingsRepository.updateTheme(mode) }
    fun updateAutoBoot(enabled: Boolean) = viewModelScope.launch { settingsRepository.updateAutoBoot(enabled) }
    fun updateFgService(enabled: Boolean) = viewModelScope.launch { settingsRepository.updateForegroundService(enabled) }
    fun updateNotifyConnect(enabled: Boolean) = viewModelScope.launch { settingsRepository.updateNotifyConnect(enabled) }
    fun updateNotifyData(enabled: Boolean) = viewModelScope.launch { settingsRepository.updateNotifyData(enabled) }

    fun showResetConfirm() = _uiState.update { it.copy(showResetConfirm = true) }
    fun dismissResetConfirm() = _uiState.update { it.copy(showResetConfirm = false) }

    fun resetUsageStats() = viewModelScope.launch {
        usageSnapshotDao.deleteAll()
        _uiState.update { it.copy(showResetConfirm = false, snackMessage = "Usage stats cleared.") }
    }

    fun exportSettings() = viewModelScope.launch {
        try {
            val json = gson.toJson(_uiState.value.settings)
            val file = File(context.cacheDir, "hotspotx_settings.json")
            file.writeText(json)
            val uri = FileProvider.getUriForFile(context, "${context.packageName}.provider", file)
            _uiState.update { it.copy(exportedUri = uri) }
        } catch (e: Exception) {
            _uiState.update { it.copy(snackMessage = "Export failed: ${e.message}") }
        }
    }

    fun importSettings(json: String) = viewModelScope.launch {
        try {
            val s = gson.fromJson(json, AppSettings::class.java)
            settingsRepository.updateTheme(s.themeMode)
            settingsRepository.updateSsid(s.hotspotSsid)
            settingsRepository.updatePassword(s.hotspotPassword)
            settingsRepository.updateBand(s.hotspotBand)
            settingsRepository.updateMaxClients(s.maxClients)
            settingsRepository.updateHidden(s.hiddenNetwork)
            settingsRepository.updateAutoOff(s.autoOffMinutes)
            settingsRepository.updateAutoBoot(s.autoStartOnBoot)
            settingsRepository.updateForegroundService(s.foregroundServiceEnabled)
            settingsRepository.updateDataCap(s.monthlyDataCapBytes)
            settingsRepository.updateCapWarning(s.dataCapWarningPercent)
            _uiState.update { it.copy(snackMessage = "Settings imported successfully.") }
        } catch (e: Exception) {
            _uiState.update { it.copy(snackMessage = "Import failed: ${e.message}") }
        }
    }

    fun clearExportUri() = _uiState.update { it.copy(exportedUri = null) }
    fun clearSnack() = _uiState.update { it.copy(snackMessage = null) }
}

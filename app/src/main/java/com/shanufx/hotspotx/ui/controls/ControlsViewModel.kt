package com.shanufx.hotspotx.ui.controls

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.shanufx.hotspotx.data.db.dao.ScheduleDao
import com.shanufx.hotspotx.data.db.entity.ScheduleEntity
import com.shanufx.hotspotx.data.repository.AppSettings
import com.shanufx.hotspotx.data.repository.SettingsRepository
import com.shanufx.hotspotx.worker.ScheduleWorker
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

data class ControlsUiState(
    val settings: AppSettings = AppSettings(),
    val schedules: List<ScheduleEntity> = emptyList(),
    val showAddSchedule: Boolean = false,
    val editingSchedule: ScheduleEntity? = null
)

@HiltViewModel
class ControlsViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    private val settingsRepository: SettingsRepository,
    private val scheduleDao: ScheduleDao
) : ViewModel() {

    private val _uiState = MutableStateFlow(ControlsUiState())
    val uiState: StateFlow<ControlsUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            settingsRepository.settings.collect { s ->
                _uiState.update { it.copy(settings = s) }
            }
        }
        viewModelScope.launch {
            scheduleDao.observeAll().collect { list ->
                _uiState.update { it.copy(schedules = list) }
            }
        }
    }

    fun updateSsid(ssid: String) = viewModelScope.launch { settingsRepository.updateSsid(ssid) }
    fun updatePassword(pw: String) = viewModelScope.launch { settingsRepository.updatePassword(pw) }
    fun updateBand(band: Int) = viewModelScope.launch { settingsRepository.updateBand(band) }
    fun updateMaxClients(max: Int) = viewModelScope.launch { settingsRepository.updateMaxClients(max) }
    fun updateHidden(hidden: Boolean) = viewModelScope.launch { settingsRepository.updateHidden(hidden) }
    fun updateAutoOff(minutes: Int) = viewModelScope.launch { settingsRepository.updateAutoOff(minutes) }
    fun updateDataCap(bytes: Long) = viewModelScope.launch { settingsRepository.updateDataCap(bytes) }
    fun updateCapWarning(percent: Int) = viewModelScope.launch { settingsRepository.updateCapWarning(percent) }

    fun showAddSchedule() = _uiState.update { it.copy(showAddSchedule = true, editingSchedule = null) }
    fun editSchedule(s: ScheduleEntity) = _uiState.update { it.copy(showAddSchedule = true, editingSchedule = s) }
    fun dismissScheduleDialog() = _uiState.update { it.copy(showAddSchedule = false, editingSchedule = null) }

    fun saveSchedule(schedule: ScheduleEntity) = viewModelScope.launch {
        if (schedule.id == 0L) scheduleDao.insert(schedule) else scheduleDao.update(schedule)
        dismissScheduleDialog()
        ScheduleWorker.enqueue(context)
    }

    fun deleteSchedule(schedule: ScheduleEntity) = viewModelScope.launch {
        scheduleDao.delete(schedule)
        ScheduleWorker.enqueue(context)
    }

    fun toggleSchedule(id: Long, enabled: Boolean) = viewModelScope.launch {
        scheduleDao.setEnabled(id, enabled)
        ScheduleWorker.enqueue(context)
    }
}

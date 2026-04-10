package com.shanufx.hotspotx.ui.devices

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.shanufx.hotspotx.data.repository.ConnectedDevice
import com.shanufx.hotspotx.data.repository.DeviceRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

data class DevicesUiState(
    val connectedDevices: List<ConnectedDevice> = emptyList(),
    val isRefreshing: Boolean = false,
    val renameDialogMac: String? = null,
    val renameDialogCurrentName: String = "",
    val blockWarningMac: String? = null
)

@HiltViewModel
class DevicesViewModel @Inject constructor(
    private val deviceRepository: DeviceRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(DevicesUiState())
    val uiState: StateFlow<DevicesUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            deviceRepository.connectedDevices.collect { devices ->
                _uiState.update { it.copy(connectedDevices = devices) }
            }
        }
    }

    fun refresh() {
        viewModelScope.launch {
            _uiState.update { it.copy(isRefreshing = true) }
            deviceRepository.refreshArp()
            _uiState.update { it.copy(isRefreshing = false) }
        }
    }

    fun blockDevice(mac: String) {
        _uiState.update { it.copy(blockWarningMac = mac) }
    }

    fun confirmBlock(mac: String) {
        viewModelScope.launch {
            deviceRepository.blockDevice(mac)
            _uiState.update { it.copy(blockWarningMac = null) }
        }
    }

    fun allowDevice(mac: String) {
        viewModelScope.launch { deviceRepository.allowDevice(mac) }
    }

    fun pinDevice(mac: String, pinned: Boolean) {
        viewModelScope.launch { deviceRepository.pinDevice(mac, pinned) }
    }

    fun showRenameDialog(mac: String, currentName: String) {
        _uiState.update { it.copy(renameDialogMac = mac, renameDialogCurrentName = currentName) }
    }

    fun confirmRename(mac: String, newName: String) {
        viewModelScope.launch {
            deviceRepository.renameDevice(mac, newName)
            _uiState.update { it.copy(renameDialogMac = null) }
        }
    }

    fun dismissDialogs() {
        _uiState.update { it.copy(renameDialogMac = null, blockWarningMac = null) }
    }
}

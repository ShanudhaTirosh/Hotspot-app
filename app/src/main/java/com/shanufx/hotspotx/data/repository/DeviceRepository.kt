package com.shanufx.hotspotx.data.repository

import com.shanufx.hotspotx.data.db.dao.DeviceDao
import com.shanufx.hotspotx.data.db.entity.DeviceEntity
import com.shanufx.hotspotx.util.ArpScanner
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

data class ConnectedDevice(
    val mac: String,
    val ip: String,
    val hostname: String?,
    val deviceType: String,
    val nickname: String?,
    val isBlocked: Boolean,
    val isPinned: Boolean,
    val firstSeen: Long,
    val lastSeen: Long,
    val uploadBps: Long = 0L,
    val downloadBps: Long = 0L,
    val status: DeviceStatus = DeviceStatus.ACTIVE
)

enum class DeviceStatus { ACTIVE, IDLE, BLOCKED }

@Singleton
class DeviceRepository @Inject constructor(
    private val deviceDao: DeviceDao
) {
    private val _connectedDevices = MutableStateFlow<List<ConnectedDevice>>(emptyList())
    val connectedDevices: StateFlow<List<ConnectedDevice>> = _connectedDevices.asStateFlow()

    /** Per-device speed tracking: mac → (lastTx, lastRx) */
    private val speedCache = mutableMapOf<String, Pair<Long, Long>>()

    /** All devices from Room (persisted across sessions) */
    val allDevices: Flow<List<DeviceEntity>> = deviceDao.observeAll()

    /**
     * Polls ARP table every [intervalMs] and updates [connectedDevices].
     * Call from a coroutine scope tied to the service lifecycle.
     */
    suspend fun startScanning(intervalMs: Long = 3000L) {
        while (true) {
            refreshArp()
            delay(intervalMs)
        }
    }

    suspend fun refreshArp() = withContext(Dispatchers.IO) {
        val arpEntries = ArpScanner.scan()
        val blockedMacs = deviceDao.getBlockedDevices().map { it.mac }.toSet()
        val dbDevices = mutableMapOf<String, DeviceEntity>()

        val connected = arpEntries.mapNotNull { entry ->
            // Skip gateway IPs (typically .1)
            if (entry.ip.endsWith(".1")) return@mapNotNull null

            val hostname = ArpScanner.resolveHostname(entry.ip)
            val deviceType = ArpScanner.guessDeviceType(hostname, entry.mac)

            // Persist device sighting to DB
            deviceDao.upsertSeen(entry.mac, hostname, deviceType)
            val dbEntity = deviceDao.getByMac(entry.mac)

            val isBlocked = entry.mac in blockedMacs
            ConnectedDevice(
                mac = entry.mac,
                ip = entry.ip,
                hostname = hostname,
                deviceType = dbEntity?.deviceType ?: deviceType,
                nickname = dbEntity?.nickname,
                isBlocked = dbEntity?.isBlocked ?: false,
                isPinned = dbEntity?.isPinned ?: false,
                firstSeen = dbEntity?.firstSeen ?: System.currentTimeMillis(),
                lastSeen = System.currentTimeMillis(),
                status = when {
                    isBlocked -> DeviceStatus.BLOCKED
                    else -> DeviceStatus.ACTIVE
                }
            )
        }

        _connectedDevices.value = connected.sortedWith(
            compareByDescending<ConnectedDevice> { it.isPinned }
                .thenBy { it.status.ordinal }
        )
    }

    suspend fun blockDevice(mac: String) = deviceDao.setBlocked(mac, true)
    suspend fun allowDevice(mac: String) = deviceDao.setBlocked(mac, false)
    suspend fun pinDevice(mac: String, pinned: Boolean) = deviceDao.setPinned(mac, pinned)
    suspend fun renameDevice(mac: String, name: String) = deviceDao.rename(mac, name)

    fun connectedCount(): Int = _connectedDevices.value.size
}

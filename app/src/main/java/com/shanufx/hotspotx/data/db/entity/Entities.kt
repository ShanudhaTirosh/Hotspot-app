package com.shanufx.hotspotx.data.db.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "devices")
data class DeviceEntity(
    @PrimaryKey val mac: String,
    val nickname: String? = null,
    val isBlocked: Boolean = false,
    val isPinned: Boolean = false,
    val firstSeen: Long = System.currentTimeMillis(),
    val lastSeen: Long = System.currentTimeMillis(),
    /** Detected device type: phone / laptop / tablet / unknown */
    val deviceType: String = "unknown"
)

@Entity(tableName = "usage_snapshots")
data class UsageSnapshotEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val timestamp: Long = System.currentTimeMillis(),
    val uploadBytes: Long = 0L,
    val downloadBytes: Long = 0L,
    /** NULL means aggregate (all devices combined) */
    val deviceMac: String? = null
)

@Entity(tableName = "schedules")
data class ScheduleEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    /** Bitmask: Sun=1,Mon=2,Tue=4,Wed=8,Thu=16,Fri=32,Sat=64 */
    val daysOfWeekMask: Int = 0b1111111,
    /** Minutes since midnight, e.g. 08:30 → 510 */
    val startMinutes: Int = 480,  // 08:00
    val endMinutes: Int = 1320,   // 22:00
    val isEnabled: Boolean = true,
    val label: String = ""
)

@Entity(tableName = "sessions")
data class SessionEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val startTime: Long = System.currentTimeMillis(),
    val endTime: Long? = null,
    val totalUploadBytes: Long = 0L,
    val totalDownloadBytes: Long = 0L,
    val peakSpeedBps: Long = 0L,
    val maxConnectedDevices: Int = 0
)

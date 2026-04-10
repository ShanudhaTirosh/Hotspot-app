package com.shanufx.hotspotx.data.db.dao

import androidx.room.*
import com.shanufx.hotspotx.data.db.entity.DeviceEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface DeviceDao {
    @Query("SELECT * FROM devices ORDER BY isPinned DESC, lastSeen DESC")
    fun observeAll(): Flow<List<DeviceEntity>>

    @Query("SELECT * FROM devices WHERE mac = :mac LIMIT 1")
    suspend fun getByMac(mac: String): DeviceEntity?

    @Query("SELECT * FROM devices WHERE isBlocked = 1")
    suspend fun getBlockedDevices(): List<DeviceEntity>

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insert(device: DeviceEntity)

    @Update
    suspend fun update(device: DeviceEntity)

    @Query("""
        INSERT INTO devices (mac, nickname, isBlocked, isPinned, firstSeen, lastSeen, deviceType)
        VALUES (:mac, :nickname, 0, 0, :now, :now, :deviceType)
        ON CONFLICT(mac) DO UPDATE SET lastSeen = :now
    """)
    suspend fun upsertSeen(mac: String, nickname: String?, deviceType: String, now: Long = System.currentTimeMillis())

    @Query("UPDATE devices SET isBlocked = :blocked WHERE mac = :mac")
    suspend fun setBlocked(mac: String, blocked: Boolean)

    @Query("UPDATE devices SET isPinned = :pinned WHERE mac = :mac")
    suspend fun setPinned(mac: String, pinned: Boolean)

    @Query("UPDATE devices SET nickname = :name WHERE mac = :mac")
    suspend fun rename(mac: String, name: String)

    @Query("DELETE FROM devices WHERE mac = :mac")
    suspend fun delete(mac: String)
}

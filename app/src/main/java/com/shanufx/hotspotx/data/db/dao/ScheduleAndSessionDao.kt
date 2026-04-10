package com.shanufx.hotspotx.data.db.dao

import androidx.room.*
import com.shanufx.hotspotx.data.db.entity.ScheduleEntity
import com.shanufx.hotspotx.data.db.entity.SessionEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface ScheduleDao {
    @Query("SELECT * FROM schedules ORDER BY id ASC")
    fun observeAll(): Flow<List<ScheduleEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(schedule: ScheduleEntity): Long

    @Update
    suspend fun update(schedule: ScheduleEntity)

    @Delete
    suspend fun delete(schedule: ScheduleEntity)

    @Query("DELETE FROM schedules WHERE id = :id")
    suspend fun deleteById(id: Long)

    @Query("UPDATE schedules SET isEnabled = :enabled WHERE id = :id")
    suspend fun setEnabled(id: Long, enabled: Boolean)
}

@Dao
interface SessionDao {
    @Query("SELECT * FROM sessions ORDER BY startTime DESC LIMIT 50")
    fun observeRecent(): Flow<List<SessionEntity>>

    @Query("SELECT COUNT(*) FROM sessions")
    suspend fun totalCount(): Int

    @Query("SELECT AVG(endTime - startTime) FROM sessions WHERE endTime IS NOT NULL")
    suspend fun avgDurationMs(): Double?

    @Query("SELECT MAX(peakSpeedBps) FROM sessions")
    suspend fun maxPeakSpeed(): Long?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(session: SessionEntity): Long

    @Update
    suspend fun update(session: SessionEntity)

    @Query("UPDATE sessions SET endTime = :end, totalUploadBytes = :up, totalDownloadBytes = :down, peakSpeedBps = :peak, maxConnectedDevices = :maxDev WHERE id = :id")
    suspend fun close(id: Long, end: Long, up: Long, down: Long, peak: Long, maxDev: Int)

    @Query("SELECT * FROM sessions WHERE endTime IS NULL LIMIT 1")
    suspend fun getOpenSession(): SessionEntity?
}

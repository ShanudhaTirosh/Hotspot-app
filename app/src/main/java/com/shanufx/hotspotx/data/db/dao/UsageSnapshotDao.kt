package com.shanufx.hotspotx.data.db.dao

import androidx.room.*
import com.shanufx.hotspotx.data.db.entity.UsageSnapshotEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface UsageSnapshotDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(snapshot: UsageSnapshotEntity)

    /** Snapshots for the last N milliseconds (aggregate only) */
    @Query("SELECT * FROM usage_snapshots WHERE deviceMac IS NULL AND timestamp >= :since ORDER BY timestamp ASC")
    fun observeSince(since: Long): Flow<List<UsageSnapshotEntity>>

    /** Hourly totals for today */
    @Query("""
        SELECT strftime('%H', datetime(timestamp/1000, 'unixepoch')) AS hour,
               SUM(uploadBytes) AS upload, SUM(downloadBytes) AS download
        FROM usage_snapshots
        WHERE deviceMac IS NULL AND timestamp >= :dayStart
        GROUP BY hour
        ORDER BY hour
    """)
    fun observeHourlyToday(dayStart: Long): Flow<List<HourlyUsage>>

    /** Daily totals for the last 30 days */
    @Query("""
        SELECT strftime('%Y-%m-%d', datetime(timestamp/1000, 'unixepoch')) AS day,
               SUM(uploadBytes) AS upload, SUM(downloadBytes) AS download
        FROM usage_snapshots
        WHERE deviceMac IS NULL AND timestamp >= :monthStart
        GROUP BY day
        ORDER BY day
    """)
    fun observeDaily(monthStart: Long): Flow<List<DailyUsage>>

    /** Session usage per device */
    @Query("""
        SELECT deviceMac, SUM(uploadBytes) AS upload, SUM(downloadBytes) AS download
        FROM usage_snapshots
        WHERE deviceMac IS NOT NULL AND timestamp >= :since
        GROUP BY deviceMac
    """)
    fun observePerDevice(since: Long): Flow<List<DeviceUsage>>

    @Query("SELECT SUM(uploadBytes + downloadBytes) FROM usage_snapshots WHERE deviceMac IS NULL AND timestamp >= :since")
    suspend fun totalBytesSince(since: Long): Long?

    @Query("DELETE FROM usage_snapshots WHERE timestamp < :before")
    suspend fun pruneOlderThan(before: Long)

    @Query("DELETE FROM usage_snapshots")
    suspend fun deleteAll()
}

data class HourlyUsage(val hour: String, val upload: Long, val download: Long)
data class DailyUsage(val day: String, val upload: Long, val download: Long)
data class DeviceUsage(val deviceMac: String?, val upload: Long, val download: Long)

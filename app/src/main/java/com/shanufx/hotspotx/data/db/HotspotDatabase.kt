package com.shanufx.hotspotx.data.db

import androidx.room.Database
import androidx.room.RoomDatabase
import com.shanufx.hotspotx.data.db.dao.*
import com.shanufx.hotspotx.data.db.entity.*

@Database(
    entities = [
        DeviceEntity::class,
        UsageSnapshotEntity::class,
        ScheduleEntity::class,
        SessionEntity::class
    ],
    version = 1,
    exportSchema = false
)
abstract class HotspotDatabase : RoomDatabase() {
    abstract fun deviceDao(): DeviceDao
    abstract fun usageSnapshotDao(): UsageSnapshotDao
    abstract fun scheduleDao(): ScheduleDao
    abstract fun sessionDao(): SessionDao
}

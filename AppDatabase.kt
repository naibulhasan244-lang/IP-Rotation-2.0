package com.yourname.iprotation2.data.local

import androidx.room.Database
import androidx.room.RoomDatabase

@Database(
    entities = [ProxyEntity::class, IpHistoryEntity::class],
    version = 1,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun proxyDao(): ProxyDao
    abstract fun historyDao(): IpHistoryDao
}

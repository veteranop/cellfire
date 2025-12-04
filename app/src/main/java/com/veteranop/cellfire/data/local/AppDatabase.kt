package com.veteranop.cellfire.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import com.veteranop.cellfire.data.model.DiscoveredPci

@Database(
    entities = [DiscoveredPci::class],
    version = 6,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun discoveredPciDao(): DiscoveredPciDao
}
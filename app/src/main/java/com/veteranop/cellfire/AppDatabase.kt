package com.veteranop.cellfire

import androidx.room.Database
import androidx.room.RoomDatabase

@Database(entities = [DiscoveredPci::class], version = 1, exportSchema = false)
abstract class AppDatabase : RoomDatabase() {
    abstract fun discoveredPciDao(): DiscoveredPciDao
}
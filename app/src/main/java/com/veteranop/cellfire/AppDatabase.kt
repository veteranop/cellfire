package com.veteranop.cellfire

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Singleton

@Database(
    entities = [DiscoveredPci::class, DriveTestPoint::class],
    version = 3,
    exportSchema = false
)
@Singleton
abstract class AppDatabase : RoomDatabase() {
    abstract fun discoveredPciDao(): DiscoveredPciDao
    abstract fun driveTestPointDao(): DriveTestPointDao

    // Remove companion object (DatabaseModule provides singleton instance)
}

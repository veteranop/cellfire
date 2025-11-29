package com.veteranop.cellfire

import android.content.Context
import androidx.room.Room
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@InstallIn(SingletonComponent::class)
@Module
object DatabaseModule {
    @Provides
    @Singleton
    fun provideAppDatabase(@ApplicationContext context: Context): AppDatabase {
        return Room.databaseBuilder(
            context,
            AppDatabase::class.java,
            "cellfire-db"
        ).build()
    }

    @Provides
    fun provideDiscoveredPciDao(appDatabase: AppDatabase): DiscoveredPciDao {
        return appDatabase.discoveredPciDao()
    }
}
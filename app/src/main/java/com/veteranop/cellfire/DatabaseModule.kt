package com.veteranop.cellfire

import android.content.Context
import androidx.room.Room
import com.veteranop.cellfire.data.local.AppDatabase
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {
    @Provides @Singleton
    fun provideDatabase(@ApplicationContext context: Context) =
        Room.databaseBuilder(context, AppDatabase::class.java, "cellfire.db")
            .fallbackToDestructiveMigration()
            .build()

    @Provides @Singleton
    fun provideDao(db: AppDatabase) = db.discoveredPciDao()
}
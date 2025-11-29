package com.veteranop.cellfire

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface DiscoveredPciDao {
    @Query("SELECT * FROM discovered_pcis")
    fun getAll(): Flow<List<DiscoveredPci>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(pci: DiscoveredPci)

    @Query("SELECT * FROM discovered_pcis WHERE pci = :pci LIMIT 1")
    suspend fun getPci(pci: Int): DiscoveredPci?
}
package com.veteranop.cellfire.data.local

import androidx.room.*
import com.veteranop.cellfire.data.local.entities.DiscoveredPci
import kotlinx.coroutines.flow.Flow

@Dao
interface DiscoveredPciDao {
    @Query("SELECT * FROM discovered_pci ORDER BY lastSeen DESC")
    fun getAll(): Flow<List<DiscoveredPci>>

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insert(pci: DiscoveredPci)

    @Update
    suspend fun update(pci: DiscoveredPci)

    @Query("SELECT * FROM discovered_pci WHERE pci = :pci")
    suspend fun getByPci(pci: Int): DiscoveredPci?

    @Query("DELETE FROM discovered_pci")
    suspend fun clearAll()
}
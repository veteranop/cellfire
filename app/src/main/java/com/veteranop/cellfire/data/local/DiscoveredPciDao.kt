package com.veteranop.cellfire.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.veteranop.cellfire.data.model.DiscoveredPci
import kotlinx.coroutines.flow.Flow

@Dao
interface DiscoveredPciDao {
    @Query("SELECT * FROM discovered_pcis")
    fun getAll(): Flow<List<DiscoveredPci>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(pci: DiscoveredPci)

    @Update
    suspend fun update(pci: DiscoveredPci)

    @Query("SELECT * FROM discovered_pcis WHERE pci = :pci LIMIT 1")
    suspend fun getByPci(pci: Int): DiscoveredPci?
    
    @Query("DELETE FROM discovered_pcis")
    suspend fun clearAll()
}

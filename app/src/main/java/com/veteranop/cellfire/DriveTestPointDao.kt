package com.veteranop.cellfire

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface DriveTestPointDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(point: DriveTestPoint)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(points: List<DriveTestPoint>)

    @Query("SELECT * FROM drive_test_points WHERE pci = :pci ORDER BY timestamp ASC")
    fun getPointsForPci(pci: Int): Flow<List<DriveTestPoint>>

    @Query("SELECT * FROM drive_test_points ORDER BY timestamp ASC")
    fun getAllPoints(): Flow<List<DriveTestPoint>>

    @Query("DELETE FROM drive_test_points WHERE pci = :pci")
    suspend fun deletePointsForPci(pci: Int)

    @Query("DELETE FROM drive_test_points")
    suspend fun clearAll()
}

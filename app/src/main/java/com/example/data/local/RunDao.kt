package com.example.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface RunDao {
    @Query("SELECT * FROM runs ORDER BY timestamp DESC")
    fun getAllRuns(): Flow<List<RunEntity>>

    @Query("SELECT * FROM runs WHERE id = :id LIMIT 1")
    suspend fun getRunById(id: String): RunEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertRun(run: RunEntity)

    @Update
    suspend fun updateRun(run: RunEntity)

    @Query("DELETE FROM runs WHERE id = :id")
    suspend fun deleteRunById(id: String)

    @Query("SELECT SUM(distanceMeters) FROM runs")
    fun getTotalDistanceMeters(): Flow<Double?>

    @Query("SELECT COUNT(*) FROM runs")
    fun getTotalRunsCount(): Flow<Int>

    @Query("SELECT COUNT(*) FROM runs")
    suspend fun getRunsCountDirect(): Int

    @Query("SELECT SUM(durationSeconds) FROM runs")
    fun getTotalDurationSeconds(): Flow<Long?>
}

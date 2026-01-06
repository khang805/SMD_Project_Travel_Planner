package com.example.smd_project_travel_planner.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query

@Dao
interface TripDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(trip: TripEntity)

    @Query("SELECT * FROM trips")
    suspend fun getAll(): List<TripEntity>

    @Query("SELECT * FROM trips WHERE isSynced = 0")
    suspend fun getUnsynced(): List<TripEntity>

    @Query("UPDATE trips SET isSynced = 1 WHERE id = :id")
    suspend fun markSynced(id: String)
    
    @Query("DELETE FROM trips WHERE id = :id")
    suspend fun deleteById(id: String)
    
    @Query("DELETE FROM trips")
    suspend fun clearAll()
}

package com.example.smd_project_travel_planner.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query

@Dao
interface ExpenseDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(expense: ExpenseEntity)

    @Query("SELECT * FROM expenses WHERE tripId = :tripId")
    suspend fun getAllForTrip(tripId: String): List<ExpenseEntity>

    @Query("SELECT * FROM expenses WHERE isSynced = 0")
    suspend fun getUnsynced(): List<ExpenseEntity>

    @Query("UPDATE expenses SET isSynced = 1 WHERE id = :id")
    suspend fun markSynced(id: String)
}

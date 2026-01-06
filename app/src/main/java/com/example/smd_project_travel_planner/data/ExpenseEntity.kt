package com.example.smd_project_travel_planner.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "expenses")
data class ExpenseEntity(
    @PrimaryKey val id: String,
    val tripId: String,
    val day: String,
    val title: String,
    val type: String,
    val time: String,
    val date: String,
    val cost: Double,
    val isSynced: Boolean = false
)

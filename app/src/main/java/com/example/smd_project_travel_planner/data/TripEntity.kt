package com.example.smd_project_travel_planner.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "trips")
data class TripEntity(
    @PrimaryKey val id: String,
    val title: String,
    val destination: String,
    val startDate: String,
    val endDate: String,
    val imageBase64: String,
    val isSynced: Boolean = false
)

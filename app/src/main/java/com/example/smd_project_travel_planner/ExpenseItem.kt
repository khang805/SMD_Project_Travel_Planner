package com.example.smd_project_travel_planner

data class ExpenseItem(
    val id: String = "",
    val tripId: String = "",
    val day: String = "",
    val title: String = "",
    val category: String = "",
    val amount: Double = 0.0
)

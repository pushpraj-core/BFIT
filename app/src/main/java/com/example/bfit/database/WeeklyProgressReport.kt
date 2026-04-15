package com.example.bfit.database

/**
 * Data class representing a weekly progress report.
 * Aggregates daily nutrition and completion data over a 7-day period
 * for display in the Progress screen.
 */
data class WeeklyProgressReport(
    val completedDays: Int,
    val totalCalories: Int,
    val totalProtein: Int,
    val averageCalories: Int,
    val averageProtein: Int,
    val daysLogged: Int
)

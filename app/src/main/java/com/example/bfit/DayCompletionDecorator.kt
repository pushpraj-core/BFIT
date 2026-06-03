package com.example.bfit

/**
 * Helper class that tracks which dates have been completed.
 * Used by PlannerActivity to visually indicate completed days.
 */
class DayCompletionDecorator(private val completedDates: Set<Long>) {

    fun shouldDecorate(day: Long): Boolean {
        return completedDates.contains(day)
    }
}


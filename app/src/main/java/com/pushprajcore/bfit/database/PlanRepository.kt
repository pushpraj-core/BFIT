package com.pushprajcore.bfit.database

import android.content.Context
import android.content.SharedPreferences
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.Calendar

/**
 * Repository layer for all local data operations.
 * Provides a clean API for ViewModels/Activities to interact with
 * Room database and SharedPreferences.
 *
 * All database operations are suspend functions that must be called
 * from a coroutine scope (e.g., viewModelScope or lifecycleScope).
 * This eliminates the previous runBlocking anti-pattern that caused ANR risk.
 */
class PlanRepository(context: Context) {

    private val prefs: SharedPreferences = context.getSharedPreferences("plan_prefs", Context.MODE_PRIVATE)
    private val planDao = PlanDatabase.getDatabase(context).planDao()

    // ─── Day Completion ───

    fun markDayAsComplete(date: Long) {
        prefs.edit().putBoolean(date.toString(), true).apply()
    }

    fun isDayComplete(date: Long): Boolean {
        return prefs.getBoolean(date.toString(), false)
    }

    // ─── Plan Item Completion ───

    /**
     * Marks a plan item as complete/incomplete and updates daily nutrition totals.
     * Must be called from a coroutine scope.
     */
    suspend fun markPlanItemAsComplete(id: String, isCompleted: Boolean, calories: Int, protein: Int) {
        withContext(Dispatchers.IO) {
            planDao.insertPlanItemCompletion(PlanItemCompletion(id, isCompleted))
            val date = id.split("-").firstOrNull()?.toLongOrNull() ?: return@withContext
            updateNutrientsForDay(date, isCompleted, calories, protein)
        }
    }

    suspend fun isPlanItemComplete(id: String): Boolean {
        return withContext(Dispatchers.IO) {
            planDao.getPlanItemCompletion(id)?.isCompleted ?: false
        }
    }

    private suspend fun updateNutrientsForDay(date: Long, isCompleted: Boolean, calories: Int, protein: Int) {
        val dailyLog = planDao.getDailyLog(date)
        if (dailyLog != null) {
            val updatedCalories = if (isCompleted) {
                dailyLog.totalCalories + calories
            } else {
                (dailyLog.totalCalories - calories).coerceAtLeast(0)
            }
            val updatedProtein = if (isCompleted) {
                dailyLog.totalProtein + protein
            } else {
                (dailyLog.totalProtein - protein).coerceAtLeast(0)
            }
            val updatedLog = dailyLog.copy(totalCalories = updatedCalories, totalProtein = updatedProtein)
            planDao.insertDailyLog(updatedLog)
        } else {
            if (isCompleted) {
                planDao.insertDailyLog(DailyLog(date, calories, protein))
            }
        }
    }

    // ─── Daily Nutrition Logs ───

    suspend fun addCaloriesToDailyLog(date: Long, calories: Int, protein: Int) {
        withContext(Dispatchers.IO) {
            val dailyLog = planDao.getDailyLog(date)
            if (dailyLog != null) {
                val updatedLog = dailyLog.copy(
                    totalCalories = dailyLog.totalCalories + calories,
                    totalProtein = dailyLog.totalProtein + protein
                )
                planDao.insertDailyLog(updatedLog)
            } else {
                planDao.insertDailyLog(DailyLog(date, calories, protein))
            }
        }
    }

    suspend fun getDailyLog(date: Long): DailyLog? {
        return withContext(Dispatchers.IO) {
            planDao.getDailyLog(date)
        }
    }

    suspend fun updateDailyLog(date: Long, calories: Int, protein: Int) {
        withContext(Dispatchers.IO) {
            planDao.insertDailyLog(DailyLog(date, calories, protein))
        }
    }

    // ─── Extra Meal Items ───

    suspend fun addExtraMealItem(extraMealItem: ExtraMealItem) {
        withContext(Dispatchers.IO) {
            planDao.insertExtraMealItem(extraMealItem)
        }
    }

    suspend fun getExtraMealItems(date: Long): List<ExtraMealItem> {
        return withContext(Dispatchers.IO) {
            planDao.getExtraMealItems(date)
        }
    }

    // ─── Streak Tracking ───

    fun getStreak(): Int {
        var streak = 0
        val calendar = Calendar.getInstance()
        calendar.set(Calendar.HOUR_OF_DAY, 0)
        calendar.set(Calendar.MINUTE, 0)
        calendar.set(Calendar.SECOND, 0)
        calendar.set(Calendar.MILLISECOND, 0)

        while (isDayComplete(calendar.timeInMillis)) {
            streak++
            calendar.add(Calendar.DAY_OF_YEAR, -1)
        }
        return streak
    }

    // ─── Weight Tracking ───

    suspend fun addWeightEntry(weight: Float, bmi: Float) {
        withContext(Dispatchers.IO) {
            val today = Calendar.getInstance().apply {
                set(Calendar.HOUR_OF_DAY, 0)
                set(Calendar.MINUTE, 0)
                set(Calendar.SECOND, 0)
                set(Calendar.MILLISECOND, 0)
            }
            planDao.insertWeightEntry(WeightEntry(date = today.timeInMillis, weight = weight, bmi = bmi))
        }
    }

    suspend fun getRecentWeightEntries(limit: Int = 30): List<WeightEntry> {
        return withContext(Dispatchers.IO) {
            planDao.getRecentWeightEntries(limit)
        }
    }

    suspend fun getLatestWeightEntry(): WeightEntry? {
        return withContext(Dispatchers.IO) {
            planDao.getLatestWeightEntry()
        }
    }

    suspend fun getWeightEntriesSince(startDate: Long): List<WeightEntry> {
        return withContext(Dispatchers.IO) {
            planDao.getWeightEntriesSince(startDate)
        }
    }

    // ─── Weight Log (for ProgressActivity) ───

    suspend fun addWeightLogEntry(date: Long, weightKg: Float) {
        withContext(Dispatchers.IO) {
            planDao.insertWeightLogEntry(WeightLogEntry(date = date, weightKg = weightKg))
        }
    }

    suspend fun getWeightLogEntriesBetween(startDate: Long, endDate: Long): List<WeightLogEntry> {
        return withContext(Dispatchers.IO) {
            planDao.getWeightLogEntriesBetween(startDate, endDate)
        }
    }

    suspend fun getLatestWeightLogEntry(): WeightLogEntry? {
        return withContext(Dispatchers.IO) {
            planDao.getLatestWeightLogEntry()
        }
    }

    // ─── Weekly Progress Report ───

    suspend fun getWeeklyProgressReport(): WeeklyProgressReport {
        return withContext(Dispatchers.IO) {
            val calendar = Calendar.getInstance().apply {
                set(Calendar.HOUR_OF_DAY, 0)
                set(Calendar.MINUTE, 0)
                set(Calendar.SECOND, 0)
                set(Calendar.MILLISECOND, 0)
            }
            val today = calendar.timeInMillis
            calendar.add(Calendar.DAY_OF_YEAR, -6)
            val weekStart = calendar.timeInMillis

            val dailyLogs = planDao.getDailyLogsBetween(weekStart, today)
            val totalCalories = dailyLogs.sumOf { it.totalCalories }
            val totalProtein = dailyLogs.sumOf { it.totalProtein }
            val daysLogged = dailyLogs.count { it.totalCalories > 0 }

            var completedDays = 0
            val cal = Calendar.getInstance().apply {
                timeInMillis = today
                set(Calendar.HOUR_OF_DAY, 0)
                set(Calendar.MINUTE, 0)
                set(Calendar.SECOND, 0)
                set(Calendar.MILLISECOND, 0)
            }
            for (i in 0 until 7) {
                if (isDayComplete(cal.timeInMillis)) {
                    completedDays++
                }
                cal.add(Calendar.DAY_OF_YEAR, -1)
            }

            val avgCalories = if (daysLogged > 0) totalCalories / daysLogged else 0
            val avgProtein = if (daysLogged > 0) totalProtein / daysLogged else 0

            WeeklyProgressReport(
                completedDays = completedDays,
                totalCalories = totalCalories,
                totalProtein = totalProtein,
                averageCalories = avgCalories,
                averageProtein = avgProtein,
                daysLogged = daysLogged
            )
        }
    }
}

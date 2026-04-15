package com.example.bfit.database

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Upsert

/**
 * Data Access Object for all plan-related database operations.
 * Handles plan item completions, daily nutrition logs,
 * extra meal items, and weight entries.
 */
@Dao
interface PlanDao {

    // ─── Plan Item Completion ───

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPlanItemCompletion(planItemCompletion: PlanItemCompletion)

    @Query("SELECT * FROM plan_item_completion WHERE id = :id")
    suspend fun getPlanItemCompletion(id: String): PlanItemCompletion?

    // ─── Daily Nutrition Log ───

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertDailyLog(dailyLog: DailyLog)

    @Query("SELECT * FROM daily_log WHERE date = :date")
    suspend fun getDailyLog(date: Long): DailyLog?

    // ─── Extra Meal Items ───

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertExtraMealItem(extraMealItem: ExtraMealItem)

    @Query("SELECT * FROM extra_meal_items WHERE date = :date")
    suspend fun getExtraMealItems(date: Long): List<ExtraMealItem>

    // ─── Weight Tracking ───

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertWeightEntry(weightEntry: WeightEntry)

    @Query("SELECT * FROM weight_entries ORDER BY date DESC LIMIT :limit")
    suspend fun getRecentWeightEntries(limit: Int = 30): List<WeightEntry>

    @Query("SELECT * FROM weight_entries ORDER BY date DESC LIMIT 1")
    suspend fun getLatestWeightEntry(): WeightEntry?

    @Query("SELECT * FROM weight_entries WHERE date >= :startDate ORDER BY date ASC")
    suspend fun getWeightEntriesSince(startDate: Long): List<WeightEntry>

    // ─── Weight Log (for ProgressActivity) ───

    @Upsert
    suspend fun insertWeightLogEntry(entry: WeightLogEntry)

    @Query("SELECT * FROM weight_log WHERE date BETWEEN :startDate AND :endDate ORDER BY date ASC")
    suspend fun getWeightLogEntriesBetween(startDate: Long, endDate: Long): List<WeightLogEntry>

    @Query("SELECT * FROM weight_log ORDER BY date DESC LIMIT 1")
    suspend fun getLatestWeightLogEntry(): WeightLogEntry?

    // ─── Daily Log range queries ───

    @Query("SELECT * FROM daily_log WHERE date BETWEEN :startDate AND :endDate ORDER BY date ASC")
    suspend fun getDailyLogsBetween(startDate: Long, endDate: Long): List<DailyLog>
}

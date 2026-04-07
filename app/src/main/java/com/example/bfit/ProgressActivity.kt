package com.example.bfit

import android.os.Bundle
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.example.bfit.database.PlanRepository
import com.example.bfit.database.WeeklyProgressReport
import com.google.android.material.progressindicator.LinearProgressIndicator

class ProgressActivity : AppCompatActivity() {

    private lateinit var planRepository: PlanRepository

    private lateinit var completedDaysText: TextView
    private lateinit var completedDaysProgress: LinearProgressIndicator
    private lateinit var weeklyCaloriesText: TextView
    private lateinit var weeklyProteinText: TextView
    private lateinit var weeklyAverageText: TextView
    private lateinit var loggedDaysText: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_progress)

        planRepository = PlanRepository(this)

        completedDaysText = findViewById(R.id.completedDaysText)
        completedDaysProgress = findViewById(R.id.completedDaysProgress)
        weeklyCaloriesText = findViewById(R.id.weeklyCaloriesText)
        weeklyProteinText = findViewById(R.id.weeklyProteinText)
        weeklyAverageText = findViewById(R.id.weeklyAverageText)
        loggedDaysText = findViewById(R.id.loggedDaysText)

        refreshWeeklyReport()
    }

    private fun refreshWeeklyReport() {
        val report = planRepository.getWeeklyProgressReport()
        bindWeeklyReport(report)
    }

    private fun bindWeeklyReport(report: WeeklyProgressReport) {
        completedDaysText.text = "Days Completed: ${report.completedDays} / 7"
        completedDaysProgress.max = 7
        completedDaysProgress.progress = report.completedDays

        weeklyCaloriesText.text = "Total Calories: ${report.totalCalories} kcal"
        weeklyProteinText.text = "Total Protein: ${report.totalProtein} g"
        weeklyAverageText.text = "Average: ${report.averageCalories} kcal/day, ${report.averageProtein} g/day"
        loggedDaysText.text = "Days with logs: ${report.daysLogged}"
    }
}

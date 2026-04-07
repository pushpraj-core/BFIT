package com.example.bfit

import android.graphics.Color
import android.os.Bundle
import android.view.View
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.bfit.database.PlanRepository
import com.example.bfit.database.WeightLogEntry
import com.example.bfit.database.WeeklyProgressReport
import com.github.mikephil.charting.charts.LineChart
import com.github.mikephil.charting.components.LimitLine
import com.github.mikephil.charting.components.XAxis
import com.github.mikephil.charting.data.Entry
import com.github.mikephil.charting.data.LineData
import com.github.mikephil.charting.data.LineDataSet
import com.github.mikephil.charting.formatter.ValueFormatter
import com.google.android.material.appbar.MaterialToolbar
import com.google.android.material.button.MaterialButton
import com.google.android.material.button.MaterialButtonToggleGroup
import com.google.android.material.progressindicator.LinearProgressIndicator
import com.google.android.material.textfield.TextInputEditText
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

class ProgressActivity : AppCompatActivity() {

    private lateinit var planRepository: PlanRepository

    private lateinit var completedDaysText: TextView
    private lateinit var completedDaysProgress: LinearProgressIndicator
    private lateinit var weeklyCaloriesText: TextView
    private lateinit var weeklyProteinText: TextView
    private lateinit var weeklyAverageText: TextView
    private lateinit var loggedDaysText: TextView
    private lateinit var kpiCurrentValue: TextView
    private lateinit var kpiChange7dValue: TextView
    private lateinit var kpiChange30dValue: TextView

    private lateinit var weightInput: TextInputEditText
    private lateinit var saveWeightButton: MaterialButton
    private lateinit var addFirstWeightBtn: MaterialButton
    private lateinit var noWeightDataText: TextView
    private lateinit var latestWeightText: TextView
    private lateinit var rangeToggleGroup: MaterialButtonToggleGroup
    private lateinit var range7dBtn: MaterialButton
    private lateinit var range30dBtn: MaterialButton
    private lateinit var weightChart: LineChart

    private var activeRangeDays: Int = 30

    private val dateFormatter = SimpleDateFormat("dd MMM yyyy", Locale.getDefault())

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_progress)

        val toolbar = findViewById<MaterialToolbar>(R.id.progressToolbar)
        toolbar.setNavigationIcon(androidx.appcompat.R.drawable.abc_ic_ab_back_material)
        toolbar.setNavigationOnClickListener {
            onBackPressedDispatcher.onBackPressed()
        }

        planRepository = PlanRepository(this)

        completedDaysText = findViewById(R.id.completedDaysText)
        completedDaysProgress = findViewById(R.id.completedDaysProgress)
        weeklyCaloriesText = findViewById(R.id.weeklyCaloriesText)
        weeklyProteinText = findViewById(R.id.weeklyProteinText)
        weeklyAverageText = findViewById(R.id.weeklyAverageText)
        loggedDaysText = findViewById(R.id.loggedDaysText)
        kpiCurrentValue = findViewById(R.id.kpiCurrentValue)
        kpiChange7dValue = findViewById(R.id.kpiChange7dValue)
        kpiChange30dValue = findViewById(R.id.kpiChange30dValue)

        weightInput = findViewById(R.id.weightInput)
        saveWeightButton = findViewById(R.id.saveWeightButton)
        addFirstWeightBtn = findViewById(R.id.addFirstWeightBtn)
        noWeightDataText = findViewById(R.id.noWeightDataText)
        latestWeightText = findViewById(R.id.latestWeightText)
        rangeToggleGroup = findViewById(R.id.rangeToggleGroup)
        range7dBtn = findViewById(R.id.range7dBtn)
        range30dBtn = findViewById(R.id.range30dBtn)
        weightChart = findViewById(R.id.weightChart)

        setupChart()

        range30dBtn.isChecked = true
        rangeToggleGroup.addOnButtonCheckedListener { _, checkedId, isChecked ->
            if (!isChecked) return@addOnButtonCheckedListener
            activeRangeDays = if (checkedId == R.id.range7dBtn) 7 else 30
            refreshWeightSection()
        }

        addFirstWeightBtn.setOnClickListener {
            weightInput.requestFocus()
        }

        saveWeightButton.setOnClickListener {
            val enteredWeight = weightInput.text?.toString()?.toFloatOrNull()
            if (enteredWeight == null || enteredWeight <= 0f) {
                Toast.makeText(this, "Enter a valid weight", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            val todayStart = Calendar.getInstance().apply {
                set(Calendar.HOUR_OF_DAY, 0)
                set(Calendar.MINUTE, 0)
                set(Calendar.SECOND, 0)
                set(Calendar.MILLISECOND, 0)
            }.timeInMillis

            planRepository.addWeightLogEntry(todayStart, enteredWeight)
            weightInput.setText("")
            Toast.makeText(this, "Weight saved", Toast.LENGTH_SHORT).show()
            refreshWeightSection()
        }

        val report = planRepository.getWeeklyProgressReport()
        bindWeeklyReport(report)
        refreshWeightSection()
        animateIntro()
    }

    private fun bindWeeklyReport(report: WeeklyProgressReport) {
        completedDaysText.text = "Days Completed: ${report.completedDays} / 7"
        completedDaysProgress.max = 7
        completedDaysProgress.progress = report.completedDays

        weeklyCaloriesText.text = "Total Calories: ${report.totalCalories} kcal"
        weeklyProteinText.text = "Total Protein: ${report.totalProtein} g"
        weeklyAverageText.text = "Average: ${report.averageCalories} kcal/day, ${report.averageProtein} g/day"
        loggedDaysText.text = "Days with logs: ${report.daysLogged}"

        if (report.completedDays >= 5) {
            completedDaysText.setTextColor(getColor(R.color.bfit_success))
        } else {
            completedDaysText.setTextColor(getColor(R.color.bfit_warning))
        }

        if (report.daysLogged == 0) {
            weeklyCaloriesText.setTextColor(getColor(R.color.bfit_warning))
        } else {
            weeklyCaloriesText.setTextColor(getColor(R.color.bfit_on_surface))
        }
    }

    private fun setupChart() {
        weightChart.description.isEnabled = false
        weightChart.legend.isEnabled = false
        weightChart.setNoDataText("No weight logs yet")
        weightChart.setNoDataTextColor(Color.DKGRAY)
        weightChart.setTouchEnabled(true)
        weightChart.setPinchZoom(false)
        weightChart.setScaleEnabled(false)
        weightChart.setDrawGridBackground(false)
        weightChart.animateX(500)
        weightChart.marker = WeightMarkerView(this)

        val xAxis = weightChart.xAxis
        xAxis.position = XAxis.XAxisPosition.BOTTOM
        xAxis.setDrawGridLines(false)
        xAxis.granularity = 1f
        xAxis.textColor = Color.DKGRAY

        val yAxisLeft = weightChart.axisLeft
        yAxisLeft.setDrawGridLines(true)
        yAxisLeft.gridColor = getColor(R.color.bfit_chart_grid)
        yAxisLeft.textColor = Color.DKGRAY
        yAxisLeft.axisMinimum = 0f

        weightChart.axisRight.isEnabled = false
    }

    private fun refreshWeightSection() {
        val end = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }.timeInMillis

        val start = Calendar.getInstance().apply {
            timeInMillis = end
            add(Calendar.DAY_OF_YEAR, -(activeRangeDays - 1))
        }.timeInMillis

        val entries = planRepository.getWeightLogEntriesBetween(start, end)
        renderChart(entries)
        bindKpiData()

        val latestEntry = planRepository.getLatestWeightLogEntry()
        if (latestEntry == null) {
            latestWeightText.text = "Latest Weight: --"
        } else {
            latestWeightText.text = "Latest Weight: ${"%.1f".format(latestEntry.weightKg)} kg on ${dateFormatter.format(latestEntry.date)}"
        }
    }

    private fun bindKpiData() {
        val today = startOfDay(System.currentTimeMillis())
        val weekStart = Calendar.getInstance().apply {
            timeInMillis = today
            add(Calendar.DAY_OF_YEAR, -6)
        }.timeInMillis
        val monthStart = Calendar.getInstance().apply {
            timeInMillis = today
            add(Calendar.DAY_OF_YEAR, -29)
        }.timeInMillis

        val weekEntries = planRepository.getWeightLogEntriesBetween(weekStart, today).sortedBy { it.date }
        val monthEntries = planRepository.getWeightLogEntriesBetween(monthStart, today).sortedBy { it.date }
        val latest = monthEntries.lastOrNull()

        kpiCurrentValue.text = latest?.let { String.format("%.1f kg", it.weightKg) } ?: "--"
        kpiChange7dValue.text = formatWeightChange(weekEntries)
        kpiChange30dValue.text = formatWeightChange(monthEntries)
    }

    private fun formatWeightChange(entries: List<WeightLogEntry>): String {
        if (entries.size < 2) return "--"
        val diff = entries.last().weightKg - entries.first().weightKg
        return String.format("%+.1f kg", diff)
    }

    private fun startOfDay(value: Long): Long {
        return Calendar.getInstance().apply {
            timeInMillis = value
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }.timeInMillis
    }

    private fun renderChart(weightEntries: List<WeightLogEntry>) {
        if (weightEntries.isEmpty()) {
            weightChart.clear()
            noWeightDataText.visibility = View.VISIBLE
            addFirstWeightBtn.visibility = View.VISIBLE
            return
        }

        noWeightDataText.visibility = View.GONE
        addFirstWeightBtn.visibility = View.GONE

        val sorted = weightEntries.sortedBy { it.date }
        val lineEntries = sorted.mapIndexed { index, item ->
            Entry(index.toFloat(), item.weightKg)
        }

        val dataSet = LineDataSet(lineEntries, "Weight").apply {
            color = Color.parseColor("#0D9488")
            lineWidth = 2.8f
            setCircleColor(Color.parseColor("#0F766E"))
            circleRadius = 4f
            setDrawCircleHole(false)
            setDrawValues(false)
            highLightColor = Color.parseColor("#111827")
            setDrawHorizontalHighlightIndicator(false)
        }

        val labels = sorted.map { dateFormatter.format(it.date) }
        weightChart.xAxis.labelCount = labels.size.coerceAtMost(5)
        weightChart.xAxis.valueFormatter = object : ValueFormatter() {
            override fun getFormattedValue(value: Float): String {
                val index = value.toInt()
                return if (index in labels.indices) labels[index] else ""
            }
        }

        val minWeight = sorted.minOf { it.weightKg }
        val maxWeight = sorted.maxOf { it.weightKg }
        val padding = ((maxWeight - minWeight) * 0.25f).coerceAtLeast(1f)

        val goalLineValue = (latestGoalWeight(sorted) ?: maxWeight)
        weightChart.axisLeft.removeAllLimitLines()
        val limitLine = LimitLine(goalLineValue, getString(R.string.goal_weight_label)).apply {
            lineColor = getColor(R.color.bfit_warning)
            textColor = getColor(R.color.bfit_warning)
            lineWidth = 1.5f
            enableDashedLine(8f, 8f, 0f)
        }
        weightChart.axisLeft.addLimitLine(limitLine)

        weightChart.axisLeft.axisMinimum = (minWeight - padding).coerceAtLeast(0f)
        weightChart.axisLeft.axisMaximum = maxWeight + padding

        weightChart.data = LineData(dataSet)
        weightChart.invalidate()
    }

    private fun latestGoalWeight(sorted: List<WeightLogEntry>): Float? {
        if (sorted.isEmpty()) return null
        val latest = sorted.last().weightKg
        return latest - 2f
    }

    private fun animateIntro() {
        val root = findViewById<android.view.View>(android.R.id.content)
        root.alpha = 0f
        root.animate().alpha(1f).setDuration(260).start()
    }
}

package com.pushprajcore.bfit

import android.os.Build
import android.os.Bundle
import android.view.View
import android.widget.TextView
import android.widget.Toast
import androidx.activity.OnBackPressedCallback
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.pushprajcore.bfit.database.PlanRepository
import com.pushprajcore.bfit.databinding.ActivityPlannerBinding
import kotlinx.coroutines.launch
import android.os.Parcelable
import java.util.Calendar

class PlannerActivity : AppCompatActivity() {

    private lateinit var binding: ActivityPlannerBinding
    private lateinit var planAdapter: PlanAdapter
    private val planByDate = mutableMapOf<Long, List<PlanListItem>>()
    private lateinit var planRepository: PlanRepository
    private var selectedDate: Long = 0
    private val completedDates = mutableSetOf<Long>()
    private var exerciseOnlyMode: Boolean = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityPlannerBinding.inflate(layoutInflater)
        setContentView(binding.root)

        planRepository = PlanRepository(this)
        setupBackNavigation()

        // Back button
        binding.backButton.setOnClickListener {
            finish()
            overridePendingTransition(R.anim.slide_in_left, R.anim.slide_out_right)
        }

        val planResult = getParcelableExtra(intent, "plan", PlanResult::class.java)
        exerciseOnlyMode = intent.getBooleanExtra("openExerciseOnly", false)

        if (exerciseOnlyMode) {
            binding.plannerTitleText.text = getString(R.string.exercise_focus_title)
        }

        if (planResult == null) {
            Toast.makeText(this, "Error: Plan data is missing.", Toast.LENGTH_LONG).show()
            finish()
            return
        }

        val today = Calendar.getInstance()
        for (i in 0 until 30) {
            val date = (today.clone() as Calendar).apply { add(Calendar.DAY_OF_YEAR, i) }
            date.set(Calendar.HOUR_OF_DAY, 0)
            date.set(Calendar.MINUTE, 0)
            date.set(Calendar.SECOND, 0)
            date.set(Calendar.MILLISECOND, 0)
            val dayInMillis = date.timeInMillis

            val dayOfWeek = ((date.get(Calendar.DAY_OF_WEEK) + 5) % 7 + 1).toString()
            val mealPlanForDay = planResult.mealPlan[dayOfWeek]

            if (mealPlanForDay != null && mealPlanForDay.isNotEmpty()) {
                val planItems = mutableListOf<PlanListItem>()

                val breakfast = mealPlanForDay.getOrNull(0)
                val lunch = mealPlanForDay.getOrNull(1)
                val dinner = mealPlanForDay.getOrNull(2)

                breakfast?.let { (breakfastText, breakfastCalories, breakfastProtein) ->
                    planItems.add(PlanListItem.Header("Breakfast"))
                    planItems.add(PlanListItem.PlanItem(id = "${dayInMillis}-FOOD-$breakfastText-0", type = ItemType.FOOD, text = "$breakfastText ($breakfastCalories kcal, $breakfastProtein g protein)"))
                }
                lunch?.let { (lunchText, lunchCalories, lunchProtein) ->
                    planItems.add(PlanListItem.Header("Lunch"))
                    planItems.add(PlanListItem.PlanItem(id = "${dayInMillis}-FOOD-$lunchText-1", type = ItemType.FOOD, text = "$lunchText ($lunchCalories kcal, $lunchProtein g protein)"))
                }
                dinner?.let { (dinnerText, dinnerCalories, dinnerProtein) ->
                    planItems.add(PlanListItem.Header("Dinner"))
                    planItems.add(PlanListItem.PlanItem(id = "${dayInMillis}-FOOD-$dinnerText-2", type = ItemType.FOOD, text = "$dinnerText ($dinnerCalories kcal, $dinnerProtein g protein)"))
                }

                // Add exercises if present
                if (planResult.exercises.isNotEmpty()) {
                    planItems.add(PlanListItem.Header("Exercise"))
                    planResult.exercises.split("\n").filter { it.isNotBlank() }.forEach { exerciseStr ->
                        val exerciseText = exerciseStr.removePrefix("- ").trim()
                        planItems.add(PlanListItem.PlanItem(
                            id = "${dayInMillis}-EXERCISE-$exerciseText", 
                            type = ItemType.EXERCISE, 
                            text = exerciseText
                        ))
                    }
                }

                planByDate[dayInMillis] = planItems
            }

            if (planRepository.isDayComplete(dayInMillis)) {
                completedDates.add(dayInMillis)
            }
        }

        selectedDate = intent.getLongExtra("selectedDate", System.currentTimeMillis())
        val calendar = Calendar.getInstance().apply {
            timeInMillis = selectedDate
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
        selectedDate = calendar.timeInMillis
        binding.calendarView.date = selectedDate
        updatePlanForDate(selectedDate)
        updateStreak()

        binding.calendarView.setOnDateChangeListener { _, year, month, dayOfMonth ->
            val c = Calendar.getInstance().apply {
                set(year, month, dayOfMonth)
                set(Calendar.HOUR_OF_DAY, 0)
                set(Calendar.MINUTE, 0)
                set(Calendar.SECOND, 0)
                set(Calendar.MILLISECOND, 0)
            }
            selectedDate = c.timeInMillis
            updatePlanForDate(selectedDate)
        }

        binding.markDayCompleteBtn.setOnClickListener {
            planRepository.markDayAsComplete(selectedDate)
            completedDates.add(selectedDate)
            val planItems = planByDate.getOrDefault(selectedDate, emptyList())
            lifecycleScope.launch {
                for (item in planItems) {
                    if (item is PlanListItem.PlanItem) {
                        val textParts = item.text.split(" ")
                        val calories = textParts.findLast { it.contains("kcal") }?.filter { it.isDigit() }?.toIntOrNull() ?: 0
                        val protein = textParts.findLast { it.contains("g") }?.filter { it.isDigit() }?.toIntOrNull() ?: 0
                        planRepository.markPlanItemAsComplete(item.id, true, calories, protein)
                    }
                }
                updatePlanForDate(selectedDate)
                updateStreak()
            }
            Toast.makeText(this, "Day marked as complete! 🎉", Toast.LENGTH_SHORT).show()
            binding.markDayCompleteBtn.text = "Day Completed ✅"
            binding.markDayCompleteBtn.isEnabled = false
            binding.markDayCompleteBtn.setBackgroundColor(getColor(R.color.bfit_success))
        }
    }

    private fun setupBackNavigation() {
        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                finish()
                overridePendingTransition(R.anim.slide_in_left, R.anim.slide_out_right)
            }
        })
    }

    private fun updateStreak() {
        val streak = planRepository.getStreak()
        binding.streakText.text = "🔥 $streak Day Streak"
    }

    private fun updatePlanForDate(date: Long) {
        lifecycleScope.launch {
            val generatedPlan = planByDate.getOrDefault(date, emptyList()).toMutableList()

            if (exerciseOnlyMode) {
                val exerciseOnly = mutableListOf<PlanListItem>()
                var include = false
                for (item in generatedPlan) {
                    when (item) {
                        is PlanListItem.Header -> {
                            include = item.title == "Exercise"
                            if (include) {
                                exerciseOnly.add(item)
                            }
                        }
                        is PlanListItem.PlanItem -> if (include) {
                            exerciseOnly.add(item)
                        }
                    }
                }
                generatedPlan.clear()
                generatedPlan.addAll(exerciseOnly)
            }

            val extraItems = planRepository.getExtraMealItems(date)
            if (extraItems.isNotEmpty()) {
                val exerciseIndex = generatedPlan.indexOfFirst { it is PlanListItem.Header && it.title == "Exercise" }
                if (exerciseIndex != -1) {
                    generatedPlan.add(exerciseIndex, PlanListItem.Header("Extras"))
                    generatedPlan.addAll(exerciseIndex + 1, extraItems.map {
                        PlanListItem.PlanItem(id = it.id, type = ItemType.FOOD, text = "${it.text} (${it.calories} kcal, ${it.protein} g protein)", isCompleted = planRepository.isPlanItemComplete(it.id))
                    })
                } else {
                    generatedPlan.add(PlanListItem.Header("Extras"))
                    generatedPlan.addAll(extraItems.map {
                        PlanListItem.PlanItem(id = it.id, type = ItemType.FOOD, text = "${it.text} (${it.calories} kcal, ${it.protein} g protein)", isCompleted = planRepository.isPlanItemComplete(it.id))
                    })
                }
            }

            // Show/hide empty state
            val emptyStateText = findViewById<TextView>(R.id.emptyStateText)
            val isDayAlreadyComplete = planRepository.isDayComplete(date) || completedDates.contains(date)

            if (generatedPlan.isEmpty()) {
                emptyStateText?.visibility = View.VISIBLE
                emptyStateText?.text = if (exerciseOnlyMode) {
                    getString(R.string.no_exercise_for_date)
                } else {
                    getString(R.string.no_plan_for_date)
                }
                binding.planRecyclerView.visibility = View.GONE
                binding.markDayCompleteBtn.isEnabled = false
            } else {
                emptyStateText?.visibility = View.GONE
                binding.planRecyclerView.visibility = View.VISIBLE
                
                if (isDayAlreadyComplete) {
                    binding.markDayCompleteBtn.text = "Day Completed ✅"
                    binding.markDayCompleteBtn.isEnabled = false
                    binding.markDayCompleteBtn.setBackgroundColor(getColor(R.color.bfit_success))
                } else {
                    binding.markDayCompleteBtn.text = getString(R.string.mark_day_complete)
                    binding.markDayCompleteBtn.isEnabled = true
                    binding.markDayCompleteBtn.setBackgroundColor(getColor(R.color.bfit_primary))
                }
            }

            val finalPlanItems = generatedPlan.map { item ->
                if (item is PlanListItem.PlanItem) {
                    item.isCompleted = planRepository.isPlanItemComplete(item.id)
                }
                item
            }

            planAdapter = PlanAdapter(finalPlanItems) { item, isCompleted ->
                val textParts = item.text.split(" ")
                val calories = textParts.findLast { it.contains("kcal") }?.filter { it.isDigit() }?.toIntOrNull() ?: 0
                val protein = textParts.findLast { it.contains("g") }?.filter { it.isDigit() }?.toIntOrNull() ?: 0
                lifecycleScope.launch {
                    planRepository.markPlanItemAsComplete(item.id, isCompleted, calories, protein)
                }
            }
            binding.planRecyclerView.adapter = planAdapter
        }
    }

    @Suppress("DEPRECATION")
    private fun <T : Parcelable> getParcelableExtra(intent: android.content.Intent, key: String, clazz: Class<T>): T? {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            intent.getParcelableExtra(key, clazz)
        } else {
            intent.getParcelableExtra(key) as? T
        }
    }
}

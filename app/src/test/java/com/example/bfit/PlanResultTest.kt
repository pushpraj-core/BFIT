package com.example.bfit

import org.junit.Assert.*
import org.junit.Test

/**
 * Tests for PlanResult data model and nutrition regex parsing
 * used in the dashboard to extract calories and protein from meal text.
 */
class PlanResultTest {

    @Test
    fun planResult_creation() {
        val mealPlan = mapOf(
            "1" to listOf(
                Triple("Oatmeal with Banana", 350, 12),
                Triple("Grilled Chicken Salad", 450, 35),
                Triple("Salmon with Rice", 600, 40)
            )
        )
        val plan = PlanResult(
            category = "Normal Weight",
            calories = 2500,
            totalProtein = 120,
            mealPlan = mealPlan,
            exercises = "- Push-ups\n- Squats"
        )
        assertEquals("Normal Weight", plan.category)
        assertEquals(2500, plan.calories)
        assertEquals(120, plan.totalProtein)
        assertEquals(1, plan.mealPlan.size)
    }

    @Test
    fun planResult_mealPlan_has7Days() {
        val mealPlan = (1..7).associate { day ->
            day.toString() to listOf(
                Triple("Breakfast $day", 300, 15),
                Triple("Lunch $day", 500, 30),
                Triple("Dinner $day", 600, 35)
            )
        }
        val plan = PlanResult("Normal Weight", 2500, 120, mealPlan, "")
        assertEquals(7, plan.mealPlan.size)
    }

    @Test
    fun planResult_exerciseSplit() {
        val exercises = "- Push-ups\n- Squats\n- Plank"
        val exerciseList = exercises.split("\n").filter { it.isNotBlank() }
        assertEquals(3, exerciseList.size)
        assertEquals("- Push-ups", exerciseList[0])
    }

    // ==================== Regex Tests (used in dashboard calorie counting) ====================

    @Test
    fun kcalRegex_extractsCalories() {
        val kcalRegex = "(\\d+)\\s*kcal".toRegex()
        val text = "Oatmeal with Banana (350 kcal, 12 g protein)"
        val match = kcalRegex.find(text)
        assertNotNull(match)
        assertEquals(350, match!!.groupValues[1].toInt())
    }

    @Test
    fun proteinRegex_extractsProtein() {
        val proteinRegex = "(\\d+)\\s*g".toRegex()
        val text = "Oatmeal with Banana (350 kcal, 12 g protein)"
        val match = proteinRegex.find(text)
        assertNotNull(match)
        assertEquals(12, match!!.groupValues[1].toInt())
    }

    @Test
    fun kcalRegex_noMatch_returnsNull() {
        val kcalRegex = "(\\d+)\\s*kcal".toRegex()
        val text = "Just a plain meal"
        val match = kcalRegex.find(text)
        assertNull(match)
    }

    @Test
    fun totalProtein_calculatedFromAllMeals() {
        val mealPlan = (1..7).associate { day ->
            day.toString() to listOf(
                Triple("B", 300, 15),
                Triple("L", 500, 30),
                Triple("D", 600, 35)
            )
        }
        val totalProtein = mealPlan.values.flatten().sumOf { it.third }
        // 7 days * (15 + 30 + 35) = 7 * 80 = 560
        assertEquals(560, totalProtein)
    }

    @Test
    fun mealPlan_dayOfWeek_mapping() {
        // The app maps Calendar.DAY_OF_WEEK to a 1-7 Monday-Sunday key
        // Formula: ((dayOfWeek + 5) % 7 + 1)
        // Sunday = 1 in Calendar -> should map to 7
        val sundayCalendar = 1
        val mapped = (sundayCalendar + 5) % 7 + 1
        assertEquals(7, mapped)

        // Monday = 2 in Calendar -> should map to 1
        val mondayCalendar = 2
        val mappedMonday = (mondayCalendar + 5) % 7 + 1
        assertEquals(1, mappedMonday)
    }
}

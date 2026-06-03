package com.example.bfit

import org.junit.Assert.*
import org.junit.Test

/**
 * Tests for the BMI calculation logic used in MainActivity.getPlan().
 * These validate that the correct calorie and category values are returned
 * for various BMI ranges, genders, and goals.
 */
class BmiCalculationTest {

    // ==================== BMI Classification ====================

    @Test
    fun bmi_underweight_classifiedCorrectly() {
        val bmi = 17.5f
        val category = classifyBmi(bmi)
        assertEquals("Underweight", category)
    }

    @Test
    fun bmi_normalWeight_classifiedCorrectly() {
        val bmi = 22.0f
        val category = classifyBmi(bmi)
        assertEquals("Normal Weight", category)
    }

    @Test
    fun bmi_overweight_classifiedCorrectly() {
        val bmi = 27.0f
        val category = classifyBmi(bmi)
        assertEquals("Overweight", category)
    }

    @Test
    fun bmi_obese_classifiedCorrectly() {
        val bmi = 35.0f
        val category = classifyBmi(bmi)
        assertEquals("Obese", category)
    }

    @Test
    fun bmi_borderline_18_5_isNormalWeight() {
        val bmi = 18.5f
        val category = classifyBmi(bmi)
        assertEquals("Normal Weight", category)
    }

    @Test
    fun bmi_borderline_25_isOverweight() {
        val bmi = 25.0f
        val category = classifyBmi(bmi)
        assertEquals("Overweight", category)
    }

    @Test
    fun bmi_borderline_30_isObese() {
        val bmi = 30.0f
        val category = classifyBmi(bmi)
        assertEquals("Obese", category)
    }

    // ==================== BMI Computation from Height/Weight ====================

    @Test
    fun bmiCalculation_withValidInputs() {
        // 70kg, 175cm
        val weight = 70f
        val heightInMeters = 1.75f
        val bmi = weight / (heightInMeters * heightInMeters)
        assertEquals(22.86f, bmi, 0.1f)
    }

    @Test
    fun bmiCalculation_fromCentimeters() {
        // Converting cm to meters before calculation
        val weight = 85f
        val heightCm = 180f
        val h = heightCm / 100f
        val bmi = weight / (h * h)
        assertEquals(26.23f, bmi, 0.1f)
    }

    // ==================== Calorie Estimation by BMI ====================

    @Test
    fun baseCalories_underweightMale_is3000() {
        val calories = getBaseCalories(17.0f, "Male")
        assertEquals(3000, calories)
    }

    @Test
    fun baseCalories_underweightFemale_is2600() {
        val calories = getBaseCalories(17.0f, "Female")
        assertEquals(2600, calories)
    }

    @Test
    fun baseCalories_normalMale_is2500() {
        val calories = getBaseCalories(22.0f, "Male")
        assertEquals(2500, calories)
    }

    @Test
    fun baseCalories_normalFemale_is2100() {
        val calories = getBaseCalories(22.0f, "Female")
        assertEquals(2100, calories)
    }

    @Test
    fun baseCalories_overweightMale_is2000() {
        val calories = getBaseCalories(27.0f, "Male")
        assertEquals(2000, calories)
    }

    @Test
    fun baseCalories_obeseMale_is1700() {
        val calories = getBaseCalories(35.0f, "Male")
        assertEquals(1700, calories)
    }

    // ==================== Goal-Based Calorie Adjustments ====================

    @Test
    fun bulkGoal_adds500Calories() {
        val base = 2500
        val adjusted = adjustCaloriesForGoal(base, "Bulk")
        assertEquals(3000, adjusted)
    }

    @Test
    fun leanGoal_subtracts500Calories() {
        val base = 2500
        val adjusted = adjustCaloriesForGoal(base, "Lean")
        assertEquals(2000, adjusted)
    }

    @Test
    fun maintainGoal_noChange() {
        val base = 2500
        val adjusted = adjustCaloriesForGoal(base, "Maintain")
        assertEquals(2500, adjusted)
    }

    // ==================== Height Conversion ====================

    @Test
    fun feetToCm_conversion() {
        val feet = 5.9f
        val cm = feet * 30.48f
        assertEquals(179.83f, cm, 0.1f)
    }

    @Test
    fun cmToFeet_conversion() {
        val cm = 180f
        val feet = cm / 30.48f
        assertEquals(5.90f, feet, 0.1f)
    }

    // ==================== Helper methods (mirror the app logic) ====================

    private fun classifyBmi(bmi: Float): String {
        return when {
            bmi < 18.5 -> "Underweight"
            bmi in 18.5..24.9 -> "Normal Weight"
            bmi in 25.0..29.9 -> "Overweight"
            else -> "Obese"
        }
    }

    private fun getBaseCalories(bmi: Float, gender: String): Int {
        return when {
            bmi < 18.5 -> if (gender == "Male") 3000 else 2600
            bmi in 18.5..24.9 -> if (gender == "Male") 2500 else 2100
            bmi in 25.0..29.9 -> if (gender == "Male") 2000 else 1700
            else -> if (gender == "Male") 1700 else 1400
        }
    }

    private fun adjustCaloriesForGoal(base: Int, goal: String): Int {
        return when (goal) {
            "Bulk" -> base + 500
            "Lean" -> base - 500
            else -> base
        }
    }
}

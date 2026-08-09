package com.pushprajcore.bfit

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.widget.ImageView
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.android.material.button.MaterialButton
import com.google.firebase.auth.FirebaseAuth
import com.pushprajcore.bfit.database.PlanDatabase
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class ProfileActivity : AppCompatActivity() {

    private lateinit var auth: FirebaseAuth

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_profile)

        auth = FirebaseAuth.getInstance()

        setupBottomNav()
        populateProfile()

        // Back button (X icon)
        findViewById<ImageView>(R.id.profileBackBtn)?.setOnClickListener { finish() }

        // Top-right edit icon → same as Edit My Plan button
        findViewById<ImageView>(R.id.editProfileBtn)?.setOnClickListener { clearPlanAndGoHome() }

        // Edit My Plan button
        findViewById<MaterialButton>(R.id.profileEditPlanBtn).setOnClickListener { clearPlanAndGoHome() }

        // Logout
        findViewById<MaterialButton>(R.id.profileLogoutBtn).setOnClickListener {
            AlertDialog.Builder(this)
                .setTitle("Sign Out")
                .setMessage("Are you sure you want to sign out?")
                .setPositiveButton("Sign Out") { _, _ ->
                    auth.signOut()
                    getSharedPreferences("user_data", Context.MODE_PRIVATE).edit().clear().apply()
                    startActivity(Intent(this, LoginActivity::class.java).apply {
                        flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                    })
                    finish()
                }
                .setNegativeButton("Cancel", null)
                .show()
        }
    }

    // ─── Helpers ───────────────────────────────────────────

    private fun clearPlanAndGoHome() {
        getSharedPreferences("user_data", Context.MODE_PRIVATE).edit().remove("plan").apply()
        startActivity(Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_NEW_TASK
        })
        finish()
    }

    private fun populateProfile() {
        val prefs = getSharedPreferences("user_data", Context.MODE_PRIVATE)
        val isDemoMode = prefs.getBoolean(LoginActivity.DEMO_MODE_KEY, false)

        // Name & Email
        val user = auth.currentUser
        val name = user?.displayName?.takeIf { it.isNotBlank() }
            ?: user?.email?.substringBefore("@")
            ?: "Athlete"
        val email = if (isDemoMode) "Demo Mode" else (user?.email ?: "")
        setText(R.id.profileName, name)
        setText(R.id.profileEmail, email)

        // Body stats
        val height = prefs.getString("height", "--") ?: "--"
        val weight = prefs.getString("weight", "--") ?: "--"
        val age    = prefs.getString("age",    "--") ?: "--"
        val goal   = prefs.getString("goal",   "--") ?: "--"
        val diet   = prefs.getString("diet",   "--") ?: "--"

        setText(R.id.profileHeight, height)
        setText(R.id.profileWeight, "$weight kg")
        setText(R.id.profileAge, age)

        // BMI
        val bmi = prefs.getFloat("bmi", 0f)
        if (bmi > 0f) {
            setText(R.id.profileBmi, String.format("%.1f", bmi))
            val category = when {
                bmi < 18.5f -> "Underweight"
                bmi < 25.0f -> "Normal"
                bmi < 30.0f -> "Overweight"
                else        -> "Obese"
            }
            setText(R.id.profileBmiCategory, category)
        } else {
            setText(R.id.profileBmi, "--")
            setText(R.id.profileBmiCategory, "")
        }

        // Goal emoji
        val goalEmoji = when {
            "lose"   in goal.lowercase() -> "🔥 Lose"
            "gain"   in goal.lowercase() -> "💪 Gain"
            "muscle" in goal.lowercase() -> "💪 Muscle"
            "maintain" in goal.lowercase() -> "⚖️ Maintain"
            else -> goal.take(8)
        }
        setText(R.id.profileGoal, goalEmoji)

        // Diet label
        val dietShort = when {
            "vegan"  in diet.lowercase() -> "Vegan 🌱"
            "non"    in diet.lowercase() -> "Non-Veg 🍗"
            "veg"    in diet.lowercase() -> "Veg 🥦"
            else -> diet.take(10)
        }
        setText(R.id.profileDiet, dietShort)

        // Calorie & protein targets
        val planCalories = prefs.getInt("plan_calories", 0)
        val planProtein  = prefs.getInt("plan_protein",  0)
        setText(R.id.profileCalorieTarget,
            if (planCalories > 0) "$planCalories kcal / day" else "Generate a plan to see target")
        setText(R.id.profileProteinTarget,
            if (planProtein  > 0) "$planProtein g protein / day" else "Generate a plan to see target")

        // Weight history from Room DB
        loadWeightHistory()
    }

    private fun loadWeightHistory() {
        val db  = PlanDatabase.getDatabase(this)
        val sdf = SimpleDateFormat("dd MMM", Locale.getDefault())
        Thread {
            val text = try {
                val entries = kotlinx.coroutines.runBlocking { db.planDao().getRecentWeightEntries(7) }
                if (entries.isEmpty()) {
                    "No weight entries yet.\nLog weight from the home screen."
                } else {
                    entries.joinToString("\n") { e ->
                        "📅 ${sdf.format(Date(e.date))}  →  ${String.format("%.1f", e.weight)} kg"
                    }
                }
            } catch (_: Exception) {
                "No weight entries yet."
            }
            runOnUiThread { setText(R.id.profileWeightHistory, text) }
        }.start()
    }

    private fun setText(id: Int, value: String) {
        try { findViewById<TextView>(id)?.text = value } catch (_: Exception) {}
    }

    private fun setupBottomNav() {
        val bottomNavigation = findViewById<BottomNavigationView>(R.id.bottom_navigation)
        bottomNavigation.selectedItemId = R.id.nav_profile
        bottomNavigation.setOnItemSelectedListener { item ->
            when (item.itemId) {
                R.id.nav_home -> {
                    startActivity(Intent(this, MainActivity::class.java).apply {
                        flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
                    })
                    overridePendingTransition(0, 0)
                    true
                }
                R.id.nav_exercise -> {
                    startActivity(Intent(this, ExerciseActivity::class.java))
                    overridePendingTransition(0, 0)
                    true
                }
                R.id.nav_profile -> true
                else -> false
            }
        }
    }
}

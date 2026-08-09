package com.pushprajcore.bfit

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken

data class ExerciseItem(val name: String, val videoId: String)

class ExerciseActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_exercise)

        val bottomNavigation = findViewById<BottomNavigationView>(R.id.bottom_navigation)
        bottomNavigation.selectedItemId = R.id.nav_exercise
        bottomNavigation.setOnItemSelectedListener { item ->
            when (item.itemId) {
                R.id.nav_home -> {
                    startActivity(Intent(this, MainActivity::class.java).apply {
                        flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
                    })
                    overridePendingTransition(0, 0)
                    true
                }
                R.id.nav_exercise -> true
                R.id.nav_profile -> {
                    startActivity(Intent(this, ProfileActivity::class.java))
                    overridePendingTransition(R.anim.slide_in_right, R.anim.slide_out_left)
                    true
                }
                else -> false
            }
        }

        val recyclerView = findViewById<RecyclerView>(R.id.exerciseRecyclerView)
        recyclerView.layoutManager = LinearLayoutManager(this)

        val exercises = loadExercisesFromPlan()
        recyclerView.adapter = ExerciseCompactAdapter(exercises) { exercise ->
            openYouTube(exercise.videoId)
        }
    }

    /** Load exercises from the saved plan in SharedPreferences, fall back to defaults. */
    private fun loadExercisesFromPlan(): List<ExerciseItem> {
        val prefs = getSharedPreferences("user_data", Context.MODE_PRIVATE)
        val planJson = prefs.getString("plan", null)

        if (planJson != null) {
            try {
                val type = object : TypeToken<Map<String, Any>>() {}.type
                val planMap: Map<String, Any> = Gson().fromJson(planJson, type)
                val exerciseStr = planMap["exercises"]?.toString() ?: ""
                val items = exerciseStr
                    .split("\n")
                    .map { it.removePrefix("- ").trim() }
                    .filter { it.isNotBlank() }
                if (items.isNotEmpty()) {
                    return items.map { name -> ExerciseItem(name, findVideoId(name)) }
                }
            } catch (_: Exception) { /* fall through to defaults */ }
        }

        // Defaults if no plan
        return listOf(
            ExerciseItem("Pushups", "IODxDxX78TI"),
            ExerciseItem("Squats", "YaXPRqUwItQ"),
            ExerciseItem("Plank", "pSHjTRCQxIw"),
            ExerciseItem("Lunges", "QOVaHwm-Q6U"),
            ExerciseItem("Jumping Jacks", "C385GzYep6I")
        )
    }

    /** Map exercise name keywords → YouTube video IDs. */
    private fun findVideoId(name: String): String {
        val lower = name.lowercase()
        return when {
            "push" in lower -> "IODxDxX78TI"
            "squat" in lower -> "YaXPRqUwItQ"
            "plank" in lower -> "pSHjTRCQxIw"
            "lunge" in lower -> "QOVaHwm-Q6U"
            "deadlift" in lower -> "op9kVnSso6Q"
            "bench" in lower -> "rT7DggyJ4gc"
            "curl" in lower || "bicep" in lower -> "ykJmrZ5v0Oo"
            "row" in lower -> "FWLR4mFrQVc"
            "shoulder" in lower || "press" in lower -> "qEwKCR5JCog"
            "run" in lower || "jog" in lower || "cardio" in lower -> "kVnyY17VS9Y"
            "yoga" in lower || "stretch" in lower -> "v7AYKMP6rOE"
            "pull" in lower || "chin" in lower -> "eGo4IYlbE5g"
            "dip" in lower -> "2z8JmcrW-As"
            "crunch" in lower || "ab" in lower || "sit" in lower -> "Xyd_fa5zoEU"
            "burpee" in lower -> "dZgVxmf6jkA"
            "jump" in lower -> "C385GzYep6I"
            "walk" in lower -> "njeZ29umqVE"
            "cycling" in lower || "cycle" in lower || "bike" in lower -> "k1MwdNr9k5k"
            "swim" in lower -> "GVX_HKWWXDY"
            "mountain" in lower -> "nmwgirgXLYM"
            "tricep" in lower -> "2-LAMcpzODU"
            "calf" in lower -> "gwLzBJYoWlA"
            "glute" in lower || "hip" in lower -> "1EqWuHH8hXw"
            else -> "dQw4w9WgXcQ" // universal fallback
        }
    }

    private fun openYouTube(videoId: String) {
        val youtubeAppUri = Uri.parse("vnd.youtube:$videoId")
        val youtubeBrowserUri = Uri.parse("https://www.youtube.com/watch?v=$videoId")
        val intent = Intent(Intent.ACTION_VIEW, youtubeAppUri).apply {
            setPackage("com.google.android.youtube")
        }
        if (intent.resolveActivity(packageManager) != null) {
            startActivity(intent)
        } else {
            startActivity(Intent(Intent.ACTION_VIEW, youtubeBrowserUri))
        }
    }
}

class ExerciseCompactAdapter(
    private val exercises: List<ExerciseItem>,
    private val onYouTubeClick: (ExerciseItem) -> Unit
) : RecyclerView.Adapter<ExerciseCompactAdapter.ViewHolder>() {

    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val nameText: TextView = view.findViewById(R.id.exerciseNameText)
        val setText: TextView = view.findViewById(R.id.exerciseSetText)
        val youtubeBtn: ImageView = view.findViewById(R.id.youtubeBtn)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.list_item_exercise_compact, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val exercise = exercises[position]
        holder.nameText.text = exercise.name
        holder.setText.text = "Tap ▶ to watch on YouTube"
        holder.youtubeBtn.setOnClickListener { onYouTubeClick(exercise) }
        holder.itemView.setOnClickListener { onYouTubeClick(exercise) }
    }

    override fun getItemCount() = exercises.size
}

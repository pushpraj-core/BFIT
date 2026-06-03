package com.pushprajcore.bfit

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.bottomnavigation.BottomNavigationView

data class ExerciseVideo(val title: String, val description: String, val videoId: String)

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
                R.id.nav_profile -> false
                else -> false
            }
        }

        val recyclerView = findViewById<RecyclerView>(R.id.exerciseRecyclerView)
        recyclerView.layoutManager = LinearLayoutManager(this)

        // Sample Exercises
        val exercises = listOf(
            ExerciseVideo("Pushups", "3 sets of 15 reps", "IODxDxX78TI"),
            ExerciseVideo("Squats", "3 sets of 20 reps", "YaXPRqUwItQ"),
            ExerciseVideo("Plank", "3 sets of 60 seconds", "pSHjTRCQxIw"),
            ExerciseVideo("Lunges", "3 sets of 12 reps per leg", "QOVaHwm-Q6U")
        )

        recyclerView.adapter = ExerciseAdapter(exercises) { exercise ->
            val intent = Intent(this, VideoPlayerActivity::class.java)
            intent.putExtra("VIDEO_ID", exercise.videoId)
            startActivity(intent)
        }
    }
}

class ExerciseAdapter(
    private val exercises: List<ExerciseVideo>,
    private val onItemClick: (ExerciseVideo) -> Unit
) : RecyclerView.Adapter<ExerciseAdapter.ViewHolder>() {

    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val title: TextView = view.findViewById(R.id.exerciseTitleText)
        val desc: TextView = view.findViewById(R.id.exerciseDescText)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.list_item_video, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val exercise = exercises[position]
        holder.title.text = exercise.title
        holder.desc.text = exercise.description
        holder.itemView.setOnClickListener { onItemClick(exercise) }
    }

    override fun getItemCount() = exercises.size
}

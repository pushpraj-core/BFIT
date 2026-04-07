package com.example.bfit

import android.net.Uri
import android.os.Bundle
import android.view.View
import android.widget.ImageView
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import com.example.bfit.database.ExtraMealItem
import com.example.bfit.database.PlanRepository
import com.google.android.material.appbar.MaterialToolbar
import com.google.android.material.button.MaterialButton
import com.google.android.material.chip.Chip
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.label.ImageLabeling
import com.google.mlkit.vision.label.defaults.ImageLabelerOptions
import java.util.Calendar

class MealRecognitionActivity : AppCompatActivity() {

    private lateinit var planRepository: PlanRepository
    private lateinit var mealImagePreview: ImageView
    private lateinit var emptyMealState: TextView
    private lateinit var selectImageButton: MaterialButton
    private lateinit var retakeImageButton: MaterialButton
    private lateinit var analyzeMealButton: MaterialButton
    private lateinit var addToLogButton: MaterialButton
    private lateinit var confidenceChip: Chip
    private lateinit var proteinMacroText: TextView
    private lateinit var carbsMacroText: TextView
    private lateinit var fatsMacroText: TextView
    private lateinit var analyzeProgress: ProgressBar
    private lateinit var mealRecognitionResult: TextView

    private var selectedImageUri: Uri? = null
    private var latestDetectedMealName: String = ""
    private var latestEstimatedCalories: Int = 0
    private var latestProtein: Int = 0

    private val imagePickerLauncher = registerForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        if (uri != null) {
            selectedImageUri = uri
            mealImagePreview.setImageURI(uri)
            emptyMealState.visibility = View.GONE
            mealRecognitionResult.text = "Ready to analyze"
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_meal_recognition)

        planRepository = PlanRepository(this)

        val toolbar = findViewById<MaterialToolbar>(R.id.mealToolbar)
        toolbar.setNavigationIcon(androidx.appcompat.R.drawable.abc_ic_ab_back_material)
        toolbar.setNavigationOnClickListener {
            onBackPressedDispatcher.onBackPressed()
        }

        mealImagePreview = findViewById(R.id.mealImagePreview)
        emptyMealState = findViewById(R.id.emptyMealState)
        selectImageButton = findViewById(R.id.selectImageButton)
        retakeImageButton = findViewById(R.id.retakeImageButton)
        analyzeMealButton = findViewById(R.id.analyzeMealButton)
        addToLogButton = findViewById(R.id.addToLogButton)
        confidenceChip = findViewById(R.id.confidenceChip)
        proteinMacroText = findViewById(R.id.proteinMacroText)
        carbsMacroText = findViewById(R.id.carbsMacroText)
        fatsMacroText = findViewById(R.id.fatsMacroText)
        analyzeProgress = findViewById(R.id.analyzeProgress)
        mealRecognitionResult = findViewById(R.id.mealRecognitionResult)

        selectImageButton.setOnClickListener {
            imagePickerLauncher.launch("image/*")
        }

        retakeImageButton.setOnClickListener {
            imagePickerLauncher.launch("image/*")
        }

        analyzeMealButton.setOnClickListener {
            analyzeSelectedImage()
        }

        addToLogButton.setOnClickListener {
            addRecognizedMealToLog()
        }

        findViewById<View>(android.R.id.content).apply {
            alpha = 0f
            animate().alpha(1f).setDuration(260).start()
        }
    }

    private fun analyzeSelectedImage() {
        val imageUri = selectedImageUri
        if (imageUri == null) {
            Toast.makeText(this, "Please select an image", Toast.LENGTH_SHORT).show()
            return
        }

        val image = try {
            InputImage.fromFilePath(this, imageUri)
        } catch (e: Exception) {
            Toast.makeText(this, "Failed to read image", Toast.LENGTH_SHORT).show()
            return
        }

        setLoading(true)

        val labeler = ImageLabeling.getClient(ImageLabelerOptions.DEFAULT_OPTIONS)
        labeler.process(image)
            .addOnSuccessListener { labels ->
                if (labels.isEmpty()) {
                    mealRecognitionResult.text = "Could not identify meal from this photo."
                    return@addOnSuccessListener
                }

                val sorted = labels.sortedByDescending { it.confidence }
                val top = sorted.take(5)
                val bestLabel = top.first().text
                val estimatedCalories = estimateCalories(top.map { it.text })
                val confidence = (top.first().confidence * 100).toInt()
                latestDetectedMealName = bestLabel
                latestEstimatedCalories = estimatedCalories
                latestProtein = estimateProtein(estimatedCalories)

                val confidenceText = top.joinToString("\n") {
                    "- ${it.text} (${(it.confidence * 100).toInt()}%)"
                }

                confidenceChip.text = "${getString(R.string.confidence_label)}: $confidence%"
                proteinMacroText.text = "${getString(R.string.macro_protein)}: ${latestProtein}g"
                carbsMacroText.text = "${getString(R.string.macro_carbs)}: ${estimateCarbs(estimatedCalories)}g"
                fatsMacroText.text = "${getString(R.string.macro_fats)}: ${estimateFats(estimatedCalories)}g"
                addToLogButton.visibility = View.VISIBLE

                mealRecognitionResult.text =
                    "Most likely: $bestLabel\nEstimated calories: ~$estimatedCalories kcal\n\nDetected labels:\n$confidenceText"
            }
            .addOnFailureListener {
                mealRecognitionResult.text = "Analysis failed. Try another image."
                addToLogButton.visibility = View.GONE
            }
            .addOnCompleteListener {
                setLoading(false)
                labeler.close()
            }
    }

    private fun estimateCalories(labels: List<String>): Int {
        val lower = labels.joinToString(" ").lowercase()
        return when {
            lower.contains("salad") -> 220
            lower.contains("rice") -> 320
            lower.contains("pizza") -> 450
            lower.contains("burger") -> 520
            lower.contains("sandwich") -> 350
            lower.contains("chicken") -> 380
            lower.contains("pasta") -> 430
            lower.contains("egg") -> 210
            lower.contains("fruit") -> 180
            lower.contains("bread") -> 260
            lower.contains("food") || lower.contains("dish") || lower.contains("meal") -> 350
            else -> 300
        }
    }

    private fun estimateProtein(calories: Int): Int = (calories * 0.2f / 4f).toInt().coerceAtLeast(5)
    private fun estimateCarbs(calories: Int): Int = (calories * 0.45f / 4f).toInt().coerceAtLeast(10)
    private fun estimateFats(calories: Int): Int = (calories * 0.35f / 9f).toInt().coerceAtLeast(3)

    private fun addRecognizedMealToLog() {
        if (latestDetectedMealName.isBlank() || latestEstimatedCalories <= 0) {
            Toast.makeText(this, "Analyze a meal first", Toast.LENGTH_SHORT).show()
            return
        }

        val today = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }.timeInMillis

        val mealName = "AI: $latestDetectedMealName"
        val item = ExtraMealItem(
            id = "${today}-FOOD-$mealName",
            date = today,
            text = mealName,
            calories = latestEstimatedCalories,
            protein = latestProtein
        )
        planRepository.addExtraMealItem(item)
        planRepository.addCaloriesToDailyLog(today, latestEstimatedCalories, latestProtein)
        Toast.makeText(this, "Added to daily log", Toast.LENGTH_SHORT).show()
    }

    private fun setLoading(isLoading: Boolean) {
        analyzeProgress.visibility = if (isLoading) View.VISIBLE else View.GONE
        analyzeMealButton.isEnabled = !isLoading
        selectImageButton.isEnabled = !isLoading
        retakeImageButton.isEnabled = !isLoading
    }
}

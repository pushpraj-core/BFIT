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
import com.google.android.material.button.MaterialButton
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.label.ImageLabeling
import com.google.mlkit.vision.label.defaults.ImageLabelerOptions

class MealRecognitionActivity : AppCompatActivity() {

    private lateinit var mealImagePreview: ImageView
    private lateinit var selectImageButton: MaterialButton
    private lateinit var analyzeMealButton: MaterialButton
    private lateinit var analyzeProgress: ProgressBar
    private lateinit var mealRecognitionResult: TextView

    private var selectedImageUri: Uri? = null

    private val imagePickerLauncher = registerForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        if (uri != null) {
            selectedImageUri = uri
            mealImagePreview.setImageURI(uri)
            mealRecognitionResult.text = "Ready to analyze"
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_meal_recognition)

        mealImagePreview = findViewById(R.id.mealImagePreview)
        selectImageButton = findViewById(R.id.selectImageButton)
        analyzeMealButton = findViewById(R.id.analyzeMealButton)
        analyzeProgress = findViewById(R.id.analyzeProgress)
        mealRecognitionResult = findViewById(R.id.mealRecognitionResult)

        selectImageButton.setOnClickListener {
            imagePickerLauncher.launch("image/*")
        }

        analyzeMealButton.setOnClickListener {
            analyzeSelectedImage()
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

                val confidenceText = top.joinToString("\n") {
                    "- ${it.text} (${(it.confidence * 100).toInt()}%)"
                }

                mealRecognitionResult.text =
                    "Most likely: $bestLabel\nEstimated calories: ~$estimatedCalories kcal\n\nDetected labels:\n$confidenceText"
            }
            .addOnFailureListener {
                mealRecognitionResult.text = "Analysis failed. Try another image."
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

    private fun setLoading(isLoading: Boolean) {
        analyzeProgress.visibility = if (isLoading) View.VISIBLE else View.GONE
        analyzeMealButton.isEnabled = !isLoading
        selectImageButton.isEnabled = !isLoading
    }
}

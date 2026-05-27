package com.pushprajcore.bfit

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.activity.OnBackPressedCallback
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.RecyclerView
import com.pushprajcore.bfit.database.FirestoreRepository
import com.google.android.material.button.MaterialButton
import kotlinx.coroutines.launch

data class Supplement(
    val id: String = "",
    val name: String,
    val description: String,
    val price: Double = 0.0,
    val url: String = ""
)

class StoreActivity : AppCompatActivity() {

    private lateinit var firestoreRepository: FirestoreRepository

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_store)

        firestoreRepository = FirestoreRepository()

        // Back button
        findViewById<Button>(R.id.backButton).setOnClickListener {
            finish()
            overridePendingTransition(R.anim.slide_in_left, R.anim.slide_out_right)
        }

        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                finish()
                overridePendingTransition(R.anim.slide_in_left, R.anim.slide_out_right)
            }
        })

        // Purchase history button
        findViewById<Button>(R.id.purchaseHistoryBtn).setOnClickListener {
            showPurchaseHistory()
        }

        val supplementsRecyclerView = findViewById<RecyclerView>(R.id.supplementsRecyclerView)

        // See More Amazon Button
        findViewById<Button>(R.id.seeMoreAmazonBtn).setOnClickListener {
            val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://www.amazon.com/s?k=fitness+supplements"))
            startActivity(intent)
        }

        // Try loading from Firestore first, fall back to defaults
        lifecycleScope.launch {
            seedDatabaseIfNeeded()
            val firestoreSupplements = firestoreRepository.getSupplements()
            val supplements = if (firestoreSupplements.isNotEmpty()) {
                firestoreSupplements.map { data ->
                    Supplement(
                        id = data["id"] as? String ?: "",
                        name = data["name"] as? String ?: "Unknown",
                        description = data["description"] as? String ?: "",
                        price = (data["price"] as? Number)?.toDouble() ?: 0.0,
                        url = data["url"] as? String ?: ""
                    )
                }
            } else {
                Toast.makeText(this@StoreActivity, "No products available at the moment.", Toast.LENGTH_SHORT).show()
                emptyList()
            }
            supplementsRecyclerView.adapter = SupplementsAdapter(supplements) { supplement ->
                if (supplement.url.isNotEmpty()) {
                    val intent = Intent(Intent.ACTION_VIEW, Uri.parse(supplement.url))
                    startActivity(intent)
                } else {
                    Toast.makeText(this@StoreActivity, "Product link coming soon!", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    private fun showPurchaseHistory() {
        lifecycleScope.launch {
            try {
                val purchases = firestoreRepository.getPurchaseHistory()
                if (purchases.isEmpty()) {
                    Toast.makeText(this@StoreActivity, getString(R.string.no_purchase_history), Toast.LENGTH_SHORT).show()
                    return@launch
                }

                val items = purchases.map { purchase ->
                    val name = purchase["supplementName"] as? String ?: "Unknown"
                    val price = (purchase["price"] as? Number)?.toDouble() ?: 0.0
                    val status = purchase["status"] as? String ?: "unknown"
                    "$name — $${"%.2f".format(price)} ($status)"
                }.toTypedArray()

                AlertDialog.Builder(this@StoreActivity)
                    .setTitle(getString(R.string.purchase_history))
                    .setItems(items, null)
                    .setPositiveButton("Close", null)
                    .show()
            } catch (e: Exception) {
                Toast.makeText(this@StoreActivity, "Error: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }

    // Default hardcoded supplements removed

    private suspend fun seedDatabaseIfNeeded() {
        val supplements = firestoreRepository.getSupplements()
        if (supplements.isEmpty()) {
            val seedItems = listOf(
                mapOf("id" to "seed1", "name" to "Optimum Nutrition Gold Standard 100% Whey", "description" to "Muscle building whey protein powder.", "price" to 44.99, "url" to "https://www.amazon.com/s?k=optimum+nutrition+whey+protein"),
                mapOf("id" to "seed2", "name" to "Cellucor C4 Original Pre Workout", "description" to "Explosive energy and performance.", "price" to 29.99, "url" to "https://www.amazon.com/s?k=c4+pre+workout"),
                mapOf("id" to "seed3", "name" to "BSN SYNTHA-6 Whey Protein Powder", "description" to "Cold stone creamery mint chocolate chip.", "price" to 54.99, "url" to "https://www.amazon.com/s?k=bsn+syntha-6"),
                mapOf("id" to "seed4", "name" to "MuscleTech Platinum Creatine", "description" to "100% pure micronized creatine powder.", "price" to 19.99, "url" to "https://www.amazon.com/s?k=muscletech+creatine"),
                mapOf("id" to "seed5", "name" to "Liquid I.V. Hydration Multiplier", "description" to "Electrolyte drink mix.", "price" to 24.99, "url" to "https://www.amazon.com/s?k=liquid+iv"),
                mapOf("id" to "seed6", "name" to "XTEND Original BCAA Powder", "description" to "Branched chain amino acids for recovery.", "price" to 27.99, "url" to "https://www.amazon.com/s?k=xtend+bcaa"),
                mapOf("id" to "seed7", "name" to "Animal Pak", "description" to "Vitamin pack supplement for sports nutrition.", "price" to 34.99, "url" to "https://www.amazon.com/s?k=animal+pak"),
                mapOf("id" to "seed8", "name" to "Orgain Organic Vegan Protein Powder", "description" to "Plant based protein powder.", "price" to 29.99, "url" to "https://www.amazon.com/s?k=orgain+vegan+protein"),
                mapOf("id" to "seed9", "name" to "Ghost Legend Pre-Workout", "description" to "Energy and focus with authentic flavor collabs.", "price" to 44.99, "url" to "https://www.amazon.com/s?k=ghost+pre+workout"),
                mapOf("id" to "seed10", "name" to "Quest Nutrition Protein Bar", "description" to "High protein, low carb gluten free bars.", "price" to 23.99, "url" to "https://www.amazon.com/s?k=quest+protein+bar")
            )
            for (item in seedItems) {
                firestoreRepository.addSupplement(item)
            }
        }
    }
}

class SupplementsAdapter(
    private val supplements: List<Supplement>,
    private val onBuyClicked: (Supplement) -> Unit
) :
    RecyclerView.Adapter<SupplementsAdapter.SupplementViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): SupplementViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.list_item_supplement, parent, false)
        return SupplementViewHolder(view)
    }

    override fun onBindViewHolder(holder: SupplementViewHolder, position: Int) {
        val supplement = supplements[position]
        holder.bind(supplement)
    }

    override fun getItemCount() = supplements.size

    inner class SupplementViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val nameTextView: TextView = itemView.findViewById(R.id.supplementName)
        private val descriptionTextView: TextView = itemView.findViewById(R.id.supplementDescription)
        private val priceTextView: TextView = itemView.findViewById(R.id.supplementPrice)
        private val buyButton: Button = itemView.findViewById(R.id.buyButton)

        fun bind(supplement: Supplement) {
            nameTextView.text = supplement.name
            descriptionTextView.text = supplement.description
            priceTextView.text = if (supplement.price > 0) "$${String.format("%.2f", supplement.price)}" else ""
            buyButton.setOnClickListener {
                onBuyClicked(supplement)
            }
        }
    }
}

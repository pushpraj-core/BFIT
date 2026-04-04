package com.example.bfit

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.RecyclerView
import com.example.bfit.database.FirestoreRepository
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
        val supplementsRecyclerView = findViewById<RecyclerView>(R.id.supplementsRecyclerView)

        // Try loading from Firestore first, fall back to defaults
        lifecycleScope.launch {
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
                getDefaultSupplements()
            }
            supplementsRecyclerView.adapter = SupplementsAdapter(supplements) { supplement ->
                showCheckoutDialog(supplement)
            }
        }

        // Purchase history button
        findViewById<MaterialButton>(R.id.purchaseHistoryBtn)?.setOnClickListener {
            showPurchaseHistory()
        }
    }

    private fun showCheckoutDialog(supplement: Supplement) {
        val dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_checkout, null)

        val productName = dialogView.findViewById<TextView>(R.id.checkoutProductName)
        val productDesc = dialogView.findViewById<TextView>(R.id.checkoutProductDesc)
        val quantityText = dialogView.findViewById<TextView>(R.id.quantityText)
        val btnMinus = dialogView.findViewById<MaterialButton>(R.id.btnMinus)
        val btnPlus = dialogView.findViewById<MaterialButton>(R.id.btnPlus)
        val subtotalText = dialogView.findViewById<TextView>(R.id.subtotalText)
        val totalText = dialogView.findViewById<TextView>(R.id.totalText)
        val placeOrderBtn = dialogView.findViewById<MaterialButton>(R.id.placeOrderBtn)

        productName.text = supplement.name
        productDesc.text = supplement.description

        var quantity = 1

        fun updatePrices() {
            val subtotal = supplement.price * quantity
            subtotalText.text = "$${String.format("%.2f", subtotal)}"
            totalText.text = "$${String.format("%.2f", subtotal)}"
        }

        updatePrices()

        btnMinus.setOnClickListener {
            if (quantity > 1) {
                quantity--
                quantityText.text = quantity.toString()
                updatePrices()
            }
        }

        btnPlus.setOnClickListener {
            if (quantity < 10) {
                quantity++
                quantityText.text = quantity.toString()
                updatePrices()
            }
        }

        val dialog = AlertDialog.Builder(this)
            .setView(dialogView)
            .create()

        placeOrderBtn.setOnClickListener {
            val totalPrice = supplement.price * quantity
            lifecycleScope.launch {
                firestoreRepository.recordPurchase(
                    supplementName = supplement.name,
                    supplementId = supplement.id,
                    price = totalPrice
                )
                dialog.dismiss()
                showOrderConfirmation(supplement.name, quantity, totalPrice)
            }
        }

        dialog.show()
    }

    private fun showOrderConfirmation(name: String, quantity: Int, total: Double) {
        AlertDialog.Builder(this)
            .setTitle("🎉 Order Placed!")
            .setMessage(
                "Your order has been confirmed!\n\n" +
                "📦 Item: $name\n" +
                "📊 Quantity: $quantity\n" +
                "💰 Total: $${String.format("%.2f", total)}\n\n" +
                "You'll receive your supplements soon. Keep grinding! 💪"
            )
            .setPositiveButton("Awesome!") { _, _ ->
                Toast.makeText(this, "✅ Order $name confirmed!", Toast.LENGTH_SHORT).show()
            }
            .show()
    }

    private fun showPurchaseHistory() {
        lifecycleScope.launch {
            val purchases = firestoreRepository.getPurchaseHistory()
            if (purchases.isEmpty()) {
                Toast.makeText(this@StoreActivity, "No purchase history yet", Toast.LENGTH_SHORT).show()
                return@launch
            }

            val historyItems = purchases.map { purchase ->
                val name = purchase["supplementName"] as? String ?: "Unknown"
                val price = (purchase["price"] as? Number)?.toDouble() ?: 0.0
                val status = purchase["status"] as? String ?: "pending"
                "🛒 $name — $${String.format("%.2f", price)} ($status)"
            }.toTypedArray()

            AlertDialog.Builder(this@StoreActivity)
                .setTitle("📜 Purchase History")
                .setItems(historyItems, null)
                .setPositiveButton("Close", null)
                .show()
        }
    }

    private fun getDefaultSupplements(): List<Supplement> {
        return listOf(
            Supplement("whey_protein", "Whey Protein", "A high-quality protein for muscle growth and repair.", 29.99),
            Supplement("creatine", "Creatine", "Improves strength, power, and muscle mass.", 19.99),
            Supplement("bcaas", "BCAAs", "Reduces muscle soreness and exercise fatigue.", 24.99),
            Supplement("pre_workout", "Pre-Workout", "Boosts energy and focus for your workouts.", 34.99),
            Supplement("multivitamin", "Multivitamin", "Ensures you get all the essential vitamins and minerals.", 14.99)
        )
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
        private val buyButton: Button = itemView.findViewById(R.id.buyButton)

        fun bind(supplement: Supplement) {
            nameTextView.text = supplement.name
            descriptionTextView.text = supplement.description
            val priceText = if (supplement.price > 0) "Buy - $${String.format("%.2f", supplement.price)}" else "Buy Now"
            buyButton.text = priceText
            buyButton.setOnClickListener {
                onBuyClicked(supplement)
            }
        }
    }
}
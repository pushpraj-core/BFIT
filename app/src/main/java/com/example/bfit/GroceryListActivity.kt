package com.example.bfit

import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.CheckBox
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.RecyclerView
import com.example.bfit.databinding.ActivityGroceryListBinding
import java.io.Serializable

data class GroceryItem(
    val name: String,
    val category: String,
    val quantity: String,
    var isChecked: Boolean = false
)

class GroceryListActivity : AppCompatActivity() {

    private lateinit var binding: ActivityGroceryListBinding
    private val groceryItems = mutableListOf<GroceryItem>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityGroceryListBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Back button
        binding.backButton.setOnClickListener {
            finish()
            overridePendingTransition(R.anim.slide_in_left, R.anim.slide_out_right)
        }

        val planResult = getSerializable(intent, "plan", PlanResult::class.java)

        if (planResult == null) {
            Toast.makeText(this, "Generate a plan first to see your grocery list", Toast.LENGTH_LONG).show()
            finish()
            return
        }

        // Generate grocery list from meal plan
        generateGroceryList(planResult)

        // Update counter
        updateCounter()

        // Setup RecyclerView
        val adapter = GroceryAdapter(groceryItems) {
            updateCounter()
        }
        binding.groceryRecyclerView.adapter = adapter

        // Share button
        binding.shareButton.setOnClickListener {
            shareGroceryList()
        }

        // Clear checked button
        binding.clearCheckedButton.setOnClickListener {
            groceryItems.forEach { it.isChecked = false }
            (binding.groceryRecyclerView.adapter as? GroceryAdapter)?.refresh()
            updateCounter()
        }
    }

    override fun onBackPressed() {
        super.onBackPressed()
        overridePendingTransition(R.anim.slide_in_left, R.anim.slide_out_right)
    }

    private fun updateCounter() {
        val checked = groceryItems.count { it.isChecked }
        val total = groceryItems.size
        binding.counterText.text = "$checked / $total items"
        binding.progressBar.max = total
        binding.progressBar.progress = checked
    }

    private fun generateGroceryList(plan: PlanResult) {
        // Extract all unique meal names from the 7-day plan
        val allMeals = plan.mealPlan.values.flatten().map { it.first }
        val uniqueMeals = allMeals.toSet()

        // Parse ingredients from meal names and categorize
        val ingredientMap = mutableMapOf<String, MutableSet<String>>()

        for (mealName in uniqueMeals) {
            val ingredients = extractIngredients(mealName)
            for ((ingredient, category) in ingredients) {
                ingredientMap.getOrPut(category) { mutableSetOf() }.add(ingredient)
            }
        }

        // Convert to GroceryItem list, sorted by category
        groceryItems.clear()
        val categoryOrder = listOf("🥬 Vegetables & Greens", "🍎 Fruits", "🥛 Dairy & Eggs", "🥩 Protein",
            "🌾 Grains & Cereals", "🫘 Legumes & Pulses", "🧴 Oils & Condiments", "🥜 Nuts & Seeds", "🧂 Spices", "📦 Other")

        for (category in categoryOrder) {
            val items = ingredientMap[category]
            if (items != null && items.isNotEmpty()) {
                for (item in items.sorted()) {
                    val count = allMeals.count { it.contains(item, ignoreCase = true) }
                    val qty = if (count > 3) "Stock up" else if (count > 1) "${count}x this week" else "1 serving"
                    groceryItems.add(GroceryItem(item, category, qty))
                }
            }
        }
    }

    private fun extractIngredients(mealName: String): List<Pair<String, String>> {
        // Common ingredient mappings for Indian & international meals
        val ingredientDatabase = mapOf(
            // Vegetables
            "Spinach" to "🥬 Vegetables & Greens", "Palak" to "🥬 Vegetables & Greens",
            "Broccoli" to "🥬 Vegetables & Greens", "Cauliflower" to "🥬 Vegetables & Greens",
            "Potato" to "🥬 Vegetables & Greens", "Aloo" to "🥬 Vegetables & Greens",
            "Tomato" to "🥬 Vegetables & Greens", "Onion" to "🥬 Vegetables & Greens",
            "Capsicum" to "🥬 Vegetables & Greens", "Carrot" to "🥬 Vegetables & Greens",
            "Peas" to "🥬 Vegetables & Greens", "Matar" to "🥬 Vegetables & Greens",
            "Mushroom" to "🥬 Vegetables & Greens", "Cabbage" to "🥬 Vegetables & Greens",
            "Cucumber" to "🥬 Vegetables & Greens", "Lettuce" to "🥬 Vegetables & Greens",
            "Sweet Potato" to "🥬 Vegetables & Greens", "Zucchini" to "🥬 Vegetables & Greens",
            "Avocado" to "🥬 Vegetables & Greens", "Kale" to "🥬 Vegetables & Greens",
            "Salad" to "🥬 Vegetables & Greens", "Bhindi" to "🥬 Vegetables & Greens",
            "Gobi" to "🥬 Vegetables & Greens", "Sabzi" to "🥬 Vegetables & Greens",

            // Fruits
            "Banana" to "🍎 Fruits", "Apple" to "🍎 Fruits", "Mango" to "🍎 Fruits",
            "Orange" to "🍎 Fruits", "Berries" to "🍎 Fruits", "Strawberry" to "🍎 Fruits",
            "Blueberry" to "🍎 Fruits", "Papaya" to "🍎 Fruits", "Fruit" to "🍎 Fruits",
            "Pineapple" to "🍎 Fruits", "Pomegranate" to "🍎 Fruits",

            // Dairy
            "Milk" to "🥛 Dairy & Eggs", "Yogurt" to "🥛 Dairy & Eggs", "Curd" to "🥛 Dairy & Eggs",
            "Dahi" to "🥛 Dairy & Eggs", "Paneer" to "🥛 Dairy & Eggs", "Cheese" to "🥛 Dairy & Eggs",
            "Butter" to "🥛 Dairy & Eggs", "Ghee" to "🥛 Dairy & Eggs", "Egg" to "🥛 Dairy & Eggs",
            "Cream" to "🥛 Dairy & Eggs", "Whey" to "🥛 Dairy & Eggs", "Lassi" to "🥛 Dairy & Eggs",
            "Buttermilk" to "🥛 Dairy & Eggs", "Cottage Cheese" to "🥛 Dairy & Eggs",

            // Protein
            "Chicken" to "🥩 Protein", "Fish" to "🥩 Protein", "Salmon" to "🥩 Protein",
            "Tuna" to "🥩 Protein", "Mutton" to "🥩 Protein", "Lamb" to "🥩 Protein",
            "Turkey" to "🥩 Protein", "Shrimp" to "🥩 Protein", "Prawn" to "🥩 Protein",
            "Meat" to "🥩 Protein", "Beef" to "🥩 Protein", "Tofu" to "🥩 Protein",
            "Soya" to "🥩 Protein", "Tempeh" to "🥩 Protein",

            // Grains
            "Rice" to "🌾 Grains & Cereals", "Roti" to "🌾 Grains & Cereals",
            "Chapati" to "🌾 Grains & Cereals", "Bread" to "🌾 Grains & Cereals",
            "Oats" to "🌾 Grains & Cereals", "Wheat" to "🌾 Grains & Cereals",
            "Quinoa" to "🌾 Grains & Cereals", "Paratha" to "🌾 Grains & Cereals",
            "Pasta" to "🌾 Grains & Cereals", "Noodle" to "🌾 Grains & Cereals",
            "Poha" to "🌾 Grains & Cereals", "Upma" to "🌾 Grains & Cereals",
            "Dosa" to "🌾 Grains & Cereals", "Idli" to "🌾 Grains & Cereals",
            "Muesli" to "🌾 Grains & Cereals", "Granola" to "🌾 Grains & Cereals",
            "Cereal" to "🌾 Grains & Cereals", "Bajra" to "🌾 Grains & Cereals",
            "Jowar" to "🌾 Grains & Cereals", "Ragi" to "🌾 Grains & Cereals",
            "Naan" to "🌾 Grains & Cereals", "Puri" to "🌾 Grains & Cereals",
            "Semolina" to "🌾 Grains & Cereals", "Rava" to "🌾 Grains & Cereals",

            // Legumes
            "Dal" to "🫘 Legumes & Pulses", "Lentil" to "🫘 Legumes & Pulses",
            "Chickpea" to "🫘 Legumes & Pulses", "Chana" to "🫘 Legumes & Pulses",
            "Rajma" to "🫘 Legumes & Pulses", "Bean" to "🫘 Legumes & Pulses",
            "Moong" to "🫘 Legumes & Pulses", "Sprout" to "🫘 Legumes & Pulses",
            "Chole" to "🫘 Legumes & Pulses", "Urad" to "🫘 Legumes & Pulses",
            "Toor" to "🫘 Legumes & Pulses", "Masoor" to "🫘 Legumes & Pulses",
            "Kidney Bean" to "🫘 Legumes & Pulses", "Soybean" to "🫘 Legumes & Pulses",

            // Oils & Condiments
            "Oil" to "🧴 Oils & Condiments", "Olive Oil" to "🧴 Oils & Condiments",
            "Coconut Oil" to "🧴 Oils & Condiments", "Honey" to "🧴 Oils & Condiments",
            "Vinegar" to "🧴 Oils & Condiments", "Soy Sauce" to "🧴 Oils & Condiments",
            "Mustard" to "🧴 Oils & Condiments", "Ketchup" to "🧴 Oils & Condiments",
            "Chutney" to "🧴 Oils & Condiments", "Pickle" to "🧴 Oils & Condiments",

            // Nuts & Seeds
            "Almond" to "🥜 Nuts & Seeds", "Cashew" to "🥜 Nuts & Seeds",
            "Walnut" to "🥜 Nuts & Seeds", "Peanut" to "🥜 Nuts & Seeds",
            "Chia" to "🥜 Nuts & Seeds", "Flax" to "🥜 Nuts & Seeds",
            "Pumpkin Seed" to "🥜 Nuts & Seeds", "Sesame" to "🥜 Nuts & Seeds",
            "Sunflower Seed" to "🥜 Nuts & Seeds", "Pistachio" to "🥜 Nuts & Seeds",
            "Dry Fruit" to "🥜 Nuts & Seeds", "Raisin" to "🥜 Nuts & Seeds",

            // Spices
            "Turmeric" to "🧂 Spices", "Cumin" to "🧂 Spices", "Coriander" to "🧂 Spices",
            "Ginger" to "🧂 Spices", "Garlic" to "🧂 Spices", "Pepper" to "🧂 Spices",
            "Cinnamon" to "🧂 Spices", "Masala" to "🧂 Spices"
        )

        val found = mutableListOf<Pair<String, String>>()
        for ((ingredient, category) in ingredientDatabase) {
            if (mealName.contains(ingredient, ignoreCase = true)) {
                found.add(ingredient to category)
            }
        }

        // If nothing matched, add the meal itself
        if (found.isEmpty()) {
            found.add(mealName to "📦 Other")
        }

        return found
    }

    private fun shareGroceryList() {
        val uncheckedItems = groceryItems.filter { !it.isChecked }
        if (uncheckedItems.isEmpty()) {
            Toast.makeText(this, "All items checked off! 🎉", Toast.LENGTH_SHORT).show()
            return
        }

        val grouped = uncheckedItems.groupBy { it.category }
        val sb = StringBuilder("🛒 BFIT Grocery List\n")
        sb.append("━━━━━━━━━━━━━━━━━━\n\n")

        for ((category, items) in grouped) {
            sb.append("$category\n")
            for (item in items) {
                sb.append("  □ ${item.name} (${item.quantity})\n")
            }
            sb.append("\n")
        }

        sb.append("Generated by BFIT 💪")

        val shareIntent = Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_TEXT, sb.toString())
            putExtra(Intent.EXTRA_SUBJECT, "BFIT Grocery List")
        }
        startActivity(Intent.createChooser(shareIntent, "Share Grocery List"))
    }

    @Suppress("DEPRECATION")
    private fun <T : Serializable?> getSerializable(intent: android.content.Intent, key: String, clazz: Class<T>): T? {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            intent.getSerializableExtra(key, clazz)
        } else {
            intent.getSerializableExtra(key) as? T
        }
    }
}

// ─── Adapter ───

sealed class GroceryListItem {
    data class Header(val category: String) : GroceryListItem()
    data class Item(val groceryItem: GroceryItem) : GroceryListItem()
}

class GroceryAdapter(
    private val items: MutableList<GroceryItem>,
    private val onCheckedChanged: () -> Unit
) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    private fun buildDisplayList(): List<GroceryListItem> {
        val result = mutableListOf<GroceryListItem>()
        var lastCategory = ""
        for (item in items) {
            if (item.category != lastCategory) {
                result.add(GroceryListItem.Header(item.category))
                lastCategory = item.category
            }
            result.add(GroceryListItem.Item(item))
        }
        return result
    }

    private var displayList = buildDisplayList()

    companion object {
        private const val TYPE_HEADER = 0
        private const val TYPE_ITEM = 1
    }

    override fun getItemViewType(position: Int) = when (displayList[position]) {
        is GroceryListItem.Header -> TYPE_HEADER
        is GroceryListItem.Item -> TYPE_ITEM
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        return if (viewType == TYPE_HEADER) {
            val view = LayoutInflater.from(parent.context)
                .inflate(R.layout.list_header_plan, parent, false)
            HeaderViewHolder(view)
        } else {
            val view = LayoutInflater.from(parent.context)
                .inflate(R.layout.list_item_grocery, parent, false)
            ItemViewHolder(view)
        }
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        when (val item = displayList[position]) {
            is GroceryListItem.Header -> {
                (holder as HeaderViewHolder).headerText.text = item.category
            }
            is GroceryListItem.Item -> {
                val vh = holder as ItemViewHolder
                val grocery = item.groceryItem
                vh.nameText.text = grocery.name
                vh.quantityText.text = grocery.quantity
                vh.checkBox.setOnCheckedChangeListener(null)
                vh.checkBox.isChecked = grocery.isChecked
                vh.nameText.alpha = if (grocery.isChecked) 0.5f else 1f
                vh.quantityText.alpha = if (grocery.isChecked) 0.5f else 1f

                vh.checkBox.setOnCheckedChangeListener { _, isChecked ->
                    grocery.isChecked = isChecked
                    vh.nameText.alpha = if (isChecked) 0.5f else 1f
                    vh.quantityText.alpha = if (isChecked) 0.5f else 1f
                    onCheckedChanged()
                }

                vh.itemView.setOnClickListener {
                    vh.checkBox.isChecked = !vh.checkBox.isChecked
                }
            }
        }
    }

    override fun getItemCount() = displayList.size

    @Suppress("NotifyDataSetChanged")
    fun refresh() {
        displayList = buildDisplayList()
        notifyDataSetChanged()
    }

    class HeaderViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val headerText: TextView = view.findViewById(R.id.headerText)
    }

    class ItemViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val nameText: TextView = view.findViewById(R.id.groceryName)
        val quantityText: TextView = view.findViewById(R.id.groceryQuantity)
        val checkBox: CheckBox = view.findViewById(R.id.groceryCheckBox)
    }
}

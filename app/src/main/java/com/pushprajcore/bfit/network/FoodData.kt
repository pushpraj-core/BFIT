package com.pushprajcore.bfit.network

import com.google.gson.annotations.SerializedName

data class FoodResponse(
    val product: Product?,
    val status: Int = 0
)

data class Product(
    @SerializedName("product_name")
    val productName: String?,
    @SerializedName("product_name_en")
    val productNameEn: String?,
    val nutriments: Nutriments?
) {
    fun displayName(): String = productName?.takeIf { it.isNotBlank() }
        ?: productNameEn?.takeIf { it.isNotBlank() }
        ?: "Unknown Product"
}

data class Nutriments(
    // Calories — covers all known OpenFoodFacts field naming variants
    @SerializedName(value = "energy-kcal_100g", alternate = ["energy-kcal", "energy_kcal100g", "energy-kcal_value"])
    val energyKcal100g: Double?,

    // Energy in kJ — fallback if kcal not present (divide by 4.184)
    @SerializedName(value = "energy_100g", alternate = ["energy", "energy_value"])
    val energyKj100g: Double?,

    @SerializedName(value = "proteins_100g", alternate = ["proteins", "protein_100g", "proteins_value"])
    val proteins_100g: Double?,

    @SerializedName(value = "carbohydrates_100g", alternate = ["carbohydrates", "carbohydrate_100g", "carbohydrates_value"])
    val carbohydrates_100g: Double?,

    @SerializedName(value = "fat_100g", alternate = ["fat", "fats_100g", "fat_value"])
    val fat_100g: Double?,

    @SerializedName(value = "sugars_100g", alternate = ["sugars", "sugar_100g"])
    val sugars_100g: Double?,

    @SerializedName(value = "fiber_100g", alternate = ["fiber", "fibers_100g"])
    val fiber_100g: Double?,

    @SerializedName(value = "sodium_100g", alternate = ["sodium", "salt_100g"])
    val sodium_100g: Double?
) {
    /** Best-effort kcal per 100g: direct kcal field → kJ converted → 0 */
    fun kcalPer100g(): Double =
        energyKcal100g
            ?: energyKj100g?.let { it / 4.184 }
            ?: 0.0
}

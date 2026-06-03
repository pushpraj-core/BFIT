package com.example.bfit

import org.junit.Assert.*
import org.junit.Test

/**
 * Tests for the Supplement data model used in StoreActivity.
 * Validates data integrity and default supplement catalog.
 */
class SupplementTest {

    @Test
    fun supplement_creation_withAllFields() {
        val supplement = Supplement(
            id = "whey_001",
            name = "Whey Protein",
            description = "High quality protein",
            price = 29.99,
            url = "https://example.com/whey"
        )
        assertEquals("whey_001", supplement.id)
        assertEquals("Whey Protein", supplement.name)
        assertEquals(29.99, supplement.price, 0.01)
        assertEquals("https://example.com/whey", supplement.url)
    }

    @Test
    fun supplement_defaultValues() {
        val supplement = Supplement(
            name = "Test",
            description = "Test desc"
        )
        assertEquals("", supplement.id)
        assertEquals(0.0, supplement.price, 0.01)
        assertEquals("", supplement.url)
    }

    @Test
    fun supplement_priceFormatting_free() {
        val supplement = Supplement(name = "Free Sample", description = "Free", price = 0.0)
        val priceText = if (supplement.price > 0) "$${String.format("%.2f", supplement.price)}" else "Free"
        assertEquals("Free", priceText)
    }

    @Test
    fun supplement_priceFormatting_paid() {
        val supplement = Supplement(name = "Creatine", description = "Power", price = 19.99)
        val priceText = if (supplement.price > 0) "$${String.format("%.2f", supplement.price)}" else "Free"
        assertEquals("$19.99", priceText)
    }

    @Test
    fun defaultSupplements_containsFiveItems() {
        val supplements = getDefaultSupplements()
        assertEquals(5, supplements.size)
    }

    @Test
    fun defaultSupplements_allHavePositivePrices() {
        val supplements = getDefaultSupplements()
        supplements.forEach { supplement ->
            assertTrue("${supplement.name} should have a positive price", supplement.price > 0)
        }
    }

    @Test
    fun defaultSupplements_allHaveUniqueIds() {
        val supplements = getDefaultSupplements()
        val ids = supplements.map { it.id }
        assertEquals("Supplement IDs must be unique", ids.size, ids.distinct().size)
    }

    @Test
    fun defaultSupplements_allHaveDescriptions() {
        val supplements = getDefaultSupplements()
        supplements.forEach { supplement ->
            assertTrue("${supplement.name} should have a description", supplement.description.isNotEmpty())
        }
    }

    // Mirror the default supplements from StoreActivity
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

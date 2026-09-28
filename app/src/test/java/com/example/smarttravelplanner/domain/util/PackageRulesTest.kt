package com.example.smarttravelplanner.domain.util

import com.example.smarttravelplanner.domain.model.Activity
import com.example.smarttravelplanner.domain.model.agent.AgentPackage
import com.example.smarttravelplanner.domain.model.agent.PackageDay
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate

class PackageRulesTest {

    private fun act(title: String) = Activity(itineraryId = 99, title = title)

    private val jaipur = AgentPackage(
        title = "Royal Jaipur Weekend",
        destination = "Jaipur",
        pricePerPerson = 12_000.0,
        highlights = listOf("Amber Fort", "Heritage hotel"),
        days = listOf(
            PackageDay(1, "Forts", listOf(act("Amber Fort"), act("Jal Mahal"))),
            PackageDay(2, "Bazaars", listOf(act("Johari Bazaar"))),
            PackageDay(3, "Departure", emptyList())
        )
    )

    @Test
    fun validPackageHasNoProblems() {
        assertTrue(PackageRules.validate(jaipur, forPublishing = true).isEmpty())
    }

    @Test
    fun missingBasicsAreReported() {
        val bad = jaipur.copy(title = " ", destination = "", pricePerPerson = 0.0, days = emptyList())
        assertEquals(4, PackageRules.validate(bad, forPublishing = false).size)
    }

    @Test
    fun publishingNeedsActivitiesButDraftsDoNot() {
        val empty = jaipur.copy(days = listOf(PackageDay(1), PackageDay(2)))
        assertTrue(PackageRules.validate(empty, forPublishing = false).isEmpty())
        assertEquals(listOf("Add at least one activity before publishing."), PackageRules.validate(empty, forPublishing = true))
    }

    @Test
    fun dayNumbersMustBeInOrder() {
        val gap = jaipur.copy(days = listOf(PackageDay(1, activities = listOf(act("A"))), PackageDay(3)))
        assertTrue(PackageRules.validate(gap, forPublishing = false).contains("Days must be numbered 1, 2, 3\u2026"))
    }

    @Test
    fun groupPriceCountsEveryTraveller() {
        assertEquals(36_000.0, PackageRules.groupPrice(12_000.0, adults = 2, kids = 1), 0.0)
        assertEquals(12_000.0, PackageRules.groupPrice(12_000.0, adults = 0, kids = 0), 0.0)
    }

    @Test
    fun choosingCopiesDaysWithRealDates() {
        val start = LocalDate.of(2026, 11, 20)
        val days = PackageRules.toItinerary(jaipur, tripId = 7, start = start)
        assertEquals(listOf("2026-11-20", "2026-11-21", "2026-11-22"), days.map { it.date })
        assertEquals(listOf("Amber Fort", "Jal Mahal"), days[0].activities.map { it.title })
        assertEquals(listOf(0L, 1L), days[0].activities.map { it.id })
        assertTrue(days.all { it.tripId == 7L })
        assertEquals(LocalDate.of(2026, 11, 22), PackageRules.endDate(jaipur, start))
    }

    @Test
    fun searchMatchesDestinationTitleAndHighlights() {
        assertTrue(PackageRules.matches(jaipur, "jaipur"))
        assertTrue(PackageRules.matches(jaipur, "royal"))
        assertTrue(PackageRules.matches(jaipur, "heritage"))
        assertTrue(PackageRules.matches(jaipur, ""))
        assertFalse(PackageRules.matches(jaipur, "goa"))
    }
}

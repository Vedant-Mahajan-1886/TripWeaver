package com.example.smarttravelplanner.domain.util

import com.example.smarttravelplanner.domain.model.Itinerary
import com.example.smarttravelplanner.domain.model.agent.AgentPackage
import java.time.LocalDate

/**
 * Rules for agents' ready-made packages. Pure Kotlin so it can be unit tested.
 */
object PackageRules {

    const val MAX_DAYS = 30

    /** Problems with a package; empty list = OK to save. Publishing also needs at least one activity. */
    fun validate(pkg: AgentPackage, forPublishing: Boolean): List<String> = buildList {
        if (pkg.title.isBlank()) add("Give the package a title.")
        if (pkg.destination.isBlank()) add("Add a destination.")
        if (pkg.pricePerPerson <= 0 || pkg.pricePerPerson.isNaN()) add("Enter the price per person.")
        if (pkg.days.isEmpty()) add("Add at least one day.")
        if (pkg.days.size > MAX_DAYS) add("A package can be at most $MAX_DAYS days long.")
        if (pkg.days.map { it.dayNumber } != (1..pkg.days.size).toList()) add("Days must be numbered 1, 2, 3…")
        if (forPublishing && pkg.activityCount == 0) add("Add at least one activity before publishing.")
    }

    /** Estimated price for a group (kids count as travellers too). */
    fun groupPrice(pricePerPerson: Double, adults: Int, kids: Int): Double =
        pricePerPerson * (adults.coerceAtLeast(1) + kids.coerceAtLeast(0))

    /** The package's days turned into the traveller's own itinerary, starting on [start]. */
    fun toItinerary(pkg: AgentPackage, tripId: Long, start: LocalDate): List<Itinerary> =
        pkg.days.sortedBy { it.dayNumber }.map { day ->
            Itinerary(
                tripId = tripId,
                dayNumber = day.dayNumber,
                date = start.plusDays((day.dayNumber - 1).toLong()).toString(),
                activities = day.activities.mapIndexed { i, a -> a.copy(id = i.toLong(), itineraryId = 0) }
            )
        }

    /** Last day of a trip made from the package. */
    fun endDate(pkg: AgentPackage, start: LocalDate): LocalDate = start.plusDays((pkg.days.size - 1).coerceAtLeast(0).toLong())

    /** Simple search over published packages (destination, title, highlights). */
    fun matches(pkg: AgentPackage, query: String): Boolean {
        val q = query.trim().lowercase()
        if (q.isEmpty()) return true
        return pkg.destination.lowercase().contains(q) ||
            pkg.title.lowercase().contains(q) ||
            pkg.highlights.any { it.lowercase().contains(q) }
    }
}

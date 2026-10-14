package com.example.smarttravelplanner.domain.util

import com.example.smarttravelplanner.domain.model.ExploreDestination
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class SoloFriendlyDestinationsTest {

    private fun dest(name: String) = ExploreDestination(
        name = name, stateCountry = "India", idealDuration = "3 days",
        bestSeason = "Oct-Mar", tag = "Nature", tagCategory = "nature", imageRes = 0
    )

    @Test
    fun knownDestination_isSoloFriendly_ignoringCaseAndSpaces() {
        assertTrue(SoloFriendlyDestinations.isSoloFriendly("Goa"))
        assertTrue(SoloFriendlyDestinations.isSoloFriendly("  rishikesh "))
        assertTrue(SoloFriendlyDestinations.isSoloFriendly("Spiti Valley"))
    }

    @Test
    fun otherDestination_isNotSoloFriendly() {
        assertFalse(SoloFriendlyDestinations.isSoloFriendly("Agra"))
        assertFalse(SoloFriendlyDestinations.isSoloFriendly(""))
    }

    @Test
    fun soloFirst_movesFriendlyOnesUp_keepingOrderInsideEachPart() {
        val sorted = SoloFriendlyDestinations.soloFirst(
            listOf(dest("Agra"), dest("Goa"), dest("Mysore"), dest("Manali"))
        )
        assertEquals(listOf("Goa", "Manali", "Agra", "Mysore"), sorted.map { it.name })
    }

    @Test
    fun soloFirst_emptyList() {
        assertTrue(SoloFriendlyDestinations.soloFirst(emptyList()).isEmpty())
    }
}

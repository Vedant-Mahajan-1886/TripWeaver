package com.example.smarttravelplanner.domain.util

import com.example.smarttravelplanner.domain.model.ExploreDestination
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class DestinationFilterTest {

    private fun dest(
        name: String,
        state: String,
        tag: String,
        category: String,
        season: String = "Best: Oct – Mar"
    ) = ExploreDestination(
        name = name,
        stateCountry = state,
        idealDuration = "3–4 days ideal",
        bestSeason = season,
        tag = tag,
        tagCategory = category,
        imageRes = 0
    )

    private val catalog = listOf(
        dest("Agra", "UTTAR PRADESH · INDIA", "Heritage", "cultural"),
        dest("Varanasi", "UTTAR PRADESH · INDIA", "Religious", "religious"),
        dest("Rishikesh", "UTTARAKHAND · INDIA", "Spiritual & Rafting", "adventure"),
        dest("Munnar", "KERALA · INDIA", "Tea Gardens", "nature", season = "Best: Sep – May")
    )

    @Test
    fun allCategoryAndBlankQueryReturnsEverything() {
        val result = DestinationFilter.apply(catalog, query = "", category = DestinationFilter.ALL)
        assertEquals(catalog, result)
    }

    @Test
    fun categoryFiltersByTagCategory() {
        val result = DestinationFilter.apply(catalog, query = "", category = "nature")
        assertEquals(listOf("Munnar"), result.map { it.name })
    }

    @Test
    fun queryMatchesNameCaseInsensitively() {
        val result = DestinationFilter.apply(catalog, query = "  aGrA ", category = DestinationFilter.ALL)
        assertEquals(listOf("Agra"), result.map { it.name })
    }

    @Test
    fun queryMatchesState() {
        val result = DestinationFilter.apply(catalog, query = "uttar pradesh", category = DestinationFilter.ALL)
        assertEquals(listOf("Agra", "Varanasi"), result.map { it.name })
    }

    @Test
    fun queryMatchesSeason() {
        val result = DestinationFilter.apply(catalog, query = "sep", category = DestinationFilter.ALL)
        assertEquals(listOf("Munnar"), result.map { it.name })
    }

    @Test
    fun queryAndCategoryAreCombined() {
        val result = DestinationFilter.apply(catalog, query = "uttar", category = "religious")
        assertEquals(listOf("Varanasi"), result.map { it.name })
    }

    @Test
    fun noMatchReturnsEmptyList() {
        val result = DestinationFilter.apply(catalog, query = "paris", category = DestinationFilter.ALL)
        assertTrue(result.isEmpty())
    }
}

package com.example.smarttravelplanner.data.remote.ai

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class SoloGuidelineTest {

    @Test
    fun oneAdultNoKids_getsSoloGuideline() {
        assertTrue(ItineraryPromptBuilder.soloGuideline(adults = 1, kids = 0).contains("SOLO TRAVELLER"))
    }

    @Test
    fun couplesFamiliesAndGroups_getNoSoloGuideline() {
        assertEquals("", ItineraryPromptBuilder.soloGuideline(adults = 2, kids = 0))
        assertEquals("", ItineraryPromptBuilder.soloGuideline(adults = 1, kids = 1))
        assertEquals("", ItineraryPromptBuilder.soloGuideline(adults = 4, kids = 2))
    }
}

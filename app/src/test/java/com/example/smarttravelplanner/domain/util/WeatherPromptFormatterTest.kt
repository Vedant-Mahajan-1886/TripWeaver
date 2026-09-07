package com.example.smarttravelplanner.domain.util

import com.example.smarttravelplanner.domain.model.DayWeather
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class WeatherPromptFormatterTest {

    @Test
    fun `formatForAiPrompt returns empty string when forecast is empty`() {
        val result = WeatherPromptFormatter.formatForAiPrompt(emptyList())
        assertEquals("", result)
    }

    @Test
    fun `formatForAiPrompt includes rain advice for heavy rain or thunderstorm`() {
        val forecast = listOf(
            DayWeather(
                date = "2026-09-10",
                maxTempC = 29.0,
                minTempC = 24.0,
                precipitationMm = 14.5,
                weatherCode = 63,
                description = "Rain",
                iconEmoji = "🌧️"
            )
        )

        val result = WeatherPromptFormatter.formatForAiPrompt(forecast)

        assertTrue(result.contains("Day 1 (2026-09-10)"))
        assertTrue(result.contains("24°C to 29°C"))
        assertTrue(result.contains("🌧️ Rain"))
        assertTrue(result.contains("14.5 mm precipitation"))
        assertTrue(result.contains("Rain/storm expected"))
        assertTrue(result.contains("indoor attractions"))
    }

    @Test
    fun `formatForAiPrompt includes heat advice for temperatures above 36C`() {
        val forecast = listOf(
            DayWeather(
                date = "2026-09-10",
                maxTempC = 39.5,
                minTempC = 28.0,
                precipitationMm = 0.0,
                weatherCode = 0,
                description = "Clear sky",
                iconEmoji = "☀️"
            )
        )

        val result = WeatherPromptFormatter.formatForAiPrompt(forecast)

        assertTrue(result.contains("High heat"))
        assertTrue(result.contains("early morning or late evening"))
    }

    @Test
    fun `formatForAiPrompt includes favorable conditions advice for clear mild weather`() {
        val forecast = listOf(
            DayWeather(
                date = "2026-09-10",
                maxTempC = 26.0,
                minTempC = 18.0,
                precipitationMm = 0.0,
                weatherCode = 1,
                description = "Mainly clear",
                iconEmoji = "🌤️"
            )
        )

        val result = WeatherPromptFormatter.formatForAiPrompt(forecast)

        assertTrue(result.contains("Favorable conditions"))
        assertTrue(result.contains("outdoor sightseeing"))
    }
}

package com.example.smarttravelplanner.domain.model

/**
 * Represents the weather forecast for a single day of a trip.
 * Sourced from Open-Meteo (no API key required).
 * WMO weather code reference: https://open-meteo.com/en/docs#weathervariables
 */
data class DayWeather(
    val date: String,             // ISO-8601 e.g. "2024-10-15"
    val maxTempC: Double,
    val minTempC: Double,
    val precipitationMm: Double,
    val weatherCode: Int,         // WMO code (0=clear, 61=rain, 71=snow, etc.)
    val description: String,      // Human-readable e.g. "Partly cloudy"
    val iconEmoji: String         // Quick emoji icon e.g. "⛅", "🌧️", "☀️"
)

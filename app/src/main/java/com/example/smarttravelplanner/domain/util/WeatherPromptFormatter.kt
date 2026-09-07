package com.example.smarttravelplanner.domain.util

import com.example.smarttravelplanner.domain.model.DayWeather

/**
 * Utility for formatting domain [DayWeather] entities into structured, actionable context
 * for AI models (Ollama / LLMs) during trip planning and assistant interactions.
 */
object WeatherPromptFormatter {

    /**
     * Formats a list of [DayWeather] daily forecasts into a clear, structured prompt section
     * with temperature ranges, weather descriptions, precipitation, and actionable planning advice.
     */
    fun formatForAiPrompt(forecast: List<DayWeather>): String {
        if (forecast.isEmpty()) return ""

        val lines = mutableListOf<String>()
        lines.add("Forecasted Weather for Destination:")

        forecast.forEachIndexed { index, day ->
            val dayNum = index + 1
            val weatherDesc = "${day.iconEmoji} ${day.description}"
            val tempRange = "${day.minTempC.toInt()}°C to ${day.maxTempC.toInt()}°C"
            val precipText = if (day.precipitationMm > 0.0) {
                "${day.precipitationMm} mm precipitation"
            } else {
                "dry / no rain"
            }

            val advice = getDayAdvice(day)
            lines.add("- Day $dayNum (${day.date}): $tempRange, $weatherDesc ($precipText). $advice")
        }

        lines.add("")
        lines.add("Weather Note Rule: Only add 'weather_note' to activities where weather is genuinely relevant (e.g. 'Clear skies expected for golden hour', 'Cool temperatures due to altitude', rain/heat warnings). For hotels, meals, or generic indoor spots, set 'weather_note': null.")

        return lines.joinToString("\n")
    }

    private fun getDayAdvice(day: DayWeather): String = when {
        // High precipitation or thunderstorm / rain codes
        day.precipitationMm >= 5.0 || day.weatherCode in listOf(61, 63, 65, 66, 67, 80, 81, 82, 95, 96, 99) ->
            "⚠️ Rain/storm expected: prioritize indoor attractions (museums, palaces, art galleries, cafes, indoor markets). Recommend umbrella/rain gear in activity weather_notes."

        // High heat
        day.maxTempC >= 36.0 ->
            "☀️ High heat: schedule outdoor activities in early morning or late evening. Mid-day should be shaded/indoor. Recommend hydration in weather_notes."

        // Cold weather
        day.minTempC <= 10.0 ->
            "❄️ Chilly weather: recommend warm clothing / layers in weather_notes."

        // Light rain / drizzle
        day.precipitationMm > 0.0 || day.weatherCode in listOf(51, 53, 55) ->
            "🌦️ Light drizzle possible: keep outdoor plans flexible and suggest carrying a light umbrella."

        // Clear or pleasant
        else ->
            "🌤️ Favorable conditions: ideal for outdoor sightseeing, monuments, nature walks, and photography."
    }
}

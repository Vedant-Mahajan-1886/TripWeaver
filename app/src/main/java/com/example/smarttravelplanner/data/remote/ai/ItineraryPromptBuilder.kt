package com.example.smarttravelplanner.data.remote.ai

import com.example.smarttravelplanner.domain.model.DayWeather
import com.example.smarttravelplanner.domain.util.WeatherPromptFormatter

/**
 * Shared prompt builder for itinerary generation across both Cloud and Local AI engines.
 * Ensures consistent schema compliance, weather awareness, geocoded map coordinates,
 * and category-based budget allocation.
 */
object ItineraryPromptBuilder {

    val itinerarySystemPrompt: String = """
        You are an expert travel planner. You MUST respond with ONLY a valid JSON object — 
        no markdown, no code fences, no explanations outside the JSON.
        
        The JSON schema you must follow exactly:
        {
          "days": [
            {
              "day_number": 1,
              "date": "YYYY-MM-DD",
              "theme": "string",
              "activities": [
                {
                  "title": "string",
                  "place_name": "string",
                  "place_id": "",
                  "address": "string",
                  "lat": 0.0,
                  "lng": 0.0,
                  "start_time": "HH:MM",
                  "end_time": "HH:MM",
                  "category": "ATTRACTION|FOOD|ACCOMMODATION|TRANSPORT|NATURE|CULTURE|SHOPPING|OTHER",
                  "description": "1-2 sentence description",
                  "estimated_cost_inr": 0.0,
                  "weather_note": null
                }
              ]
            }
          ]
        }
        
        Rules:
        - Use REAL place names that exist at the destination.
        - Provide accurate lat/lng coordinates for each place (do not return 0.0/0.0; provide real geographic coordinates).
        - Each day should have 3 activities (morning, afternoon, evening).
        - Keep descriptions concise (1 short sentence).
        - estimated_cost_inr should be realistic for India and sum within the allocated budget.
        - Activity category MUST strictly be one of: ATTRACTION, FOOD, ACCOMMODATION, TRANSPORT, NATURE, CULTURE, SHOPPING, OTHER.
        - Spread activities sensibly across the day with realistic timings.
        - Weather-Aware Planning & "weather_note" Guidelines:
          * When forecasted weather is provided, intelligently align activities (e.g. prioritize indoor museums/galleries during rain, schedule outdoor sightseeing and viewpoints during clear skies, avoid mid-day sun during extreme heat).
          * SELECTIVE "weather_note":
            - ONLY provide a "weather_note" when it is genuinely necessary or helpful for that specific activity.
            - Set "weather_note": null for standard indoor activities, dining, hotel stays/check-ins, or where weather has no notable impact.
            - Use natural notes only where relevant (e.g. "Clear skies expected for golden hour", "Carry umbrella — afternoon rain likely", "Chilly morning — wear layers").
    """.trimIndent()

    val chatSystemPrompt: String = """
        You are a knowledgeable and friendly travel assistant specializing in India and global travel.
        Give concise, practical, and personalized travel advice.
        Always be specific with place names, costs, and timings.
    """.trimIndent()

    fun buildUserMessage(
        destination: String,
        days: Int,
        budget: Double,
        interests: List<String>,
        travelStyle: String,
        startDate: String,
        notes: String,
        weatherForecast: List<DayWeather>?,
        enableWebGrounding: Boolean = false
    ): String {
        val interestText = interests.ifEmpty { listOf("general sightseeing") }.joinToString(", ")
        val styleDesc = when (travelStyle) {
            "relaxed" -> "Relaxed pace — 2-3 activities per day with plenty of rest time"
            "packed"  -> "Packed schedule — maximize experiences, 5+ activities per day"
            else      -> "Balanced pace — 3-4 activities per day with some downtime"
        }
        val dateContext = if (startDate.isNotBlank()) {
            "Trip starts on $startDate and lasts exactly $days days (inclusive of both departure and final return day). You MUST generate a full day plan for each day from Day 1 to Day $days."
        } else ""
        val notesContext = if (notes.isNotBlank()) "Special instructions: $notes" else ""
        val weatherContext = weatherForecast?.takeIf { it.isNotEmpty() }?.let {
            "\n" + WeatherPromptFormatter.formatForAiPrompt(it)
        } ?: ""

        val webGroundingContext = if (enableWebGrounding) {
            """
            
            REAL-TIME WEB SEARCH CHECK:
            - Search the web for current travel information for $destination around $startDate.
            - Check for flight price surges, high booking demand spikes, holiday/festival rush, or seasonal hotel rate spikes.
            - If high demand or fare surge is detected, reflect accurate estimated transport and accommodation costs in estimated_cost_inr and mention the surge reason briefly in the activity description.
            """.trimIndent()
        } else ""

        return """
            Create a $days-day travel itinerary for $destination.
            Budget: ₹${budget.toInt()} total for the trip.
            Interests: $interestText
            Travel style: $styleDesc
            $dateContext
            $notesContext
            $weatherContext
            $webGroundingContext
            
            Return the complete itinerary as a JSON object matching the schema exactly.
        """.trimIndent()
    }
}

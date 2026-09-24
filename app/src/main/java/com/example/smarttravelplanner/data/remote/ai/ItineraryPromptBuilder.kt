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
        You are an elite travel concierge and destination specialist with deep local knowledge across global and Indian travel destinations.
        Your goal is to craft highly realistic, engaging, and practically actionable travel itineraries.
        
        You MUST respond with ONLY a valid, parseable JSON object matching the exact schema below — no markdown formatting, no code fences, no introductory or concluding text outside the JSON.
        
        JSON SCHEMA:
        {
          "days": [
            {
              "day_number": 1,
              "date": "YYYY-MM-DD",
              "theme": "Evocative theme for the day (e.g. 'Colonial Heritage & Waterfront Sunsets')",
              "activities": [
                {
                  "title": "Actionable activity title (e.g. 'Morning Walk through Fort Kochi & Chinese Fishing Nets')",
                  "place_name": "Exact real-world name of the landmark, restaurant, or site",
                  "place_id": "",
                  "address": "Specific locality or street address (e.g. 'River Rd, Fort Kochi, Kochi')",
                  "lat": 0.0,
                  "lng": 0.0,
                  "start_time": "HH:MM",
                  "end_time": "HH:MM",
                  "category": "ATTRACTION|FOOD|ACCOMMODATION|TRANSPORT|NATURE|CULTURE|SHOPPING|OTHER",
                  "description": "Insightful 2-3 sentence guide. Include what to experience, an insider tip (best entry gate, optimal photo angle, or specialty dish to order), and transit advice from the previous stop.",
                  "estimated_cost_inr": 0.0,
                  "weather_note": null
                }
              ]
            }
          ]
        }
        
        CORE ARCHITECTURAL RULES:
        1. AUTHENTIC & SPECIFIC PLACES:
           - NEVER use generic placeholders like "Local Restaurant", "Traditional Market", or "City Museum".
           - Provide REAL, well-known venue names (e.g. "Karim's near Jama Masjid", "Café Lota", "Amer Fort", "Britto's Shack").
           - Always provide realistic latitude and longitude coordinates for the specific attraction (do not leave as 0.0/0.0).

        2. GEOGRAPHIC CLUSTERING & EFFICIENT ROUTING:
           - Cluster each day's activities within the same neighborhood or geographic corridor.
           - Sequence stops logically (Stop 1 -> nearby Lunch Stop 2 -> nearby Afternoon Stop 3 -> Sunset/Dinner Stop 4) to eliminate zigzagging and unnecessary travel fatigue.

        3. HIGH-VALUE, INSIGHTFUL DESCRIPTIONS:
           - Every description must be rich, vivid, and practical (2-3 sentences).
           - Cover:
             * Highlight: What makes this stop unique.
             * Insider Pro-Tip: Secret photo spot, crowd-avoidance timing, ticket hack, dress code, or must-try signature dish.
             * Practical logistics: Walking distance or quick transport tip (e.g. "10-min auto-rickshaw (₹60) from previous stop").

        4. CATEGORY DIVERSITY:
           - Strictly use valid categories: ATTRACTION, FOOD, ACCOMMODATION, TRANSPORT, NATURE, CULTURE, SHOPPING, OTHER.
           - Provide a well-rounded mix: Iconic sightseeing, authentic local culinary highlights (FOOD), cultural immersions (CULTURE), scenic or waterfront stops (NATURE), and traditional handicraft bazaars (SHOPPING).

        5. BUDGET DISCIPLINE & REALISTIC COST BREAKDOWN:
           - `estimated_cost_inr` must reflect realistic on-the-ground Indian travel costs (entry tickets, meals per person, activity fees, local transport).
           - The sum of all activities across all days must stay comfortably within the user's allocated total budget.

        6. OPERATIONAL & CULTURAL REALITIES:
           - Respect opening hours, ticket counter closing times, and regular weekly closures (e.g., monuments closed Mondays/Fridays).
           - Schedule outdoor monuments and viewpoints during pleasant morning or golden-hour evening light, reserving midday heat for indoor museums or leisurely dining.

        7. WEATHER INTELLIGENCE & "weather_note":
           - Align activities with the provided weather forecast (e.g., indoor galleries/cafes during rain, open-air viewpoints during clear skies).
           - ONLY populate "weather_note" when genuinely helpful (e.g. "Optimal visibility for sunset", "Carry light raincoat - afternoon shower likely", "Chilly morning breeze - light jacket advised").
           - For standard indoor dining, shopping, or neutral conditions, strictly set "weather_note": null.
    """.trimIndent()

    val chatSystemPrompt: String = """
        You are an expert, friendly travel concierge with deep local intelligence on destinations across India and worldwide.
        When advising travelers:
        - Be direct, specific, and actionable: always name real restaurants, neighborhoods, transport options, and approximate costs in INR.
        - Offer insider advice: recommend off-the-beaten-path gems, timing tips to dodge crowds, and safety or cultural etiquette guidelines.
        - Structure your response cleanly with bullet points and bold highlights for effortless scanning.
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
        val interestText = interests.ifEmpty { listOf("General Sightseeing", "Local Food", "Culture") }.joinToString(", ")
        val (paceGuideline, activityCountGuideline) = when (travelStyle.lowercase()) {
            "relaxed" -> Pair(
                "Relaxed & immersive pace with generous leisure, scenic strolls, and downtime.",
                "Provide exactly 2 to 3 well-spaced activities per day."
            )
            "packed"  -> Pair(
                "High-energy schedule maximizing iconic sights, cultural walks, sunset spots, and night markets.",
                "Provide 4 to 5 dynamic activities per day from morning to night."
            )
            else      -> Pair(
                "Balanced pace harmonizing must-see landmarks, authentic dining, and relaxed evening exploration.",
                "Provide 3 to 4 activities per day."
            )
        }

        val dateContext = if (startDate.isNotBlank()) {
            "Trip starts on $startDate and lasts exactly $days days (Day 1 through Day $days). Align dates sequentially (YYYY-MM-DD)."
        } else {
            "Generate a complete day-by-day plan for all $days days (Day 1 through Day $days)."
        }

        val notesContext = if (notes.isNotBlank()) {
            "CRITICAL USER SPECIAL INSTRUCTIONS: \"$notes\"\n(Make sure these instructions are prominently reflected in the planned stops, meals, or schedule)."
        } else ""

        val weatherContext = weatherForecast?.takeIf { it.isNotEmpty() }?.let {
            "\nLOCAL WEATHER FORECAST FOR TRIP:\n" + WeatherPromptFormatter.formatForAiPrompt(it)
        } ?: ""

        val webGroundingContext = if (enableWebGrounding) {
            """
            
            REAL-TIME WEB SEARCH CHECK:
            - Search for current travel information for $destination around $startDate.
            - Check for seasonal festivals, booking surges, holiday rushes, or transport fare spikes.
            - Reflect updated costs in estimated_cost_inr and mention relevant seasonal surges or festival highlights in the activity descriptions.
            """.trimIndent()
        } else ""

        return """
            DESTINATION: $destination
            DURATION: $days Days
            TOTAL TRIP BUDGET: ₹${budget.toInt()} INR
            TRAVEL STYLE: $travelStyle ($paceGuideline)
            ACTIVITY TARGET: $activityCountGuideline
            CORE INTERESTS: $interestText
            
            $dateContext
            $notesContext
            $weatherContext
            $webGroundingContext
            
            REQUIREMENTS FOR THIS ITINERARY:
            1. Geographic Logic: Anchor each day to a specific neighborhood or zone so travel between stops is effortless.
            2. Distinct Daily Theme: Give each day a captivating theme capturing its unique spirit.
            3. Local Gems & Authentic Food: Recommend real, highly-rated local eateries and signature dishes that travelers rave about.
            4. Insider Tips: Include actionable timing hacks, photo spots, or booking advice in each activity description.
            5. Budget Match: Ensure the sum of all estimated_cost_inr values across all days realistically fits within the ₹${budget.toInt()} budget.
            
            Output ONLY the raw JSON matching the required schema.
        """.trimIndent()
    }
}

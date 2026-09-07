package com.example.smarttravelplanner.data.repository

import com.example.smarttravelplanner.data.remote.api.OllamaApi
import com.example.smarttravelplanner.data.remote.dto.GeneratedItineraryDto
import com.example.smarttravelplanner.data.remote.dto.OllamaChatRequest
import com.example.smarttravelplanner.data.remote.dto.OllamaMessage
import com.example.smarttravelplanner.domain.model.DayWeather
import com.example.smarttravelplanner.domain.util.WeatherPromptFormatter
import com.google.gson.Gson
import com.google.gson.JsonSyntaxException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import javax.inject.Inject

class AiRepositoryImpl @Inject constructor(
    private val ollamaApi: OllamaApi,
    private val gson: Gson
) : AiRepository {

    // ── System prompt for itinerary generation ────────────────────────────────
    private val itinerarySystemPrompt = """
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
        - Provide accurate lat/lng coordinates for each place.
        - Each day should have 3 activities (morning, afternoon, evening).
        - Keep descriptions concise (1 short sentence).
        - estimated_cost_inr should be realistic for India.
        - Spread activities sensibly across the day with realistic timings.
        - Weather-Aware Planning & "weather_note" Guidelines:
          * When forecasted weather is provided, intelligently align activities (e.g. prioritize indoor museums/galleries during rain, schedule outdoor sightseeing and viewpoints during clear skies, avoid mid-day sun during extreme heat).
          * SELECTIVE "weather_note" (CRITICAL):
            - ONLY provide a "weather_note" when it is genuinely necessary or helpful for that specific activity.
            - Do NOT force a weather note on every activity! Set "weather_note": null for standard indoor activities, dining, hotel stays/check-ins, or where weather has no notable impact.
            - Use natural, practical notes only where relevant, such as:
              • "Clear skies expected for golden hour" (for sunrise/sunset/viewpoints/outdoor photography)
              • "Cool temperatures due to altitude" (for hills/mountains/high elevations)
              • "Carry umbrella — rain showers likely in afternoon" (for outdoor rainy activities)
              • "Visit early to avoid peak afternoon sun and heat" (for open-air forts/monuments/walking tours)
              • "Pleasant evening breeze ideal for strolls" (for lakefront/beach/gardens)
    """.trimIndent()

    // ── System prompt for general chat ────────────────────────────────────────
    private val chatSystemPrompt = """
        You are a knowledgeable and friendly travel assistant specializing in India travel.
        Give concise, practical, and personalized travel advice.
        Always be specific with place names, costs, and timings.
    """.trimIndent()

    // ─────────────────────────────────────────────────────────────────────────
    override suspend fun generateItinerary(
        destination: String,
        days: Int,
        budget: Double,
        interests: List<String>,
        travelStyle: String,
        startDate: String,
        notes: String,
        model: String,
        weatherForecast: List<DayWeather>?
    ): Result<GeneratedItineraryDto> = withContext(Dispatchers.IO) {
        runCatching {

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

        val userMessage = """
            Create a $days-day travel itinerary for $destination.
            Budget: ₹${budget.toInt()} total for the trip.
            Interests: $interestText
            Travel style: $styleDesc
            $dateContext
            $notesContext
            $weatherContext
            
            Return the complete itinerary as a JSON object matching the schema exactly.
        """.trimIndent()

        val response = ollamaApi.chat(
            OllamaChatRequest(
                model    = model,
                messages = listOf(
                    OllamaMessage(role = "system", content = itinerarySystemPrompt),
                    OllamaMessage(role = "user",   content = userMessage)
                ),
                stream  = false,
                format  = "json",
                think   = false,
                options = mapOf(
                    "num_ctx" to 8192,
                    "num_predict" to 4096,
                    "temperature" to 0.7
                )
            )
        )

        val raw = (response.message?.content?.takeIf { it.isNotBlank() }
            ?: response.message?.thinking?.takeIf { it.isNotBlank() })
            ?: error("Empty response from AI model '$model'. Make sure Ollama is running and the model is loaded.")

        // 1. Remove <think>...</think> block if model emitted reasoning tags
        val noThinking = raw.replace(Regex("<think>[\\s\\S]*?</think>"), "").trim()

        // 2. Extract JSON object from first '{' to last '}'
        val firstBrace = noThinking.indexOf('{')
        val lastBrace  = noThinking.lastIndexOf('}')
        val cleaned = if (firstBrace != -1 && lastBrace != -1 && lastBrace > firstBrace) {
            noThinking.substring(firstBrace, lastBrace + 1)
        } else {
            noThinking.removePrefix("```json").removePrefix("```").removeSuffix("```").trim()
        }

        if (cleaned.isBlank()) {
            error("AI model '$model' returned an empty response. Please try again or switch model.")
        }

        try {
            val dto = tryParseOrRepairItineraryJson(cleaned)
            if (dto.days.isNullOrEmpty()) {
                error("AI returned an itinerary with 0 days — please try again.")
            }
            dto
        } catch (e: JsonSyntaxException) {
            error("AI response could not be parsed as JSON: ${e.message}")
        }
    } }

    // ─────────────────────────────────────────────────────────────────────────
    override suspend fun chat(userMessage: String, model: String): Result<String> = withContext(Dispatchers.IO) {
        runCatching {
            val response = ollamaApi.chat(
                OllamaChatRequest(
                    model    = model,
                    messages = listOf(
                        OllamaMessage(role = "system", content = chatSystemPrompt),
                        OllamaMessage(role = "user",   content = userMessage)
                    ),
                    stream = false,
                    think  = false
                )
            )
            val content = (response.message?.content?.takeIf { it.isNotBlank() }
                ?: response.message?.thinking?.takeIf { it.isNotBlank() })
                ?: error("Empty response from AI")
            content.replace(Regex("<think>[\\s\\S]*?</think>"), "").trim()
        }
    }

    // ── Helper: Robust JSON parser with auto-repair for truncated output ─────
    private fun tryParseOrRepairItineraryJson(jsonStr: String): GeneratedItineraryDto {
        // Attempt 1: Direct standard parse
        try {
            val dto = gson.fromJson(jsonStr, GeneratedItineraryDto::class.java)
            if (dto != null && !dto.days.isNullOrEmpty()) return dto
        } catch (_: Exception) { }

        // Attempt 2: If truncated (EOFException / unclosed object/array), auto-close braces
        try {
            var candidate = jsonStr.trim()
            val quoteCount = candidate.count { it == '"' }
            if (quoteCount % 2 != 0) {
                val lastQuote = candidate.lastIndexOf('"')
                if (lastQuote != -1) {
                    candidate = candidate.substring(0, lastQuote)
                }
            }
            candidate = candidate.trim().trimEnd(',', ':', '{', '[')

            var openBraces = candidate.count { it == '{' } - candidate.count { it == '}' }
            var openBrackets = candidate.count { it == '[' } - candidate.count { it == ']' }

            val sb = StringBuilder(candidate)
            while (openBrackets > 0) {
                sb.append("]")
                openBrackets--
            }
            while (openBraces > 0) {
                sb.append("}")
                openBraces--
            }

            val repaired = sb.toString()
            val dto = gson.fromJson(repaired, GeneratedItineraryDto::class.java)
            if (dto != null && !dto.days.isNullOrEmpty()) return dto
        } catch (_: Exception) { }

        // Attempt 3: Slice to last complete day object
        try {
            val lastDayEnd = jsonStr.lastIndexOf("},")
            if (lastDayEnd != -1) {
                val candidate = jsonStr.substring(0, lastDayEnd + 1).trim() + "]}"
                val dto = gson.fromJson(candidate, GeneratedItineraryDto::class.java)
                if (dto != null && !dto.days.isNullOrEmpty()) return dto
            }
        } catch (_: Exception) { }

        // Final attempt: rethrow standard parse exception so user gets detailed syntax error
        return gson.fromJson(jsonStr, GeneratedItineraryDto::class.java)
            ?: error("AI model returned an empty or invalid JSON structure.")
    }
}


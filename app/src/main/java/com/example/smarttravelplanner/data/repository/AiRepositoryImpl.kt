package com.example.smarttravelplanner.data.repository

import com.example.smarttravelplanner.data.remote.api.OllamaApi
import com.example.smarttravelplanner.data.remote.dto.GeneratedItineraryDto
import com.example.smarttravelplanner.data.remote.dto.OllamaChatRequest
import com.example.smarttravelplanner.data.remote.dto.OllamaMessage
import com.google.gson.Gson
import com.google.gson.JsonSyntaxException
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
        - Each day should have 3-5 activities (morning, afternoon, evening).
        - estimated_cost_inr should be realistic for India.
        - Spread activities sensibly across the day with realistic timings.
        - The "theme" for each day should be a short engaging title.
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
        model: String
    ): Result<GeneratedItineraryDto> = runCatching {

        val interestText = interests.ifEmpty { listOf("general sightseeing") }.joinToString(", ")
        val styleDesc = when (travelStyle) {
            "relaxed" -> "Relaxed pace — 2-3 activities per day with plenty of rest time"
            "packed"  -> "Packed schedule — maximize experiences, 5+ activities per day"
            else      -> "Balanced pace — 3-4 activities per day with some downtime"
        }
        val dateContext = if (startDate.isNotBlank()) "Trip starts on $startDate." else ""
        val notesContext = if (notes.isNotBlank()) "Special instructions: $notes" else ""

        val userMessage = """
            Create a $days-day travel itinerary for $destination.
            Budget: ₹${budget.toInt()} total for the trip.
            Interests: $interestText
            Travel style: $styleDesc
            $dateContext
            $notesContext
            
            Return the complete itinerary as a JSON object matching the schema exactly.
        """.trimIndent()

        val response = ollamaApi.chat(
            OllamaChatRequest(
                model    = model,
                messages = listOf(
                    OllamaMessage(role = "system", content = itinerarySystemPrompt),
                    OllamaMessage(role = "user",   content = userMessage)
                ),
                stream = false,
                format = "json"
            )
        )

        val raw = response.message?.content
            ?: error("Empty response from AI model")

        // Strip markdown code fences if the model wraps the JSON anyway
        val cleaned = raw
            .removePrefix("```json").removePrefix("```")
            .removeSuffix("```")
            .trim()

        try {
            val dto = gson.fromJson(cleaned, GeneratedItineraryDto::class.java)
            if (dto.days.isEmpty()) error("AI returned empty itinerary — try again")
            dto
        } catch (e: JsonSyntaxException) {
            error("AI response could not be parsed. Please try again. (${e.message})")
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    override suspend fun chat(userMessage: String, model: String): Result<String> = runCatching {
        val response = ollamaApi.chat(
            OllamaChatRequest(
                model    = model,
                messages = listOf(
                    OllamaMessage(role = "system", content = chatSystemPrompt),
                    OllamaMessage(role = "user",   content = userMessage)
                ),
                stream = false
                // No format = "json" here — freeform chat response is fine
            )
        )
        response.message?.content ?: error("Empty response from AI")
    }
}


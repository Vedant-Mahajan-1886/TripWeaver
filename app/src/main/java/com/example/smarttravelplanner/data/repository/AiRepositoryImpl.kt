package com.example.smarttravelplanner.data.repository

import com.example.smarttravelplanner.data.remote.api.OllamaApi
import com.example.smarttravelplanner.data.remote.dto.OllamaChatRequest
import com.example.smarttravelplanner.data.remote.dto.OllamaMessage
import javax.inject.Inject

class AiRepositoryImpl @Inject constructor(
    private val ollamaApi: OllamaApi
) : AiRepository {

    private val systemPrompt = """
        You are an expert travel planner AI assistant. 
        When generating itineraries, format each day clearly with:
        - Morning, Afternoon, Evening activities
        - Specific places with brief descriptions
        - Estimated costs where applicable
        - Local tips and cultural notes
        Always be concise, practical, and personalized to the user's interests.
    """.trimIndent()

    override suspend fun generateItinerary(
        destination: String,
        days: Int,
        budget: Double,
        interests: List<String>,
        model: String
    ): Result<String> = runCatching {
        val interestText = if (interests.isNotEmpty()) interests.joinToString(", ") else "general sightseeing"
        val userMessage = """
            Create a detailed $days-day travel itinerary for $destination.
            Budget: $${budget.toInt()} USD total
            Interests: $interestText
            
            Format each day with Morning, Afternoon, and Evening sections.
            Include specific place names, estimated visit durations, and cost estimates.
        """.trimIndent()

        val response = ollamaApi.chat(
            OllamaChatRequest(
                model = model,
                messages = listOf(
                    OllamaMessage(role = "system", content = systemPrompt),
                    OllamaMessage(role = "user", content = userMessage)
                ),
                stream = false
            )
        )
        response.message?.content ?: error("Empty response from AI")
    }

    override suspend fun chat(userMessage: String, model: String): Result<String> = runCatching {
        val response = ollamaApi.chat(
            OllamaChatRequest(
                model = model,
                messages = listOf(
                    OllamaMessage(role = "system", content = systemPrompt),
                    OllamaMessage(role = "user", content = userMessage)
                ),
                stream = false
            )
        )
        response.message?.content ?: error("Empty response from AI")
    }
}

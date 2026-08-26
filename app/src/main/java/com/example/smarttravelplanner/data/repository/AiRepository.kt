package com.example.smarttravelplanner.data.repository

interface AiRepository {
    /**
     * Generates a travel itinerary as a formatted string.
     * @param destination Target travel destination
     * @param days Number of days
     * @param budget Budget in USD
     * @param interests List of user interests (e.g. ["food", "history"])
     * @param model Ollama model name (e.g. "llama3.2")
     */
    suspend fun generateItinerary(
        destination: String,
        days: Int,
        budget: Double,
        interests: List<String>,
        model: String
    ): Result<String>

    /**
     * Sends a freeform chat message to the AI (for the AI assistant screen).
     */
    suspend fun chat(userMessage: String, model: String): Result<String>
}

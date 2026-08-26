package com.example.smarttravelplanner.data.remote.api

import com.example.smarttravelplanner.data.remote.dto.OllamaChatRequest
import com.example.smarttravelplanner.data.remote.dto.OllamaChatResponse
import retrofit2.http.Body
import retrofit2.http.POST

interface OllamaApi {

    /**
     * Calls Ollama's OpenAI-compatible chat completions endpoint.
     * stream = false returns a single response body.
     */
    @POST("api/chat")
    suspend fun chat(@Body request: OllamaChatRequest): OllamaChatResponse
}

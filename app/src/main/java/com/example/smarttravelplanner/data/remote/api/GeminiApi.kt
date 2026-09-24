package com.example.smarttravelplanner.data.remote.api

import com.example.smarttravelplanner.data.remote.dto.GeminiGenerateRequest
import com.example.smarttravelplanner.data.remote.dto.GeminiGenerateResponse
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.POST
import retrofit2.http.Path
import retrofit2.http.Query

/**
 * Retrofit interface for Google Generative Language (Gemini) API.
 * Returns [Response] wrapper so callers can inspect HTTP error bodies on 4xx/5xx responses.
 */
interface GeminiApi {

    @POST("v1beta/models/{model}:generateContent")
    suspend fun generateContent(
        @Path("model") model: String,
        @Query("key") apiKey: String,
        @Body request: GeminiGenerateRequest
    ): Response<GeminiGenerateResponse>
}

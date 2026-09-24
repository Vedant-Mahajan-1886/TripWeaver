package com.example.smarttravelplanner.data.remote.dto

import com.google.gson.annotations.SerializedName

/**
 * Request body for Google Gemini API generateContent endpoint.
 */
data class GeminiGenerateRequest(
    @SerializedName("contents")
    val contents: List<GeminiContent>,

    @SerializedName("system_instruction")
    val systemInstruction: GeminiContent? = null,

    @SerializedName("generationConfig")
    val generationConfig: GeminiGenerationConfig? = null,

    @SerializedName("tools")
    val tools: List<GeminiTool>? = null
)

data class GeminiContent(
    @SerializedName("role")
    val role: String? = null,

    @SerializedName("parts")
    val parts: List<GeminiPart>
)

data class GeminiPart(
    @SerializedName("text")
    val text: String
)

data class GeminiGenerationConfig(
    @SerializedName("responseMimeType")
    val responseMimeType: String? = "application/json",

    @SerializedName("temperature")
    val temperature: Double? = 0.7
)

data class GeminiTool(
    @SerializedName("google_search")
    val googleSearch: GeminiGoogleSearch? = null
)

/**
 * Google Search Grounding tool configuration.
 * Empty body indicates standard search grounding.
 */
class GeminiGoogleSearch

/**
 * Response payload from Gemini API generateContent.
 */
data class GeminiGenerateResponse(
    @SerializedName("candidates")
    val candidates: List<GeminiCandidate>? = null
)

data class GeminiCandidate(
    @SerializedName("content")
    val content: GeminiContent? = null,

    @SerializedName("finishReason")
    val finishReason: String? = null
)

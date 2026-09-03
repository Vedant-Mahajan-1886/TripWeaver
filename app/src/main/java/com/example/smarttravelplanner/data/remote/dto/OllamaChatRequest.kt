package com.example.smarttravelplanner.data.remote.dto

import com.google.gson.annotations.SerializedName

data class OllamaChatRequest(
    @SerializedName("model")    val model: String,
    @SerializedName("messages") val messages: List<OllamaMessage>,
    @SerializedName("stream")   val stream: Boolean = false,
    /**
     * Set to "json" to enable Ollama's structured JSON output mode.
     * Requires the system prompt to describe the expected JSON schema.
     * Supported in llama3.2 and later models.
     */
    @SerializedName("format")   val format: String? = null
)

data class OllamaMessage(
    @SerializedName("role") val role: String,   // "system" | "user" | "assistant"
    @SerializedName("content") val content: String
)

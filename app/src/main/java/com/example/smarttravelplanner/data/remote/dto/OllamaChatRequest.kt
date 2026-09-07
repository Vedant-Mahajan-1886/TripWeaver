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
    @SerializedName("format")   val format: String? = null,
    /**
     * Set to false to disable reasoning/thinking mode on models that support it (e.g. Qwen 3.5).
     * Prevents reasoning tokens from consuming the response or breaking JSON formatting.
     */
    @SerializedName("think")    val think: Boolean? = false,
    /**
     * Runtime options passed to Ollama (num_ctx, num_predict, temperature, etc.).
     */
    @SerializedName("options")  val options: Map<String, Any>? = null
)

data class OllamaMessage(
    @SerializedName("role") val role: String,   // "system" | "user" | "assistant"
    @SerializedName("content") val content: String = "",
    @SerializedName("thinking") val thinking: String? = null
)

package com.example.smarttravelplanner.data.remote.dto

import com.google.gson.annotations.SerializedName

data class OllamaChatRequest(
    @SerializedName("model") val model: String,
    @SerializedName("messages") val messages: List<OllamaMessage>,
    @SerializedName("stream") val stream: Boolean = false
)

data class OllamaMessage(
    @SerializedName("role") val role: String,   // "system" | "user" | "assistant"
    @SerializedName("content") val content: String
)

package com.example.smarttravelplanner.data.remote.dto

import com.google.gson.annotations.SerializedName

data class OllamaChatResponse(
    @SerializedName("model") val model: String = "",
    @SerializedName("message") val message: OllamaMessage? = null,
    @SerializedName("done") val done: Boolean = false,
    @SerializedName("done_reason") val doneReason: String? = null
)

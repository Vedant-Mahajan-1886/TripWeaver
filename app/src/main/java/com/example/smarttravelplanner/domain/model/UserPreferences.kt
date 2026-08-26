package com.example.smarttravelplanner.domain.model

data class UserPreferences(
    val name: String = "",
    val currency: String = "USD",
    val preferredActivities: List<String> = emptyList(),
    val defaultBudget: Double = 1000.0,
    val ollamaModel: String = "llama3.2",
    // For physical device: replace with your laptop's LAN IP
    val ollamaBaseUrl: String = "http://10.0.2.2:11434/"
)

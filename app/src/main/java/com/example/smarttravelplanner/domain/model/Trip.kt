package com.example.smarttravelplanner.domain.model

import java.time.LocalDate

data class Trip(
    val id: Long = 0,
    val title: String,
    val destination: String,
    val startDate: LocalDate,
    val endDate: LocalDate,
    val budget: Double,
    val interests: List<String> = emptyList(),
    val notes: String = "",
    val coverImageUrl: String = "",
    val createdAt: Long = System.currentTimeMillis()
)

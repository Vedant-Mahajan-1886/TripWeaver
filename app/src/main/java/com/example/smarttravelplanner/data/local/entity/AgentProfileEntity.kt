package com.example.smarttravelplanner.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Public details of a travel-agent account. One row per agent, keyed by the
 * agent's (lower-case) email, which matches users.email.
 */
@Entity(tableName = "agent_profiles")
data class AgentProfileEntity(
    @PrimaryKey val email: String,
    val agencyName: String,
    val city: String,
    val specialities: List<String> = emptyList(),
    val updatedAt: Long = System.currentTimeMillis()
)

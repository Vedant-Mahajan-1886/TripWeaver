package com.example.smarttravelplanner.data.local.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "users",
    indices = [Index(value = ["email"], unique = true)]
)
data class UserEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val email: String,
    val fullName: String,
    val passwordHash: String,
    val salt: String,
    val isEmailVerified: Boolean = true,
    val isGoogleUser: Boolean = false,
    val phoneNumber: String = "",
    val profilePictureUrl: String? = null,
    val gender: String = "",
    val homeLocation: String = "",
    val dateOfBirth: String = "",
    val maritalStatus: String = "",
    val createdAt: Long = System.currentTimeMillis(),
    /** "TRAVELLER" or "AGENT" — see UserRole. Existing users are travellers. */
    val role: String = "TRAVELLER"
)

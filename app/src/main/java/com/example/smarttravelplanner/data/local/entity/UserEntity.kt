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
    val createdAt: Long = System.currentTimeMillis()
)

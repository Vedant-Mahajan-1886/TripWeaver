package com.example.smarttravelplanner.data.repository

import kotlinx.coroutines.flow.Flow

interface AuthRepository {
    /** Reactive flow emitting whether a user is currently authenticated */
    val isLoggedIn: Flow<Boolean>

    /** Current logged-in user's email */
    val currentUserEmail: Flow<String?>

    /** Validates credentials and logs in the user */
    suspend fun login(email: String, password: String): Result<Unit>

    /** Sends a 6-digit verification OTP to the specified email */
    suspend fun sendRegistrationOtp(email: String): Result<Unit>

    /** Verifies OTP, hashes password, saves user in Room, and starts session */
    suspend fun verifyOtpAndCreateAccount(
        fullName: String,
        email: String,
        password: String,
        otp: String
    ): Result<Unit>

    /** Google Sign-In helper */
    suspend fun loginWithGoogle(
        email: String = "google.traveler@gmail.com",
        fullName: String = "Google Traveler",
        profilePictureUrl: String? = null
    ): Result<Unit>

    /** Sends a 6-digit password reset OTP to the registered email */
    suspend fun sendPasswordResetOtp(email: String): Result<Unit>

    /** Verifies OTP, updates password with salted hash, and signs user in */
    suspend fun resetPassword(email: String, otp: String, newPassword: String): Result<Unit>

    /** Clears session and logs out */
    suspend fun logout(): Result<Unit>
}

package com.example.smarttravelplanner.data.repository

import com.example.smarttravelplanner.domain.model.AuthResult
import kotlinx.coroutines.flow.Flow

interface AuthRepository {
    /** Reactive flow emitting whether a user is currently authenticated */
    val isLoggedIn: Flow<Boolean>

    /** Current logged-in user's email */
    val currentUserEmail: Flow<String?>

    /** Validates credentials and logs in the returning user */
    suspend fun login(email: String, password: String): Result<AuthResult>

    /** Sends a 6-digit verification OTP to the specified email */
    suspend fun sendRegistrationOtp(email: String): Result<Unit>

    /** Verifies OTP, hashes password, saves user in Room, initializes onboarding, and starts session */
    suspend fun verifyOtpAndCreateAccount(
        fullName: String,
        email: String,
        password: String,
        otp: String
    ): Result<AuthResult>

    /** Google Sign-In helper (distinguishes new accounts from returning users) */
    suspend fun loginWithGoogle(
        email: String = "google.traveler@gmail.com",
        fullName: String = "Google Traveler",
        profilePictureUrl: String? = null
    ): Result<AuthResult>

    /**
     * After a Google login: sign in to Firebase with the Google ID token so the account
     * also exists in the cloud. Never fails the login (cloud is a bonus here).
     */
    suspend fun connectGoogleToCloud(email: String, idToken: String) {}

    /** Sends a 6-digit password reset OTP to the registered email */
    suspend fun sendPasswordResetOtp(email: String): Result<Unit>

    /** Verifies OTP, updates password with salted hash, and signs user in */
    suspend fun resetPassword(email: String, otp: String, newPassword: String): Result<AuthResult>

    /**
     * True when cloud sync is on: "Forgot password" emails a reset link (Firebase)
     * instead of a 6-digit code.
     */
    val passwordResetByEmailLink: Boolean get() = false

    /** Clears session and logs out */
    suspend fun logout(): Result<Unit>
}

package com.example.smarttravelplanner.data.remote.service

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.os.Build
import android.util.Log
import androidx.core.app.NotificationCompat
import com.example.smarttravelplanner.R
import com.example.smarttravelplanner.util.SecurityUtils
import dagger.hilt.android.qualifiers.ApplicationContext
import java.util.concurrent.ConcurrentHashMap
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class EmailVerificationService @Inject constructor(
    @param:ApplicationContext private val context: Context
) {
    private val channelId = "auth_verification_channel"
    private val notificationManager =
        context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

    data class OtpSession(
        val email: String,
        val code: String,
        val expiresAt: Long,
        var attemptsLeft: Int = 3
    )

    // In-memory store of active OTP sessions keyed by lowercase email
    private val activeSessions = ConcurrentHashMap<String, OtpSession>()

    init {
        createNotificationChannel()
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                channelId,
                "Email Verification Codes",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Sends one-time verification codes for account creation"
            }
            notificationManager.createNotificationChannel(channel)
        }
    }

    /**
     * Generates a 6-digit OTP valid for 5 minutes and sends it via Gmail SMTP
     * directly to the recipient's email inbox.
     */
    suspend fun sendVerificationOtp(email: String): Result<String> = runCatching {
        val normalizedEmail = email.trim().lowercase()
        val code = SecurityUtils.generateOtp()
        val expiresAt = System.currentTimeMillis() + 5 * 60 * 1000 // 5 minutes

        activeSessions[normalizedEmail] = OtpSession(
            email = normalizedEmail,
            code = code,
            expiresAt = expiresAt
        )

        Log.d("AuthOTP", "Verification code for $normalizedEmail: $code")

        // 1. Send actual email via Gmail SMTP
        if (EmailConfig.isConfigured) {
            GmailSmtpSender.sendOtpEmail(
                senderEmail = EmailConfig.SENDER_EMAIL,
                senderAppPassword = EmailConfig.SENDER_APP_PASSWORD,
                recipientEmail = normalizedEmail,
                otp = code
            ).getOrThrow()
        } else {
            throw IllegalStateException(
                "Please configure your Gmail address and 16-character Google App Password in EmailConfig.kt to send real OTP emails."
            )
        }

        // 2. Also post notification for device convenience
        runCatching {
            val notification = NotificationCompat.Builder(context, channelId)
                .setSmallIcon(R.drawable.ic_launcher_foreground)
                .setContentTitle("SmartTravelPlanner Verification")
                .setContentText("Your verification code is: $code")
                .setStyle(NotificationCompat.BigTextStyle().bigText("Your 6-digit verification code is: $code. This code will expire in 5 minutes."))
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setAutoCancel(true)
                .build()
            notificationManager.notify(normalizedEmail.hashCode(), notification)
        }

        code
    }

    /**
     * Sends a 6-digit password reset OTP to the user's email via Gmail SMTP.
     */
    suspend fun sendPasswordResetOtp(email: String): Result<String> = runCatching {
        val normalizedEmail = email.trim().lowercase()
        val code = SecurityUtils.generateOtp()
        val expiresAt = System.currentTimeMillis() + 5 * 60 * 1000 // 5 minutes

        activeSessions[normalizedEmail] = OtpSession(
            email = normalizedEmail,
            code = code,
            expiresAt = expiresAt
        )

        Log.d("AuthOTP", "Password reset code for $normalizedEmail: $code")

        if (EmailConfig.isConfigured) {
            GmailSmtpSender.sendOtpEmail(
                senderEmail = EmailConfig.SENDER_EMAIL,
                senderAppPassword = EmailConfig.SENDER_APP_PASSWORD,
                recipientEmail = normalizedEmail,
                otp = code,
                isPasswordReset = true
            ).getOrThrow()
        } else {
            throw IllegalStateException(
                "Please configure your Gmail address and 16-character Google App Password in local.properties"
            )
        }

        runCatching {
            val notification = NotificationCompat.Builder(context, channelId)
                .setSmallIcon(R.drawable.ic_launcher_foreground)
                .setContentTitle("Password Reset Code")
                .setContentText("Your password reset code is: $code")
                .setStyle(NotificationCompat.BigTextStyle().bigText("Your 6-digit password reset code is: $code. This code will expire in 5 minutes."))
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setAutoCancel(true)
                .build()
            notificationManager.notify(normalizedEmail.hashCode(), notification)
        }

        code
    }

    /**
     * Verifies the entered 6-digit code against the active session for the email.
     */
    fun verifyOtp(email: String, enteredCode: String): Result<Boolean> = runCatching {
        val normalizedEmail = email.trim().lowercase()
        val session = activeSessions[normalizedEmail]
            ?: throw IllegalStateException("No verification code sent to this email. Please request a new code.")

        if (System.currentTimeMillis() > session.expiresAt) {
            activeSessions.remove(normalizedEmail)
            throw IllegalStateException("Verification code has expired. Please request a new code.")
        }

        if (session.attemptsLeft <= 0) {
            activeSessions.remove(normalizedEmail)
            throw IllegalStateException("Too many incorrect attempts. Please request a new code.")
        }

        if (session.code != enteredCode.trim()) {
            session.attemptsLeft -= 1
            throw IllegalArgumentException("Invalid verification code. ${session.attemptsLeft} attempts remaining.")
        }

        // Successfully verified
        activeSessions.remove(normalizedEmail)
        true
    }

    /**
     * Clears any active OTP session for the email.
     */
    fun clearSession(email: String) {
        activeSessions.remove(email.trim().lowercase())
    }
}

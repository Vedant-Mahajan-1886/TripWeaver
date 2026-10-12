package com.example.smarttravelplanner.data.remote.service

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import com.example.smarttravelplanner.R
import com.example.smarttravelplanner.util.SecurityUtils
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

interface EmailVerificationService {
    suspend fun sendVerificationOtp(email: String): Result<String>
    suspend fun sendPasswordResetOtp(email: String): Result<String>
    fun verifyOtp(email: String, enteredCode: String): Result<Boolean>
    fun clearSession(email: String)
}

@Singleton
class EmailVerificationServiceImpl @Inject constructor(
    @param:ApplicationContext private val context: Context
) : EmailVerificationService {
    private val channelId = "auth_verification_channel"
    private val notificationManager =
        context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
    private val prefs = context.getSharedPreferences("otp_sessions_prefs", Context.MODE_PRIVATE)

    data class OtpSession(
        val email: String,
        val code: String,
        val expiresAt: Long,
        var attemptsLeft: Int = 3
    )

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

    private fun saveSession(session: OtpSession) {
        prefs.edit()
            .putString("session_${session.email}_code", session.code)
            .putLong("session_${session.email}_expires", session.expiresAt)
            .putInt("session_${session.email}_attempts", session.attemptsLeft)
            .apply()
    }

    private fun getSession(email: String): OtpSession? {
        val code = prefs.getString("session_${email}_code", null) ?: return null
        val expiresAt = prefs.getLong("session_${email}_expires", 0L)
        val attempts = prefs.getInt("session_${email}_attempts", 3)
        return OtpSession(
            email = email,
            code = code,
            expiresAt = expiresAt,
            attemptsLeft = attempts
        )
    }

    private fun postNotificationIfPermitted(
        title: String,
        text: String,
        bigText: String,
        notificationId: Int
    ) {
        runCatching {
            val hasPermission = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                ContextCompat.checkSelfPermission(
                    context,
                    Manifest.permission.POST_NOTIFICATIONS
                ) == PackageManager.PERMISSION_GRANTED
            } else {
                notificationManager.areNotificationsEnabled()
            }

            if (hasPermission && notificationManager.areNotificationsEnabled()) {
                val notification = NotificationCompat.Builder(context, channelId)
                    .setSmallIcon(R.drawable.ic_launcher_foreground)
                    .setContentTitle(title)
                    .setContentText(text)
                    .setStyle(NotificationCompat.BigTextStyle().bigText(bigText))
                    .setPriority(NotificationCompat.PRIORITY_HIGH)
                    .setAutoCancel(true)
                    .build()
                notificationManager.notify(notificationId, notification)
            }
        }.onFailure { e ->
            Log.w("AuthOTP", "Could not post verification notification: ${e.message}")
        }
    }

    /**
     * Generates a 6-digit OTP valid for 5 minutes and sends it via Gmail SMTP
     * directly to the recipient's email inbox. Persists session to SharedPreferences.
     */
    override suspend fun sendVerificationOtp(email: String): Result<String> = runCatching {
        val normalizedEmail = email.trim().lowercase()
        val code = SecurityUtils.generateOtp()
        val expiresAt = System.currentTimeMillis() + 5 * 60 * 1000 // 5 minutes

        saveSession(
            OtpSession(
                email = normalizedEmail,
                code = code,
                expiresAt = expiresAt,
                attemptsLeft = 3
            )
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
                "Please configure your Gmail address and 16-character Google App Password in local.properties to send OTP emails."
            )
        }

        // 2. Also post notification for device convenience if permitted
        postNotificationIfPermitted(
            title = "SmartTravelPlanner Verification",
            text = "Your verification code is: $code",
            bigText = "Your 6-digit verification code is: $code. This code will expire in 5 minutes.",
            notificationId = normalizedEmail.hashCode()
        )

        code
    }

    /**
     * Sends a 6-digit password reset OTP to the user's email via Gmail SMTP.
     */
    override suspend fun sendPasswordResetOtp(email: String): Result<String> = runCatching {
        val normalizedEmail = email.trim().lowercase()
        val code = SecurityUtils.generateOtp()
        val expiresAt = System.currentTimeMillis() + 5 * 60 * 1000 // 5 minutes

        saveSession(
            OtpSession(
                email = normalizedEmail,
                code = code,
                expiresAt = expiresAt,
                attemptsLeft = 3
            )
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

        postNotificationIfPermitted(
            title = "Password Reset Code",
            text = "Your password reset code is: $code",
            bigText = "Your 6-digit password reset code is: $code. This code will expire in 5 minutes.",
            notificationId = normalizedEmail.hashCode()
        )

        code
    }

    /**
     * Verifies the entered 6-digit code against the active session for the email.
     */
    override fun verifyOtp(email: String, enteredCode: String): Result<Boolean> = runCatching {
        val normalizedEmail = email.trim().lowercase()
        val session = getSession(normalizedEmail)
            ?: throw IllegalStateException("No verification code sent to this email. Please request a new code.")

        if (System.currentTimeMillis() > session.expiresAt) {
            clearSession(normalizedEmail)
            throw IllegalStateException("Verification code has expired. Please request a new code.")
        }

        if (session.attemptsLeft <= 0) {
            clearSession(normalizedEmail)
            throw IllegalStateException("Too many incorrect attempts. Please request a new code.")
        }

        if (session.code != enteredCode.trim()) {
            session.attemptsLeft -= 1
            saveSession(session)
            throw IllegalArgumentException("Invalid verification code. ${session.attemptsLeft} attempts remaining.")
        }

        // Successfully verified
        clearSession(normalizedEmail)
        true
    }

    /**
     * Clears any active OTP session for the email.
     */
    override fun clearSession(email: String) {
        val normalized = email.trim().lowercase()
        prefs.edit()
            .remove("session_${normalized}_code")
            .remove("session_${normalized}_expires")
            .remove("session_${normalized}_attempts")
            .apply()
    }
}

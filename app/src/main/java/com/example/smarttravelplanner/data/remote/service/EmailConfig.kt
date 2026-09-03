package com.example.smarttravelplanner.data.remote.service

import com.example.smarttravelplanner.BuildConfig

/**
 * Configuration for sending real verification OTPs via Gmail SMTP.
 *
 * SECURE STORAGE IN local.properties:
 * Add the following two lines to your project root `local.properties` file:
 *
 * SMTP_SENDER_EMAIL=your_email@gmail.com
 * SMTP_APP_PASSWORD=your_16_char_google_app_password
 *
 * `local.properties` is ignored by Git, ensuring your credentials are NEVER committed or leaked.
 */
object EmailConfig {
    /**
     * The Gmail address used to send verification emails.
     * Automatically populated from `SMTP_SENDER_EMAIL` in `local.properties`.
     */
    var SENDER_EMAIL: String = BuildConfig.SMTP_SENDER_EMAIL

    /**
     * The 16-character Google App Password.
     * Automatically populated from `SMTP_APP_PASSWORD` in `local.properties`.
     */
    var SENDER_APP_PASSWORD: String = BuildConfig.SMTP_APP_PASSWORD

    val isConfigured: Boolean
        get() = SENDER_EMAIL.isNotBlank() &&
                SENDER_APP_PASSWORD.isNotBlank() &&
                !SENDER_EMAIL.contains("your-gmail") &&
                !SENDER_APP_PASSWORD.contains("your-app-password")
}

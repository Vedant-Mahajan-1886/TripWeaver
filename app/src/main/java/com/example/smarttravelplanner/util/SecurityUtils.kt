package com.example.smarttravelplanner.util

import java.security.MessageDigest
import java.security.SecureRandom

object SecurityUtils {

    private val secureRandom = SecureRandom()

    /**
     * Generates a cryptographically secure random hexadecimal salt.
     */
    fun generateSalt(): String {
        val saltBytes = ByteArray(16)
        secureRandom.nextBytes(saltBytes)
        return saltBytes.joinToString("") { "%02x".format(it) }
    }

    /**
     * Hashes a password with a unique salt using SHA-256.
     */
    fun hashPassword(password: String, salt: String): String {
        val md = MessageDigest.getInstance("SHA-256")
        md.update(salt.toByteArray(Charsets.UTF_8))
        val hashedBytes = md.digest(password.toByteArray(Charsets.UTF_8))
        return hashedBytes.joinToString("") { "%02x".format(it) }
    }

    /**
     * Verifies if an entered password matches the stored salted hash.
     */
    fun verifyPassword(password: String, salt: String, expectedHash: String): Boolean {
        val computedHash = hashPassword(password, salt)
        return computedHash.equals(expectedHash, ignoreCase = true)
    }

    /**
     * Generates a 6-digit numeric OTP (e.g. "482910").
     */
    fun generateOtp(): String {
        val code = 100000 + secureRandom.nextInt(900000)
        return code.toString()
    }
}

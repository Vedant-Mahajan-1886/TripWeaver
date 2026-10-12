package com.example.smarttravelplanner.domain.model

/**
 * Result of an authentication operation distinguishing newly registered accounts
 * from returning-user logins.
 *
 * @param email Normalized email of the authenticated account.
 * @param isNewAccount True if this authentication operation created a brand-new account
 *                     (eligible for onboarding); false if it authenticated an existing account.
 */
data class AuthResult(
    val email: String,
    val isNewAccount: Boolean
)

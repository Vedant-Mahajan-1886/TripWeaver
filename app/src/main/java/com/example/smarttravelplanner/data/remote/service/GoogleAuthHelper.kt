package com.example.smarttravelplanner.data.remote.service

import android.content.Context
import android.util.Log
import androidx.credentials.CredentialManager
import androidx.credentials.CustomCredential
import androidx.credentials.GetCredentialRequest
import com.example.smarttravelplanner.BuildConfig
import com.google.android.libraries.identity.googleid.GetGoogleIdOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential

object GoogleAuthHelper {

    private const val TAG = "GoogleAuthHelper"

    val isConfigured: Boolean
        get() = BuildConfig.GOOGLE_WEB_CLIENT_ID.isNotBlank()

    data class GoogleUser(
        val email: String,
        val displayName: String,
        val profilePictureUri: String? = null
    )

    /**
     * Launches the official Android Credential Manager Google Account bottom sheet.
     * Extracts and returns the user's verified Google email and display name.
     */
    suspend fun signInWithGoogle(context: Context): Result<GoogleUser> = runCatching {
        if (!isConfigured) {
            throw IllegalStateException("GOOGLE_WEB_CLIENT_ID is not configured in local.properties")
        }

        val credentialManager = CredentialManager.create(context)

        val googleIdOption = GetGoogleIdOption.Builder()
            .setFilterByAuthorizedAccounts(false)
            .setServerClientId(BuildConfig.GOOGLE_WEB_CLIENT_ID)
            .setAutoSelectEnabled(false)
            .build()

        val request = GetCredentialRequest.Builder()
            .addCredentialOption(googleIdOption)
            .build()

        val response = credentialManager.getCredential(
            request = request,
            context = context
        )

        val credential = response.credential
        if (credential is CustomCredential && credential.type == GoogleIdTokenCredential.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL) {
            val googleIdToken = GoogleIdTokenCredential.createFrom(credential.data)
            GoogleUser(
                email = googleIdToken.id,
                displayName = googleIdToken.displayName ?: googleIdToken.id.substringBefore("@"),
                profilePictureUri = googleIdToken.profilePictureUri?.toString()
            )
        } else {
            throw IllegalStateException("Unexpected credential type: ${credential.type}")
        }
    }
}

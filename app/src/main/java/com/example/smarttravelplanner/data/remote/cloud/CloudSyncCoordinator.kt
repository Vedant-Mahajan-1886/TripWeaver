package com.example.smarttravelplanner.data.remote.cloud

import com.example.smarttravelplanner.data.repository.AuthRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Starts a full sync whenever the app's logged-in user and the Firebase user are the same
 * person: after login (email, Google or agent) and every time the app starts.
 */
@Singleton
class CloudSyncCoordinator @Inject constructor(
    private val authRepository: AuthRepository,
    private val cloudAuth: CloudAuth,
    private val tripSync: TripSync
) {
    fun start(scope: CoroutineScope) {
        if (!cloudAuth.isEnabled) return
        scope.launch {
            combine(authRepository.currentUserEmail, cloudAuth.signedInEmailFlow) { appEmail, cloudEmail ->
                val email = appEmail?.trim()?.lowercase()
                if (!email.isNullOrEmpty() && email == cloudEmail) email else null
            }
                .distinctUntilChanged()
                .filterNotNull()
                .collect { email -> tripSync.syncAll(email) }
        }
    }
}

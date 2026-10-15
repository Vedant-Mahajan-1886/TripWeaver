package com.example.smarttravelplanner.data.remote.cloud

import com.google.firebase.FirebaseNetworkException
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseAuthInvalidCredentialsException
import com.google.firebase.auth.FirebaseAuthInvalidUserException
import com.google.firebase.auth.FirebaseAuthUserCollisionException
import com.google.firebase.auth.UserProfileChangeRequest
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import javax.inject.Singleton

/** Outcome of a Firebase Auth call, in words the login code can act on. */
sealed interface CloudAuthResult {
    data class Success(val displayName: String?) : CloudAuthResult
    /** Wrong password, or no cloud account (Firebase doesn't say which, to protect privacy). */
    data object InvalidCredentials : CloudAuthResult
    /** createAccount: a cloud account with this email already exists. */
    data object AlreadyExists : CloudAuthResult
    data object Offline : CloudAuthResult
    /** Cloud sync is off for this build (no google-services.json). */
    data object Disabled : CloudAuthResult
    data class Failed(val message: String) : CloudAuthResult
}

/** Cloud accounts used by the login code. [NoCloudAuth] = cloud off (also used in unit tests). */
interface CloudAuth {
    val isEnabled: Boolean
    /** Email of the Firebase user on this phone (null if signed out or cloud off). */
    val signedInEmail: String?
    suspend fun signIn(email: String, password: String): CloudAuthResult
    suspend fun createAccount(email: String, password: String, fullName: String): CloudAuthResult
    /** Firebase emails a link to choose a new password. Silent if no such account (privacy). */
    suspend fun sendPasswordResetEmail(email: String): CloudAuthResult
    fun signOut()
}

/** Cloud switched off: every call says so, and the app uses only local accounts. */
object NoCloudAuth : CloudAuth {
    override val isEnabled = false
    override val signedInEmail: String? = null
    override suspend fun signIn(email: String, password: String) = CloudAuthResult.Disabled
    override suspend fun createAccount(email: String, password: String, fullName: String) = CloudAuthResult.Disabled
    override suspend fun sendPasswordResetEmail(email: String) = CloudAuthResult.Disabled
    override fun signOut() = Unit
}

/**
 * Firebase Auth (email + password). The app's own login (Room + OTP) stays in charge;
 * this keeps a matching Firebase account so other phones can log in and Firestore
 * security rules know who the user is.
 */
@Singleton
class FirebaseCloudAuth @Inject constructor(
    private val cloudStatus: CloudStatus,
    private val firebaseAuth: dagger.Lazy<FirebaseAuth>
) : CloudAuth {
    private val auth: FirebaseAuth get() = firebaseAuth.get()

    override val isEnabled: Boolean get() = cloudStatus.isEnabled

    override val signedInEmail: String?
        get() = if (isEnabled) auth.currentUser?.email?.lowercase() else null

    override suspend fun signIn(email: String, password: String): CloudAuthResult = call {
        val user = auth.signInWithEmailAndPassword(email, password).await().user
        CloudAuthResult.Success(user?.displayName)
    }

    override suspend fun createAccount(email: String, password: String, fullName: String): CloudAuthResult = call {
        val user = auth.createUserWithEmailAndPassword(email, password).await().user
        user?.updateProfile(UserProfileChangeRequest.Builder().setDisplayName(fullName).build())?.await()
        CloudAuthResult.Success(fullName)
    }

    override suspend fun sendPasswordResetEmail(email: String): CloudAuthResult = call {
        auth.sendPasswordResetEmail(email).await()
        CloudAuthResult.Success(null)
    }

    override fun signOut() {
        if (isEnabled) auth.signOut()
    }

    private suspend fun call(block: suspend () -> CloudAuthResult): CloudAuthResult {
        if (!isEnabled) return CloudAuthResult.Disabled
        return try {
            block()
        } catch (e: FirebaseAuthUserCollisionException) {
            CloudAuthResult.AlreadyExists
        } catch (e: FirebaseAuthInvalidUserException) {
            CloudAuthResult.InvalidCredentials
        } catch (e: FirebaseAuthInvalidCredentialsException) {
            CloudAuthResult.InvalidCredentials
        } catch (e: FirebaseNetworkException) {
            CloudAuthResult.Offline
        } catch (e: kotlinx.coroutines.CancellationException) {
            throw e
        } catch (e: Exception) {
            CloudAuthResult.Failed(e.message ?: "Cloud sign-in failed.")
        }
    }
}

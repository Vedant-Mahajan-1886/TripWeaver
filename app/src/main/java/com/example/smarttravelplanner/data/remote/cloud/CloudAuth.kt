package com.example.smarttravelplanner.data.remote.cloud

import com.google.firebase.FirebaseNetworkException
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseAuthInvalidCredentialsException
import com.google.firebase.auth.FirebaseAuthInvalidUserException
import com.google.firebase.auth.FirebaseAuthUserCollisionException
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.auth.UserProfileChangeRequest
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.flowOf
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
    /** Firebase user id (used as the owner of cloud documents). */
    val signedInUid: String? get() = null
    /** Emits the signed-in Firebase email whenever it changes (sign-in, sign-out). */
    val signedInEmailFlow: Flow<String?> get() = flowOf(null)
    suspend fun signIn(email: String, password: String): CloudAuthResult
    suspend fun createAccount(email: String, password: String, fullName: String): CloudAuthResult
    /** Firebase emails a link to choose a new password. Silent if no such account (privacy). */
    suspend fun sendPasswordResetEmail(email: String): CloudAuthResult
    /** Signs in to Firebase with a Google ID token (creates the cloud account the first time). */
    suspend fun signInWithGoogle(idToken: String): CloudAuthResult = CloudAuthResult.Disabled
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

    override val signedInUid: String?
        get() = if (isEnabled) auth.currentUser?.uid else null

    override val signedInEmailFlow: Flow<String?>
        get() = if (!isEnabled) flowOf(null) else callbackFlow {
            val listener = FirebaseAuth.AuthStateListener { trySend(it.currentUser?.email?.lowercase()) }
            auth.addAuthStateListener(listener) // also sends the current state right away
            awaitClose { auth.removeAuthStateListener(listener) }
        }

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

    override suspend fun signInWithGoogle(idToken: String): CloudAuthResult = call {
        val credential = GoogleAuthProvider.getCredential(idToken, null)
        val user = auth.signInWithCredential(credential).await().user
        CloudAuthResult.Success(user?.displayName)
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

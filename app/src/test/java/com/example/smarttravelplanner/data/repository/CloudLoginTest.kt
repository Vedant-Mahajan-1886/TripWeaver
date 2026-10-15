package com.example.smarttravelplanner.data.repository

import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import com.example.smarttravelplanner.data.local.dao.UserDao
import com.example.smarttravelplanner.data.local.entity.UserEntity
import com.example.smarttravelplanner.data.remote.cloud.CloudAuth
import com.example.smarttravelplanner.data.remote.cloud.CloudAuthResult
import com.example.smarttravelplanner.data.remote.service.EmailVerificationService
import com.example.smarttravelplanner.util.SecurityUtils
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder

/** Login / sign-up / reset when cloud accounts (Firebase) are switched on. */
class CloudLoginTest {

    @get:Rule
    val tempFolder = TemporaryFolder()

    private lateinit var scope: CoroutineScope
    private lateinit var userDao: FakeUserDao
    private lateinit var cloud: FakeCloudAuth
    private lateinit var repo: AuthRepositoryImpl

    /** Pretend Firebase: a map of email -> password. */
    private class FakeCloudAuth : CloudAuth {
        val accounts = mutableMapOf<String, String>()
        var offline = false
        var resetEmailsSent = 0
        override val isEnabled = true
        override var signedInEmail: String? = null

        override suspend fun signIn(email: String, password: String): CloudAuthResult = when {
            offline -> CloudAuthResult.Offline
            accounts[email] == password -> { signedInEmail = email; CloudAuthResult.Success("Cloud Name") }
            else -> CloudAuthResult.InvalidCredentials
        }

        override suspend fun createAccount(email: String, password: String, fullName: String): CloudAuthResult = when {
            offline -> CloudAuthResult.Offline
            email in accounts -> CloudAuthResult.AlreadyExists
            else -> { accounts[email] = password; signedInEmail = email; CloudAuthResult.Success(fullName) }
        }

        override suspend fun sendPasswordResetEmail(email: String): CloudAuthResult {
            resetEmailsSent++
            return if (offline) CloudAuthResult.Offline else CloudAuthResult.Success(null)
        }

        override fun signOut() { signedInEmail = null }
    }

    private class FakeEmailVerificationService : EmailVerificationService {
        var lastSentOtp: String? = null

        override suspend fun sendVerificationOtp(email: String): Result<String> {
            lastSentOtp = "123456"
            return Result.success("123456")
        }

        override fun verifyOtp(email: String, enteredCode: String): Result<Boolean> {
            return if (enteredCode == "123456") Result.success(true)
            else Result.failure(IllegalArgumentException("Invalid verification code."))
        }

        override suspend fun sendPasswordResetOtp(email: String): Result<String> {
            lastSentOtp = "123456"
            return Result.success("123456")
        }

        override fun clearSession(email: String) {
            lastSentOtp = null
        }
    }

    private class FakeUserDao : UserDao {
        val users = mutableMapOf<String, UserEntity>()
        private var idCounter = 1L

        override suspend fun getUserByEmail(email: String): UserEntity? =
            users[email.trim().lowercase()]

        override suspend fun getUserById(userId: Long): UserEntity? =
            users.values.firstOrNull { it.id == userId }

        override suspend fun emailExists(email: String): Int =
            if (users.containsKey(email.trim().lowercase())) 1 else 0

        override suspend fun insertUser(user: UserEntity): Long {
            val normalized = user.email.trim().lowercase()
            if (users.containsKey(normalized)) throw IllegalStateException("User already exists")
            val id = idCounter++
            val saved = user.copy(id = id, email = normalized)
            users[normalized] = saved
            return id
        }

        override suspend fun updateUser(user: UserEntity) {
            users[user.email.trim().lowercase()] = user
        }

        override suspend fun updatePassword(email: String, passwordHash: String, salt: String) {
            val current = users[email.trim().lowercase()] ?: return
            users[email.trim().lowercase()] = current.copy(passwordHash = passwordHash, salt = salt)
        }

        override suspend fun updateProfileDetails(
            email: String,
            fullName: String,
            phoneNumber: String,
            profilePictureUrl: String?,
            gender: String,
            homeLocation: String,
            dateOfBirth: String,
            maritalStatus: String
        ) {
            val current = users[email.trim().lowercase()] ?: return
            users[email.trim().lowercase()] = current.copy(
                fullName = fullName,
                phoneNumber = phoneNumber,
                profilePictureUrl = profilePictureUrl ?: current.profilePictureUrl,
                gender = gender,
                homeLocation = homeLocation,
                dateOfBirth = dateOfBirth,
                maritalStatus = maritalStatus
            )
        }

        override suspend fun updateProfilePicture(email: String, profilePictureUrl: String?) {
            val current = users[email.trim().lowercase()] ?: return
            users[email.trim().lowercase()] = current.copy(profilePictureUrl = profilePictureUrl)
        }

        override fun observeRole(email: String): Flow<String?> =
            flowOf(users[email.trim().lowercase()]?.role)

        override suspend fun updateRole(email: String, role: String) {
            val current = users[email.trim().lowercase()] ?: return
            users[email.trim().lowercase()] = current.copy(role = role)
        }

        override suspend fun deleteUser(userId: Long) {
            users.entries.removeIf { it.value.id == userId }
        }
    }

    @Before
    fun setUp() {
        scope = CoroutineScope(Dispatchers.IO + SupervisorJob())
        val dataStore = PreferenceDataStoreFactory.create(
            scope = scope,
            produceFile = { tempFolder.newFile("test_cloud_auth.preferences_pb") }
        )
        userDao = FakeUserDao()
        cloud = FakeCloudAuth()
        repo = AuthRepositoryImpl(
            userDao = userDao,
            emailVerificationService = FakeEmailVerificationService(),
            userPreferencesRepository = UserPreferencesRepositoryImpl(dataStore),
            dataStore = dataStore,
            cloudAuth = cloud
        )
    }

    @After
    fun tearDown() {
        scope.cancel()
    }

    private suspend fun addLocalUser(email: String, password: String) {
        val salt = SecurityUtils.generateSalt()
        userDao.insertUser(
            UserEntity(email = email, fullName = "Local User", passwordHash = SecurityUtils.hashPassword(password, salt),
                salt = salt, isEmailVerified = true)
        )
    }

    @Test
    fun accountMadeOnAnotherPhone_logsInAndIsCopiedLocally() = runBlocking {
        cloud.accounts["riya@x.com"] = "secret1"
        val result = repo.login("riya@x.com", "secret1")
        assertTrue(result.isSuccess)
        assertNotNull(userDao.getUserByEmail("riya@x.com"))
    }

    @Test
    fun oldLocalAccount_isMovedToCloudOnFirstLogin() = runBlocking {
        addLocalUser("priya@wanderlust.demo", "agent123")
        assertTrue(repo.login("priya@wanderlust.demo", "agent123").isSuccess)
        assertEquals("agent123", cloud.accounts["priya@wanderlust.demo"])
    }

    @Test
    fun wrongPassword_fails() = runBlocking {
        addLocalUser("a@x.com", "right12")
        val result = repo.login("a@x.com", "wrong12")
        assertTrue(result.isFailure)
        assertTrue(cloud.accounts.isEmpty())
    }

    @Test
    fun unknownEverywhere_fails() = runBlocking {
        assertTrue(repo.login("nobody@x.com", "whatever").isFailure)
    }

    @Test
    fun passwordChangedOnAnotherPhone_usesNewPassword() = runBlocking {
        addLocalUser("a@x.com", "oldpass")
        cloud.accounts["a@x.com"] = "newpass"
        // Old password: cloud says invalid, local matches, but the cloud account exists -> refuse
        assertTrue(repo.login("a@x.com", "oldpass").isFailure)
        // New password works and updates the local copy
        assertTrue(repo.login("a@x.com", "newpass").isSuccess)
        cloud.offline = true
        assertTrue(repo.login("a@x.com", "newpass").isSuccess)
    }

    @Test
    fun offline_fallsBackToLocalLogin() = runBlocking {
        addLocalUser("a@x.com", "pass123")
        cloud.offline = true
        assertTrue(repo.login("a@x.com", "pass123").isSuccess)
    }

    @Test
    fun forgotPassword_sendsCloudResetEmail() = runBlocking {
        assertTrue(repo.passwordResetByEmailLink)
        assertTrue(repo.sendPasswordResetOtp("someone@x.com").isSuccess)
        assertEquals(1, cloud.resetEmailsSent)
    }

    @Test
    fun logout_signsOutOfCloud() = runBlocking {
        cloud.accounts["riya@x.com"] = "secret1"
        repo.login("riya@x.com", "secret1")
        repo.logout()
        assertEquals(null, cloud.signedInEmail)
    }
}

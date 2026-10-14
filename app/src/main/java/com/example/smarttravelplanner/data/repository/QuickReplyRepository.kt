package com.example.smarttravelplanner.data.repository

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import com.example.smarttravelplanner.domain.util.QuickReplies
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

/** The logged-in agent's quick-reply templates. */
interface QuickReplyRepository {
    fun observeQuickReplies(): Flow<List<String>>
    suspend fun addQuickReply(text: String)
    suspend fun removeQuickReply(text: String)
    suspend fun resetQuickReplies()
}

/** Stores templates in Preferences DataStore, one entry per agent email. */
@Singleton
class DataStoreQuickReplyRepository @Inject constructor(
    private val dataStore: DataStore<Preferences>,
    private val authRepository: AuthRepository
) : QuickReplyRepository {

    private fun keyFor(email: String) = stringPreferencesKey("agent_quick_replies_$email")

    @OptIn(ExperimentalCoroutinesApi::class)
    override fun observeQuickReplies(): Flow<List<String>> =
        authRepository.currentUserEmail
            .map { it?.trim()?.lowercase().orEmpty() }
            .distinctUntilChanged()
            .flatMapLatest { email ->
                if (email.isEmpty()) flowOf(emptyList())
                else dataStore.data.map { prefs ->
                    prefs[keyFor(email)]?.let(QuickReplies::decode) ?: QuickReplies.DEFAULTS
                }
            }
            .distinctUntilChanged()

    override suspend fun addQuickReply(text: String) = update { QuickReplies.add(it, text) }

    override suspend fun removeQuickReply(text: String) = update { list -> list.filterNot { it == text } }

    override suspend fun resetQuickReplies() {
        val email = currentEmail() ?: return
        dataStore.edit { it.remove(keyFor(email)) }
    }

    private suspend fun update(change: (List<String>) -> List<String>) {
        val email = currentEmail() ?: return
        dataStore.edit { prefs ->
            val current = prefs[keyFor(email)]?.let(QuickReplies::decode) ?: QuickReplies.DEFAULTS
            prefs[keyFor(email)] = QuickReplies.encode(change(current))
        }
    }

    private suspend fun currentEmail(): String? =
        authRepository.currentUserEmail.first()?.trim()?.lowercase()?.ifEmpty { null }
}

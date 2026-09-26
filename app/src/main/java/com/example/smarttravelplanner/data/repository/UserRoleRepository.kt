package com.example.smarttravelplanner.data.repository

import com.example.smarttravelplanner.domain.model.UserRole
import kotlinx.coroutines.flow.Flow

/** Role (Traveller / Agent) of the logged-in user. */
interface UserRoleRepository {

    /**
     * Role of whoever is logged in right now. Emits again when the role changes
     * or a different account logs in. Defaults to [UserRole.TRAVELLER].
     */
    val currentUserRole: Flow<UserRole>

    /** Changes the logged-in user's role. Does nothing if nobody is logged in. */
    suspend fun setCurrentUserRole(role: UserRole)

    /** Role of the account registered with [email], or null if there is no such account. */
    suspend fun roleForEmail(email: String): UserRole?
}

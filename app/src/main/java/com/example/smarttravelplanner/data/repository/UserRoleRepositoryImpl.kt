package com.example.smarttravelplanner.data.repository

import com.example.smarttravelplanner.data.local.dao.UserDao
import com.example.smarttravelplanner.domain.model.UserRole
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import javax.inject.Inject

class UserRoleRepositoryImpl @Inject constructor(
    private val userDao: UserDao,
    private val authRepository: AuthRepository
) : UserRoleRepository {

    @OptIn(ExperimentalCoroutinesApi::class)
    override val currentUserRole: Flow<UserRole> =
        authRepository.currentUserEmail
            .map { it?.trim().orEmpty() }
            .distinctUntilChanged()
            .flatMapLatest { email ->
                if (email.isEmpty()) flowOf(UserRole.TRAVELLER)
                else userDao.observeRole(email).map { UserRole.fromStorage(it) }
            }
            .distinctUntilChanged()

    override suspend fun setCurrentUserRole(role: UserRole) {
        val email = authRepository.currentUserEmail.first()?.trim().orEmpty()
        if (email.isNotEmpty()) userDao.updateRole(email, role.name)
    }
}

package com.example.smarttravelplanner.di

import com.example.smarttravelplanner.data.repository.UserRoleRepository
import com.example.smarttravelplanner.data.repository.UserRoleRepositoryImpl
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/**
 * Hilt bindings for the travel-agent feature.
 * Kept separate from RepositoryModule so agent work doesn't conflict with other features.
 */
@Module
@InstallIn(SingletonComponent::class)
abstract class AgentModule {

    @Binds @Singleton
    abstract fun bindUserRoleRepository(impl: UserRoleRepositoryImpl): UserRoleRepository
}

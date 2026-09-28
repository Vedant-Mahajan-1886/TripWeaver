package com.example.smarttravelplanner.di

import com.example.smarttravelplanner.data.local.dao.AgentDao
import com.example.smarttravelplanner.data.local.dao.BookingDao
import com.example.smarttravelplanner.data.local.dao.PackageDao
import com.example.smarttravelplanner.data.local.db.TravelDatabase
import com.example.smarttravelplanner.data.repository.AgentRepository
import com.example.smarttravelplanner.data.repository.BookingRepository
import com.example.smarttravelplanner.data.repository.LocalBookingRepository
import com.example.smarttravelplanner.data.repository.LocalPackageRepository
import com.example.smarttravelplanner.data.repository.PackageRepository
import com.example.smarttravelplanner.data.repository.LocalAgentRepository
import com.example.smarttravelplanner.data.repository.UserRoleRepository
import com.example.smarttravelplanner.data.repository.UserRoleRepositoryImpl
import dagger.Binds
import dagger.Module
import dagger.Provides
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

    /** Phase 1: local (Room). Phase 2 can bind a Firebase implementation here instead. */
    @Binds @Singleton
    abstract fun bindAgentRepository(impl: LocalAgentRepository): AgentRepository

    @Binds @Singleton
    abstract fun bindBookingRepository(impl: LocalBookingRepository): BookingRepository

    @Binds @Singleton
    abstract fun bindPackageRepository(impl: LocalPackageRepository): PackageRepository
}

@Module
@InstallIn(SingletonComponent::class)
object AgentDatabaseModule {

    @Provides
    fun provideAgentDao(database: TravelDatabase): AgentDao = database.agentDao()

    @Provides
    fun provideBookingDao(database: TravelDatabase): BookingDao = database.bookingDao()

    @Provides
    fun providePackageDao(database: TravelDatabase): PackageDao = database.packageDao()
}

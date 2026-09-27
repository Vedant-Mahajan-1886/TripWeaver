package com.example.smarttravelplanner.di

import com.example.smarttravelplanner.data.local.dao.TripEditDao
import com.example.smarttravelplanner.data.local.db.TravelDatabase
import com.example.smarttravelplanner.data.repository.TripEditRepository
import com.example.smarttravelplanner.data.repository.TripEditRepositoryImpl
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/** Hilt bindings for trip editing (kept separate from RepositoryModule to avoid conflicts). */
@Module
@InstallIn(SingletonComponent::class)
abstract class TripEditModule {

    @Binds @Singleton
    abstract fun bindTripEditRepository(impl: TripEditRepositoryImpl): TripEditRepository
}

@Module
@InstallIn(SingletonComponent::class)
object TripEditDatabaseModule {

    @Provides
    fun provideTripEditDao(database: TravelDatabase): TripEditDao = database.tripEditDao()
}

package com.example.smarttravelplanner.di

import com.example.smarttravelplanner.data.repository.AiRepository
import com.example.smarttravelplanner.data.repository.AiRepositoryImpl
import com.example.smarttravelplanner.data.repository.TripRepository
import com.example.smarttravelplanner.data.repository.TripRepositoryImpl
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class RepositoryModule {

    @Binds
    @Singleton
    abstract fun bindTripRepository(impl: TripRepositoryImpl): TripRepository

    @Binds
    @Singleton
    abstract fun bindAiRepository(impl: AiRepositoryImpl): AiRepository
}

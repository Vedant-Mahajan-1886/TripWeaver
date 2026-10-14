package com.example.smarttravelplanner.di

import com.example.smarttravelplanner.data.repository.DataStoreSoloModeRepository
import com.example.smarttravelplanner.data.repository.SoloModeRepository
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class SoloModeModule {

    @Binds @Singleton
    abstract fun bindSoloModeRepository(impl: DataStoreSoloModeRepository): SoloModeRepository
}

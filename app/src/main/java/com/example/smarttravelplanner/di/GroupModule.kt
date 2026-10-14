package com.example.smarttravelplanner.di

import com.example.smarttravelplanner.data.local.dao.GroupDao
import com.example.smarttravelplanner.data.local.db.TravelDatabase
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

/** Hilt wiring for group trips (solo travellers joining an agent's group). */
@Module
@InstallIn(SingletonComponent::class)
object GroupModule {

    @Provides
    fun provideGroupDao(database: TravelDatabase): GroupDao = database.groupDao()
}

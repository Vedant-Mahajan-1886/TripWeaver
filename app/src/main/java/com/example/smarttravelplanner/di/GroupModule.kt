package com.example.smarttravelplanner.di

import com.example.smarttravelplanner.data.local.dao.GroupDao
import com.example.smarttravelplanner.data.local.db.TravelDatabase
import com.example.smarttravelplanner.data.repository.GroupRepository
import com.example.smarttravelplanner.data.repository.LocalGroupRepository
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/** Hilt wiring for group trips (solo travellers joining an agent's group). */
@Module
@InstallIn(SingletonComponent::class)
abstract class GroupModule {

    @Binds @Singleton
    abstract fun bindGroupRepository(impl: LocalGroupRepository): GroupRepository

    companion object {
        @Provides
        fun provideGroupDao(database: TravelDatabase): GroupDao = database.groupDao()
    }
}

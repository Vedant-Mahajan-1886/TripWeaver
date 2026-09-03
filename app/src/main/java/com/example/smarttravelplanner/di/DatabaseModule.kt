package com.example.smarttravelplanner.di

import android.content.Context
import androidx.room.Room
import com.example.smarttravelplanner.data.local.dao.ItineraryDao
import com.example.smarttravelplanner.data.local.dao.TripDao
import com.example.smarttravelplanner.data.local.dao.UserDao
import com.example.smarttravelplanner.data.local.db.TravelDatabase
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    @Provides
    @Singleton
    fun provideTravelDatabase(@ApplicationContext context: Context): TravelDatabase =
        Room.databaseBuilder(
            context,
            TravelDatabase::class.java,
            TravelDatabase.DATABASE_NAME
        )
        .fallbackToDestructiveMigration(true)
        .build()

    @Provides
    fun provideTripDao(database: TravelDatabase): TripDao = database.tripDao()

    @Provides
    fun provideItineraryDao(database: TravelDatabase): ItineraryDao = database.itineraryDao()

    @Provides
    fun provideUserDao(database: TravelDatabase): UserDao = database.userDao()
}

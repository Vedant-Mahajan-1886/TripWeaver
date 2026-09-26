package com.example.smarttravelplanner.data.local.db

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.example.smarttravelplanner.data.local.dao.ItineraryDao
import com.example.smarttravelplanner.data.local.dao.TripDao
import com.example.smarttravelplanner.data.local.dao.UserDao
import com.example.smarttravelplanner.data.local.entity.ItineraryEntity
import com.example.smarttravelplanner.data.local.entity.TripEntity
import com.example.smarttravelplanner.data.local.entity.UserEntity

@Database(
    entities = [TripEntity::class, ItineraryEntity::class, UserEntity::class],
    version = 3,
    // Schema JSON is written to app/schemas (see ksp block in app/build.gradle.kts)
    exportSchema = true
)
@TypeConverters(Converters::class)
abstract class TravelDatabase : RoomDatabase() {
    abstract fun tripDao(): TripDao
    abstract fun itineraryDao(): ItineraryDao
    abstract fun userDao(): UserDao

    companion object {
        const val DATABASE_NAME = "travel_database"
    }
}

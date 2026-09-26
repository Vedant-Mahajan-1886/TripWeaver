package com.example.smarttravelplanner.data.local.db

import androidx.room.AutoMigration
import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.example.smarttravelplanner.data.local.dao.AgentDao
import com.example.smarttravelplanner.data.local.dao.ItineraryDao
import com.example.smarttravelplanner.data.local.dao.TripDao
import com.example.smarttravelplanner.data.local.dao.UserDao
import com.example.smarttravelplanner.data.local.entity.ActivityFeedbackEntity
import com.example.smarttravelplanner.data.local.entity.AgentProfileEntity
import com.example.smarttravelplanner.data.local.entity.ItineraryEntity
import com.example.smarttravelplanner.data.local.entity.TripEntity
import com.example.smarttravelplanner.data.local.entity.TripShareEntity
import com.example.smarttravelplanner.data.local.entity.UserEntity

@Database(
    entities = [
        TripEntity::class,
        ItineraryEntity::class,
        UserEntity::class,
        // Travel-agent feature (v7)
        AgentProfileEntity::class,
        TripShareEntity::class,
        ActivityFeedbackEntity::class
    ],
    version = 7,
    // Schema JSON is written to app/schemas (see ksp block in app/build.gradle.kts)
    exportSchema = true,
    autoMigrations = [
        // v6 → v7 only adds new tables, so Room can generate it from 6.json
        AutoMigration(from = 6, to = 7)
    ]
)
@TypeConverters(Converters::class)
abstract class TravelDatabase : RoomDatabase() {
    abstract fun tripDao(): TripDao
    abstract fun itineraryDao(): ItineraryDao
    abstract fun userDao(): UserDao
    abstract fun agentDao(): AgentDao

    companion object {
        const val DATABASE_NAME = "travel_database"
    }
}

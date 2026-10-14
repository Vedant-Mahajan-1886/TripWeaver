package com.example.smarttravelplanner.data.local.db

import androidx.room.AutoMigration
import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.example.smarttravelplanner.data.local.dao.AgentDao
import com.example.smarttravelplanner.data.local.dao.BookingDao
import com.example.smarttravelplanner.data.local.dao.ItineraryDao
import com.example.smarttravelplanner.data.local.dao.PackageDao
import com.example.smarttravelplanner.data.local.dao.TripEditDao
import com.example.smarttravelplanner.data.local.dao.TripDao
import com.example.smarttravelplanner.data.local.dao.UserDao
import com.example.smarttravelplanner.data.local.entity.ActivityFeedbackEntity
import com.example.smarttravelplanner.data.local.entity.AgentPackageEntity
import com.example.smarttravelplanner.data.local.entity.AgentReviewEntity
import com.example.smarttravelplanner.data.local.entity.BookingEntity
import com.example.smarttravelplanner.data.local.entity.AgentProfileEntity
import com.example.smarttravelplanner.data.local.entity.ItineraryEntity
import com.example.smarttravelplanner.data.local.entity.PackageDayEntity
import com.example.smarttravelplanner.data.local.entity.TripEditEntity
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
        ActivityFeedbackEntity::class,
        // Trip editing (v8)
        TripEditEntity::class,
        // Booking lifecycle + agent ratings (v9)
        BookingEntity::class,
        AgentReviewEntity::class,
        // Agent packages (v10)
        AgentPackageEntity::class,
        PackageDayEntity::class
        // v11: agent_reviews gets agentReply + agentRepliedAt (agent answers a rating)
    ],
    version = 11,
    // Schema JSON is written to app/schemas (see ksp block in app/build.gradle.kts)
    exportSchema = true,
    autoMigrations = [
        // v6 → v7 only adds new tables, so Room can generate it from 6.json
        AutoMigration(from = 6, to = 7),
        // v7 → v8 only adds the trip_edits table
        AutoMigration(from = 7, to = 8),
        // v8 → v9 only adds the bookings and agent_reviews tables
        AutoMigration(from = 8, to = 9),
        // v9 → v10 adds agent_packages + package_days and a nullable trip_shares.packageId
        AutoMigration(from = 9, to = 10),
        // v10 -> v11 adds agent_reviews.agentReply (NOT NULL DEFAULT '') and agentRepliedAt (nullable)
        AutoMigration(from = 10, to = 11)
    ]
)
@TypeConverters(Converters::class)
abstract class TravelDatabase : RoomDatabase() {
    abstract fun tripDao(): TripDao
    abstract fun itineraryDao(): ItineraryDao
    abstract fun userDao(): UserDao
    abstract fun agentDao(): AgentDao
    abstract fun tripEditDao(): TripEditDao
    abstract fun bookingDao(): BookingDao
    abstract fun packageDao(): PackageDao

    companion object {
        const val DATABASE_NAME = "travel_database"
    }
}

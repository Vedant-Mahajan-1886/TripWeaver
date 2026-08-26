package com.example.smarttravelplanner.data.local.db

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.example.smarttravelplanner.data.local.dao.ItineraryDao
import com.example.smarttravelplanner.data.local.dao.TripDao
import com.example.smarttravelplanner.data.local.entity.ItineraryEntity
import com.example.smarttravelplanner.data.local.entity.TripEntity

@Database(
    entities = [TripEntity::class, ItineraryEntity::class],
    version = 1,
    exportSchema = false
)
@TypeConverters(Converters::class)
abstract class TravelDatabase : RoomDatabase() {
    abstract fun tripDao(): TripDao
    abstract fun itineraryDao(): ItineraryDao

    companion object {
        const val DATABASE_NAME = "travel_database"
    }
}

package com.example.smarttravelplanner.data.local.db

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

/**
 * Every schema change to [TravelDatabase] must come with a migration here,
 * so users keep their accounts and trips when they update the app.
 *
 * How to add one:
 *  1. Change the entity and bump `version` in [TravelDatabase].
 *  2. Add a Migration below and add it to [ALL].
 *  3. Build once and commit the new file in app/schemas.
 */
object DatabaseMigrations {

    /**
     * v3 → v4: trips get traveller counts (adults, kids).
     * Existing trips default to 1 adult and 0 kids.
     */
    val MIGRATION_3_4 = object : Migration(3, 4) {
        override fun migrate(db: SupportSQLiteDatabase) {
            db.execSQL("ALTER TABLE trips ADD COLUMN adults INTEGER NOT NULL DEFAULT 1")
            db.execSQL("ALTER TABLE trips ADD COLUMN kids INTEGER NOT NULL DEFAULT 0")
        }
    }

    /**
     * v4 → v5: trips get an owner.
     * Existing trips get ownerEmail = '' and are claimed by the next user who logs in.
     */
    val MIGRATION_4_5 = object : Migration(4, 5) {
        override fun migrate(db: SupportSQLiteDatabase) {
            db.execSQL("ALTER TABLE trips ADD COLUMN ownerEmail TEXT NOT NULL DEFAULT ''")
            db.execSQL("CREATE INDEX IF NOT EXISTS index_trips_ownerEmail ON trips(ownerEmail)")
        }
    }

    /**
     * v5 → v6: users get a role (Traveller / Agent).
     * Every existing account becomes a Traveller.
     */
    val MIGRATION_5_6 = object : Migration(5, 6) {
        override fun migrate(db: SupportSQLiteDatabase) {
            db.execSQL("ALTER TABLE users ADD COLUMN role TEXT NOT NULL DEFAULT 'TRAVELLER'")
        }
    }

    /** All migrations, oldest first. Registered in DatabaseModule. */
    val ALL: Array<Migration> = arrayOf(
        MIGRATION_3_4,
        MIGRATION_4_5,
        MIGRATION_5_6
    )
}

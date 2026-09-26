package com.example.smarttravelplanner.data.local.db

import androidx.room.migration.Migration

/**
 * Every schema change to [TravelDatabase] must come with a migration here,
 * so users keep their accounts and trips when they update the app.
 *
 * How to add one (example for version 3 → 4):
 *  1. Change the entity and bump `version` in [TravelDatabase].
 *  2. Add a Migration below:
 *
 *     val MIGRATION_3_4 = object : Migration(3, 4) {
 *         override fun migrate(db: SupportSQLiteDatabase) {
 *             db.execSQL("ALTER TABLE trips ADD COLUMN ownerUserId INTEGER NOT NULL DEFAULT 0")
 *         }
 *     }
 *
 *  3. Add it to [ALL].
 *  4. Build once and commit the new file in app/schemas.
 */
object DatabaseMigrations {

    /** All migrations, oldest first. Registered in DatabaseModule. */
    val ALL: Array<Migration> = arrayOf(
        // MIGRATION_3_4 will be added in the next part (trip ownership)
    )
}

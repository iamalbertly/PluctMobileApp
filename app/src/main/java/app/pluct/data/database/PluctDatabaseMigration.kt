package app.pluct.data.database

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

/**
 * Pluct-Data-Database-00Migration - Placeholder migrations to keep Room happy.
 * Declares no-op migrations for historical schema versions so the builder compiles.
 */
object PluctDatabaseMigration {
    val MIGRATION_2_3 = object : Migration(2, 3) {
        // UX FIX #5: Renamed parameter to match Migration interface signature
        override fun migrate(db: SupportSQLiteDatabase) {
            // No schema change required; kept for compatibility.
        }
    }

    val MIGRATION_3_4 = object : Migration(3, 4) {
        // UX FIX #5: Renamed parameter to match Migration interface signature
        override fun migrate(db: SupportSQLiteDatabase) {
            // No schema change required; kept for compatibility.
        }
    }

    /**
     * UX FIX #1: Migration for transcriptCachedAt field
     * Adds nullable Long column for cache invalidation tracking
     */
    val MIGRATION_4_5 = object : Migration(4, 5) {
        // UX FIX #5: Renamed parameter to match Migration interface signature
        override fun migrate(db: SupportSQLiteDatabase) {
            // Add transcriptCachedAt column to videos table
            db.execSQL("ALTER TABLE videos ADD COLUMN transcriptCachedAt INTEGER DEFAULT NULL")
        }
    }

    /**
     * UX FIX #3: Migration for confidence field
     * Adds nullable Double column for transcript confidence score (0.0-1.0)
     */
    val MIGRATION_5_6 = object : Migration(5, 6) {
        override fun migrate(db: SupportSQLiteDatabase) {
            // Add confidence column to videos table
            db.execSQL("ALTER TABLE videos ADD COLUMN confidence REAL DEFAULT NULL")
        }
    }
}

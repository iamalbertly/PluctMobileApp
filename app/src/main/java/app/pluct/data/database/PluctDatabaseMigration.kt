package app.pluct.data.database

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

/**
 * Pluct-Data-Database-Migration
 * Database migrations for videos table schema updates
 */
object PluctDatabaseMigration {
    
    val MIGRATION_2_3 = object : Migration(2, 3) {
        override fun migrate(database: SupportSQLiteDatabase) {
            // Add queueReason column (TEXT, nullable)
            database.execSQL("ALTER TABLE videos ADD COLUMN queueReason TEXT")
            
            // Add queuedAt column (INTEGER, nullable)
            database.execSQL("ALTER TABLE videos ADD COLUMN queuedAt INTEGER")
        }
    }
    
    val MIGRATION_3_4 = object : Migration(3, 4) {
        override fun migrate(database: SupportSQLiteDatabase) {
            // Add jobId column (TEXT, nullable) for transcription job ID
            database.execSQL("ALTER TABLE videos ADD COLUMN jobId TEXT")
        }
    }
}


package app.pluct.data.database

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

/**
 * Pluct-Data-Database-00Migration - Placeholder migrations to keep Room happy.
 * Declares no-op migrations for historical schema versions so the builder compiles.
 */
object PluctDatabaseMigration {
    val MIGRATION_2_3 = object : Migration(2, 3) {
        override fun migrate(database: SupportSQLiteDatabase) {
            // No schema change required; kept for compatibility.
        }
    }

    val MIGRATION_3_4 = object : Migration(3, 4) {
        override fun migrate(database: SupportSQLiteDatabase) {
            // No schema change required; kept for compatibility.
        }
    }
}

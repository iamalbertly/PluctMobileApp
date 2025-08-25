package app.pluct.data.database

import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import android.content.Context
import android.util.Log
import app.pluct.data.converter.Converters
import app.pluct.data.dao.OutputArtifactDao
import app.pluct.data.dao.TranscriptDao
import app.pluct.data.dao.VideoItemDao
import app.pluct.data.entity.OutputArtifact
import app.pluct.data.entity.Transcript
import app.pluct.data.entity.VideoItem

@Database(
    entities = [
        VideoItem::class,
        Transcript::class,
        OutputArtifact::class
    ],
    version = 2,
    exportSchema = false
)
@TypeConverters(Converters::class)
abstract class PluctDatabase : RoomDatabase() {
    abstract fun videoItemDao(): VideoItemDao
    abstract fun transcriptDao(): TranscriptDao
    abstract fun outputArtifactDao(): OutputArtifactDao

    companion object {
        private const val TAG = "PluctDatabase"
        
        private val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(database: SupportSQLiteDatabase) {
                try {
                    Log.d(TAG, "Starting migration from version 1 to 2")
                    
                    // Check if columns already exist before adding them
                    val cursor = database.query("PRAGMA table_info(video_items)")
                    val existingColumns = mutableSetOf<String>()
                    while (cursor.moveToNext()) {
                        val columnName = cursor.getString(cursor.getColumnIndex("name"))
                        existingColumns.add(columnName)
                    }
                    cursor.close()
                    
                    Log.d(TAG, "Existing columns: $existingColumns")
                    
                    // Add new columns only if they don't exist
                    if (!existingColumns.contains("description")) {
                        database.execSQL("ALTER TABLE video_items ADD COLUMN description TEXT")
                        Log.d(TAG, "Added description column")
                    }
                    
                    if (!existingColumns.contains("author")) {
                        database.execSQL("ALTER TABLE video_items ADD COLUMN author TEXT")
                        Log.d(TAG, "Added author column")
                    }
                    
                    if (!existingColumns.contains("thumbnailUrl")) {
                        database.execSQL("ALTER TABLE video_items ADD COLUMN thumbnailUrl TEXT")
                        Log.d(TAG, "Added thumbnailUrl column")
                    }
                    
                    // Add UNIQUE constraint on sourceUrl (this might fail if it already exists)
                    try {
                        database.execSQL("CREATE UNIQUE INDEX index_video_items_sourceUrl ON video_items(sourceUrl)")
                        Log.d(TAG, "Added UNIQUE index on sourceUrl")
                    } catch (e: Exception) {
                        Log.w(TAG, "UNIQUE index might already exist: ${e.message}")
                    }
                    
                    Log.d(TAG, "Migration completed successfully")
                } catch (e: Exception) {
                    Log.e(TAG, "Migration failed: ${e.message}", e)
                    throw e
                }
            }
        }
        
        @Volatile
        private var INSTANCE: PluctDatabase? = null

        fun getDatabase(context: Context): PluctDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    PluctDatabase::class.java,
                    "pluct.db"
                )
                .addMigrations(MIGRATION_1_2) // Use proper migrations instead of destructive recreation
                .build()
                INSTANCE = instance
                instance
            }
        }
    }
}


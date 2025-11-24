package app.pluct.data.database

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import app.pluct.data.entity.VideoItem
import app.pluct.data.dao.PluctVideoDao

/**
 * Pluct-Data-Database-01Main - Main Room database
 * Follows naming convention: [Project]-[ParentScope]-[ChildScope]-[CoreResponsibility]
 * Single source of truth for local video storage
 */
@Database(
    entities = [VideoItem::class],
    version = 1,
    exportSchema = true
)
@TypeConverters(PluctDatabaseConverters::class)
abstract class PluctDatabase : RoomDatabase() {
    abstract fun videoDao(): PluctVideoDao
    
    companion object {
        const val DATABASE_NAME = "pluct_database"
    }
}

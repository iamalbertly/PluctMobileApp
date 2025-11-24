package app.pluct.data.database

import androidx.room.TypeConverter
import app.pluct.data.entity.ProcessingStatus
import app.pluct.data.entity.ProcessingTier

/**
 * Pluct-Data-Database-Converters - Type converters for Room database
 * Converts enum types to string for database storage
 */
class PluctDatabaseConverters {
    
    @TypeConverter
    fun fromProcessingStatus(value: ProcessingStatus): String {
        return value.name
    }
    
    @TypeConverter
    fun toProcessingStatus(value: String): ProcessingStatus {
        return ProcessingStatus.valueOf(value)
    }
    
    @TypeConverter
    fun fromProcessingTier(value: ProcessingTier): String {
        return value.name
    }
    
    @TypeConverter
    fun toProcessingTier(value: String): ProcessingTier {
        return ProcessingTier.valueOf(value)
    }
}

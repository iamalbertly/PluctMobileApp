package app.pluct.data.database

import androidx.room.TypeConverter
import app.pluct.data.entity.ProcessingStatus
import app.pluct.data.entity.ProcessingTier
import app.pluct.data.entity.QueueReason

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
        return try {
            ProcessingTier.valueOf(value)
        } catch (e: IllegalArgumentException) {
            // Handle legacy enum values from old database entries
            // Map old values to current ones
            when (value) {
                "FREE", "STANDARD" -> ProcessingTier.EXTRACT_SCRIPT
                "PREMIUM", "AI_ANALYSIS", "DEEP_ANALYSIS", "PREMIUM_INSIGHTS" -> ProcessingTier.GENERATE_INSIGHTS
                else -> ProcessingTier.EXTRACT_SCRIPT // Default fallback
            }
        }
    }
    
    @TypeConverter
    fun fromQueueReason(value: QueueReason?): String? {
        return value?.name
    }
    
    @TypeConverter
    fun toQueueReason(value: String?): QueueReason? {
        return value?.let { QueueReason.valueOf(it) }
    }
}

package app.pluct.data.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import app.pluct.data.entity.DebugLogEntry
import kotlinx.coroutines.flow.Flow

/**
 * Pluct-Data-DAO-DebugLog - Database access object for debug logs
 */
@Dao
interface PluctDebugLogDAO {
    
    @Insert
    suspend fun insertLog(log: DebugLogEntry): Long
    
    @Query("SELECT * FROM debug_logs ORDER BY timestamp DESC LIMIT :limit")
    fun getRecentLogs(limit: Int = 100): Flow<List<DebugLogEntry>>
    
    @Query("SELECT * FROM debug_logs WHERE category = :category ORDER BY timestamp DESC LIMIT :limit")
    fun getLogsByCategory(category: String, limit: Int = 100): Flow<List<DebugLogEntry>>
    
    @Query("SELECT * FROM debug_logs ORDER BY timestamp DESC")
    fun getAllLogs(): Flow<List<DebugLogEntry>>
    
    @Query("DELETE FROM debug_logs WHERE timestamp < :beforeTimestamp")
    suspend fun deleteOldLogs(beforeTimestamp: Long): Int
    
    @Query("DELETE FROM debug_logs WHERE id = :logId")
    suspend fun deleteLog(log: DebugLogEntry): Int {
        return deleteLogById(log.id)
    }
    
    @Query("DELETE FROM debug_logs WHERE id = :logId")
    suspend fun deleteLogById(logId: Long): Int
    
    @Query("DELETE FROM debug_logs")
    suspend fun clearAllLogs(): Int
    
    @Query("SELECT COUNT(*) FROM debug_logs")
    suspend fun getLogCount(): Int
    
    @Query("SELECT * FROM debug_logs WHERE category = :category AND operation = :operation AND message = :message AND timestamp > :sinceTimestamp ORDER BY timestamp DESC LIMIT :limit")
    suspend fun findSimilarLogs(category: String, operation: String, message: String, sinceTimestamp: Long, limit: Int = 5): List<DebugLogEntry>
    
    @Query("DELETE FROM debug_logs WHERE id IN (SELECT id FROM debug_logs WHERE timestamp < :beforeTimestamp ORDER BY timestamp ASC LIMIT :maxDelete)")
    suspend fun deleteOldLogsBatch(beforeTimestamp: Long, maxDelete: Int = 1000): Int
    
    @Query("DELETE FROM debug_logs WHERE category = :category AND operation = :operation AND message = :message AND timestamp < :beforeTimestamp")
    suspend fun deleteDuplicateLogs(category: String, operation: String, message: String, beforeTimestamp: Long): Int
    
    // Delete duplicate logs keeping only the most recent one per category+operation
    // Note: Room doesn't support window functions, so we use a simpler approach
    @Query("DELETE FROM debug_logs WHERE id IN (SELECT id FROM debug_logs WHERE category = :category AND operation = :operation AND timestamp < :beforeTimestamp AND id NOT IN (SELECT id FROM debug_logs WHERE category = :category AND operation = :operation AND timestamp < :beforeTimestamp ORDER BY timestamp DESC LIMIT 1))")
    suspend fun deleteDuplicateLogsByCategoryAndOperation(category: String, operation: String, beforeTimestamp: Long): Int
}

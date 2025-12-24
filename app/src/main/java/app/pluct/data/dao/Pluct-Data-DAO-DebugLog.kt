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
}

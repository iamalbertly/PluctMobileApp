package app.pluct.core.optimization

import android.app.ActivityManager
import android.content.Context
import android.util.Log
import kotlin.system.measureTimeMillis

/**
 * Pluct-Optimization-01MemoryManager-02Monitor - Memory monitoring component
 * Follows naming convention: [Project]-[ParentScope]-[ChildScope]-[Separation of Concern][CoreResponsibility]
 * Single source of truth for memory monitoring functionality
 */
class PluctMemoryMonitor(
    private val context: Context
) {
    private val activityManager = context.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
    
    fun getCurrentMemoryStatus(): MemoryStatus {
        val memoryInfo = ActivityManager.MemoryInfo()
        activityManager.getMemoryInfo(memoryInfo)
        
        val usedMemoryMB = (memoryInfo.totalMem - memoryInfo.availMem) / (1024 * 1024)
        val maxMemoryMB = memoryInfo.totalMem / (1024 * 1024)
        val availableMemoryMB = memoryInfo.availMem / (1024 * 1024)
        
        val memoryPressure = when {
            usedMemoryMB > maxMemoryMB * 0.9 -> MemoryPressure.CRITICAL
            usedMemoryMB > maxMemoryMB * 0.8 -> MemoryPressure.HIGH
            usedMemoryMB > maxMemoryMB * 0.6 -> MemoryPressure.NORMAL
            else -> MemoryPressure.LOW
        }
        
        return MemoryStatus(
            usedMemoryMB = usedMemoryMB,
            maxMemoryMB = maxMemoryMB,
            availableMemoryMB = availableMemoryMB,
            memoryPressure = memoryPressure
        )
    }
    
    fun getMemoryInfo(): ActivityManager.MemoryInfo {
        val memoryInfo = ActivityManager.MemoryInfo()
        activityManager.getMemoryInfo(memoryInfo)
        return memoryInfo
    }
}

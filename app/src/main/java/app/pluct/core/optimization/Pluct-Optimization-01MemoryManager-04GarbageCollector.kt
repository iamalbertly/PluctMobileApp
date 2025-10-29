package app.pluct.core.optimization

import android.util.Log
import kotlin.system.measureTimeMillis

/**
 * Pluct-Optimization-01MemoryManager-04GarbageCollector - Garbage collection component
 * Follows naming convention: [Project]-[ParentScope]-[ChildScope]-[Separation of Concern][CoreResponsibility]
 * Single source of truth for garbage collection functionality
 */
class PluctGarbageCollector {
    private var lastGCTime = 0L
    private val gcInterval = 10000L // 10 seconds minimum between GCs
    
    fun forceGarbageCollection() {
        val currentTime = System.currentTimeMillis()
        if (currentTime - lastGCTime < gcInterval) {
            Log.d("PluctGarbageCollector", "Skipping GC - too soon since last collection")
            return
        }
        
        val gcTime = measureTimeMillis {
            System.gc()
            System.runFinalization()
            System.gc()
        }
        
        lastGCTime = currentTime
        Log.i("PluctGarbageCollector", "Garbage collection completed in ${gcTime}ms")
    }
    
    fun canTriggerGC(): Boolean {
        return System.currentTimeMillis() - lastGCTime >= gcInterval
    }
}

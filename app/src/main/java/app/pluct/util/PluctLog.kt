package app.pluct.util

import android.util.Log
import java.util.UUID

/**
 * Pipeline logging utility for milestone tracking
 * Follows naming convention: [Project]-[ParentScope]-[ChildScope]-[CoreResponsibility]
 */
object PluctLog {
    
    fun vended(id: String) = Log.i("TTT", "BE_TOKEN_VENDED id=$id")
    
    fun tttQueued(id: String) = Log.i("TTT", "TTT_ENQUEUED id=$id")
    
    fun tttDone(id: String) = Log.i("TTT", "TRANSCRIPT_READY id=$id")
    
    fun error(stage: String, msg: String) = Log.e("TTT", "ERR stage=$stage msg=$msg")
    
    fun quickScanStart(url: String, clientRequestId: String) = Log.i("TTT", "QUICK_SCAN_START url=$url clientRequestId=$clientRequestId")
    
    fun quickScanComplete(url: String, clientRequestId: String) = Log.i("TTT", "QUICK_SCAN_COMPLETE url=$url clientRequestId=$clientRequestId")
}

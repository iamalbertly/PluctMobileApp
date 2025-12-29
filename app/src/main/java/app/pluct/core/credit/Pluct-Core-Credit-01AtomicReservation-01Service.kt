package app.pluct.core.credit

import android.util.Log
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

/**
 * Pluct-Core-Credit-01AtomicReservation-01Service
 * Follows naming convention: [Project]-[Core]-[Credit]-[AtomicReservation]-[Service]
 * 5 scope layers: Project, Core, Credit, AtomicReservation, Service
 * Provides atomic credit reservation to prevent race conditions
 */
data class ReservationResult(
    val success: Boolean,
    val reservationId: String?,
    val error: String? = null
)

class PluctCoreCredit01AtomicReservation01Service {
    private val TAG = "AtomicCreditReservation"
    private val reservations = ConcurrentHashMap<String, Reservation>()
    private val mutex = Mutex()
    
    data class Reservation(
        val id: String,
        val amount: Int,
        val timestamp: Long,
        val committed: Boolean = false
    )
    
    /**
     * Atomically reserve credit
     * Returns reservation ID if successful, null if insufficient credits
     */
    suspend fun reserveCredit(
        amount: Int,
        currentBalance: Int,
        currentFreeUses: Int
    ): ReservationResult {
        return mutex.withLock {
            // Check if we have enough credits
            val hasEnough = currentFreeUses > 0 || currentBalance >= amount
            
            if (!hasEnough) {
                Log.d(TAG, "Insufficient credits: balance=$currentBalance, freeUses=$currentFreeUses, required=$amount")
                return ReservationResult(
                    success = false,
                    reservationId = null,
                    error = "Insufficient credits"
                )
            }
            
            // Create reservation
            val reservationId = UUID.randomUUID().toString()
            val reservation = Reservation(
                id = reservationId,
                amount = amount,
                timestamp = System.currentTimeMillis()
            )
            
            reservations[reservationId] = reservation
            Log.d(TAG, "Credit reserved: reservationId=$reservationId, amount=$amount")
            
            ReservationResult(
                success = true,
                reservationId = reservationId
            )
        }
    }
    
    /**
     * Release reservation if not used
     */
    suspend fun releaseReservation(reservationId: String) {
        mutex.withLock {
            val reservation = reservations.remove(reservationId)
            if (reservation != null && !reservation.committed) {
                Log.d(TAG, "Reservation released: reservationId=$reservationId")
            }
        }
    }
    
    /**
     * Commit reservation (mark as used)
     */
    suspend fun commitReservation(reservationId: String): Boolean {
        return mutex.withLock {
            val reservation = reservations[reservationId]
            if (reservation != null && !reservation.committed) {
                reservations[reservationId] = reservation.copy(committed = true)
                Log.d(TAG, "Reservation committed: reservationId=$reservationId")
                true
            } else {
                Log.w(TAG, "Reservation not found or already committed: reservationId=$reservationId")
                false
            }
        }
    }
    
    /**
     * Get active reservation count
     */
    fun getActiveReservationCount(): Int {
        return reservations.values.count { !it.committed }
    }
    
    /**
     * Cleanup expired reservations (older than 5 minutes)
     */
    suspend fun cleanupExpiredReservations() {
        mutex.withLock {
            val now = System.currentTimeMillis()
            val expired = reservations.values.filter { 
                !it.committed && (now - it.timestamp) > 300000 // 5 minutes
            }
            expired.forEach { reservation ->
                reservations.remove(reservation.id)
                Log.d(TAG, "Expired reservation cleaned up: ${reservation.id}")
            }
        }
    }
}


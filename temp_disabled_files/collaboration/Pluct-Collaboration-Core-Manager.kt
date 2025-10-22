package app.pluct.collaboration

import android.content.Context
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.withContext
import java.util.concurrent.ConcurrentHashMap
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Pluct Core Collaboration Manager - Single source of truth for collaboration
 * Follows naming convention: [Project]-[ParentScope]-[ChildScope]-[CoreResponsibility]
 */
@Singleton
class PluctCollaborationCoreManager @Inject constructor(
    private val context: Context
) {
    companion object {
        private const val TAG = "PluctCollaborationCore"
    }

    private val activeCollaborations = ConcurrentHashMap<String, CollaborationSession>()
    private val userSessions = ConcurrentHashMap<String, UserSession>()
    private val _collaborationEvents = MutableSharedFlow<CollaborationEvent>()
    val collaborationEvents: Flow<CollaborationEvent> = _collaborationEvents.asSharedFlow()

    /**
     * Create a new collaboration session
     */
    suspend fun createCollaborationSession(
        videoId: String,
        creatorId: String,
        sessionName: String,
        permissions: CollaborationPermissions = CollaborationPermissions()
    ): CollaborationSession = withContext(Dispatchers.IO) {
        try {
            Log.d(TAG, "Creating collaboration session for video: $videoId")
            
            val session = CollaborationSession(
                id = generateSessionId(),
                videoId = videoId,
                creatorId = creatorId,
                sessionName = sessionName,
                permissions = permissions,
                participants = mutableSetOf(creatorId),
                createdAt = System.currentTimeMillis(),
                isActive = true
            )
            
            activeCollaborations[session.id] = session
            _collaborationEvents.emit(CollaborationEvent.SessionCreated(session))
            
            Log.d(TAG, "Collaboration session created: ${session.id}")
            session
        } catch (e: Exception) {
            Log.e(TAG, "Error creating collaboration session: ${e.message}", e)
            throw e
        }
    }

    /**
     * Join an existing collaboration session
     */
    suspend fun joinCollaborationSession(
        sessionId: String,
        userId: String
    ): CollaborationSession? = withContext(Dispatchers.IO) {
        try {
            Log.d(TAG, "User $userId joining session: $sessionId")
            
            val session = activeCollaborations[sessionId]
            if (session == null) {
                Log.w(TAG, "Session not found: $sessionId")
                return@withContext null
            }
            
            if (!session.isActive) {
                Log.w(TAG, "Session is not active: $sessionId")
                return@withContext null
            }
            
            session.participants.add(userId)
            _collaborationEvents.emit(CollaborationEvent.UserJoined(sessionId, userId))
            
            Log.d(TAG, "User $userId joined session: $sessionId")
            session
        } catch (e: Exception) {
            Log.e(TAG, "Error joining collaboration session: ${e.message}", e)
            null
        }
    }

    /**
     * Leave a collaboration session
     */
    suspend fun leaveCollaborationSession(
        sessionId: String,
        userId: String
    ) = withContext(Dispatchers.IO) {
        try {
            Log.d(TAG, "User $userId leaving session: $sessionId")
            
            val session = activeCollaborations[sessionId]
            if (session != null) {
                session.participants.remove(userId)
                _collaborationEvents.emit(CollaborationEvent.UserLeft(sessionId, userId))
                
                // If no participants left, close session
                if (session.participants.isEmpty()) {
                    closeCollaborationSession(sessionId)
                }
                
                Log.d(TAG, "User $userId left session: $sessionId")
            } else {
                Log.w(TAG, "Session not found: $sessionId")
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error leaving collaboration session: ${e.message}", e)
        }
    }

    /**
     * Close a collaboration session
     */
    suspend fun closeCollaborationSession(sessionId: String) = withContext(Dispatchers.IO) {
        try {
            Log.d(TAG, "Closing collaboration session: $sessionId")
            
            val session = activeCollaborations.remove(sessionId)
            if (session != null) {
                session.isActive = false
                _collaborationEvents.emit(CollaborationEvent.SessionClosed(sessionId))
                Log.d(TAG, "Collaboration session closed: $sessionId")
            } else {
                Log.w(TAG, "Session not found for closing: $sessionId")
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error closing collaboration session: ${e.message}", e)
        }
    }

    /**
     * Get active collaboration sessions for a user
     */
    suspend fun getUserCollaborationSessions(userId: String): List<CollaborationSession> = withContext(Dispatchers.IO) {
        try {
            activeCollaborations.values.filter { session ->
                session.participants.contains(userId) && session.isActive
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error getting user collaboration sessions: ${e.message}", e)
            emptyList()
        }
    }

    private fun generateSessionId(): String {
        return "session_${System.currentTimeMillis()}_${(1000..9999).random()}"
    }
}

/**
 * Collaboration data classes
 */
data class CollaborationSession(
    val id: String,
    val videoId: String,
    val creatorId: String,
    val sessionName: String,
    val permissions: CollaborationPermissions,
    val participants: MutableSet<String>,
    val createdAt: Long,
    var isActive: Boolean
)

data class UserSession(
    val userId: String,
    val sessionId: String,
    val joinedAt: Long,
    val permissions: CollaborationPermissions
)

data class CollaborationPermissions(
    val canEdit: Boolean = false,
    val canComment: Boolean = true,
    val canShare: Boolean = true,
    val canInvite: Boolean = false
)

sealed class CollaborationEvent {
    data class SessionCreated(val session: CollaborationSession) : CollaborationEvent()
    data class SessionClosed(val sessionId: String) : CollaborationEvent()
    data class UserJoined(val sessionId: String, val userId: String) : CollaborationEvent()
    data class UserLeft(val sessionId: String, val userId: String) : CollaborationEvent()
}

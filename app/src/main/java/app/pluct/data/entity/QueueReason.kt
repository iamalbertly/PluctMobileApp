package app.pluct.data.entity

/**
 * Pluct-Data-Entity-QueueReason
 * Enum representing the reason why a video was queued
 * Follows naming convention: [Project]-[ParentScope]-[ChildScope]-[CoreResponsibility]
 */
enum class QueueReason {
    NO_INTERNET,
    INSUFFICIENT_CREDITS,
    RATE_LIMITED,
    SERVICE_UNAVAILABLE
}




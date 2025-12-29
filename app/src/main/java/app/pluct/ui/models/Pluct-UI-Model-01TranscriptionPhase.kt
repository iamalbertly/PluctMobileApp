package app.pluct.ui.models

/**
 * Pluct-UI-Model-01TranscriptionPhase - Transcription phase enumeration
 * Follows naming convention: [Project]-[ParentScope]-[ChildScope]-[Separation]-[Responsibility]
 * Shared model for transcription phases used across UI components
 */
enum class TranscriptionPhase {
    PREPARING,
    DOWNLOADING,
    EXTRACTING,
    TRANSCRIBING,
    FINALIZING,
    COMPLETED
}







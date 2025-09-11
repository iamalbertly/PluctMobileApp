package app.pluct.viewmodel

/**
 * Utility class for generating value propositions from transcripts
 */
object ValuePropositionGenerator {
    
    /**
     * Generate a value proposition from transcript text
     */
    fun generateValuePropositionFromTranscript(transcript: String): String {
        // Simple value proposition generation - in a real app, this would use AI/ML
        val sentences = transcript.split(Regex("[.!?]+")).filter { it.trim().isNotEmpty() }
        val keyPoints = sentences.take(3).joinToString(". ") + "."
        
        return buildString {
            appendLine("Value Proposition:")
            appendLine()
            appendLine("Key Points:")
            appendLine(keyPoints)
            appendLine()
            appendLine("Potential Applications:")
            appendLine("• Content creation")
            appendLine("• Social media engagement")
            appendLine("• Educational content")
            appendLine("• Entertainment value")
        }
    }
}

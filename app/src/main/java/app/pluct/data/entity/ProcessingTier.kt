package app.pluct.data.entity

enum class ProcessingTier {
    /**
     * Free tier transcription - extracts script from video
     * Costs: 1 credit or uses free use allowance
     */
    EXTRACT_SCRIPT,
    
    /**
     * Premium tier transcription - generates AI insights
     * Costs: 2 credits
     */
    GENERATE_INSIGHTS
}

package app.pluct.search

import android.content.Context
import android.util.Log
import app.pluct.data.entity.VideoItem
import app.pluct.data.entity.Transcript
import app.pluct.data.entity.OutputArtifact
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.concurrent.ConcurrentHashMap
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Pluct-Search-Core-Engine - Simple search functionality
 * Follows naming convention: [Project]-[ParentScope]-[ChildScope]-[CoreResponsibility]
 * Simplified to remove complex processing
 */
@Singleton
class PluctSearchCoreEngine @Inject constructor(
    private val context: Context
) {
    companion object {
        private const val TAG = "PluctSearchCore"
    }

    private val searchIndex = ConcurrentHashMap<String, SearchableContent>()
    private val userPreferences = ConcurrentHashMap<String, UserPreference>()

    /**
     * Index content for search
     */
    suspend fun indexContent(
        video: VideoItem,
        transcript: Transcript?,
        artifacts: List<OutputArtifact>
    ) = withContext(Dispatchers.IO) {
        try {
            Log.d(TAG, "Indexing content for video: ${video.id}")
            
            val searchableContent = SearchableContent(
                videoId = video.id,
                title = video.title ?: "",
                description = video.description ?: "",
                author = video.author ?: "",
                transcript = transcript?.text ?: "",
                artifacts = artifacts,
                tags = extractTags(video, transcript),
                categories = extractCategories(video, transcript),
                sentiment = "neutral",
                createdAt = video.createdAt,
                updatedAt = System.currentTimeMillis()
            )
            
            searchIndex[video.id] = searchableContent
            Log.d(TAG, "Content indexed successfully for video: ${video.id}")
        } catch (e: Exception) {
            Log.e(TAG, "Error indexing content: ${e.message}", e)
        }
    }

    /**
     * Search content with query and filters
     */
    suspend fun searchContent(
        query: String,
        filters: SearchFilters = SearchFilters(),
        userId: String? = null
    ): SearchResult = withContext(Dispatchers.IO) {
        try {
            Log.d(TAG, "Searching content with query: '$query'")
            
            val queryTerms = query.lowercase().split(" ").filter { it.isNotBlank() }
            val searchableContents = searchIndex.values.toList()
            
            val matchingContents = searchableContents.mapNotNull { content ->
                val relevanceScore = calculateRelevanceScore(content, queryTerms, filters)
                if (relevanceScore > 0) {
                    SearchResultItem(content, relevanceScore)
                } else {
                    null
                }
            }.sortedByDescending { it.relevanceScore }
            
            val recommendations = generateRecommendations(query, userId)
            val trendingTopics = getTrendingTopics()
            
            SearchResult(
                query = query,
                results = matchingContents,
                recommendations = recommendations,
                trendingTopics = trendingTopics,
                totalResults = matchingContents.size
            )
        } catch (e: Exception) {
            Log.e(TAG, "Error searching content: ${e.message}", e)
            SearchResult(
                query = query,
                results = emptyList(),
                recommendations = emptyList(),
                trendingTopics = emptyList(),
                totalResults = 0
            )
        }
    }

    /**
     * Get search suggestions based on query
     */
    suspend fun getSearchSuggestions(query: String): List<String> = withContext(Dispatchers.IO) {
        try {
            val queryLower = query.lowercase()
            val suggestions = mutableSetOf<String>()
            
            searchIndex.values.forEach { content ->
                // Title suggestions
                if (content.title.lowercase().contains(queryLower)) {
                    suggestions.add(content.title)
                }
                
                // Tag suggestions
                content.tags.forEach { tag ->
                    if (tag.lowercase().contains(queryLower)) {
                        suggestions.add(tag)
                    }
                }
                
                // Category suggestions
                content.categories.forEach { category ->
                    if (category.lowercase().contains(queryLower)) {
                        suggestions.add(category)
                    }
                }
            }
            
            suggestions.take(10).toList()
        } catch (e: Exception) {
            Log.e(TAG, "Error getting search suggestions: ${e.message}", e)
            emptyList()
        }
    }

    private fun extractTags(
        video: VideoItem,
        transcript: Transcript?
    ): List<String> {
        val tags = mutableListOf<String>()
        
        // Extract from video metadata
        video.tagsCsv?.split(",")?.forEach { tag ->
            tags.add(tag.trim())
        }
        
        // Extract from transcript text
        transcript?.text?.split(" ")?.take(10)?.forEach { word ->
            if (word.length > 3) tags.add(word.lowercase())
        }
        
        return tags.distinct()
    }

    private fun extractCategories(video: VideoItem, transcript: Transcript?): List<String> {
        return listOf("general") // Simple categorization
    }

    // Removed extractSentiment - using simple sentiment

    private fun calculateRelevanceScore(
        content: SearchableContent,
        queryTerms: List<String>,
        filters: SearchFilters
    ): Float {
        var score = 0f
        
        // Text matching
        val searchableText = "${content.title} ${content.description} ${content.author} ${content.transcript} ${content.tags.joinToString(" ")}".lowercase()
        queryTerms.forEach { term ->
            if (searchableText.contains(term)) {
                score += 1.0f
            }
        }
        
        // Apply filters
        if (filters.author != null && content.author.lowercase() != filters.author.lowercase()) {
            score *= 0.5f
        }
        
        if (filters.category != null && !content.categories.any { it.equals(filters.category, ignoreCase = true) }) {
            score *= 0.5f
        }
        
        if (filters.sentiment != null && content.sentiment.lowercase() != filters.sentiment.lowercase()) {
            score *= 0.5f
        }
        
        return score
    }

    private fun generateRecommendations(query: String, userId: String?): List<String> {
        // Simple recommendation logic - can be enhanced with ML
        return listOf(
            "Related to: $query",
            "Popular content",
            "Trending topics"
        )
    }

    private fun getTrendingTopics(): List<String> {
        // Simple trending logic - can be enhanced with analytics
        return listOf(
            "Technology",
            "Business",
            "Lifestyle",
            "Education"
        )
    }
}

/**
 * Search data classes
 */
data class SearchableContent(
    val videoId: String,
    val title: String,
    val description: String,
    val author: String,
    val transcript: String,
    val artifacts: List<OutputArtifact>,
    val tags: List<String>,
    val categories: List<String>,
    val sentiment: String,
    val createdAt: Long,
    val updatedAt: Long
)

data class SearchFilters(
    val author: String? = null,
    val category: String? = null,
    val sentiment: String? = null,
    val startDate: Long? = null,
    val endDate: Long? = null
)

data class SearchResult(
    val query: String,
    val results: List<SearchResultItem>,
    val recommendations: List<String>,
    val trendingTopics: List<String>,
    val totalResults: Int
)

data class SearchResultItem(
    val content: SearchableContent,
    val relevanceScore: Float
)

data class UserPreference(
    val userId: String,
    val preferredCategories: List<String>,
    val preferredAuthors: List<String>,
    val searchHistory: List<String>
)

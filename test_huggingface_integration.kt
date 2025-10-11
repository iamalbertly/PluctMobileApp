/**
 * Test script to verify Hugging Face transcription integration
 * This can be run to test the API connectivity and basic functionality
 */

import kotlinx.coroutines.runBlocking
import app.pluct.data.provider.HuggingFaceTranscriptionProvider

fun main() {
    runBlocking {
        val provider = HuggingFaceTranscriptionProvider()
        
        println("Testing Hugging Face Transcription Provider...")
        
        // Test 1: Health Check
        println("\n1. Testing health check...")
        val isHealthy = provider.checkHealth()
        println("Health check result: $isHealthy")
        
        if (!isHealthy) {
            println("❌ Service is not healthy. Please check the Hugging Face service.")
            return@runBlocking
        }
        
        // Test 2: Start transcription with a sample URL
        println("\n2. Testing transcription start...")
        val testUrl = "https://vm.tiktok.com/ZMA2jFqyJ" // Sample TikTok URL from README
        val startResponse = provider.startTranscription(testUrl)
        
        if (startResponse != null) {
            println("✅ Transcription started successfully")
            println("Job ID: ${startResponse.id ?: startResponse.job_id}")
            println("Status: ${startResponse.status}")
            println("Queue position: ${startResponse.queue_position}")
            println("Estimated wait: ${startResponse.estimated_wait_seconds}s")
        } else {
            println("❌ Failed to start transcription")
        }
        
        println("\n✅ Integration test completed!")
    }
}





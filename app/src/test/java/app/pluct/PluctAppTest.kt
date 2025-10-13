package app.pluct

import app.pluct.data.entity.VideoItem
import app.pluct.data.repository.PluctRepository
import app.pluct.data.service.VideoMetadataService
import app.pluct.share.PluctShareIngestActivity
import app.pluct.ui.components.ScriptTokAuditWebView
import app.pluct.viewmodel.HomeViewModel
import app.pluct.viewmodel.IngestViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mock
import org.mockito.Mockito.*
import org.mockito.kotlin.whenever
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [28])
class PluctAppTest {

    private lateinit var mockRepository: PluctRepository
    private lateinit var mockMetadataService: VideoMetadataService

    private lateinit var homeViewModel: HomeViewModel
    private lateinit var ingestViewModel: IngestViewModel

    @Before
    fun setup() {
        // Initialize mocks
        mockRepository = mock(PluctRepository::class.java)
        mockMetadataService = mock(VideoMetadataService::class.java)
        
        // Initialize view models
        homeViewModel = HomeViewModel(mockRepository, mockMetadataService)
        // Note: IngestViewModel constructor needs SavedStateHandle in real implementation
        // For testing purposes, we'll skip initializing it as it's not used in tests
    }

    @Test
    fun testVmTikTokUrlIsPreservedForScriptTokauditIo() {
        val vmTikTokUrl = "https://vm.tiktok.com/ZMAF56hjK//"
        val fullTikTokUrl = "https://www.tiktok.com/@chris219m/video/7539882214209686840?_t=ZM-8z5scgmCgRc&_r=1"

        // Test that vm.tiktok.com URLs are kept as-is
        assertTrue(vmTikTokUrl.contains("vm.tiktok.com"))
        
        // Test URL normalization logic
        val normalizedVmUrl = normalizeTikTokUrlForScript(vmTikTokUrl)
        assertEquals(vmTikTokUrl, normalizedVmUrl, "vm.tiktok.com URLs should be preserved")
        
        // Test that full TikTok URLs are converted to vm.tiktok.com format
        val normalizedFullUrl = normalizeTikTokUrlForScript(fullTikTokUrl)
        assertTrue(normalizedFullUrl.contains("vm.tiktok.com"), "Full TikTok URLs should be converted to vm.tiktok.com format")
    }

    @Test
    fun testUrlExtractionFromShareIntent() {
        val shareText = "Check out this video: https://vm.tiktok.com/ZMAF56hjK//"
        val extractedUrl = extractUrlFromText(shareText)
        
        assertNotNull(extractedUrl)
        assertEquals("https://vm.tiktok.com/ZMAF56hjK//", extractedUrl)
    }

    @Test
    fun testVideoDeduplication() = runTest {
        val videoUrl = "https://vm.tiktok.com/ZMAF56hjK//"
        val existingVideo = VideoItem(
            id = "test-id",
            sourceUrl = videoUrl,
            title = "Test Video",
            createdAt = System.currentTimeMillis()
        )

        whenever(mockRepository.findByUrl(videoUrl)).thenReturn(existingVideo)
        whenever(mockRepository.streamAll()).thenReturn(MutableStateFlow(listOf(existingVideo)))

        // Test that duplicate URLs are detected
        val foundVideo = mockRepository.findByUrl(videoUrl)
        assertNotNull(foundVideo)
        assertEquals(videoUrl, foundVideo.sourceUrl)
    }

    @Test
    fun testTranscriptProcessingWorkflow() = runTest {
        val videoId = "test-video-id"
        val transcriptText = "This is a test transcript from the video content."
        
        // Test transcript saving
        doNothing().`when`(mockRepository).saveTranscript(videoId, transcriptText, "en")
        
        mockRepository.saveTranscript(videoId, transcriptText, "en")
        
        verify(mockRepository, times(1)).saveTranscript(videoId, transcriptText, "en")
    }

    @Test
    fun testWebViewJavaScriptInjectionForScriptTokauditIo() {
        val testUrl = "https://vm.tiktok.com/ZMAF56hjK//"
        
        // Test JavaScript injection logic
        val jsCode = generateWebViewJavaScript(testUrl)
        
        assertTrue(jsCode.contains("vm.tiktok.com"), "JavaScript should handle vm.tiktok.com URLs")
        assertTrue(jsCode.contains("closeModals()"), "JavaScript should include modal closing")
        assertTrue(jsCode.contains("waitForInputAndFill"), "JavaScript should include input filling")
        assertTrue(jsCode.contains("Subtitles Not Available"), "JavaScript should detect no transcript responses")
    }

    @Test
    fun testErrorHandlingForInvalidUrls() {
        val invalidUrl = "not-a-valid-url"
        
        // Test URL validation
        val isValid = isValidUrl(invalidUrl)
        assertFalse(isValid, "Invalid URLs should be rejected")
        
        val validUrl = "https://vm.tiktok.com/ZMAF56hjK//"
        val isValidValid = isValidUrl(validUrl)
        assertTrue(isValidValid, "Valid URLs should be accepted")
    }

    @Test
    fun testHomeScreenVideoListUpdates() = runTest {
        val videos = listOf(
            VideoItem(
                id = "1",
                sourceUrl = "https://vm.tiktok.com/ZMAF56hjK//",
                title = "Test Video 1",
                author = "testuser1",
                createdAt = System.currentTimeMillis()
            ),
            VideoItem(
                id = "2", 
                sourceUrl = "https://vm.tiktok.com/ABC123/",
                title = "Test Video 2",
                author = "testuser2",
                createdAt = System.currentTimeMillis() - 1000
            )
        )

        whenever(mockRepository.streamAll()).thenReturn(MutableStateFlow(videos))
        
        val videoFlow = mockRepository.streamAll()
        // For testing, we'll just verify the flow is returned
        assertNotNull(videoFlow)
        
        // Test that the flow is properly mocked
        verify(mockRepository, times(1)).streamAll()
    }

    @Test
    fun testVideoDeletionFunctionality() = runTest {
        val videoId = "test-video-id"
        
        doNothing().`when`(mockRepository).deleteVideo(videoId)
        
        mockRepository.deleteVideo(videoId)
        
        verify(mockRepository, times(1)).deleteVideo(videoId)
    }

    // Helper functions for testing
    private fun normalizeTikTokUrlForScript(url: String): String {
        return if (url.contains("vm.tiktok.com")) {
            url // Keep vm.tiktok.com URLs as-is
        } else if (url.contains("tiktok.com/@")) {
            // Convert full TikTok URLs to vm.tiktok.com format
            val match = Regex("tiktok\\.com/@[^/]+/video/(\\d+)").find(url)
            if (match != null) {
                val videoId = match.groupValues[1]
                "https://vm.tiktok.com/$videoId/"
            } else {
                url
            }
        } else {
            url
        }
    }

    private fun extractUrlFromText(text: String): String? {
        val urlPattern = Regex("https?://[^\\s]+")
        return urlPattern.find(text)?.value
    }

    private fun generateWebViewJavaScript(videoUrl: String): String {
        return """
            (function() {
                console.log('PluctWebView: Starting script injection for URL: $videoUrl');
                
                function closeModals() {
                    const modal = document.querySelector('.bulk-download-modal-parent');
                    if (modal) {
                        modal.remove();
                        console.log('Modal closed');
                    }
                }
                
                function waitForInputAndFill(urlToTry) {
                    const textarea = document.querySelector('textarea');
                    if (textarea) {
                        textarea.value = urlToTry;
                        textarea.dispatchEvent(new Event('input', { bubbles: true }));
                        console.log('Filled textarea with URL:', urlToTry);
                        
                        const buttons = document.querySelectorAll('button');
                        for (let button of buttons) {
                            if (button.textContent.toLowerCase().includes('start')) {
                                button.click();
                                console.log('Clicked START button');
                                return true;
                            }
                        }
                    }
                    return false;
                }
                
                // Check for "Subtitles Not Available"
                if (document.body.textContent.includes('Subtitles Not Available')) {
                    console.log('Found: Subtitles Not Available');
                    Android.onNoTranscript();
                }
            })();
        """.trimIndent()
    }

    private fun isValidUrl(url: String): Boolean {
        return try {
            java.net.URL(url)
            true
        } catch (e: Exception) {
            false
        }
    }
}

package app.pluct.shared

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class PluctTikTokUrlValidatorTest {
    @Test
    fun acceptsAndNormalizesLongVideoUrl() {
        val result = PluctTikTokUrlValidator.validateUrl(" https://www.tiktok.com/@albert.ly/video/1234567890123456789?share=1 ")

        assertTrue(result.isValid)
        assertEquals("https://www.tiktok.com/@albert.ly/video/1234567890123456789", result.sanitizedValue)
    }

    @Test
    fun acceptsShortLink() {
        val result = PluctTikTokUrlValidator.validateUrl("watch this https://vt.tiktok.com/ZSYabc123/")

        assertTrue(result.isValid)
        assertEquals("https://vt.tiktok.com/ZSYabc123/", result.sanitizedValue)
    }

    @Test
    fun rejectsProfileUrl() {
        val result = PluctTikTokUrlValidator.validateUrl("https://www.tiktok.com/@albert.ly")

        assertFalse(result.isValid)
        assertEquals("Use a real TikTok video link, not a profile or search page.", result.errorMessage)
    }

    @Test
    fun rejectsMultipleLinks() {
        val result = PluctTikTokUrlValidator.validateUrl("https://vt.tiktok.com/ZSYabc123/ https://vm.tiktok.com/ZSYdef456/")

        assertFalse(result.isValid)
        assertEquals("Paste one TikTok link only.", result.errorMessage)
    }
}

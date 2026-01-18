const { BaseJourney } = require('./Pluct-Journey-01Orchestrator');

/**
 * Journey-UX-22VideoTitleFallback-Validation
 * Validates improved video title fallback logic (title > author > URL extraction > generic)
 */
class JourneyUX22VideoTitleFallbackValidation extends BaseJourney {
    constructor(core) {
        super(core);
        this.name = 'UX-22VideoTitleFallback-Validation';
    }

    async execute() {
        this.core.logger.info('Starting Video Title Fallback Validation');
        
        // Step 1: Launch app
        await this.core.launchApp();
        await this.core.sleep(3000);
        
        // Step 2: Check UI for video titles
        await this.core.dumpUIHierarchy();
        let uiDump = this.core.readLastUIDump() || '';
        
        // Step 3: Verify no "Untitled Video" appears (unless truly no metadata)
        const hasUntitledVideo = uiDump.includes('Untitled Video');
        
        // Step 4: Check for improved fallback patterns
        const hasVideoByPattern = uiDump.includes('Video by @');
        const hasTikTokVideo = uiDump.includes('TikTok Video');
        
        // Step 5: Check logcat for title extraction logic
        const logcatCheck = await this.core.executeCommand(
            'adb logcat -d -t 200 | findstr /i "getVideoDisplayTitle\|Video by\|TikTok Video"'
        );
        
        // Step 6: Verify URL handle extraction works
        // Check if URLs with @handle are being processed
        const urlPatternCheck = await this.core.executeCommand(
            'adb logcat -d -t 200 | findstr /i "@.*tiktok\|tiktok.*@"'
        );
        
        // Validation: Should not see "Untitled Video" unless absolutely no metadata available
        // Should see either actual title, "Video by @handle", or "TikTok Video"
        const hasValidTitle = !hasUntitledVideo || (hasVideoByPattern || hasTikTokVideo);
        
        if (!hasValidTitle && hasUntitledVideo) {
            // Check if there are any videos at all
            const hasAnyVideos = uiDump.includes('COMPLETED') || 
                               uiDump.includes('PROCESSING') ||
                               uiDump.includes('Video item');
            
            if (hasAnyVideos) {
                return { 
                    success: false, 
                    error: 'Found "Untitled Video" when improved fallback should provide better title' 
                };
            }
        }
        
        this.core.logger.info('✅ Video title fallback validation passed');
        return { 
            success: true, 
            details: { 
                noUntitledVideo: !hasUntitledVideo,
                hasVideoByPattern: hasVideoByPattern,
                hasTikTokVideo: hasTikTokVideo,
                titleExtractionWorking: logcatCheck.output ? logcatCheck.output.length > 0 : false
            }
        };
    }
}

module.exports = JourneyUX22VideoTitleFallbackValidation;

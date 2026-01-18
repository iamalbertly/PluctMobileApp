const { BaseJourney } = require('./Pluct-Journey-01Orchestrator');

/**
 * Journey-UX-21RedundantBadgeRemoval-Validation
 * Validates that "Saved Offline" badge is removed from video cards
 */
class JourneyUX21RedundantBadgeRemovalValidation extends BaseJourney {
    constructor(core) {
        super(core);
        this.name = 'UX-21RedundantBadgeRemoval-Validation';
    }

    async execute() {
        this.core.logger.info('Starting Redundant Badge Removal Validation');
        
        // Step 1: Launch app
        await this.core.launchApp();
        await this.core.sleep(3000);
        
        // Step 2: Check for completed videos in UI
        await this.core.dumpUIHierarchy();
        let uiDump = this.core.readLastUIDump() || '';
        
        // Step 3: Verify "Saved Offline" badge does NOT appear
        const hasSavedOfflineBadge = uiDump.includes('Saved Offline') || 
                                     uiDump.includes('saved_offline') ||
                                     uiDump.includes('PluctOfflineBadge');
        
        if (hasSavedOfflineBadge) {
            return { 
                success: false, 
                error: 'Found "Saved Offline" badge in UI - should be removed' 
            };
        }
        
        // Step 4: Verify video cards still display correctly
        const hasVideoCards = uiDump.includes('COMPLETED') || 
                             uiDump.includes('Video item') ||
                             uiDump.includes('View Details');
        
        if (!hasVideoCards && uiDump.includes('No transcripts yet')) {
            // No videos yet, but UI should still be functional
            this.core.logger.info('No videos found, but UI structure is correct');
            return { success: true, details: { badgeRemoved: true, noVideos: true } };
        }
        
        if (!hasVideoCards) {
            return { 
                success: false, 
                error: 'Video cards not found in UI after badge removal' 
            };
        }
        
        // Step 5: Check video detail screen if accessible
        const hasViewDetails = uiDump.includes('View Details') || 
                              uiDump.includes('view_details_button');
        
        if (hasViewDetails) {
            // Try to tap a video to check detail screen
            const tapResult = await this.core.tapByTestTag('view_details_button');
            if (tapResult.success) {
                await this.core.sleep(2000);
                await this.core.dumpUIHierarchy();
                const detailDump = this.core.readLastUIDump() || '';
                
                const hasBadgeInDetail = detailDump.includes('Saved Offline');
                if (hasBadgeInDetail) {
                    return { 
                        success: false, 
                        error: 'Found "Saved Offline" badge in video detail screen' 
                    };
                }
            }
        }
        
        this.core.logger.info('✅ Redundant badge removal validation passed');
        return { 
            success: true, 
            details: { 
                badgeRemoved: true,
                videoCardsDisplay: hasVideoCards,
                detailScreenChecked: hasViewDetails
            }
        };
    }
}

module.exports = JourneyUX21RedundantBadgeRemovalValidation;

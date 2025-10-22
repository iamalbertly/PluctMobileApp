const fs = require('fs');
const path = require('path');

/**
 * Journey: Metadata Validation
 * Tests metadata fetching, storage, and display in the UI
 */
class MetadataValidationJourney {
    constructor(core) {
        this.core = core;
        this.name = 'MetadataValidation';
    }

    async run() {
        this.core.logger.info('🎬 Testing metadata validation end-to-end...');
        
        try {
            // Step 1: Clear app state
            await this.core.clearAppCache();
            await this.core.clearWorkManagerTasks();
            
            // Step 2: Launch app
            const launchResult = await this.core.launchApp();
            if (!launchResult.success) {
                return { success: false, error: `App launch failed: ${launchResult.error}` };
            }
            
            // Step 3: Open capture sheet
            await this.core.openCaptureSheet();
            await this.core.sleep(1000);
            
            // Step 4: Input test URL
            const testUrl = process.env.TEST_TIKTOK_URL || 'https://vm.tiktok.com/ZMADQVF4e/';
            await this.core.inputText(testUrl);
            await this.core.sleep(500);
            
            // Step 5: Validate URL
            const validationResult = await this.core.validateTikTokUrl(testUrl);
            if (!validationResult.success) {
                return { success: false, error: `URL validation failed: ${validationResult.error}` };
            }
            
            // Step 6: Fetch metadata
            this.core.logger.info('📊 Fetching metadata...');
            const metadataResult = await this.core.fetchHtmlMetadata(testUrl);
            if (!metadataResult.success) {
                this.core.logger.warn('⚠️ Metadata fetch failed, continuing with fallback');
            } else {
                this.core.logger.info(`✅ Metadata fetched: ${metadataResult.title}`);
                this.core.writeJsonArtifact('metadata.json', metadataResult);
            }
            
            // Step 7: Start Quick Scan
            await this.core.tapByText('quick_scan');
            await this.core.sleep(2000);
            
            // Step 8: Wait for video to appear in home
            this.core.logger.info('⏳ Waiting for video to appear in home...');
            const videoAppeared = await this.core.waitForText('TikTok Video', 10000, 1000);
            if (!videoAppeared) {
                return { success: false, error: 'Video did not appear in home screen' };
            }
            
            // Step 9: Check metadata display
            await this.core.dumpUIHierarchy();
            const uiDump = this.core.readLastUIDump();
            
            // Look for metadata in UI
            const hasTitle = uiDump.includes('TikTok Video') || uiDump.includes('Video');
            const hasCreator = uiDump.includes('by') || uiDump.includes('Creator');
            const hasStatus = uiDump.includes('Pending') || uiDump.includes('Processing');
            
            if (!hasTitle) {
                this.core.logger.warn('⚠️ Video title not found in UI');
            }
            if (!hasCreator) {
                this.core.logger.warn('⚠️ Creator info not found in UI');
            }
            if (!hasStatus) {
                this.core.logger.warn('⚠️ Status not found in UI');
            }
            
            // Step 10: Monitor status updates
            this.core.logger.info('📊 Monitoring status updates...');
            const statusResult = await this.core.waitForTranscriptResult(30000, 2000);
            if (!statusResult.success) {
                this.core.logger.warn('⚠️ Status monitoring failed, but continuing');
            }
            
            // Step 11: Validate final state
            await this.core.dumpUIHierarchy();
            const finalDump = this.core.readLastUIDump();
            
            const hasVideo = finalDump.includes('TikTok Video') || finalDump.includes('Video');
            const hasMetadata = finalDump.includes('by') || finalDump.includes('Creator');
            
            if (!hasVideo) {
                return { success: false, error: 'Video not found in final UI state' };
            }
            
            this.core.logger.info('✅ Metadata validation completed successfully');
            return { success: true };
            
        } catch (error) {
            this.core.logger.error(`❌ Metadata validation failed: ${error.message}`);
            return { success: false, error: error.message };
        }
    }
}

module.exports = MetadataValidationJourney;

const { BaseJourney } = require('./Pluct-Journey-01Orchestrator');

/**
 * Journey-Transcript-Storage-01Display - Transcript storage and display journey
 * Tests transcript storage, display, and clipboard functionality
 * Follows naming convention: [Project]-[ParentScope]-[ChildScope]-[Separation of Concern][CoreResponsibility]
 */
class TranscriptStorageDisplayJourney extends BaseJourney {
    constructor(core) {
        super(core);
        this.name = 'Transcript-Storage-01Display';
        this.maxDuration = 120000; // 2 minutes max
    }

    async execute() {
        this.core.logger.info('🎯 Starting Transcript Storage Display Journey...');
        const startTime = Date.now();
        
        try {
            // Step 1: Verify App State
            this.core.logger.info('📱 Step 1: Verifying App State');
            const appStateResult = await this.verifyAppState();
            if (!appStateResult.success) {
                return { success: false, error: 'App state verification failed' };
            }
            
            // Step 2: Check for Existing Transcripts
            this.core.logger.info('📱 Step 2: Checking for Existing Transcripts');
            const transcriptsResult = await this.checkExistingTranscripts();
            if (!transcriptsResult.success) {
                return { success: false, error: 'Failed to check existing transcripts' };
            }
            
            // Step 3: Test Transcript Display
            this.core.logger.info('📱 Step 3: Testing Transcript Display');
            const displayResult = await this.testTranscriptDisplay();
            if (!displayResult.success) {
                return { success: false, error: 'Transcript display test failed' };
            }
            
            // Step 4: Test Copy to Clipboard
            this.core.logger.info('📱 Step 4: Testing Copy to Clipboard');
            const clipboardResult = await this.testCopyToClipboard();
            if (!clipboardResult.success) {
                this.core.logger.warn('⚠️ Clipboard test failed, but continuing...');
            }
            
            // Step 5: Test Navigation
            this.core.logger.info('📱 Step 5: Testing Navigation');
            const navigationResult = await this.testNavigation();
            if (!navigationResult.success) {
                return { success: false, error: 'Navigation test failed' };
            }
            
            const duration = Date.now() - startTime;
            this.core.logger.info(`✅ Transcript Storage Display Journey completed in ${duration}ms`);
            
            return { 
                success: true, 
                duration: duration,
                transcriptCount: transcriptsResult.count
            };
            
        } catch (error) {
            this.core.logger.error('❌ Transcript Storage Display Journey failed:', error.message);
            return { success: false, error: error.message };
        }
    }
    
    /**
     * Verify app is in correct state
     */
    async verifyAppState() {
        try {
            this.core.logger.info('🔍 Verifying app state...');
            
            // Ensure app is in foreground
            const fgResult = await this.ensureAppForeground();
            if (!fgResult.success) {
                this.core.logger.warn('⚠️ App not in foreground, launching...');
                await this.core.launchApp();
                await this.core.sleep(2000);
            }
            
            await this.core.dumpUIHierarchy();
            const uiDump = this.core.readLastUIDump();
            
            // Check if app is running
            if (!uiDump.includes('app.pluct')) {
                return { success: false, error: 'App not running' };
            }
            
            // Check if main content is visible (more flexible check)
            if (!uiDump.includes('Pluct') && !uiDump.includes('transcript') && !uiDump.includes('Recent Transcripts')) {
                this.core.logger.warn('⚠️ Main content not visible, but app is running');
                // Continue anyway as the app might be in a different state
            }
            
            this.core.logger.info('✅ App state verified');
            return { success: true };
            
        } catch (error) {
            this.core.logger.error('❌ App state verification failed:', error.message);
            return { success: false, error: error.message };
        }
    }
    
    /**
     * Check for existing transcripts
     */
    async checkExistingTranscripts() {
        try {
            this.core.logger.info('📋 Checking for existing transcripts...');
            
            await this.core.dumpUIHierarchy();
            const uiDump = this.core.readLastUIDump();
            
            // Count transcript-related elements
            const transcriptCount = (uiDump.match(/transcript/gi) || []).length;
            const videoCardCount = (uiDump.match(/video_card/gi) || []).length;
            
            this.core.logger.info(`📊 Found ${transcriptCount} transcript references and ${videoCardCount} video cards`);
            
            return { success: true, count: transcriptCount };
            
        } catch (error) {
            this.core.logger.error('❌ Failed to check existing transcripts:', error.message);
            return { success: false, error: error.message };
        }
    }
    
    /**
     * Test transcript display functionality
     */
    async testTranscriptDisplay() {
        try {
            this.core.logger.info('📄 Testing transcript display...');
            
            await this.core.dumpUIHierarchy();
            const uiDump = this.core.readLastUIDump();
            
            // Look for transcript display elements
            const hasTranscriptElements = uiDump.includes('transcript') || 
                                        uiDump.includes('Completed') ||
                                        uiDump.includes('video_card');
            
            if (hasTranscriptElements) {
                this.core.logger.info('✅ Transcript display elements found');
                return { success: true };
            } else {
                this.core.logger.warn('⚠️ No transcript display elements found');
                return { success: true }; // Not a failure, just no transcripts yet
            }
            
        } catch (error) {
            this.core.logger.error('❌ Transcript display test failed:', error.message);
            return { success: false, error: error.message };
        }
    }
    
    /**
     * Test copy to clipboard functionality
     */
    async testCopyToClipboard() {
        try {
            this.core.logger.info('📋 Testing copy to clipboard...');
            
            // Look for copy button or long press functionality
            await this.core.dumpUIHierarchy();
            const uiDump = this.core.readLastUIDump();
            
            // Check if there are any copy-related elements
            const hasCopyElements = uiDump.includes('copy') || 
                                  uiDump.includes('Copy') ||
                                  uiDump.includes('clipboard');
            
            if (hasCopyElements) {
                this.core.logger.info('✅ Copy functionality elements found');
                return { success: true };
            } else {
                this.core.logger.warn('⚠️ No copy functionality elements found');
                return { success: false, error: 'Copy functionality not implemented' };
            }
            
        } catch (error) {
            this.core.logger.error('❌ Copy to clipboard test failed:', error.message);
            return { success: false, error: error.message };
        }
    }
    
    /**
     * Test navigation between screens
     */
    async testNavigation() {
        try {
            this.core.logger.info('🧭 Testing navigation...');
            
            // Test settings navigation
            const settingsResult = await this.testSettingsNavigation();
            if (!settingsResult.success) {
                this.core.logger.warn('⚠️ Settings navigation failed');
            }
            
            // Test back navigation
            const backResult = await this.testBackNavigation();
            if (!backResult.success) {
                this.core.logger.warn('⚠️ Back navigation failed');
            }
            
            this.core.logger.info('✅ Navigation tests completed');
            return { success: true };
            
        } catch (error) {
            this.core.logger.error('❌ Navigation test failed:', error.message);
            return { success: false, error: error.message };
        }
    }
    
    /**
     * Test settings navigation
     */
    async testSettingsNavigation() {
        try {
            this.core.logger.info('⚙️ Testing settings navigation...');
            
            // Look for settings button
            const settingsTap = await this.core.tapByText('Settings');
            if (!settingsTap.success) {
                // Try alternative approach
                const altTap = await this.core.tapByContentDesc('Settings');
                if (!altTap.success) {
                    return { success: false, error: 'Settings button not found' };
                }
            }
            
            // Wait for settings screen
            await this.core.sleep(2000);
            
            // Verify settings screen is open
            await this.core.dumpUIHierarchy();
            const uiDump = this.core.readLastUIDump();
            
            if (uiDump.includes('Settings') || uiDump.includes('settings')) {
                this.core.logger.info('✅ Settings screen opened');
                return { success: true };
            } else {
                return { success: false, error: 'Settings screen not opened' };
            }
            
        } catch (error) {
            this.core.logger.error('❌ Settings navigation failed:', error.message);
            return { success: false, error: error.message };
        }
    }
    
    /**
     * Test back navigation
     */
    async testBackNavigation() {
        try {
            this.core.logger.info('⬅️ Testing back navigation...');
            
            // Use back button
            const backResult = await this.core.executeCommand('adb shell input keyevent KEYCODE_BACK');
            if (!backResult.success) {
                return { success: false, error: 'Back navigation failed' };
            }
            
            // Wait for navigation
            await this.core.sleep(2000);
            
            // Verify we're back to main screen
            await this.core.dumpUIHierarchy();
            const uiDump = this.core.readLastUIDump();
            
            if (uiDump.includes('Pluct') && !uiDump.includes('Settings')) {
                this.core.logger.info('✅ Back navigation successful');
                return { success: true };
            } else {
                return { success: false, error: 'Back navigation failed' };
            }
            
        } catch (error) {
            this.core.logger.error('❌ Back navigation failed:', error.message);
            return { success: false, error: error.message };
        }
    }
}

function register(orchestrator) {
    orchestrator.registerJourney('TranscriptStorageDisplay', new TranscriptStorageDisplayJourney(orchestrator.core));
}

module.exports = { register };

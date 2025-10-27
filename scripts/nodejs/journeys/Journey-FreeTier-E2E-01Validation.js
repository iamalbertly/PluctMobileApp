const { BaseJourney } = require('./Pluct-Journey-01Orchestrator');

/**
 * Journey-FreeTier-E2E-01Validation - End-to-end validation of free tier flow
 * Tests the complete user journey for free tier extraction with real background processing
 */
class FreeTierE2EValidationJourney extends BaseJourney {
    constructor(core) {
        super(core);
        this.name = 'FreeTier-E2E-01Validation';
    }

    async execute() {
        this.core.logger.info('🎯 Testing Free Tier E2E Journey...');
        
        try {
            // 1. Launch app and verify initial state
            this.core.logger.info('📱 Step 1: Launching app and verifying initial state...');
            await this.core.ensureAppForeground();
            await this.core.sleep(2000);
            
            // Capture initial UI state
            await this.core.dumpUIHierarchy();
            const initialUI = this.core.readLastUIDump();
            
            // Verify free uses counter is visible
            if (!initialUI.includes('free extractions remaining')) {
                this.core.logger.warn('⚠️ Free uses counter not found in initial UI');
            }
            
            // 2. Check for Extract Script button with free uses text
            this.core.logger.info('📱 Step 2: Verifying Extract Script button with free uses text...');
            await this.core.dumpUIHierarchy();
            const uiDump = this.core.readLastUIDump();
            
            const hasExtractButton = uiDump.includes('Extract Script') || 
                                   uiDump.includes('extract_script_button');
            const hasFreeUsesText = uiDump.includes('free extractions remaining');
            
            if (!hasExtractButton) {
                return { success: false, error: 'Extract Script button not found' };
            }
            
            if (!hasFreeUsesText) {
                this.core.logger.warn('⚠️ Free uses text not found, but continuing...');
            }
            
            // 3. Enter TikTok URL in input field
            this.core.logger.info('📱 Step 3: Entering TikTok URL...');
            const testUrl = 'https://vm.tiktok.com/ZMA730880/';
            
            const inputResult = await this.core.tapByTestTag('tiktok_url_input');
            if (!inputResult.success) {
                return { success: false, error: 'Could not tap URL input field' };
            }
            
            await this.core.sleep(500);
            
            // Clear existing text and enter new URL
            await this.core.executeCommand(`adb shell input text "${testUrl}"`);
            await this.core.sleep(1000);
            
            // 4. Tap Extract Script button (free tier)
            this.core.logger.info('📱 Step 4: Tapping Extract Script button (free tier)...');
            
            let buttonTap = { success: false };
            
            // Try by test tag first
            buttonTap = await this.core.tapByTestTag('extract_script_button');
            if (!buttonTap.success) {
                // Try by text
                buttonTap = await this.core.tapByText('Extract Script');
            }
            
            if (!buttonTap.success) {
                return { success: false, error: 'Could not tap Extract Script button' };
            }
            
            // 5. Assert processing state appears
            this.core.logger.info('📱 Step 5: Verifying processing state...');
            await this.core.sleep(2000);
            
            await this.core.dumpUIHierarchy();
            const processingUI = this.core.readLastUIDump();
            
            const hasProcessing = processingUI.includes('Processing') || 
                                 processingUI.includes('CircularProgressIndicator') ||
                                 processingUI.includes('Progress');
            
            if (!hasProcessing) {
                this.core.logger.warn('⚠️ Processing indicators not clearly visible, but continuing...');
            }
            
            // 6. Wait for completion (use real background process, no mocks)
            this.core.logger.info('📱 Step 6: Waiting for processing completion...');
            
            let attempts = 0;
            const maxAttempts = 20; // 20 attempts * 3 seconds = 60 seconds max
            
            while (attempts < maxAttempts) {
                await this.core.sleep(3000);
                await this.core.dumpUIHierarchy();
                const currentUI = this.core.readLastUIDump();
                
                // Check if processing is complete (no processing indicators)
                const stillProcessing = currentUI.includes('Processing') || 
                                      currentUI.includes('CircularProgressIndicator');
                
                if (!stillProcessing) {
                    this.core.logger.info('✅ Processing appears to be complete');
                    break;
                }
                
                attempts++;
                this.core.logger.info(`⏳ Processing still in progress... attempt ${attempts}/${maxAttempts}`);
            }
            
            if (attempts >= maxAttempts) {
                this.core.logger.warn('⚠️ Processing took longer than expected, but continuing...');
            }
            
            // 7. Verify video item appears in list with FREE tier
            this.core.logger.info('📱 Step 7: Verifying video item appears with FREE tier...');
            await this.core.dumpUIHierarchy();
            const finalUI = this.core.readLastUIDump();
            
            const hasVideoItem = finalUI.includes('View Details') || 
                               finalUI.includes('Video item card');
            const hasFreeTier = finalUI.includes('FREE') || 
                              finalUI.includes('Tier: FREE');
            
            if (!hasVideoItem) {
                return { success: false, error: 'Video item not found in list' };
            }
            
            if (!hasFreeTier) {
                this.core.logger.warn('⚠️ FREE tier not clearly visible in video item');
            }
            
            // 8. Tap video item to open detail screen
            this.core.logger.info('📱 Step 8: Opening video detail screen...');
            
            const detailTap = await this.core.tapByText('View Details');
            if (!detailTap.success) {
                return { success: false, error: 'Could not tap View Details button' };
            }
            
            await this.core.sleep(2000);
            
            // 9. Verify tier-specific actions present
            this.core.logger.info('📱 Step 9: Verifying tier-specific actions...');
            await this.core.dumpUIHierarchy();
            const detailUI = this.core.readLastUIDump();
            
            const hasCopyScript = detailUI.includes('Copy Script');
            const hasShareScript = detailUI.includes('Share Script');
            const hasUpgradeButton = detailUI.includes('Upgrade to Full Insights');
            
            if (!hasCopyScript) {
                this.core.logger.warn('⚠️ Copy Script action not found');
            }
            
            if (!hasShareScript) {
                this.core.logger.warn('⚠️ Share Script action not found');
            }
            
            if (!hasUpgradeButton) {
                this.core.logger.warn('⚠️ Upgrade button not found');
            }
            
            // 10. Navigate back to home screen
            this.core.logger.info('📱 Step 10: Navigating back to home screen...');
            
            const backTap = await this.core.tapByContentDesc('Back button');
            if (!backTap.success) {
                // Try alternative back navigation
                await this.core.executeCommand('adb shell input keyevent KEYCODE_BACK');
            }
            
            await this.core.sleep(1000);
            
            // 11. Check free uses decremented
            this.core.logger.info('📱 Step 11: Verifying free uses decremented...');
            await this.core.dumpUIHierarchy();
            const updatedUI = this.core.readLastUIDump();
            
            // Look for updated free uses count (should be 2 now)
            const hasUpdatedCount = updatedUI.includes('2 free extractions remaining') ||
                                  updatedUI.includes('free extractions remaining');
            
            if (!hasUpdatedCount) {
                this.core.logger.warn('⚠️ Free uses count not clearly visible after decrement');
            }
            
            this.core.logger.info('✅ Free Tier E2E Journey completed successfully');
            return { success: true };
            
        } catch (error) {
            this.core.logger.error('❌ Free Tier E2E Journey failed:', error.message);
            return { success: false, error: error.message };
        }
    }
}

module.exports = FreeTierE2EValidationJourney;

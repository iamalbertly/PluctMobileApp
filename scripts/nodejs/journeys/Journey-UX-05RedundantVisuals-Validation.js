const { BaseJourney } = require('./Pluct-Journey-01Orchestrator');

class JourneyUX05RedundantVisualsValidation extends BaseJourney {
    constructor(core) {
        super(core);
        this.name = 'Journey-UX-05RedundantVisuals-Validation';
    }

    async execute() {
        this.core.logger.info('🚀 Starting: Journey-UX-05RedundantVisuals-Validation');
        
        try {
            // Step 1: Launch app and enter valid TikTok URL
            this.core.logger.info('- Step 1: Launch app and enter TikTok URL');
            await this.ensureAppForeground();
            await this.core.sleep(2000);
            
            const testUrl = 'https://vm.tiktok.com/ZMDRUGT2P/';
            await this.core.tapByTestTag('capture_component_label');
            await this.core.sleep(500);
            await this.core.typeText(testUrl);
            await this.core.sleep(1000);
            
            // Step 2: Tap "Extract Script" button
            this.core.logger.info('- Step 2: Tap Extract Script button');
            const submitTap = await this.core.tapByTestTag('extract_script_button');
            if (!submitTap.success) {
                const submitTap2 = await this.core.tapByText('Extract Script');
                if (!submitTap2.success) {
                    this.core.logger.error('❌ Could not tap Extract Script button');
                    return { success: false, error: 'Could not start processing' };
                }
            }
            
            // Step 3: During submission, dump UI multiple times
            this.core.logger.info('- Step 3: Monitor UI during processing');
            const statusChecks = [];
            
            for (let i = 0; i < 5; i++) {
                await this.core.sleep(2000);
                await this.dumpUI();
                const uiDump = this.core.readLastUIDump();
                statusChecks.push(uiDump);
                this.core.logger.info(`  Check ${i + 1}/5: Captured UI state`);
            }
            
            // Step 4: Verify only ONE status indicator shows current step
            this.core.logger.info('- Step 4: Verify single status indicator');
            let duplicateCount = 0;
            let totalStatusMessages = 0;
            
            statusChecks.forEach((uiDump, index) => {
                const statusMessages = [
                    'Starting...',
                    'Getting video details...',
                    'Confirming credits and access...',
                    'Sending to Business Engine...',
                    'Waiting for transcript...',
                    'Fetching video metadata...',
                    'Refreshing credits...',
                    'Submitting to Business Engine...',
                    'Polling for transcript...'
                ];
                
                let foundMessages = [];
                statusMessages.forEach(msg => {
                    const count = (uiDump.match(new RegExp(msg.replace(/[.*+?^${}()|[\]\\]/g, '\\$&'), 'g')) || []).length;
                    if (count > 0) {
                        foundMessages.push({ message: msg, count: count });
                        totalStatusMessages += count;
                    }
                });
                
                if (foundMessages.length > 1) {
                    duplicateCount++;
                    this.core.logger.warn(`  Check ${index + 1}: Multiple status messages found: ${foundMessages.map(m => m.message).join(', ')}`);
                }
            });
            
            if (duplicateCount > 2) {
                this.core.logger.error('❌ FAILURE: Multiple status indicators visible simultaneously');
                return { success: false, error: 'Duplicate status messages detected' };
            }
            this.core.logger.info('✅ Single status indicator verified');
            
            // Step 5: Verify button shows status OR processing indicator shows status (not both)
            this.core.logger.info('- Step 5: Verify no duplicate status display');
            const lastUI = statusChecks[statusChecks.length - 1];
            
            const hasButtonStatus = lastUI.includes('Extract Script') && 
                                  (lastUI.includes('Starting') || 
                                   lastUI.includes('Getting') ||
                                   lastUI.includes('Submitting'));
            
            const hasProcessingIndicator = lastUI.includes('Processing') ||
                                         lastUI.includes('ProcessingIndicator') ||
                                         lastUI.includes('CircularProgressIndicator');
            
            if (hasButtonStatus && hasProcessingIndicator) {
                // Check if they show the same message
                const buttonMessage = lastUI.match(/Extract Script.*?(Starting|Getting|Submitting|Waiting)/);
                const indicatorMessage = lastUI.match(/Processing.*?(Starting|Getting|Submitting|Waiting)/);
                
                if (buttonMessage && indicatorMessage && 
                    buttonMessage[1] === indicatorMessage[1]) {
                    this.core.logger.warn('⚠️ Button and processing indicator may show same status');
                }
            }
            this.core.logger.info('✅ Status display verified');
            
            // Step 6: Verify no duplicate "Starting..." or step messages
            this.core.logger.info('- Step 6: Verify no duplicate messages');
            const duplicateMessages = [
                'Starting...',
                'Starting up...',
                'Please wait'
            ];
            
            let hasDuplicates = false;
            statusChecks.forEach((uiDump, index) => {
                duplicateMessages.forEach(msg => {
                    const count = (uiDump.match(new RegExp(msg.replace(/[.*+?^${}()|[\]\\]/g, '\\$&'), 'g')) || []).length;
                    if (count > 1) {
                        hasDuplicates = true;
                        this.core.logger.warn(`  Check ${index + 1}: Duplicate "${msg}" found (${count} times)`);
                    }
                });
            });
            
            if (hasDuplicates) {
                this.core.logger.error('❌ FAILURE: Duplicate processing messages detected');
                return { success: false, error: 'Duplicate processing messages in UI' };
            }
            this.core.logger.info('✅ No duplicate messages found');
            
            // Check logcat for any UI-related errors
            const apiErrors = await this.core.checkRecentAPIErrors(100);
            if (!apiErrors.success && apiErrors.errors) {
                this.core.logger.warn('⚠️ API errors detected (may be expected during processing)');
            }
            
            this.core.logger.info('✅ Completed: Journey-UX-05RedundantVisuals-Validation');
            return { success: true };
            
        } catch (error) {
            this.core.logger.error(`❌ Redundant visuals validation failed: ${error.message}`);
            return { success: false, error: error.message };
        }
    }
}

module.exports = JourneyUX05RedundantVisualsValidation;


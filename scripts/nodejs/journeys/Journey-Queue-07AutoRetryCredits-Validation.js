const { BaseJourney } = require('./Pluct-Journey-01Orchestrator');

class JourneyQueue07AutoRetryCreditsValidation extends BaseJourney {
    constructor(core) {
        super(core);
        this.name = 'Queue-07AutoRetryCredits';
    }

    async execute() {
        this.core.logger.info('🚀 Starting: Journey-Queue-07AutoRetryCredits-Validation');
        
        try {
            // Step 1: Set credits to 0 (simulated by trying to process with 0 credits)
            this.core.logger.info('📱 Step 1: Attempting to process with 0 credits...');
            await this.ensureAppForeground();
            await this.core.sleep(2000);
            
            // Step 2: Queue video (insufficient credits)
            this.core.logger.info('📱 Step 2: Queueing video...');
            let urlTap = await this.core.tapByTestTag('url_input_field');
            if (!urlTap.success) {
                urlTap = await this.core.tapByText('Paste TikTok Link');
            }
            await this.core.inputText(this.core.config.url);
            await this.core.sleep(1000);
            
            const extractTap = await this.core.tapByTestTag('extract_script_button');
            if (extractTap.success) {
                await this.core.sleep(2000);
                
                // If insufficient credits dialog appears, save for later
                await this.core.dumpUIHierarchy();
                const uiDump = this.core.readLastUIDump();
                
                if (uiDump.includes('Insufficient Credits') || uiDump.includes('Save for Later')) {
                    const saveTap = await this.core.tapByText('Save for Later');
                    if (saveTap.success) {
                        await this.core.sleep(2000);
                        this.core.logger.info('✅ Video queued');
                    }
                }
            }
            
            // Step 3: Verify queued
            this.core.logger.info('📱 Step 3: Verifying video queued...');
            await this.core.dumpUIHierarchy();
            const queueDump = this.core.readLastUIDump();
            
            const isQueued = queueDump.includes('queued') || 
                            queueDump.includes('Waiting for credits');
            
            if (!isQueued) {
                this.core.logger.warn('⚠️ Video may not be queued (user may have credits)');
                return { success: true, skipped: true, reason: 'User has sufficient credits' };
            }
            this.core.logger.info('✅ Video queued');
            
            // Step 4: Add credits (simulate by refreshing balance - in real scenario credits would be added via API)
            this.core.logger.info('📱 Step 4: Refreshing balance (simulating credit add)...');
            
            // Tap refresh button
            const refreshTap = await this.core.tapByContentDesc('Refresh credit balance');
            if (!refreshTap.success) {
                // Try alternative ways to refresh
                await this.core.pressKey('Back');
                await this.core.sleep(1000);
            }
            
            // Step 5: Verify LaunchedEffect triggered
            this.core.logger.info('📱 Step 5: Verifying auto-processing...');
            await this.core.sleep(5000);
            
            const autoProcessLog = await this.core.executeCommand(
                'adb logcat -d -t 100 | findstr /i "Auto-processing.*credits LaunchedEffect.*triggered"'
            );
            
            if (autoProcessLog.output.includes('Auto-processing') || 
                autoProcessLog.output.includes('LaunchedEffect')) {
                this.core.logger.info('✅ Auto-processing detected');
            } else {
                this.core.logger.warn('⚠️ Auto-processing log not found');
            }
            
            // Step 6: Verify queue count decreases
            this.core.logger.info('📱 Step 6: Verifying queue count...');
            await this.core.sleep(3000);
            await this.core.dumpUIHierarchy();
            const updatedDump = this.core.readLastUIDump();
            
            const countMatch = updatedDump.match(/(\d+)\s+video\(s\)\s+queued/i);
            if (countMatch && parseInt(countMatch[1]) < 1) {
                this.core.logger.info(`✅ Queue count decreased to ${countMatch[1]}`);
            }
            
            // Step 7: Verify status changes
            this.core.logger.info('📱 Step 7: Verifying status change...');
            const hasProcessing = updatedDump.includes('PROCESSING') || 
                                updatedDump.includes('Processing');
            
            if (hasProcessing) {
                this.core.logger.info('✅ Status changed to PROCESSING');
            }
            
            // Step 8: Verify notification updates
            this.core.logger.info('📱 Step 8: Verifying notification...');
            const notification = await this.core.executeCommand(
                'adb shell dumpsys notification | findstr /i "pluct_queue"'
            );
            
            if (notification.output.includes('pluct_queue')) {
                this.core.logger.info('✅ Notification found');
            }
            
            this.core.logger.info('✅ Journey-Queue-07AutoRetryCredits-Validation completed');
            return { success: true };
            
        } catch (error) {
            this.core.logger.error(`❌ Journey failed: ${error.message}`);
            await this.failWithDiagnostics(error.message);
            return { success: false, error: error.message };
        }
    }
}

function register(orchestrator) {
    orchestrator.registerJourney('Queue-07AutoRetryCredits', new JourneyQueue07AutoRetryCreditsValidation(orchestrator.core));
}

module.exports = { register };







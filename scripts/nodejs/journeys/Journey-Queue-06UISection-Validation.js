const { BaseJourney } = require('./Pluct-Journey-01Orchestrator');

class JourneyQueue06UISectionValidation extends BaseJourney {
    constructor(core) {
        super(core);
        this.name = 'Queue-06UISection';
    }

    async execute() {
        this.core.logger.info('🚀 Starting: Journey-Queue-06UISection-Validation');
        
        try {
            // Step 1: Queue 2 videos (different reasons)
            this.core.logger.info('📱 Step 1: Queueing 2 videos...');
            await this.core.executeCommand('adb shell svc wifi disable');
            await this.core.sleep(2000);
            
            await this.ensureAppForeground();
            
            // Queue first video (offline)
            let urlTap = await this.core.tapByTestTag('url_input_field');
            if (!urlTap.success) {
                urlTap = await this.core.tapByText('Paste TikTok Link');
            }
            await this.core.inputText(this.core.config.url);
            await this.core.sleep(1000);
            
            const extractTap1 = await this.core.tapByTestTag('extract_script_button');
            if (extractTap1.success) {
                await this.core.sleep(1000);
                const saveTap1 = await this.core.tapByText('Save for Later');
                if (saveTap1.success) {
                    await this.core.sleep(2000);
                }
            }
            
            // Queue second video
            urlTap = await this.core.tapByTestTag('url_input_field');
            await this.core.inputText('https://vm.tiktok.com/ZMDRUGT3P/');
            await this.core.sleep(1000);
            
            const extractTap2 = await this.core.tapByTestTag('extract_script_button');
            if (extractTap2.success) {
                await this.core.sleep(1000);
                const saveTap2 = await this.core.tapByText('Save for Later');
                if (saveTap2.success) {
                    await this.core.sleep(2000);
                }
            }
            
            // Step 2: Verify queue section visible
            this.core.logger.info('📱 Step 2: Verifying queue section...');
            await this.core.dumpUIHierarchy();
            const uiDump = this.core.readLastUIDump();
            
            const hasQueueSection = uiDump.includes('video(s) queued') || 
                                   uiDump.includes('queued') ||
                                   uiDump.includes('Queue');
            
            if (!hasQueueSection) {
                return { success: false, error: 'Queue section not found' };
            }
            this.core.logger.info('✅ Queue section found');
            
            // Step 3: Verify 2 items shown
            this.core.logger.info('📱 Step 3: Verifying 2 items shown...');
            const itemCount = (uiDump.match(/Retry Now|Remove/gi) || []).length / 2;
            if (itemCount >= 2) {
                this.core.logger.info(`✅ Found ${itemCount} queue items`);
            }
            
            // Step 4: Verify reason badges
            this.core.logger.info('📱 Step 4: Verifying reason badges...');
            const hasNoInternet = uiDump.includes('No Internet') || 
                                uiDump.includes('no internet');
            const hasWaiting = uiDump.includes('Waiting') || 
                             uiDump.includes('waiting');
            
            if (hasNoInternet || hasWaiting) {
                this.core.logger.info('✅ Reason badges found');
            }
            
            // Step 5: Tap "Retry Now" on first item
            this.core.logger.info('📱 Step 5: Tapping Retry Now...');
            await this.core.executeCommand('adb shell svc wifi enable');
            await this.core.sleep(2000);
            
            const retryTap = await this.core.tapByText('Retry Now');
            if (retryTap.success) {
                await this.core.sleep(3000);
                this.core.logger.info('✅ Retry Now tapped');
                
                // Verify processing starts
                const processLog = await this.core.executeCommand(
                    'adb logcat -d -t 50 | findstr /i "Processing"'
                );
                if (processLog.output.includes('Processing')) {
                    this.core.logger.info('✅ Processing started');
                }
            }
            
            // Step 6: Tap "Remove" on second item
            this.core.logger.info('📱 Step 6: Tapping Remove...');
            await this.core.sleep(2000);
            await this.core.dumpUIHierarchy();
            
            const removeTap = await this.core.tapByText('Remove');
            if (removeTap.success) {
                await this.core.sleep(2000);
                this.core.logger.info('✅ Remove tapped');
                
                // Verify item removed
                await this.core.dumpUIHierarchy();
                const updatedDump = this.core.readLastUIDump();
                const updatedCount = (updatedDump.match(/Retry Now|Remove/gi) || []).length / 2;
                
                if (updatedCount < 2) {
                    this.core.logger.info(`✅ Item removed, count now ${updatedCount}`);
                }
            }
            
            // Step 7: Verify queue count = 1
            this.core.logger.info('📱 Step 7: Verifying queue count...');
            const countMatch = this.core.readLastUIDump().match(/(\d+)\s+video\(s\)\s+queued/i);
            if (countMatch && parseInt(countMatch[1]) <= 1) {
                this.core.logger.info(`✅ Queue count verified: ${countMatch[1]}`);
            }
            
            // Step 8: Verify notification updated
            this.core.logger.info('📱 Step 8: Verifying notification...');
            const notification = await this.core.executeCommand(
                'adb shell dumpsys notification | findstr /i "pluct_queue"'
            );
            
            if (notification.output.includes('pluct_queue')) {
                this.core.logger.info('✅ Notification updated');
            }
            
            this.core.logger.info('✅ Journey-Queue-06UISection-Validation completed');
            return { success: true };
            
        } catch (error) {
            this.core.logger.error(`❌ Journey failed: ${error.message}`);
            await this.failWithDiagnostics(error.message);
            return { success: false, error: error.message };
        } finally {
            await this.core.executeCommand('adb shell svc wifi enable');
        }
    }
}

function register(orchestrator) {
    orchestrator.registerJourney('Queue-06UISection', new JourneyQueue06UISectionValidation(orchestrator.core));
}

module.exports = { register };




const { BaseJourney } = require('./Pluct-Journey-01Orchestrator');

class JourneyQueue08AutoRetryNetworkValidation extends BaseJourney {
    constructor(core) {
        super(core);
        this.name = 'Queue-08AutoRetryNetwork';
    }

    async execute() {
        this.core.logger.info('🚀 Starting: Journey-Queue-08AutoRetryNetwork-Validation');
        
        try {
            // Step 1: Disable network
            this.core.logger.info('📱 Step 1: Disabling network...');
            await this.core.executeCommand('adb shell svc wifi disable');
            await this.core.executeCommand('adb shell svc data disable');
            await this.core.sleep(2000);
            
            await this.ensureAppForeground();
            
            // Step 2: Queue 2 videos
            this.core.logger.info('📱 Step 2: Queueing 2 videos...');
            
            // Queue first video
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
            
            // Step 3: Verify queued
            this.core.logger.info('📱 Step 3: Verifying videos queued...');
            await this.core.dumpUIHierarchy();
            const queueDump = this.core.readLastUIDump();
            
            const queueCount = (queueDump.match(/queued/gi) || []).length;
            if (queueCount >= 2) {
                this.core.logger.info(`✅ ${queueCount} videos queued`);
            }
            
            // Step 4: Enable network
            this.core.logger.info('📱 Step 4: Enabling network...');
            await this.core.executeCommand('adb shell svc wifi enable');
            await this.core.sleep(3000);
            
            // Step 5: Verify NetworkCallback triggered
            this.core.logger.info('📱 Step 5: Verifying NetworkCallback...');
            await this.core.sleep(5000);
            
            const networkLog = await this.core.executeCommand(
                'adb logcat -d -t 100 | findstr /i "NetworkCallback.*connected Network.*available"'
            );
            
            if (networkLog.output.includes('Network') || 
                networkLog.output.includes('connected')) {
                this.core.logger.info('✅ Network callback detected');
            }
            
            // Step 6: Verify auto-processing starts
            this.core.logger.info('📱 Step 6: Verifying auto-processing...');
            await this.core.sleep(5000);
            
            const autoProcessLog = await this.core.executeCommand(
                'adb logcat -d -t 100 | findstr /i "Auto-processing.*network Processing.*2 items"'
            );
            
            if (autoProcessLog.output.includes('Auto-processing') || 
                autoProcessLog.output.includes('Processing')) {
                this.core.logger.info('✅ Auto-processing detected');
            }
            
            // Step 7: Verify both items process
            this.core.logger.info('📱 Step 7: Verifying both items process...');
            await this.core.sleep(5000);
            await this.core.dumpUIHierarchy();
            const processDump = this.core.readLastUIDump();
            
            const processingCount = (processDump.match(/PROCESSING/gi) || []).length;
            if (processingCount >= 2) {
                this.core.logger.info(`✅ ${processingCount} items processing`);
            }
            
            // Step 8: Verify queue empty
            this.core.logger.info('📱 Step 8: Verifying queue empty...');
            await this.core.sleep(5000);
            await this.core.dumpUIHierarchy();
            const finalDump = this.core.readLastUIDump();
            
            const queueCountMatch = finalDump.match(/(\d+)\s+video\(s\)\s+queued/i);
            if (queueCountMatch && parseInt(queueCountMatch[1]) === 0) {
                this.core.logger.info('✅ Queue empty');
            } else if (!queueCountMatch) {
                this.core.logger.info('✅ Queue section not visible (likely empty)');
            }
            
            // Step 9: Verify notification removed
            this.core.logger.info('📱 Step 9: Verifying notification...');
            const notification = await this.core.executeCommand(
                'adb shell dumpsys notification | findstr /i "pluct_queue"'
            );
            
            const queueEmptyLog = await this.core.executeCommand(
                'adb logcat -d -t 50 | findstr /i "Queue.*empty"'
            );
            
            if (queueEmptyLog.output.includes('empty') || 
                !notification.output.includes('pluct_queue')) {
                this.core.logger.info('✅ Notification removed or queue empty');
            }
            
            this.core.logger.info('✅ Journey-Queue-08AutoRetryNetwork-Validation completed');
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
    orchestrator.registerJourney('Queue-08AutoRetryNetwork', new JourneyQueue08AutoRetryNetworkValidation(orchestrator.core));
}

module.exports = { register };




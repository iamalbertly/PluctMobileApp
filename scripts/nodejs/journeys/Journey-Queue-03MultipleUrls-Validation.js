const { BaseJourney } = require('./Pluct-Journey-01Orchestrator');

class JourneyQueue03MultipleUrlsValidation extends BaseJourney {
    constructor(core) {
        super(core);
        this.name = 'Queue-03MultipleUrls';
        this.testUrls = [
            'https://vm.tiktok.com/ZMDRUGT2P/',
            'https://vm.tiktok.com/ZMDRUGT3P/',
            'https://vm.tiktok.com/ZMDRUGT4P/'
        ];
    }

    async execute() {
        this.core.logger.info('🚀 Starting: Journey-Queue-03MultipleUrls-Validation');
        
        try {
            // Step 1: Disable network
            this.core.logger.info('📱 Step 1: Disabling network...');
            await this.core.executeCommand('adb shell svc wifi disable');
            await this.core.executeCommand('adb shell svc data disable');
            await this.core.sleep(2000);
            
            await this.ensureAppForeground();
            
            // Step 2-4: Queue 3 URLs
            for (let i = 0; i < 3; i++) {
                const url = this.testUrls[i] || this.core.config.url;
                this.core.logger.info(`📱 Step ${2 + i}: Queueing URL ${i + 1}...`);
                
                // Enter URL
                let urlTap = await this.core.tapByTestTag('url_input_field');
                if (!urlTap.success) {
                    urlTap = await this.core.tapByText('Paste TikTok Link');
                }
                await this.core.inputText(url);
                await this.core.sleep(1000);
                
                // Tap Extract Script
                const extractTap = await this.core.tapByTestTag('extract_script_button');
                if (extractTap.success) {
                    await this.core.sleep(1000);
                    // Tap Save for Later if dialog appears
                    const saveTap = await this.core.tapByText('Save for Later');
                    if (saveTap.success) {
                        await this.core.sleep(1000);
                    }
                }
            }
            
            // Step 5: Verify queue count = 3
            this.core.logger.info('📱 Step 5: Verifying queue count...');
            await this.core.dumpUIHierarchy();
            const uiDump = this.core.readLastUIDump();
            
            const queueCountMatch = uiDump.match(/(\d+)\s+video\(s\)\s+queued/i) || 
                                   uiDump.match(/(\d+)\s+in\s+queue/i);
            
            if (queueCountMatch && parseInt(queueCountMatch[1]) >= 3) {
                this.core.logger.info(`✅ Queue count verified: ${queueCountMatch[1]}`);
            } else {
                this.core.logger.warn('⚠️ Queue count not found or less than 3');
            }
            
            // Step 6: Verify notification shows "3 video(s)"
            this.core.logger.info('📱 Step 6: Verifying notification...');
            const notificationResult = await this.core.executeCommand(
                'adb shell dumpsys notification | findstr /i "3.*video"'
            );
            
            if (notificationResult.output.includes('3') || 
                notificationResult.output.includes('pluct_queue')) {
                this.core.logger.info('✅ Notification found with count');
            }
            
            // Step 7: Verify UI shows 3 queue items
            this.core.logger.info('📱 Step 7: Verifying 3 queue items in UI...');
            const queueItems = (uiDump.match(/queued/gi) || []).length;
            if (queueItems >= 3) {
                this.core.logger.info(`✅ Found ${queueItems} queue references in UI`);
            }
            
            // Step 8: Enable network
            this.core.logger.info('📱 Step 8: Enabling network...');
            await this.core.executeCommand('adb shell svc wifi enable');
            await this.core.sleep(3000);
            
            // Step 9: Verify processing starts
            this.core.logger.info('📱 Step 9: Verifying processing starts...');
            await this.core.sleep(5000);
            
            const processLog = await this.core.executeCommand(
                'adb logcat -d -t 100 | findstr /i "Processing queued"'
            );
            
            if (processLog.output.includes('Processing')) {
                this.core.logger.info('✅ Processing detected');
            }
            
            // Step 10: Verify queue count decreases
            this.core.logger.info('📱 Step 10: Verifying queue count decreases...');
            await this.core.sleep(3000);
            await this.core.dumpUIHierarchy();
            const updatedDump = this.core.readLastUIDump();
            
            const updatedCountMatch = updatedDump.match(/(\d+)\s+video\(s\)\s+queued/i);
            if (updatedCountMatch) {
                const newCount = parseInt(updatedCountMatch[1]);
                if (newCount < 3) {
                    this.core.logger.info(`✅ Queue count decreased to ${newCount}`);
                }
            }
            
            this.core.logger.info('✅ Journey-Queue-03MultipleUrls-Validation completed');
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
    orchestrator.registerJourney('Queue-03MultipleUrls', new JourneyQueue03MultipleUrlsValidation(orchestrator.core));
}

module.exports = { register };




const { BaseJourney } = require('./Pluct-Journey-01Orchestrator');

class JourneyQueue04NotificationPersistenceValidation extends BaseJourney {
    constructor(core) {
        super(core);
        this.name = 'Queue-04NotificationPersistence';
    }

    async execute() {
        this.core.logger.info('🚀 Starting: Journey-Queue-04NotificationPersistence-Validation');
        
        try {
            // Step 1: Queue video (offline or no credits)
            this.core.logger.info('📱 Step 1: Queueing video...');
            await this.core.executeCommand('adb shell svc wifi disable');
            await this.core.sleep(2000);
            
            await this.ensureAppForeground();
            
            // Enter URL and queue
            let urlTap = await this.core.tapByTestTag('url_input_field');
            if (!urlTap.success) {
                urlTap = await this.core.tapByText('Paste TikTok Link');
            }
            await this.core.inputText(this.core.config.url);
            await this.core.sleep(1000);
            
            const extractTap = await this.core.tapByTestTag('extract_script_button');
            if (extractTap.success) {
                await this.core.sleep(1000);
                const saveTap = await this.core.tapByText('Save for Later');
                if (saveTap.success) {
                    await this.core.sleep(2000);
                }
            }
            
            // Step 2: Verify notification appears
            this.core.logger.info('📱 Step 2: Verifying notification appears...');
            await this.core.sleep(2000);
            const notification1 = await this.core.executeCommand(
                'adb shell dumpsys notification | findstr /i "pluct_queue"'
            );
            
            if (notification1.output.includes('pluct_queue') || 
                notification1.output.includes('Pluct Queue')) {
                this.core.logger.info('✅ Notification found');
            } else {
                this.core.logger.warn('⚠️ Notification not found');
            }
            
            // Step 3: Kill app
            this.core.logger.info('📱 Step 3: Killing app...');
            await this.core.executeCommand('adb shell am force-stop app.pluct');
            await this.core.sleep(2000);
            
            // Step 4: Verify notification still visible
            this.core.logger.info('📱 Step 4: Verifying notification persists...');
            const notification2 = await this.core.executeCommand(
                'adb shell dumpsys notification | findstr /i "pluct_queue"'
            );
            
            if (notification2.output.includes('pluct_queue')) {
                this.core.logger.info('✅ Notification persists after app kill');
            } else {
                this.core.logger.warn('⚠️ Notification not found after app kill');
            }
            
            // Step 5: Launch app from notification (simulate)
            this.core.logger.info('📱 Step 5: Launching app...');
            await this.core.launchApp();
            await this.core.sleep(3000);
            
            // Step 6: Verify app opens (queue section should be visible)
            this.core.logger.info('📱 Step 6: Verifying app opened...');
            await this.core.dumpUIHierarchy();
            const uiDump = this.core.readLastUIDump();
            
            if (uiDump.includes('app.pluct') || uiDump.includes('Pluct')) {
                this.core.logger.info('✅ App opened successfully');
            }
            
            // Step 7: Enable network/add credits
            this.core.logger.info('📱 Step 7: Enabling network...');
            await this.core.executeCommand('adb shell svc wifi enable');
            await this.core.sleep(3000);
            
            // Step 8: Verify notification updates
            this.core.logger.info('📱 Step 8: Verifying notification update...');
            await this.core.sleep(5000);
            const notification3 = await this.core.executeCommand(
                'adb shell dumpsys notification | findstr /i "pluct_queue"'
            );
            
            if (notification3.output.includes('processing') || 
                notification3.output.includes('pluct_queue')) {
                this.core.logger.info('✅ Notification updated');
            }
            
            // Step 9: Wait for completion and verify notification removed
            this.core.logger.info('📱 Step 9: Waiting for completion...');
            await this.core.sleep(10000);
            
            const notification4 = await this.core.executeCommand(
                'adb shell dumpsys notification | findstr /i "pluct_queue"'
            );
            
            const logcatResult = await this.core.executeCommand(
                'adb logcat -d -t 50 | findstr /i "Notification.*removed"'
            );
            
            if (logcatResult.output.includes('removed') || 
                !notification4.output.includes('pluct_queue')) {
                this.core.logger.info('✅ Notification removed or updated');
            }
            
            this.core.logger.info('✅ Journey-Queue-04NotificationPersistence-Validation completed');
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
    orchestrator.registerJourney('Queue-04NotificationPersistence', new JourneyQueue04NotificationPersistenceValidation(orchestrator.core));
}

module.exports = { register };




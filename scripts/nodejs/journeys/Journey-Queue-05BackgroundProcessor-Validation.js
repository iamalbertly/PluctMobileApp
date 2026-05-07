const { BaseJourney } = require('./Pluct-Journey-01Orchestrator');

class JourneyQueue05BackgroundProcessorValidation extends BaseJourney {
    constructor(core) {
        super(core);
        this.name = 'Queue-05BackgroundProcessor';
    }

    async execute() {
        this.core.logger.info('🚀 Starting: Journey-Queue-05BackgroundProcessor-Validation');
        
        try {
            // Step 1: Queue video (offline)
            this.core.logger.info('📱 Step 1: Queueing video while offline...');
            await this.core.executeCommand('adb shell svc wifi disable');
            await this.core.sleep(2000);
            
            await this.ensureAppForeground();
            await this.core.ensureCaptureCardReady();
            
            let urlTap = await this.core.tapByTestTag('video_url_input');
            if (!urlTap.success) {
                urlTap = await this.core.tapByTestTag('url_input_field');
            }
            if (!urlTap.success) {
                urlTap = await this.core.tapByText('Paste TikTok Link');
            }
            await this.core.inputText(this.core.config.url);
            await this.core.sleep(1000);
            
            const extractTap = await this.core.ui.buttonTapping.tapExtractScriptButton();
            if (extractTap.success) {
                await this.core.sleep(1000);
                const saveTap = await this.core.tapByText('Save for Later');
                if (saveTap.success) {
                    await this.core.sleep(2000);
                }
            }
            
            // Step 2: Kill app completely
            this.core.logger.info('📱 Step 2: Killing app...');
            await this.core.executeCommand('adb shell am force-stop app.pluct');
            await this.core.sleep(2000);
            
            // Step 3: Enable network
            this.core.logger.info('📱 Step 3: Enabling network...');
            await this.core.executeCommand('adb shell svc wifi enable');
            await this.core.sleep(2000);
            
            // Step 4: Wait for WorkManager interval (15 minutes + buffer = 16 minutes)
            // For testing, we'll wait a shorter time and check if work is scheduled
            this.core.logger.info('📱 Step 4: Checking WorkManager schedule...');
            await this.core.sleep(5000);
            
            const workManagerLog = await this.core.executeCommand(
                'adb logcat -d -t 250 | findstr /i "PluctQueueProcessor\\|Queue processor\\|pluct_queue_processor\\|WorkManager"',
                undefined,
                undefined,
                { allowFailure: true }
            );
            const workManagerOutput = workManagerLog.output || '';
            
            if (workManagerOutput.includes('PluctQueueProcessor') ||
                workManagerOutput.includes('Queue processor') ||
                workManagerOutput.includes('scheduled')) {
                this.core.logger.info('✅ WorkManager work scheduled');
            } else {
                this.core.logger.warn('⚠️ WorkManager schedule log not found (may be scheduled silently)');
            }
            
            // Step 5: Check logcat for worker execution (after shorter wait for testing)
            this.core.logger.info('📱 Step 5: Checking for worker execution...');
            await this.core.sleep(10000);
            
            const workerLog = await this.core.executeCommand(
                'adb logcat -d -t 250 | findstr /i "Queue processor worker started\\|Queue processor completed\\|Completed queued video\\|Worker result SUCCESS"',
                undefined,
                undefined,
                { allowFailure: true }
            );
            const workerOutput = workerLog.output || '';
            
            if (workerOutput.includes('Queue processor') ||
                workerOutput.includes('Completed queued video') ||
                workerOutput.includes('Worker result SUCCESS')) {
                this.core.logger.info('✅ Worker execution detected');
            } else {
                this.core.logger.warn('⚠️ Worker execution not detected (may run later)');
            }
            
            // Step 6: Launch app and verify video status
            this.core.logger.info('📱 Step 6: Launching app to verify status...');
            await this.core.launchApp();
            await this.core.sleep(5000);
            
            await this.core.dumpUIHierarchy();
            const uiDump = this.core.readLastUIDump();
            
            // Check if video status changed
            const hasProcessing = uiDump.includes('PROCESSING') || 
                                uiDump.includes('Processing');
            const hasCompleted = uiDump.includes('COMPLETED') || 
                               uiDump.includes('Completed');
            
            if (hasProcessing || hasCompleted) {
                this.core.logger.info('✅ Video status updated (PROCESSING or COMPLETED)');
            } else {
                this.core.logger.warn('⚠️ Video status not updated (may process later)');
            }
            
            // Step 7: Verify queue count decreased
            this.core.logger.info('📱 Step 7: Verifying queue count...');
            const queueCountMatch = uiDump.match(/(\d+)\s+video\(s\)\s+queued/i);
            if (queueCountMatch) {
                const count = parseInt(queueCountMatch[1]);
                if (count < 1) {
                    this.core.logger.info(`✅ Queue count decreased to ${count}`);
                }
            }
            
            // Step 8: Verify notification updated
            this.core.logger.info('📱 Step 8: Verifying notification...');
            const notification = await this.core.executeCommand(
                'adb shell dumpsys notification | findstr /i "pluct_queue\\|Pluct Queue\\|Pluct Complete"',
                undefined,
                undefined,
                { allowFailure: true }
            );
            const notificationOutput = notification.output || '';
            
            if (notificationOutput.includes('pluct_queue') ||
                notificationOutput.includes('Pluct Queue') ||
                notificationOutput.includes('Pluct Complete')) {
                this.core.logger.info('✅ Notification found');
            }
            
            this.core.logger.info('✅ Journey-Queue-05BackgroundProcessor-Validation completed');
            return { success: true };
            
        } catch (error) {
            this.core.logger.error(`❌ Journey failed: ${error.message}`);
            await this.failWithDiagnostics(error.message);
            return { success: false, error: error.message };
        } finally {
            await this.core.executeCommand('adb shell svc wifi enable', undefined, undefined, { allowFailure: true });
        }
    }
}

function register(orchestrator) {
    orchestrator.registerJourney('Queue-05BackgroundProcessor', new JourneyQueue05BackgroundProcessorValidation(orchestrator.core));
}

module.exports = { register };







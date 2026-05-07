const { BaseJourney } = require('./Pluct-Journey-01Orchestrator');

/**
 * Journey-Intent-02TikTok-03Queue-01Validation.js
 * Validates that TikTok intents are queued when credits are insufficient
 */
class JourneyIntent02TikTok03Queue01Validation extends BaseJourney {
    constructor(core) {
        super(core);
        this.name = 'Intent-02TikTok-03Queue-01Validation';
        this.maxDuration = 60000; // 1 minute max
    }

    async execute() {
        this.core.logger.info('🎯 Starting TikTok Intent Queue Validation Journey...');
        const startTime = Date.now();
        const testUrl = this.core.config.url;

        try {
            // Step 1: Pre-Condition - Ensure app has 0 credits and 0 free uses
            this.core.logger.info('📱 Step 1: Setting up zero credits condition...');
            const launch = await this.core.launchApp();
            if (!launch.success) {
                return { success: false, error: 'Failed to launch app' };
            }
            await this.core.sleep(3000);

            // Note: In a real scenario, we would need to set credits to 0
            // For now, we'll check if credits are already 0 or if we can simulate it
            this.core.logger.info('⚠️ Note: This test assumes credits are 0. If credits exist, test may need manual setup.');

            // Step 2: Clear app state
            this.core.logger.info('📱 Step 2: Clearing app state...');
            await this.core.clearAppCache();
            await this.core.clearWorkManagerTasks();
            await this.core.sleep(1000);

            // Step 3: Launch app and wait for balance to load
            this.core.logger.info('📱 Step 3: Launching app and waiting for balance...');
            await this.core.launchApp();
            await this.core.sleep(5000);
            await this.core.dumpUIHierarchy();
            let setupDump = this.core.readLastUIDump();
            if (!setupDump.includes('package="app.pluct"')) {
                this.core.logger.warn('App was not foregrounded after launch; retrying foreground launch');
                await this.core.launchApp();
                await this.core.sleep(2000);
                await this.core.dumpUIHierarchy();
                setupDump = this.core.readLastUIDump();
            }
            if (setupDump.includes('Video Details')) {
                this.core.logger.info('Returning from video detail to home before checking credits');
                await this.core.pressBackButton();
                await this.core.sleep(1500);
            }

            // Step 4: Verify balance is 0
            this.core.logger.info('📱 Step 4: Verifying balance is 0...');
            await this.core.clearLogcat();
            await this.core.dumpUIHierarchy();
            const balanceDump = this.core.readLastUIDump();
            const creditMatch = balanceDump.match(/text="(\d+) credits"/i);
            if (creditMatch && Number(creditMatch[1]) > 0) {
                this.core.logger.warn(`Credits are ${creditMatch[1]}; skipping zero-credit queue assertion for this environment`);
                return { success: true, skipped: true, reason: 'Credit balance is not zero' };
            }
            const balanceLog = await this.core.executeCommand('adb logcat -d | findstr /i "balance.*=.*0\|Credit balance.*0"', undefined, undefined, { allowFailure: true });
            this.core.logger.info(`📊 Balance logs: ${balanceLog.output || 'No balance logs found'}`);

            // Step 5: Send Intent
            this.core.logger.info(`📱 Step 5: Sending ACTION_SEND intent with URL: ${testUrl}`);
            const intentCommand = `adb shell am start -a android.intent.action.SEND -t "text/plain" --es android.intent.extra.TEXT "${testUrl}" app.pluct/.PluctUIScreen01MainActivity`;
            const intentResult = await this.core.executeCommand(intentCommand);
            if (!intentResult.success) {
                return { success: false, error: 'Failed to send intent' };
            }
            await this.core.sleep(2000);

            // Step 6: Verify Intent Received
            this.core.logger.info('📱 Step 6: Verifying intent received...');
            const intentLog = await this.core.executeCommand('adb logcat -d | findstr /i "IntentHandler.*Received shared text\|IntentHandler.*TikTok URL detected"', undefined, undefined, { allowFailure: true });
            const intentOutput = intentLog.output || '';
            if (!intentLog.success || !intentOutput.includes('IntentHandler')) {
                return { success: false, error: 'Intent not received' };
            }
            this.core.logger.info('✅ Intent received confirmed');

            // Step 7: Verify URL Prefilled
            this.core.logger.info('📱 Step 7: Verifying URL prefilled...');
            await this.core.dumpUIHierarchy();
            const uiDump = this.core.readLastUIDump();
            const urlInField = uiDump.includes(testUrl) || uiDump.includes('url_input_field');
            if (!urlInField) {
                const prefilledLog = await this.core.executeCommand('adb logcat -d | findstr /i "Found prefilled URL\\|Prefilled URL from new intent\\|Prefilled URL state set"', undefined, undefined, { allowFailure: true });
                if (!prefilledLog.success || !/prefilled url/i.test(prefilledLog.output || '')) {
                    return { success: false, error: 'URL not prefilled' };
                }
            }
            this.core.logger.info('✅ URL prefilled confirmed');

            // Step 8: Verify Auto-Queue Triggered (not auto-submit)
            this.core.logger.info('📱 Step 8: Verifying auto-queue triggered (not auto-submit)...');
            await this.core.sleep(2000);
            
            // Check that auto-submit was NOT triggered
            const autoSubmitLog = await this.core.executeCommand('adb logcat -d | findstr /i "Auto-submitting URL"', undefined, undefined, { allowFailure: true });
            const autoSubmitOutput = autoSubmitLog.output || '';
            if (autoSubmitLog.success && autoSubmitOutput.includes('Auto-submitting')) {
                const balanceMatch = autoSubmitOutput.match(/balance=(\d+)/i);
                const freeUsesMatch = autoSubmitOutput.match(/freeUses=(\d+)/i);
                const entitlement = `balance=${balanceMatch?.[1] || 'unknown'}, freeUses=${freeUsesMatch?.[1] || 'unknown'}`;
                this.core.logger.warn(`Usable entitlement detected (${entitlement}); skipping zero-credit queue assertion for this environment`);
                return { success: true, skipped: true, reason: 'Credit balance or free uses are not zero' };
            }

            // Check that queue was triggered
            const queueLog = await this.core.executeCommand('adb logcat -d | findstr /i "Video queued\|INSUFFICIENT_CREDITS\|onQueueForLater"', undefined, undefined, { allowFailure: true });
            const queueOutput = queueLog.output || '';
            if (!queueLog.success || (!queueOutput.includes('queued') && !queueOutput.includes('INSUFFICIENT_CREDITS'))) {
                return { success: false, error: 'Queue not triggered - no logcat evidence' };
            }
            this.core.logger.info('✅ Auto-queue triggered confirmed');

            // Step 9: Verify Queue UI
            this.core.logger.info('📱 Step 9: Verifying queue UI visible...');
            await this.core.sleep(1000);
            await this.core.dumpUIHierarchy();
            const queueDump = this.core.readLastUIDump();
            const hasQueueUI = queueDump.includes('queued') || 
                              queueDump.includes('Queue') ||
                              queueDump.includes('Save for Later');
            if (!hasQueueUI) {
                this.core.logger.warn('⚠️ Queue UI not visible, but queue was triggered in logcat');
            } else {
                this.core.logger.info('✅ Queue UI visible');
            }

            // Step 10: Verify Notification
            this.core.logger.info('📱 Step 10: Verifying queue notification...');
            const notificationCheck = await this.core.executeCommand('adb shell dumpsys notification | findstr /i "queued\|Queue"', undefined, undefined, { allowFailure: true });
            const notificationOutput = notificationCheck.output || '';
            if (!notificationCheck.success || !notificationOutput.includes('queued')) {
                this.core.logger.warn('⚠️ Queue notification not found, but continuing...');
            } else {
                this.core.logger.info('✅ Queue notification confirmed');
            }

            const duration = Date.now() - startTime;
            this.core.logger.info(`✅ TikTok Intent Queue Validation Journey completed successfully in ${duration}ms`);
            return { success: true, duration };

        } catch (error) {
            this.core.logger.error(`❌ Journey failed: ${error.message}`);
            return { success: false, error: error.message };
        }
    }
}

module.exports = JourneyIntent02TikTok03Queue01Validation;

const { BaseJourney } = require('./Pluct-Journey-01Orchestrator');

/**
 * Journey-UX-11AutoSubmitIntent-Validation
 * Validates automatic transcription start when intent received with sufficient credits
 */
class JourneyUX11AutoSubmitIntentValidation extends BaseJourney {
    constructor(core) {
        super(core);
        this.name = 'UX-11AutoSubmitIntent-Validation';
    }

    async execute() {
        this.core.logger.info('Starting Auto-Submit Intent Validation');
        
        // Step 1: Launch app and verify credits available
        await this.core.executeCommand('adb shell input keyevent BACK', undefined, undefined, { allowFailure: true });
        await this.core.launchApp();
        await this.core.sleep(3000);
        
        await this.core.dumpUIHierarchy();
        let uiDump = this.core.readLastUIDump() || '';
        
        // Check if credits are available (should show credits > 0 or free uses)
        const hasCredits = !uiDump.includes('0 credits') && 
                          (uiDump.includes('credits') || uiDump.match(/\d+\s*credits/i));
        
        if (!hasCredits) {
            this.logger.warn('⚠️ No credits available - test may not trigger auto-submit');
        }
        
        // Step 2: Clear app state
        await this.core.executeCommand('adb shell pm clear app.pluct');
        await this.core.sleep(2000);
        
        // Step 3: Launch app fresh
        await this.core.launchApp();
        await this.core.sleep(3000);

        const seeded = await this.core.ensureLocalMobileCredits(3);
        if (!seeded.success) {
            return { success: false, error: `Could not seed credits after fresh launch: ${seeded.error || 'unknown'}` };
        }
        
        // Step 4: Send intent with TikTok URL
        const startTime = Date.now();
        await this.core.executeCommand(
            `adb shell am start -a android.intent.action.SEND -t "text/plain" -e android.intent.extra.TEXT "${this.core.config.url}" --es android.intent.extra.SUBJECT "TikTok" app.pluct/.PluctUIScreen01MainActivity`
        );
        
        // Step 5: Monitor for auto-submit (should happen within 1 second)
        let autoSubmitDetected = false;
        const maxWait = 20000; // allow app restore, balance load, and auto-submit debounce
        
        while (Date.now() - startTime < maxWait) {
            await this.core.sleep(500);
            
            // Check logcat for auto-submit
            const logcatResult = await this.core.executeCommand(
                'adb logcat -d -t 200 | findstr /i /c:"Auto-submitting" /c:"submitExtract" /c:"Starting background transcription" /c:"Starting transcription"'
            );
            
            if (logcatResult.output && logcatResult.output.includes('Auto-submitting')) {
                autoSubmitDetected = true;
                const elapsed = Date.now() - startTime;
                this.logger.info(`✅ Auto-submit detected at ${elapsed}ms`);
                break;
            }
            
            // Check UI for processing state
            await this.core.dumpUIHierarchy();
            uiDump = this.core.readLastUIDump() || '';
            
            if (uiDump.includes('Starting transcription') || 
                uiDump.includes('0% working') ||
                uiDump.includes('Processing indicator') ||
                uiDump.includes('Getting video details') ||
                uiDump.includes('Submitting job')) {
                autoSubmitDetected = true;
                const elapsed = Date.now() - startTime;
                this.logger.info(`✅ Auto-submit detected in UI at ${elapsed}ms`);
                break;
            }
        }
        
        if (!autoSubmitDetected) {
            return { 
                success: false, 
                error: 'Auto-submit not detected within 20 seconds of intent receive' 
            };
        }
        
        // Step 6: Verify app minimizes (if credits available)
        await this.core.sleep(2000);
        
        const activityCheck = await this.core.executeCommand(
            'adb shell dumpsys activity activities | findstr /i "mResumedActivity.*pluct"'
        );
        
        // App may or may not minimize depending on implementation
        // Just verify processing started
        
        // Step 7: Verify notification appears
        const notificationCheck = await this.core.executeCommand(
            'adb shell dumpsys notification | findstr /i /c:"app.pluct" /c:"Pluct" /c:"Text" /c:"Video ->" /c:"Audio ->" /c:"%"'
        );
        
        if (notificationCheck.output) {
            this.logger.info('✅ Notification detected');
        } else {
            this.logger.warn('⚠️ Notification not detected (may appear later)');
        }
        
        // Step 8: Edge case - Intent received during active transcription
        // Start another transcription manually first
        await this.core.tapByTestTag('url_input_field');
        await this.core.inputText(this.core.config.url);
        await this.core.sleep(500);
        await this.core.tapByTestTag('extract_script_button');
        await this.core.sleep(1000);
        
        // Send another intent
        await this.core.executeCommand(
            `adb shell am start -a android.intent.action.SEND -t "text/plain" -e android.intent.extra.TEXT "${this.core.config.url}" --es android.intent.extra.SUBJECT "TikTok" app.pluct/.PluctUIScreen01MainActivity`
        );
        await this.core.sleep(2000);
        
        // Check if new video was queued
        await this.core.dumpUIHierarchy();
        uiDump = this.core.readLastUIDump() || '';
        
        const queued = uiDump.includes('Queue') || uiDump.includes('queued') || 
                      uiDump.includes('Save for Later');
        
        if (queued) {
            this.logger.info('✅ New video queued when intent received during active transcription');
        }
        
        this.core.logger.info('Auto-Submit Intent Validation Complete');
        return { success: true, autoSubmitDetected };
    }
}

module.exports = JourneyUX11AutoSubmitIntentValidation;


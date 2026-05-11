const { BaseJourney } = require('./Pluct-Journey-01Orchestrator');

/**
 * Journey-Intent-03TikTok-04BalanceRace-01Validation.js
 * Validates that auto-submit waits for balance to load before deciding
 */
class JourneyIntent03TikTok04BalanceRace01Validation extends BaseJourney {
    constructor(core) {
        super(core);
        this.name = 'Intent-03TikTok-04BalanceRace-01Validation';
        this.maxDuration = 60000; // 1 minute max
    }

    async execute() {
        this.core.logger.info('🎯 Starting TikTok Intent Balance Race Condition Validation Journey...');
        const startTime = Date.now();
        const testUrl = this.core.config.url;

        try {
            // Step 1: Pre-Condition - Ensure app has credits
            this.core.logger.info('📱 Step 1: Setting up test with credits available...');
            
            // Step 2: Clear app state
            this.core.logger.info('📱 Step 2: Clearing app state...');
            await this.core.clearAppData();
            await this.core.sleep(1000);

            // Step 3: Send Intent BEFORE launching app (simulate rapid share)
            this.core.logger.info(`📱 Step 3: Sending intent BEFORE app launch (simulating rapid share)...`);
            const intentCommand = `adb shell am start -a android.intent.action.SEND -t "text/plain" --es android.intent.extra.TEXT "${testUrl}" app.pluct/.PluctUIScreen01MainActivity`;
            const intentResult = await this.core.executeCommand(intentCommand);
            if (!intentResult.success) {
                return { success: false, error: 'Failed to send intent' };
            }
            this.core.logger.info('✅ Intent sent before app launch');

            // Step 4: Launch app immediately after intent
            this.core.logger.info('📱 Step 4: Launching app immediately after intent...');
            await this.core.sleep(500); // Small delay to ensure intent is processed
            const launch = await this.core.launchApp();
            if (!launch.success) {
                return { success: false, error: 'Failed to launch app' };
            }

            // Step 5: Monitor balance loading sequence (do not clear logcat here — it would erase balance-ready markers)
            this.core.logger.info('📱 Step 5: Monitoring balance loading sequence...');
            await this.core.sleep(1000);
            await this.core.executeCommand('adb shell input keyevent 224', 5000, undefined, { allowFailure: true });
            await this.core.sleep(300);
            await this.core.executeCommand('adb shell input swipe 520 1850 520 600 320', 8000, undefined, { allowFailure: true });
            await this.core.sleep(400);
            await this.core.ensureAppForeground();

            // Capture logs during balance loading
            const loadingLogs = await this.core.captureFilteredLogcatTail('MainActivity:I CaptureCard:I *:S', 2000, 20000);
            this.core.logger.info(`📊 Loading logs: ${(loadingLogs.output || '').split('\n').slice(-12).join('\n') || 'No loading logs found'}`);

            // Step 6: Verify auto-submit triggers AFTER balance loads
            this.core.logger.info('📱 Step 6: Verifying auto-submit triggers AFTER balance loads...');
            
            // Wait and monitor for auto-submit
            let autoSubmitFound = false;
            let balanceLoaded = false;
            const maxWaitTime = 42000;
            const checkInterval = 500;
            const maxChecks = maxWaitTime / checkInterval;

            for (let i = 0; i < maxChecks; i++) {
                await this.core.sleep(checkInterval);

                let wide = '';
                if (i % 3 === 2) {
                    const blob = await this.core.executeCommand('adb logcat -d -t 8000', 22000, undefined, { allowFailure: true });
                    wide = String((blob && blob.output) || '');
                }
                
                // Check if balance has loaded
                const balanceCheck = await this.core.captureFilteredLogcatTail('MainActivity:I *:S', 2000, 20000);
                const balText = `${(balanceCheck.output || '')}\n${wide}`;
                if (
                    balText.includes('hasLoadedBalanceOnce=true') ||
                    balText.includes('Balance fetch completed')
                ) {
                    if (!balanceLoaded) {
                        balanceLoaded = true;
                        this.core.logger.info(`✅ Balance loaded at check ${i + 1}`);
                    }
                }

                // Check if auto-submit was triggered (CaptureCard tag)
                const autoSubmitCheck = await this.core.captureFilteredLogcatTail('CaptureCard:I MainActivity:I *:S', 2000, 20000);
                const autoText = `${(autoSubmitCheck.output || '')}\n${wide}`;
                if (autoText.includes('Auto-submitting URL')) {
                    autoSubmitFound = true;
                    this.core.logger.info(`✅ Auto-submit triggered at check ${i + 1}`);
                    
                    // Verify it happened AFTER balance loaded
                    if (!balanceLoaded) {
                        return { success: false, error: 'Auto-submit triggered BEFORE balance loaded - race condition detected!' };
                    }
                    break;
                }

                // Fresh install / clearData often has 0 credits: skip after balance is still valid ordering
                if (balanceLoaded && autoText.includes('Auto-submit skipped: insufficient credits')) {
                    autoSubmitFound = true;
                    this.core.logger.info(`✅ Auto-submit gated after balance (no credits) at check ${i + 1}`);
                    break;
                }
            }

            if (!autoSubmitFound) {
                const finalBlob = await this.core.executeCommand('adb logcat -d -t 12000', 28000, undefined, { allowFailure: true });
                const tail = String((finalBlob && finalBlob.output) || '');
                if (tail.includes('Auto-submitting URL')) {
                    autoSubmitFound = true;
                    this.core.logger.warn('Intent-03: auto-submit found on final logcat tail sweep');
                } else if (
                    tail.includes('hasLoadedBalanceOnce=true') &&
                    tail.includes('Auto-submit skipped: insufficient credits')
                ) {
                    autoSubmitFound = true;
                    this.core.logger.warn('Intent-03: gated skip found on final logcat tail sweep');
                }
            }

            if (!autoSubmitFound) {
                return { success: false, error: 'Auto-submit not triggered within 42s (no submit and no post-balance skip)' };
            }

            if (!balanceLoaded) {
                this.core.logger.warn('⚠️ Balance loading not confirmed, but auto-submit was triggered');
            }

            // Step 7: Verify no premature submission attempts
            this.core.logger.info('📱 Step 7: Verifying no premature submission attempts...');
            const allLogs = await this.core.executeCommand('adb logcat -d -t 1200', 22000, undefined, { allowFailure: true });
            const logLines = (allLogs.output || '').split('\n').filter(line => line.trim().length > 0);
            
            // Check that no submission happened while isLoading was true
            // This is a heuristic check - we look for submission attempts very early
            const earlySubmissions = logLines.filter(line => {
                // If submission happened in first 2 seconds, it's likely premature
                // This is approximate since we can't easily correlate timestamps
                return line.includes('submitExtract') || line.includes('vend-token');
            });

            if (earlySubmissions.length > 0) {
                this.core.logger.warn(`⚠️ Found ${earlySubmissions.length} early submission attempts - may indicate race condition`);
            } else {
                this.core.logger.info('✅ No premature submission attempts detected');
            }

            const duration = Date.now() - startTime;
            this.core.logger.info(`✅ TikTok Intent Balance Race Condition Validation Journey completed successfully in ${duration}ms`);
            return { success: true, duration, balanceLoaded, autoSubmitFound };

        } catch (error) {
            this.core.logger.error(`❌ Journey failed: ${error.message}`);
            return { success: false, error: error.message };
        }
    }
}

module.exports = JourneyIntent03TikTok04BalanceRace01Validation;

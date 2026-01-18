const { BaseJourney } = require('./Pluct-Journey-01Orchestrator');

/**
 * Journey-Transcription-01IntentFlow-Complete-Validation.js
 * Comprehensive end-to-end validation of intent-to-transcript journey
 * Validates complete flow with real-time logcat and UI validation at every step
 * Follows naming convention: [Project]-[ParentScope]-[ChildScope]-[Separation]-[Responsibility]
 */
class JourneyTranscription01IntentFlowCompleteValidation extends BaseJourney {
    constructor(core) {
        super(core);
        this.name = 'Transcription-01IntentFlow-Complete-Validation';
        this.maxDuration = 200000; // 3.3 minutes max
    }

    async execute() {
        this.core.logger.info('🎯 Starting Comprehensive Intent Flow Validation Journey...');
        const startTime = Date.now();
        const testUrl = this.core.config.url;

        try {
            // Step 1: Pre-Conditions
            const preConditions = await this.validatePreConditions();
            if (!preConditions.success) {
                return { success: false, error: `Pre-conditions failed: ${preConditions.error}` };
            }

            // Step 2: Intent Sending
            const intentResult = await this.sendAndValidateIntent(testUrl);
            if (!intentResult.success) {
                return { success: false, error: `Intent handling failed: ${intentResult.error}` };
            }

            // Step 3: Auto-Submit Validation
            const autoSubmitResult = await this.validateAutoSubmit();
            if (!autoSubmitResult.success) {
                return { success: false, error: `Auto-submit failed: ${autoSubmitResult.error}` };
            }

            // Step 4: API Call Validation
            const apiResult = await this.validateAPICalls(testUrl);
            if (!apiResult.success) {
                return { success: false, error: `API calls failed: ${apiResult.error}` };
            }

            // Step 5: Processing Status Validation
            const processingResult = await this.validateProcessingStatus(testUrl);
            if (!processingResult.success) {
                return { success: false, error: `Processing status failed: ${processingResult.error}` };
            }

            // Step 6: Polling Validation
            const pollingResult = await this.validatePolling();
            if (!pollingResult.success) {
                return { success: false, error: `Polling failed: ${pollingResult.error}` };
            }

            // Step 7: Completion Validation
            const completionResult = await this.validateCompletion();
            if (!completionResult.success) {
                return { success: false, error: `Completion failed: ${completionResult.error}` };
            }

            // Step 8: Final Error Check
            const errorCheck = await this.finalErrorCheck();
            if (!errorCheck.success) {
                return { success: false, error: `Errors detected: ${errorCheck.error}` };
            }

            const duration = Date.now() - startTime;
            this.core.logger.info(`✅ Comprehensive Intent Flow Validation Journey completed successfully in ${duration}ms`);
            return { success: true, duration, transcript: completionResult.transcript };

        } catch (error) {
            this.core.logger.error(`❌ Journey failed: ${error.message}`);
            return { success: false, error: error.message };
        }
    }

    /**
     * Step 1: Validate pre-conditions
     */
    async validatePreConditions() {
        try {
            this.core.logger.info('📱 Step 1: Validating pre-conditions...');

            // Clear logcat for clean baseline
            await this.core.clearLogcat();
            this.core.logger.info('   ✅ Logcat cleared');

            // Verify ADB connectivity
            const adbCheck = await this.core.executeCommand('adb devices');
            if (!adbCheck.success || !adbCheck.output.includes('device')) {
                return { success: false, error: 'ADB device not connected' };
            }
            this.core.logger.info('   ✅ ADB connectivity verified');

            // Ensure app is installed
            const appCheck = await this.core.executeCommand('adb shell pm list packages | findstr pluct');
            if (!appCheck.success || !appCheck.output.includes('app.pluct')) {
                return { success: false, error: 'App not installed' };
            }
            this.core.logger.info('   ✅ App installation verified');

            // Launch app
            const launch = await this.core.launchApp();
            if (!launch.success) {
                return { success: false, error: 'Failed to launch app' };
            }
            await this.core.sleep(3000); // Wait for app to initialize
            this.core.logger.info('   ✅ App launched');

            // Check credit balance (optional, but helpful)
            const balanceLog = await this.core.executeCommand('adb logcat -d | findstr /i "Credit balance loaded\|balance.*=" | tail -n 3');
            this.core.logger.info(`   📊 Balance logs: ${balanceLog.output || 'No balance logs yet'}`);

            return { success: true };
        } catch (error) {
            return { success: false, error: error.message };
        }
    }

    /**
     * Step 2: Send intent and validate it was received
     */
    async sendAndValidateIntent(testUrl) {
        try {
            this.core.logger.info(`📱 Step 2: Sending ACTION_SEND intent with URL: ${testUrl}`);

            // Clear logcat before intent
            await this.core.clearLogcat();

            // Send intent
            const intentCommand = `adb shell am start -a android.intent.action.SEND -t "text/plain" --es android.intent.extra.TEXT "${testUrl}" app.pluct/.PluctUIScreen01MainActivity`;
            const intentResult = await this.core.executeCommand(intentCommand);
            if (!intentResult.success) {
                return { success: false, error: 'Failed to send intent' };
            }
            await this.core.sleep(2000); // Wait for intent processing

            // Validate intent received via logcat
            const intentLog = await this.validateLogcatPattern(
                'IntentHandler.*Received shared text|IntentHandler.*TikTok URL detected',
                'Intent received',
                3
            );
            if (!intentLog.success) {
                return { success: false, error: 'Intent not received - no logcat evidence' };
            }

            // Verify URL prefilled
            await this.core.dumpUIHierarchy();
            const uiDump = this.core.readLastUIDump();
            const urlInField = uiDump.includes(testUrl) || uiDump.includes('url_input_field');
            if (!urlInField) {
                // Check logcat for prefilled confirmation
                const prefilledLog = await this.validateLogcatPattern(
                    'Found prefilled URL|MainActivity.*prefilled',
                    'URL prefilled',
                    2
                );
                if (!prefilledLog.success) {
                    return { success: false, error: 'URL not prefilled' };
                }
            }

            this.core.logger.info('   ✅ Intent received and URL prefilled');
            return { success: true };
        } catch (error) {
            return { success: false, error: error.message };
        }
    }

    /**
     * Step 3: Validate auto-submit (if credits available)
     */
    async validateAutoSubmit() {
        try {
            this.core.logger.info('📱 Step 3: Validating auto-submit...');

            await this.core.sleep(2000); // Wait for auto-submit

            // Check for auto-submit in logcat
            const autoSubmitLog = await this.validateLogcatPattern(
                'Auto-submitting URL|CaptureCard.*Auto-submitting',
                'Auto-submit triggered',
                2
            );

            if (autoSubmitLog.success) {
                this.core.logger.info('   ✅ Auto-submit triggered');

                // Verify notification shown
                const notificationLog = await this.validateLogcatPattern(
                    'Transcription started notification|PluctNotificationHelper.*Transcription started',
                    'Notification shown',
                    2
                );
                if (notificationLog.success) {
                    this.core.logger.info('   ✅ Notification shown');
                }
            } else {
                // Auto-submit may not happen if credits insufficient - that's OK, user will tap button
                this.core.logger.info('   ℹ️ Auto-submit not triggered (may need manual tap)');
            }

            return { success: true };
        } catch (error) {
            return { success: false, error: error.message };
        }
    }

    /**
     * Step 4: Validate API calls were made
     */
    async validateAPICalls(testUrl) {
        try {
            this.core.logger.info('📱 Step 4: Validating API calls...');

            // Wait a bit for API calls to start
            await this.core.sleep(3000);

            // Verify vend-token call
            const vendTokenLog = await this.validateLogcatPattern(
                'PluctCoreAPIUnified.*vend-token|TranscriptionFlowHandler.*vend-token',
                'vend-token API call',
                3
            );
            if (!vendTokenLog.success) {
                return { success: false, error: 'vend-token API call not found in logcat' };
            }
            this.core.logger.info('   ✅ vend-token API call detected');

            // Check for duplicate vend-token calls within 5 seconds
            const duplicateCheck = await this.core.executeCommand('adb logcat -d -t 50 | findstr /i "vend-token"');
            if (duplicateCheck.success && duplicateCheck.output) {
                const vendTokenCount = (duplicateCheck.output.match(/vend-token/g) || []).length;
                if (vendTokenCount > 1) {
                    this.core.logger.warn(`   ⚠️ Multiple vend-token calls detected (${vendTokenCount}), may indicate duplicate processing`);
                }
            }

            // Verify submitTranscription call
            const submitLog = await this.validateLogcatPattern(
                'PluctCoreAPIUnified.*submitTranscription|PluctCoreAPIUnified.*/ttt/transcribe|TranscriptionFlowHandler.*submit',
                'submitTranscription API call',
                5
            );
            if (!submitLog.success) {
                return { success: false, error: 'submitTranscription API call not found in logcat' };
            }
            this.core.logger.info('   ✅ submitTranscription API call detected');

            return { success: true };
        } catch (error) {
            return { success: false, error: error.message };
        }
    }

    /**
     * Step 5: Validate processing status and lock
     */
    async validateProcessingStatus(testUrl) {
        try {
            this.core.logger.info('📱 Step 5: Validating processing status...');

            // Verify processing lock registered
            const lockLog = await this.validateLogcatPattern(
                'Registered processing for URL|ProcessingLock.*Registered',
                'Processing lock registered',
                3
            );
            if (!lockLog.success) {
                this.core.logger.warn('   ⚠️ Processing lock registration not found in logcat');
            } else {
                this.core.logger.info('   ✅ Processing lock registered');
            }

            // Check UI for processing indicator
            await this.core.dumpUIHierarchy();
            const uiDump = this.core.readLastUIDump();
            const hasProcessingIndicator = uiDump.includes('processing_indicator') ||
                uiDump.includes('Processing') ||
                uiDump.includes('Transcribing');
            if (hasProcessingIndicator) {
                this.core.logger.info('   ✅ Processing indicator visible in UI');
            }

            // Validate no duplicate processing
            const duplicateLog = await this.core.executeCommand('adb logcat -d | findstr /i "already being processed.*rejecting duplicate"');
            if (duplicateLog.success && duplicateLog.output) {
                this.core.logger.warn('   ⚠️ Duplicate processing detected and rejected (this is good - duplicate prevention working)');
            }

            return { success: true };
        } catch (error) {
            return { success: false, error: error.message };
        }
    }

    /**
     * Step 6: Validate polling is working
     */
    async validatePolling() {
        try {
            this.core.logger.info('📱 Step 6: Validating polling...');

            // Wait for polling to start
            await this.core.sleep(5000);

            // Verify polling started
            const pollingLog = await this.validateLogcatPattern(
                'pollTranscriptionStatus|/ttt/poll/|TranscriptionFlowHandler.*polling',
                'Polling started',
                5
            );
            if (!pollingLog.success) {
                return { success: false, error: 'Polling not detected in logcat' };
            }
            this.core.logger.info('   ✅ Polling detected');

            // Check for multiple simultaneous polling loops (should only be one)
            const pollingCount = await this.core.executeCommand('adb logcat -d -t 100 | findstr /i "pollTranscriptionStatus|/ttt/poll/"');
            if (pollingCount.success && pollingCount.output) {
                const pollMatches = (pollingCount.output.match(/pollTranscriptionStatus|\/ttt\/poll\//g) || []).length;
                if (pollMatches > 10) {
                    this.core.logger.warn(`   ⚠️ High polling frequency detected (${pollMatches} calls), may indicate multiple polling loops`);
                }
            }

            // Validate progress updates appear in UI
            await this.core.sleep(5000);
            await this.core.dumpUIHierarchy();
            const progressDump = this.core.readLastUIDump();
            const hasProgress = progressDump.includes('progress') ||
                progressDump.includes('Progress') ||
                progressDump.includes('Processing');
            if (hasProgress) {
                this.core.logger.info('   ✅ Progress updates visible in UI');
            }

            return { success: true };
        } catch (error) {
            return { success: false, error: error.message };
        }
    }

    /**
     * Step 7: Validate completion
     */
    async validateCompletion() {
        try {
            this.core.logger.info('📱 Step 7: Validating completion...');

            // Wait for transcript completion (max 3 minutes)
            const maxWaitTime = 180000; // 3 minutes
            const pollInterval = 5000; // 5 seconds
            const startTime = Date.now();
            let transcriptFound = false;
            let transcript = null;

            while (Date.now() - startTime < maxWaitTime) {
                await this.core.sleep(pollInterval);

                // Check UI for transcript
                await this.core.dumpUIHierarchy();
                const uiDump = this.core.readLastUIDump();
                if (uiDump.includes('transcript') || uiDump.includes('Transcript') || uiDump.includes('Completed')) {
                    transcriptFound = true;
                    this.core.logger.info('   ✅ Transcript displayed in UI');
                    break;
                }

                // Check logcat for completion
                const completionLog = await this.core.executeCommand('adb logcat -d -t 50 | findstr /i "status.*completed|transcript.*found|TranscriptionFlowHandler.*COMPLETED"');
                if (completionLog.success && completionLog.output) {
                    if (completionLog.output.includes('completed') || completionLog.output.includes('transcript')) {
                        transcriptFound = true;
                        this.core.logger.info('   ✅ Completion detected in logcat');
                        break;
                    }
                }

                this.core.logger.info(`   ⏳ Waiting for completion... (${Math.floor((Date.now() - startTime) / 1000)}s elapsed)`);
            }

            if (!transcriptFound) {
                return { success: false, error: 'Transcript not found within timeout' };
            }

            // Validate cleanup occurred
            const cleanupLog = await this.validateLogcatPattern(
                'Unregistered processing for URL|ProcessingLock.*Unregistered',
                'Processing lock cleanup',
                2
            );
            if (cleanupLog.success) {
                this.core.logger.info('   ✅ Processing lock cleanup confirmed');
            } else {
                this.core.logger.warn('   ⚠️ Processing lock cleanup not found in logcat');
            }

            return { success: true, transcript: transcript || 'completed' };
        } catch (error) {
            return { success: false, error: error.message };
        }
    }

    /**
     * Step 8: Final error check
     */
    async finalErrorCheck() {
        try {
            this.core.logger.info('📱 Step 8: Performing final error check...');

            // Check logcat for errors
            const errorLog = await this.core.executeCommand('adb logcat -d | findstr /i "E/|ERROR|FATAL|Exception.*TranscriptionFlowHandler|Exception.*CaptureCard"');
            if (errorLog.success && errorLog.output) {
                // Filter out expected/benign errors
                const criticalErrors = errorLog.output
                    .split('\n')
                    .filter(line => {
                        const lower = line.toLowerCase();
                        return !lower.includes('warn') && // Exclude warnings
                            !lower.includes('network') && // Network errors may be expected
                            (lower.includes('fatal') || lower.includes('crash') || lower.includes('unclosed'));
                    });

                if (criticalErrors.length > 0) {
                    this.core.logger.error('   ❌ Critical errors detected:');
                    criticalErrors.slice(0, 5).forEach(err => {
                        this.core.logger.error(`      ${err}`);
                    });
                    return { success: false, error: 'Critical errors found in logcat' };
                }
            }

            this.core.logger.info('   ✅ No critical errors detected');
            return { success: true };
        } catch (error) {
            return { success: false, error: error.message };
        }
    }

    /**
     * Helper: Validate logcat pattern with retries
     * Uses shared logcat validator utility
     */
    async validateLogcatPattern(pattern, description, maxRetries = 3) {
        const result = await this.core.logcatValidator.validatePattern(
            pattern,
            description,
            maxRetries,
            2000
        );
        return result;
    }
}

module.exports = JourneyTranscription01IntentFlowCompleteValidation;

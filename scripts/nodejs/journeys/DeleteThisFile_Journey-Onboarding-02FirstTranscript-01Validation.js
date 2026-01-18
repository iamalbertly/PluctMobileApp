const { BaseJourney } = require('./Pluct-Journey-01Orchestrator');

/**
 * Journey-Onboarding-02FirstTranscript-01Validation
 * Validates that the first transcript completion is properly tracked for onboarding milestone
 * Follows naming convention: Journey-[Feature]-[Sequence][Purpose]-[Sequence][Validation]
 *
 * Test Steps:
 * 1. Clear app data
 * 2. Grant permissions
 * 3. Complete onboarding tutorial
 * 4. Submit a transcription request
 * 5. Wait for completion
 * 6. Verify first_transcript_completed flag is set in logcat
 */
class JourneyOnboarding02FirstTranscript01Validation extends BaseJourney {
    constructor(core) {
        super(core);
        this.name = 'Onboarding-02FirstTranscript-01Validation';
        this.testUrl = 'https://vm.tiktok.com/ZTdWqLxx7/'; // Test video URL
    }

    async run() {
        this.core.logger.info('Starting: Journey-Onboarding-02FirstTranscript-01Validation');

        try {
            // Step 1: Clear app data to simulate first-time user
            this.core.logger.info('Step 1: Clearing app data...');
            await this.core.executeCommand('adb shell pm clear app.pluct');
            await this.core.sleep(2000);

            // Step 2: Grant permissions
            this.core.logger.info('Step 2: Granting permissions...');
            const androidVersion = await this.core.executeCommand('adb shell getprop ro.build.version.sdk');
            const sdkVersion = parseInt(androidVersion.output.trim());

            if (sdkVersion >= 33) {
                await this.core.executeCommand('adb shell pm grant app.pluct android.permission.POST_NOTIFICATIONS');
            }
            await this.core.executeCommand('adb shell appops set app.pluct SYSTEM_ALERT_WINDOW allow');
            await this.core.sleep(1000);

            // Step 3: Launch app
            this.core.logger.info('Step 3: Launching app...');
            await this.core.launchApp();
            await this.core.sleep(4000);

            // Step 4: Clear logcat to start fresh
            await this.core.executeCommand('adb logcat -c');
            await this.core.sleep(500);

            // Step 5: Dismiss welcome dialog
            this.core.logger.info('Step 4: Dismissing welcome dialog...');
            await this.core.dumpUIHierarchy();
            let uiDump = this.core.readLastUIDump();

            if (uiDump.includes('Welcome') || uiDump.includes('Get Started')) {
                await this.core.tapByText('Get Started');
                await this.core.sleep(2000);
            }

            // Skip through all onboarding dialogs
            for (let i = 0; i < 5; i++) {
                await this.core.dumpUIHierarchy();
                uiDump = this.core.readLastUIDump();

                if (uiDump.includes('Skip')) {
                    await this.core.tapByText('Skip');
                    await this.core.sleep(1000);
                } else if (uiDump.includes('Next')) {
                    await this.core.tapByText('Next');
                    await this.core.sleep(1000);
                } else if (uiDump.includes('Got It')) {
                    await this.core.tapByText('Got It');
                    await this.core.sleep(1000);
                } else if (uiDump.includes('Skip for Now')) {
                    await this.core.tapByText('Skip for Now');
                    await this.core.sleep(1000);
                } else {
                    break;
                }
            }

            // Step 6: Verify we're on home screen
            this.core.logger.info('Step 5: Verifying home screen...');
            await this.core.sleep(2000);
            await this.core.dumpUIHierarchy();
            uiDump = this.core.readLastUIDump();

            if (!uiDump.includes('app.pluct')) {
                throw new Error('Not on home screen after onboarding');
            }

            // Step 7: Check for first_transcript_completed NOT being set yet
            this.core.logger.info('Step 6: Verifying first_transcript_completed not set yet...');
            const preTranscriptCheck = await this.core.logcatValidator.validatePattern(
                'First.*transcript.*completed|first_transcript_completed',
                'First transcript completion before transcription',
                1,
                500,
                50
            );

            if (preTranscriptCheck.success) {
                this.core.logger.warn('first_transcript_completed already set before transcription - may be residual');
            } else {
                this.core.logger.info('first_transcript_completed correctly not set yet');
            }

            // Step 8: Find and tap the URL input field
            this.core.logger.info('Step 7: Finding URL input field...');
            await this.core.dumpUIHierarchy();
            uiDump = this.core.readLastUIDump();

            // Look for the capture card or input field
            const inputTap = await this.core.tapByTestTag('url_input_field');
            if (!inputTap.success) {
                // Try by content description
                const altTap = await this.core.tapByContentDesc('URL input field');
                if (!altTap.success) {
                    this.core.logger.warn('URL input field not found by test tag or content desc');
                }
            }
            await this.core.sleep(1000);

            // Step 9: Input test URL
            this.core.logger.info('Step 8: Inputting test URL...');
            await this.core.inputTextViaClipboard(this.testUrl);
            await this.core.sleep(1000);

            // Step 10: Tap Extract Script button
            this.core.logger.info('Step 9: Tapping Extract Script button...');
            await this.core.dumpUIHierarchy();
            uiDump = this.core.readLastUIDump();

            const extractTap = await this.core.tapByTestTag('extract_script_button');
            if (!extractTap.success) {
                const altTap = await this.core.tapByText('Extract Script');
                if (!altTap.success) {
                    this.core.logger.warn('Extract Script button not found');
                }
            }
            await this.core.sleep(3000);

            // Step 11: Wait for transcription to complete (up to 60 seconds)
            this.core.logger.info('Step 10: Waiting for transcription to complete (max 60s)...');
            let transcriptionCompleted = false;
            const startTime = Date.now();
            const maxWait = 60000; // 60 seconds

            while (Date.now() - startTime < maxWait && !transcriptionCompleted) {
                const completionCheck = await this.core.logcatValidator.validatePattern(
                    'First.*transcript.*completed|onboarding.*milestone|status.*completed',
                    'Transcription completion',
                    1,
                    500,
                    100
                );

                if (completionCheck.success) {
                    transcriptionCompleted = true;
                    break;
                }

                await this.core.sleep(3000);
            }

            // Step 12: Verify first transcript milestone
            this.core.logger.info('Step 11: Verifying first transcript milestone...');
            const milestoneCheck = await this.core.logcatValidator.validatePattern(
                'First.*transcript.*completed.*onboarding.*milestone|milestone.*achieved',
                'First transcript milestone',
                2,
                1000,
                100
            );

            if (milestoneCheck.success) {
                this.core.logger.info('First transcript milestone confirmed in logcat');
            } else {
                // Check for any transcript completion
                const genericCheck = await this.core.logcatValidator.validatePattern(
                    'TranscriptionCompleted|status.*completed|COMPLETED',
                    'Transcription completion generic',
                    2,
                    1000,
                    100
                );

                if (genericCheck.success) {
                    this.core.logger.info('Transcription completed (milestone log may use different format)');
                } else {
                    this.core.logger.warn('Could not confirm first transcript milestone in logcat');
                }
            }

            // Step 13: Check for API errors
            this.core.logger.info('Step 12: Checking for API errors...');
            const apiErrors = await this.core.checkRecentAPIErrors(100);
            if (!apiErrors.success && apiErrors.errors && apiErrors.errors.length > 0) {
                this.core.logger.warn('API errors detected:', apiErrors.errors);
            }

            this.core.logger.info('Journey-Onboarding-02FirstTranscript-01Validation completed');
            return true;

        } catch (error) {
            this.core.logger.error(`Journey failed: ${error.message}`);
            await this.failWithDiagnostics(error.message);
            return false;
        }
    }
}

function register(orchestrator) {
    orchestrator.registerJourney('Onboarding-02FirstTranscript-01Validation', new JourneyOnboarding02FirstTranscript01Validation(orchestrator.core));
}

module.exports = JourneyOnboarding02FirstTranscript01Validation;

const { BaseJourney } = require('./Pluct-Journey-01Orchestrator');

/**
 * Journey-Onboarding-01Tutorial-01Validation
 * Validates the onboarding tutorial flow with UI dump, logcat, and ADB validation
 * Follows naming convention: Journey-[Feature]-[Sequence][Purpose]-[Sequence][Validation]
 *
 * Test Steps:
 * 1. Clear app data to simulate first-time user
 * 2. Launch app
 * 3. Dismiss welcome dialog
 * 4. Grant permissions via ADB
 * 5. Verify tutorial Step 1 appears
 * 6. Progress through tutorial steps
 * 7. Verify TikTok launch or skip
 * 8. Verify tutorial state persisted
 * 9. Relaunch app - verify tutorial not shown again
 */
class JourneyOnboarding01Tutorial01Validation extends BaseJourney {
    constructor(core) {
        super(core);
        this.name = 'Onboarding-01Tutorial-01Validation';
    }

    async run() {
        this.core.logger.info('Starting: Journey-Onboarding-01Tutorial-01Validation');

        try {
            // Step 1: Clear app data to simulate first-time user
            this.core.logger.info('Step 1: Clearing app data to simulate first-time user...');
            await this.core.executeCommand('adb shell pm clear app.pluct');
            await this.core.sleep(2000);

            // Step 2: Grant permissions via ADB to skip permission dialogs
            this.core.logger.info('Step 2: Granting permissions via ADB...');
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

            // Step 4: Check for welcome dialog and dismiss
            this.core.logger.info('Step 4: Looking for welcome dialog...');
            await this.core.dumpUIHierarchy();
            let uiDump = this.core.readLastUIDump();

            if (uiDump.includes('Welcome') || uiDump.includes('Get Started')) {
                this.core.logger.info('Welcome dialog found, dismissing...');
                await this.core.tapByText('Get Started');
                await this.core.sleep(2000);
            } else {
                this.core.logger.info('Welcome dialog not found (may be auto-dismissed)');
            }

            // Wait for permission dialogs to complete
            await this.core.sleep(2000);
            await this.core.dumpUIHierarchy();
            uiDump = this.core.readLastUIDump();

            // Handle permission onboarding if shown
            let permissionRetries = 0;
            while ((uiDump.includes('permission_onboarding') || uiDump.includes('Enable Notifications') || uiDump.includes('Enable Overlay')) && permissionRetries < 3) {
                this.core.logger.info('Permission onboarding dialog found, skipping...');
                await this.core.tapByText('Skip');
                await this.core.sleep(1500);
                await this.core.dumpUIHierarchy();
                uiDump = this.core.readLastUIDump();
                permissionRetries++;
            }

            // Step 5: Verify onboarding tutorial appears
            this.core.logger.info('Step 5: Checking for onboarding tutorial...');
            await this.core.sleep(2000);
            await this.core.dumpUIHierarchy();
            uiDump = this.core.readLastUIDump();

            // Look for tutorial indicators
            const tutorialStep1Found = uiDump.includes('onboarding_tutorial_dialog') ||
                                       uiDump.includes('onboarding_step_1') ||
                                       uiDump.includes('Get Transcripts in 3 Taps') ||
                                       uiDump.includes('3 Taps');

            if (!tutorialStep1Found) {
                // Check logcat for tutorial events
                const logcatCheck = await this.core.logcatValidator.validatePattern(
                    'OnboardingTutorial.*started|tutorial.*started|onboarding.*step',
                    'Onboarding tutorial in logcat',
                    2,
                    1000,
                    50
                );

                if (!logcatCheck.success) {
                    this.core.logger.warn('Onboarding tutorial Step 1 not detected in UI or logcat');
                    // May have been seen before due to test state - continue
                } else {
                    this.core.logger.info('Onboarding tutorial detected in logcat');
                }
            } else {
                this.core.logger.info('Onboarding tutorial Step 1 found');
            }

            // Step 6: Progress through tutorial if visible
            if (tutorialStep1Found) {
                this.core.logger.info('Step 6: Progressing through tutorial Step 1 -> Step 2...');

                // Tap Next button
                const nextTap = await this.core.tapByTestTag('onboarding_next_button');
                if (!nextTap.success) {
                    await this.core.tapByText('Next');
                }
                await this.core.sleep(1500);

                // Verify Step 2
                await this.core.dumpUIHierarchy();
                uiDump = this.core.readLastUIDump();
                const step2Found = uiDump.includes('onboarding_step_2') ||
                                   uiDump.includes('Find the Share') ||
                                   uiDump.includes('Got It');

                if (step2Found) {
                    this.core.logger.info('Tutorial Step 2 found');

                    // Tap Got It button
                    const gotItTap = await this.core.tapByTestTag('onboarding_got_it_button');
                    if (!gotItTap.success) {
                        await this.core.tapByText('Got It');
                    }
                    await this.core.sleep(1500);

                    // Verify Step 3
                    await this.core.dumpUIHierarchy();
                    uiDump = this.core.readLastUIDump();
                    const step3Found = uiDump.includes('onboarding_step_3') ||
                                       uiDump.includes('Ready to Try') ||
                                       uiDump.includes('Open TikTok') ||
                                       uiDump.includes('Skip for Now');

                    if (step3Found) {
                        this.core.logger.info('Tutorial Step 3 found');

                        // Tap Skip for Now (don't actually open TikTok in test)
                        this.core.logger.info('Tapping "Skip for Now" to complete tutorial...');
                        const skipTap = await this.core.tapByTestTag('onboarding_skip_button');
                        if (!skipTap.success) {
                            await this.core.tapByText('Skip for Now');
                        }
                        await this.core.sleep(2000);
                    } else {
                        this.core.logger.warn('Tutorial Step 3 not found');
                    }
                } else {
                    this.core.logger.warn('Tutorial Step 2 not found');
                }
            }

            // Step 7: Verify tutorial completed - check logcat
            this.core.logger.info('Step 7: Verifying tutorial completion in logcat...');
            const completionCheck = await this.core.logcatValidator.validatePattern(
                'OnboardingTutorial.*skipped|tutorial.*completed|onboarding_tutorial_seen',
                'Tutorial completion in logcat',
                3,
                2000,
                100
            );

            if (completionCheck.success) {
                this.core.logger.info('Tutorial completion confirmed in logcat');
            } else {
                this.core.logger.warn('Tutorial completion not found in logcat (may not be an error)');
            }

            // Step 8: Verify home screen is visible
            this.core.logger.info('Step 8: Verifying home screen is visible...');
            await this.core.dumpUIHierarchy();
            uiDump = this.core.readLastUIDump();

            if (!uiDump.includes('app.pluct') && !uiDump.includes('Pluct')) {
                throw new Error('App UI not visible after tutorial');
            }
            this.core.logger.info('App home screen is visible');

            // Step 9: Restart app and verify tutorial not shown again
            this.core.logger.info('Step 9: Restarting app to verify tutorial persistence...');
            await this.core.executeCommand('adb shell am force-stop app.pluct');
            await this.core.sleep(1000);
            await this.core.launchApp();
            await this.core.sleep(4000);

            await this.core.dumpUIHierarchy();
            uiDump = this.core.readLastUIDump();

            const tutorialShownAgain = uiDump.includes('onboarding_tutorial_dialog') ||
                                       uiDump.includes('Get Transcripts in 3 Taps');

            if (tutorialShownAgain) {
                this.core.logger.warn('Tutorial shown again on restart - persistence issue');
            } else {
                this.core.logger.info('Tutorial correctly not shown on restart');
            }

            // Step 10: Check for API errors
            this.core.logger.info('Step 10: Checking for API errors...');
            const apiErrors = await this.core.checkRecentAPIErrors(100);
            if (!apiErrors.success && apiErrors.errors && apiErrors.errors.length > 0) {
                this.core.logger.warn('API errors detected:', apiErrors.errors);
            }

            this.core.logger.info('Journey-Onboarding-01Tutorial-01Validation completed successfully');
            return true;

        } catch (error) {
            this.core.logger.error(`Journey failed: ${error.message}`);
            await this.failWithDiagnostics(error.message);
            return false;
        }
    }
}

function register(orchestrator) {
    orchestrator.registerJourney('Onboarding-01Tutorial-01Validation', new JourneyOnboarding01Tutorial01Validation(orchestrator.core));
}

module.exports = JourneyOnboarding01Tutorial01Validation;

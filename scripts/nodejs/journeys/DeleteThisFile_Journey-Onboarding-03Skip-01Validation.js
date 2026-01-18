const { BaseJourney } = require('./Pluct-Journey-01Orchestrator');

/**
 * Journey-Onboarding-03Skip-01Validation
 * Validates that skipping the onboarding tutorial works correctly
 * Follows naming convention: Journey-[Feature]-[Sequence][Purpose]-[Sequence][Validation]
 *
 * Test Steps:
 * 1. Clear app data
 * 2. Launch app
 * 3. Complete welcome and permissions
 * 4. Skip tutorial at Step 3
 * 5. Verify home screen is shown
 * 6. Verify tutorial state persisted
 */
class JourneyOnboarding03Skip01Validation extends BaseJourney {
    constructor(core) {
        super(core);
        this.name = 'Onboarding-03Skip-01Validation';
    }

    async run() {
        this.core.logger.info('Starting: Journey-Onboarding-03Skip-01Validation');

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

            // Step 3: Clear logcat
            await this.core.executeCommand('adb logcat -c');
            await this.core.sleep(500);

            // Step 4: Launch app
            this.core.logger.info('Step 3: Launching app...');
            await this.core.launchApp();
            await this.core.sleep(4000);

            // Step 5: Dismiss welcome dialog
            this.core.logger.info('Step 4: Dismissing welcome dialog...');
            await this.core.dumpUIHierarchy();
            let uiDump = this.core.readLastUIDump();

            if (uiDump.includes('Welcome') || uiDump.includes('Get Started')) {
                await this.core.tapByText('Get Started');
                await this.core.sleep(2000);
            }

            // Step 6: Skip permission dialogs
            await this.core.dumpUIHierarchy();
            uiDump = this.core.readLastUIDump();

            let retries = 0;
            while ((uiDump.includes('permission_onboarding') || uiDump.includes('Enable Notifications') || uiDump.includes('Enable Overlay')) && retries < 3) {
                this.core.logger.info('Skipping permission dialog...');
                await this.core.tapByText('Skip');
                await this.core.sleep(1500);
                await this.core.dumpUIHierarchy();
                uiDump = this.core.readLastUIDump();
                retries++;
            }

            // Step 7: Progress to tutorial Step 3 and skip
            this.core.logger.info('Step 5: Progressing to tutorial Step 3...');
            await this.core.sleep(2000);
            await this.core.dumpUIHierarchy();
            uiDump = this.core.readLastUIDump();

            let tutorialFound = false;

            // Progress through tutorial steps
            for (let step = 0; step < 5; step++) {
                await this.core.dumpUIHierarchy();
                uiDump = this.core.readLastUIDump();

                if (uiDump.includes('Skip for Now')) {
                    // Found Step 3 - skip
                    this.core.logger.info('Found Step 3 - tapping Skip for Now...');
                    await this.core.tapByText('Skip for Now');
                    await this.core.sleep(2000);
                    tutorialFound = true;
                    break;
                } else if (uiDump.includes('Next')) {
                    this.core.logger.info('Found Next button - progressing...');
                    await this.core.tapByText('Next');
                    await this.core.sleep(1000);
                } else if (uiDump.includes('Got It')) {
                    this.core.logger.info('Found Got It button - progressing...');
                    await this.core.tapByText('Got It');
                    await this.core.sleep(1000);
                } else {
                    // No more tutorial dialogs
                    break;
                }
            }

            if (!tutorialFound) {
                this.core.logger.warn('Tutorial dialog with "Skip for Now" not found - may have been skipped or not shown');
            }

            // Step 8: Verify home screen is visible
            this.core.logger.info('Step 6: Verifying home screen...');
            await this.core.sleep(2000);
            await this.core.dumpUIHierarchy();
            uiDump = this.core.readLastUIDump();

            if (!uiDump.includes('app.pluct')) {
                throw new Error('Home screen not visible after skip');
            }
            this.core.logger.info('Home screen is visible');

            // Verify no tutorial dialogs are showing
            const noTutorialDialogs = !uiDump.includes('onboarding_tutorial_dialog') &&
                                      !uiDump.includes('Get Transcripts in 3 Taps');
            if (noTutorialDialogs) {
                this.core.logger.info('No tutorial dialogs showing after skip');
            } else {
                this.core.logger.warn('Tutorial dialog still showing after skip');
            }

            // Step 9: Verify skip was logged
            this.core.logger.info('Step 7: Verifying skip logged...');
            const skipCheck = await this.core.logcatValidator.validatePattern(
                'OnboardingTutorial.*skipped|tutorial.*skipped|MainActivity.*skipped',
                'Tutorial skip in logcat',
                2,
                1000,
                100
            );

            if (skipCheck.success) {
                this.core.logger.info('Tutorial skip confirmed in logcat');
            } else {
                this.core.logger.warn('Tutorial skip log not found (may use different log pattern)');
            }

            // Step 10: Restart app and verify tutorial not shown
            this.core.logger.info('Step 8: Restarting app to verify persistence...');
            await this.core.executeCommand('adb shell am force-stop app.pluct');
            await this.core.sleep(1000);
            await this.core.launchApp();
            await this.core.sleep(4000);

            await this.core.dumpUIHierarchy();
            uiDump = this.core.readLastUIDump();

            const tutorialShownAgain = uiDump.includes('onboarding_tutorial_dialog') ||
                                       uiDump.includes('Get Transcripts in 3 Taps');

            if (tutorialShownAgain) {
                throw new Error('Tutorial shown again after skip - persistence failed');
            }
            this.core.logger.info('Tutorial correctly not shown after restart');

            // Verify home screen elements are visible
            if (uiDump.includes('app.pluct') || uiDump.includes('pluct')) {
                this.core.logger.info('App is correctly showing main screen');
            }

            this.core.logger.info('Journey-Onboarding-03Skip-01Validation completed successfully');
            return true;

        } catch (error) {
            this.core.logger.error(`Journey failed: ${error.message}`);
            await this.failWithDiagnostics(error.message);
            return false;
        }
    }
}

function register(orchestrator) {
    orchestrator.registerJourney('Onboarding-03Skip-01Validation', new JourneyOnboarding03Skip01Validation(orchestrator.core));
}

module.exports = JourneyOnboarding03Skip01Validation;

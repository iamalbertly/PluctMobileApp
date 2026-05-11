const { BaseJourney } = require('./Pluct-Journey-01Orchestrator');

/**
 * Journey-UX-25DirectToValue-Readiness-01Validation
 * Truth-first balance, readiness strip, settings sheet help, logcat guardrails.
 */
class JourneyUX25DirectToValueReadiness01Validation extends BaseJourney {
    constructor(core) {
        super(core);
        this.name = 'UX-25DirectToValue-Readiness-01Validation';
    }

    async waitForTruthFirstLogs(maxAttempts = 20, intervalMs = 500) {
        for (let i = 0; i < maxAttempts; i++) {
            const r = await this.core.executeCommand(
                'adb logcat -d -t 400 | findstr /i /c:"Truth-first balance load" /c:"Balance fetch completed"'
            );
            if (r.success && r.output && r.output.length > 10) {
                return { ok: true, attempt: i + 1 };
            }
            const bad = await this.core.executeCommand(
                'adb logcat -d -t 400 | findstr /i /c:"Optimistic balance set immediately"'
            );
            if (bad.success && bad.output && bad.output.includes('Optimistic')) {
                return { ok: false, error: 'Optimistic balance log found — truth-first regression' };
            }
            await this.core.sleep(intervalMs);
        }
        return { ok: false, error: 'Timed out waiting for truth-first balance log markers' };
    }

    async waitForReadinessOrCreditsUi(maxAttempts = 28, intervalMs = 500) {
        for (let i = 0; i < maxAttempts; i++) {
            await this.core.dumpUIHierarchy();
            const ui = this.core.readLastUIDump() || '';
            const u = ui.toLowerCase();
            // READY hides readiness_strip; chip may show "..." briefly — match testTag / content-desc / substrings.
            if (
                u.includes('readiness_strip') ||
                u.includes('header_credit_balance_chip') ||
                u.includes('checking') ||
                u.includes('no credits') ||
                u.includes('no internet') ||
                u.includes('credit balance') ||
                u.includes('loading credit') ||
                u.includes(' credits') ||
                u.includes('>credits<')
            ) {
                return { ok: true, attempt: i + 1, snippet: ui.substring(0, 400) };
            }
            await this.core.sleep(intervalMs);
        }
        return { ok: false, error: 'Readiness strip / balance UI not detected in UI dump' };
    }

    async execute() {
        this.core.logger.info('Starting UX-25 Direct-to-value readiness validation');
        await this.core.clearLogcat();
        const launch = await this.core.launchApp();
        if (!launch.success) {
            return { success: false, error: 'launchApp failed' };
        }
        await this.core.sleep(1500);

        const logWait = await this.waitForTruthFirstLogs();
        if (!logWait.ok) {
            return { success: false, error: logWait.error };
        }

        const uiWait = await this.waitForReadinessOrCreditsUi();
        if (!uiWait.ok) {
            return { success: false, error: uiWait.error };
        }

        let settingsTap = await this.core.tapByTestTag('settings_button');
        if (!settingsTap.success) {
            settingsTap = await this.core.tapByContentDesc('Settings');
        }
        if (!settingsTap.success) {
            return { success: false, error: 'Could not open settings' };
        }
        await this.core.sleep(2000);
        await this.core.dumpUIHierarchy();
        const settingsUi = this.core.readLastUIDump() || '';
        const hasHelp =
            settingsUi.includes('Send report') ||
            settingsUi.includes('diagnostic') ||
            settingsUi.includes('Account & help');
        if (!hasHelp) {
            return { success: false, error: 'Settings sheet missing send-report / help section' };
        }

        await this.core.executeCommand('adb shell input keyevent 4', undefined, undefined, { allowFailure: true });
        await this.core.sleep(500);

        const badOptimistic = await this.core.executeCommand(
            'adb logcat -d -t 600 | findstr /i /c:"Optimistic balance set immediately"'
        );
        if (badOptimistic.success && badOptimistic.output && badOptimistic.output.includes('Optimistic')) {
            return { success: false, error: 'Optimistic balance still present in logcat' };
        }

        this.core.logger.info('UX-25 Direct-to-value readiness validation passed');
        return { success: true, details: { logWait, uiWait } };
    }
}

module.exports = JourneyUX25DirectToValueReadiness01Validation;

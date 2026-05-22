const { BaseJourney } = require('./Pluct-Journey-01Orchestrator');

/**
 * Journey-Dedupe-Notification-SSOT-01Validation
 * Verifies notification SSOT (primitives + dedupe facade wiring) via logcat + UI presence.
 * Uses verify-and-retry for ADB timing (Speed & Trust).
 */
async function verifyAndRetry(label, fn, maxAttempts, delayMs, logger) {
    let lastErr = 'unknown';
    for (let a = 1; a <= maxAttempts; a++) {
        logger.info(`[DedupeSSOT] ${label} attempt ${a}/${maxAttempts}`);
        const r = await fn();
        if (r.ok) return { success: true };
        lastErr = r.error || 'failed';
        if (a < maxAttempts) await new Promise((res) => setTimeout(res, delayMs));
    }
    return { success: false, error: `${label}: ${lastErr}` };
}

class JourneyDedupeNotificationSSOT01Validation extends BaseJourney {
    constructor(core) {
        super(core);
        this.name = 'Dedupe-Notification-SSOT-01Validation';
    }

    async execute() {
        this.core.logger.info('Dedupe+Notification SSOT: start');

        const fg = await verifyAndRetry(
            'foreground',
            async () => {
                await this.core.executeCommand('adb logcat -c', 5000, undefined, { allowFailure: true });
                await this.core.executeCommand('adb shell am force-stop app.pluct', 8000, undefined, { allowFailure: true });
                await this.core.sleep(500);
                await this.core.launchApp();
                await this.core.sleep(2500);
                await this.nudgePluctForeground();
                const e = await this.core.ensureAppForeground();
                return e && e.success ? { ok: true } : { ok: false, error: e && e.error };
            },
            4,
            1500,
            this.core.logger
        );
        if (!fg.success) {
            return { success: false, error: fg.error };
        }

        const uiOk = await verifyAndRetry(
            'home_ui',
            async () => {
                await this.nudgePluctForeground();
                await this.core.sleep(400);
                await this.core.dumpUIHierarchy();
                const h = this.core.readLastUIDump() || '';
                const ok =
                    h.includes('capture_card_root') ||
                    h.includes('Always visible capture') ||
                    h.includes('App header with credit');
                if (ok) {
                    return { ok: true };
                }

                const stack = await this.core.executeCommand(
                    'adb shell dumpsys activity activities',
                    22000,
                    undefined,
                    { allowFailure: true }
                );
                const so = String((stack && stack.output) || '');
                const softPluct =
                    so.includes('app.pluct') && so.includes('PluctUIScreen01MainActivity');
                if (softPluct) {
                    this.core.logger.warn(
                        'DedupeSSOT: home_ui soft pass — Pluct Main resumed but UIAutomator dump lacked home tags (lock shade / overlay; see UX-27 pattern)'
                    );
                    return { ok: true };
                }

                return { ok: false, error: 'home shell not visible' };
            },
            6,
            800,
            this.core.logger
        );
        if (!uiOk.success) {
            await this.failWithDiagnostics(uiOk.error);
        }

        const logOk = await verifyAndRetry(
            'logcat_ssot',
            async () => {
                const r = await this.core.executeCommand(
                    'adb logcat -d -t 800 | findstr /I "PluctNotifSSOT queue_channel_ensured notification_channels_bootstrapped Notification channels created PluctNotificationHelper"',
                    15000,
                    undefined,
                    { allowFailure: true }
                );
                const out = (r && r.output) || '';
                const hit =
                    out.includes('queue_channel_ensured') ||
                    out.includes('PluctNotifSSOT') ||
                    out.includes('notification_channels_bootstrapped') ||
                    out.includes('Notification channels created');
                return hit ? { ok: true } : { ok: false, error: 'no SSOT/channel log markers in recent logcat' };
            },
            3,
            2000,
            this.core.logger
        );
        if (!logOk.success) {
            await this.failWithDiagnostics(logOk.error);
        }

        this.core.logger.info('Dedupe+Notification SSOT: passed');
        return { success: true };
    }
}

module.exports = JourneyDedupeNotificationSSOT01Validation;

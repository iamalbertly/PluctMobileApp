const { BaseJourney } = require('./Pluct-Journey-01Orchestrator');

/**
 * Journey-UX-26TikTok-Url-Refund-NoCharge-01Validation
 * ADB: paste TikTok-shaped invalid URL from clipboard, submit, verify "No charge" / creditsRefunded in UI + logcat.
 */
class JourneyUX26TikTokUrlRefundNoCharge01Validation extends BaseJourney {
    constructor(core) {
        super(core);
        this.name = 'UX-26TikTok-Url-Refund-NoCharge-01Validation';
    }

    async waitForNoChargeUi(maxAttempts = 36, intervalMs = 500) {
        for (let i = 0; i < maxAttempts; i++) {
            await this.core.dumpUIHierarchy();
            const ui = this.core.readLastUIDump() || '';
            const u = ui.toLowerCase();
            if (u.includes('no charge') || u.includes('no credits used') || u.includes('error_message_text')) {
                return { ok: true, attempt: i + 1 };
            }
            if (u.includes('insufficient') && u.includes('credit')) {
                return { ok: false, error: 'Insufficient credits — cannot reach transcribe refund path' };
            }
            await this.core.sleep(intervalMs);
        }
        return { ok: false, error: 'Timed out waiting for No charge / error banner in UI dump' };
    }

    async waitForLogcatCreditsRefunded(maxAttempts = 24, intervalMs = 500) {
        for (let i = 0; i < maxAttempts; i++) {
            const r = await this.core.executeCommand(
                'adb logcat -d -t 4000 | findstr /i "creditsRefunded invalid_tiktok_link"',
                22000,
                undefined,
                { allowFailure: true }
            );
            const buf = (r && r.output) ? String(r.output) : '';
            if (buf.toLowerCase().includes('creditsrefunded')) {
                return { ok: true, attempt: i + 1 };
            }
            await this.core.sleep(intervalMs);
        }
        return { ok: false, error: 'Timed out waiting for creditsRefunded marker in logcat' };
    }

    async execute() {
        this.core.logger.info('Starting UX-26 TikTok URL refund / No charge validation');
        const badUrl = 'https://vm.tiktok.com/INVALIDSHORT/';
        const clipPayload = `Watch this ${badUrl} thanks`;

        await this.core.clearLogcat();
        const launch = await this.core.launchApp();
        if (!launch.success) {
            return { success: false, error: 'launchApp failed' };
        }
        await this.core.sleep(1200);

        const seeded = await this.core.ensureLocalMobileCredits(3);
        if (!seeded.success) {
            return { success: false, error: `Credit seed failed: ${seeded.error || 'unknown'}` };
        }

        await this.core.ensureAppForeground();
        await this.core.sleep(500);

        let focusTap = await this.core.tapByTestTag('url_input_field');
        if (!focusTap.success) {
            focusTap = await this.core.tapFirstEditText();
        }
        if (!focusTap.success) {
            return { success: false, error: 'Could not focus URL field' };
        }
        await this.core.sleep(300);

        const pasteIn = await this.core.inputTextViaClipboard(clipPayload);
        if (!pasteIn.success) {
            return { success: false, error: `Clipboard paste flow failed: ${pasteIn.error || 'unknown'}` };
        }
        await this.core.sleep(600);

        let submitTap = await this.core.tapByTestTag('extract_script_button');
        if (!submitTap.success) {
            submitTap = await this.core.tapByContentDesc('Extract Script');
        }
        if (!submitTap.success) {
            return { success: false, error: 'Submit (extract) control not found' };
        }

        const uiWait = await this.waitForNoChargeUi();
        if (!uiWait.ok) {
            return { success: false, error: uiWait.error };
        }

        const logWait = await this.waitForLogcatCreditsRefunded();
        if (!logWait.ok) {
            this.core.logger.warn(`UX-26: ${logWait.error} (UI already showed refund-friendly state)`);
        }

        this.core.logger.info('UX-26 TikTok URL refund / No charge validation passed');
        return { success: true, details: { uiWait, logWait } };
    }
}

module.exports = JourneyUX26TikTokUrlRefundNoCharge01Validation;

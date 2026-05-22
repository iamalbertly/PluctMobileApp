const { BaseJourney } = require('./Pluct-Journey-01Orchestrator');

class JourneyUX31DirectValueFatigueGuard01Validation extends BaseJourney {
    constructor(core) {
        super(core);
        this.name = 'UX-31DirectValue-FatigueGuard-01Validation';
    }

    async dumpHome() {
        await this.core.dumpUIHierarchy();
        return this.core.readLastUIDump() || '';
    }

    async dismissBlockingOverlays() {
        for (let i = 0; i < 3; i++) {
            const ui = (await this.dumpHome()).toLowerCase();
            let tapped = false;
            for (const tag of [
                'welcome_get_started_button',
                'onboarding_skip_button',
                'permission_onboarding_skip_button'
            ]) {
                if (ui.includes(tag)) {
                    const r = await this.core.tapByTestTag(tag);
                    tapped = tapped || !!r.success;
                }
            }
            if (!tapped && (ui.includes('capture sheet') || ui.includes('capture this insight'))) {
                const close = await this.core.tapByContentDesc('Close');
                if (close.success) tapped = true;
                else {
                    await this.core.executeCommand('adb shell input keyevent 4', 5000, undefined, { allowFailure: true });
                    tapped = true;
                }
            }
            if (!tapped) break;
            await this.core.sleep(500);
        }
    }

    async startHome() {
        await this.core.executeCommand('adb shell am force-stop app.pluct', 12000, undefined, { allowFailure: true });
        await this.core.sleep(350);
        await this.core.executeCommand('adb shell am start -S -n app.pluct/.PluctUIScreen01MainActivity', 25000);
        await this.core.sleep(2200);
        await this.core.ensureAppForeground();
        await this.dismissBlockingOverlays();
    }

    async waitForHome(maxAttempts = 30) {
        for (let i = 0; i < maxAttempts; i++) {
            const raw = await this.dumpHome();
            const ui = raw.toLowerCase();
            const hasHome =
                (ui.includes('capture_card_root') || ui.includes('always visible capture card')) &&
                (ui.includes('paste tiktok link') || ui.includes('url_input_field')) &&
                (ui.includes('nav_home') || ui.includes('home tab'));
            if (hasHome) return { ok: true, raw, attempt: i + 1 };
            if (i % 4 === 0) await this.core.ensureAppForeground();
            await this.core.sleep(500);
        }
        return { ok: false, error: 'Home direct-value UI did not become ready' };
    }

    countText(raw, needle) {
        return (raw.toLowerCase().match(new RegExp(needle, 'g')) || []).length;
    }

    assertFatigueGuard(raw) {
        const ui = raw.toLowerCase();
        const issues = [];
        if (!ui.includes('paste tiktok link')) issues.push('primary paste action missing');
        if (!ui.includes('uses left') && !ui.includes('capture_wallet_chip')) issues.push('uses-left wallet signal missing');
        if (ui.includes('balance ') || ui.includes('balance:')) issues.push('legacy balance copy still visible');
        if (ui.includes('not charged')) issues.push('negative not-charged copy still visible');
        if (ui.includes('paste tiktok link here')) issues.push('placeholder is longer than needed');
        if (this.countText(raw, 'view all') > 0) issues.push('duplicate View all shortcuts still visible');
        if (!ui.includes('text will appear here') && !ui.includes('home_value_promise_line')) {
            issues.push('short value promise missing');
        }
        return issues;
    }

    async assertBusinessEnginePolicy() {
        const beBase = this.core.config.businessEngineUrl;
        const policy = await this.core.httpGet(`${beBase}/v1/public/client-policy`);
        if (policy.status !== 200) {
            return { ok: false, error: `client-policy failed: ${policy.status || policy.error}` };
        }
        let body = {};
        try {
            body = JSON.parse(policy.body || '{}');
        } catch (_) {
            return { ok: false, error: 'client-policy returned non-JSON' };
        }
        const apkUrl = String(body.apkDownloadUrl || body.platforms?.android?.apkUrl || '');
        if (!body.ok || !body.platforms?.android || !apkUrl) {
            return { ok: false, error: 'client-policy missing android APK URL contract' };
        }
        if (/play\.google\.com/i.test(apkUrl) && !body.playStoreUrl) {
            return { ok: false, error: 'client-policy invented Play Store APK fallback' };
        }
        const download = await this.core.httpGet(`${beBase}/downloads/android/latest.apk`);
        if (![200, 302, 307, 308].includes(download.status)) {
            return { ok: false, error: `latest.apk did not resolve: ${download.status || download.error}` };
        }
        return { ok: true, apkUrl, updateMode: body.updateMode };
    }

    async assertLogcatClean() {
        const log = await this.core.captureFilteredLogcatTail('AndroidRuntime:E System.err:W MainActivity:I CaptureCard:I *:S', 1800, 18000);
        const out = String((log && log.output) || '');
        if (/\bFATAL EXCEPTION\b|\bANR\b|IndexOutOfBoundsException|IllegalStateException/i.test(out)) {
            return { ok: false, error: 'critical Android logcat issue detected' };
        }
        return { ok: true };
    }

    async exerciseBottomNav() {
        const size = await this.core.executeCommand('adb shell wm size', 8000, undefined, { allowFailure: true });
        let w = 1080;
        let h = 2340;
        const m = String(size.output || '').match(/Physical size:\s*(\d+)x(\d+)/i);
        if (m) {
            w = parseInt(m[1], 10);
            h = parseInt(m[2], 10);
        }
        const y = Math.round(h - Math.max(110, h * 0.06));
        await this.core.executeCommand(`adb shell input tap ${Math.round(w * 0.5)} ${y}`, 8000, undefined, { allowFailure: true });
        await this.core.sleep(700);
        await this.core.executeCommand(`adb shell input tap ${Math.round(w * 0.17)} ${y}`, 8000, undefined, { allowFailure: true });
        await this.core.sleep(700);
        return this.waitForHome(12);
    }

    async execute() {
        this.core.logger.info('Starting UX-31 direct-value fatigue guard validation');
        await this.core.clearLogcat();
        await this.startHome();

        const home = await this.waitForHome();
        if (!home.ok) return { success: false, error: home.error };

        const issues = this.assertFatigueGuard(home.raw);
        if (issues.length > 0) {
            this.core.logger.error(home.raw.substring(0, 1600));
            return { success: false, error: `UX fatigue guard failed: ${issues.join(', ')}` };
        }

        const policy = await this.assertBusinessEnginePolicy();
        if (!policy.ok) return { success: false, error: policy.error };

        const nav = await this.exerciseBottomNav();
        if (!nav.ok) return { success: false, error: `bottom nav return failed: ${nav.error}` };

        const logs = await this.assertLogcatClean();
        if (!logs.ok) return { success: false, error: logs.error };

        return { success: true, details: { homeAttempts: home.attempt, apkUrl: policy.apkUrl } };
    }
}

module.exports = JourneyUX31DirectValueFatigueGuard01Validation;

const { BaseJourney } = require('./Pluct-Journey-01Orchestrator');
const fs = require('fs');
const path = require('path');

/**
 * Journey-UX-30PluctMockupParityVisual-01Validation
 * Customer / Realism & Simplicity / Speed & Trust:
 * Cold start, dismiss overlays, verify-retry UI dump for mockup-parity surfaces:
 * wallet copy ("uses left"), paste placeholder, promise line, section tags,
 * thicker progress semantics, logcat FATAL guard, bottom-nav round trip.
 */
class JourneyUX30PluctMockupParityVisual01Validation extends BaseJourney {
    constructor(core) {
        super(core);
        this.name = 'UX-30PluctMockupParityVisual-01Validation';
    }

    async wakeDismissLockShade() {
        const r = await this.core.executeCommand('adb shell wm size', 8000, undefined, { allowFailure: true });
        let w = 540;
        let h = 960;
        const m = r.output ? String(r.output).match(/Physical size:\s*(\d+)\s*x\s*(\d+)/i) : null;
        if (m) {
            w = parseInt(m[1], 10);
            h = parseInt(m[2], 10);
        }
        const x = Math.round(w * 0.52);
        const yBottom = Math.min(h - 8, Math.round(h * 0.92));
        const yTop = Math.max(8, Math.round(h * 0.18));
        await this.core.executeCommand('adb shell input keyevent 224', 5000, undefined, { allowFailure: true });
        await this.core.sleep(300);
        await this.core.executeCommand(`adb shell input swipe ${x} ${yBottom} ${x} ${yTop} 320`, 8000, undefined, { allowFailure: true });
        await this.core.sleep(300);
    }

    async dumpOnce() {
        await this.core.dumpUIHierarchy();
        return this.core.readLastUIDump() || '';
    }

    async dismissBlockingOverlays() {
        for (let k = 0; k < 3; k++) {
            const raw = (await this.dumpOnce()).toLowerCase();
            let tapped = false;
            if (raw.includes('onboarding_skip_button') || raw.includes('onboarding_tutorial_dialog')) {
                const t = await this.core.tapByTestTag('onboarding_skip_button');
                if (t.success) tapped = true;
            } else if (raw.includes('welcome_get_started_button') || raw.includes('welcome_dialog')) {
                const t = await this.core.tapByTestTag('welcome_get_started_button');
                if (t.success) tapped = true;
            } else if (raw.includes('permission_onboarding_skip_button') || raw.includes('permission_onboarding_dialog')) {
                const t = await this.core.tapByTestTag('permission_onboarding_skip_button');
                if (t.success) tapped = true;
            }
            if (tapped) await this.core.sleep(550);
            else break;
        }
    }

    async waitForHomeUi(maxAttempts = 38, intervalMs = 480) {
        for (let i = 0; i < maxAttempts; i++) {
            if (i % 4 === 0) {
                await this.core.ensureAppForeground();
                await this.core.sleep(320);
            }
            if (i % 3 === 0) await this.dismissBlockingOverlays();
            const raw = await this.dumpOnce();
            const u = raw.toLowerCase();
            const inPluct = u.includes('package="app.pluct"') || u.includes('app.pluct');
            const hasCapture =
                u.includes('always visible capture') ||
                u.includes('capture_card_root') ||
                u.includes('capture_component_label');
            const hasNav =
                u.includes('home tab') ||
                u.includes('nav_home') ||
                u.includes('library tab') ||
                u.includes('settings tab');
            if (inPluct && hasCapture && hasNav) {
                return { ok: true, raw, attempt: i + 1 };
            }
            await this.core.sleep(intervalMs);
            await this.wakeDismissLockShade();
        }
        return { ok: false, error: 'UX-30: home shell not ready' };
    }

    async coldStartMainActivity() {
        await this.core.executeCommand('adb shell am force-stop app.pluct', 12000, undefined, { allowFailure: true });
        await this.core.sleep(400);
        const r = await this.core.executeCommand(
            'adb shell am start -S -n app.pluct/.PluctUIScreen01MainActivity',
            25000
        );
        await this.core.sleep(4000);
        return !!(r && r.success);
    }

    assertMockupParity(homeRaw) {
        const u = homeRaw.toLowerCase();
        const missing = [];
        if (!u.includes('paste tiktok')) missing.push('paste_placeholder');
        if (!u.includes('text will appear here') && !u.includes('home_value_promise_line')) missing.push('value_promise');
        if (!u.includes('tiktok link:') && !u.includes('vt.tiktok.com')) missing.push('example_hint');
        const walletOk =
            u.includes('uses left') ||
            u.includes('loading uses left') ||
            u.includes('capture_wallet_chip');
        if (!walletOk) missing.push('wallet_semantics');
        if (!u.includes('home_section_active') && !(u.includes('view all') && u.includes('active'))) {
            this.core.logger.warn('UX-30: Active section header not obvious (OK when queue empty)');
        }
        return missing;
    }

    async execute() {
        this.core.logger.info('Starting UX-30 mockup parity visual validation');
        const apkPath = path.join(__dirname, '../../../app/build/outputs/apk/debug/app-debug.apk');
        if (fs.existsSync(apkPath)) {
            await this.core.executeCommand(`adb install -r "${apkPath}"`, 180000, undefined, { allowFailure: true });
            await this.core.sleep(650);
        }
        await this.core.clearLogcat();
        await this.wakeDismissLockShade();
        if (!(await this.coldStartMainActivity())) {
            return { success: false, error: 'UX-30: cold start failed' };
        }
        await this.core.ensureAppForeground();

        const home = await this.waitForHomeUi();
        if (!home.ok) {
            this.core.logger.error(`UX-30 dump head: ${home.raw.substring(0, 1200)}`);
            return { success: false, error: home.error };
        }

        const miss = this.assertMockupParity(home.raw);
        if (miss.length > 0) {
            this.core.logger.error(`UX-30 mockup parity missing: ${miss.join(', ')}`);
            this.core.logger.error(`UX-30 dump slice: ${home.raw.substring(0, 1400)}`);
            return { success: false, error: `Mockup parity incomplete: ${miss.join(', ')}` };
        }

        const u = home.raw.toLowerCase();
        if (u.includes('processing') || u.includes('waiting') || u.includes('progress bar')) {
            this.core.logger.info('UX-30: detected job row / progress affordance in dump');
        } else {
            this.core.logger.warn('UX-30: no active job row in dump (soft — empty queue OK)');
        }

        const logWide = await this.core.executeCommand('adb logcat -d -t 2000', 22000, undefined, { allowFailure: true });
        const lb = String((logWide && logWide.output) || '').toLowerCase();
        if (/\bfatal exception\b/i.test(lb)) {
            return { success: false, error: 'UX-30: FATAL EXCEPTION in logcat' };
        }

        const r = await this.core.executeCommand('adb shell wm size', 8000, undefined, { allowFailure: true });
        let w = 1080;
        let h = 2340;
        const m = r.output ? String(r.output).match(/Physical size:\s*(\d+)\s*x\s*(\d+)/i) : null;
        if (m) {
            w = parseInt(m[1], 10);
            h = parseInt(m[2], 10);
        }
        const yNav = Math.round(h - Math.max(110, h * 0.06));
        const xLib = Math.round((1 + 0.5) * (w / 3));
        await this.core.executeCommand(`adb shell input tap ${xLib} ${yNav}`, 8000, undefined, { allowFailure: true });
        await this.core.sleep(600);
        let homeTap = await this.core.tapByTestTag('nav_home');
        if (!homeTap.success) {
            const xHome = Math.round((0 + 0.5) * (w / 3));
            await this.core.executeCommand(`adb shell input tap ${xHome} ${yNav}`, 8000, undefined, { allowFailure: true });
            await this.core.sleep(500);
        }
        const back = await this.waitForHomeUi(16, 450);
        if (!back.ok) {
            return { success: false, error: 'UX-30: could not return to Home after nav exercise' };
        }

        this.core.logger.info('UX-30 mockup parity visual validation passed');
        return { success: true, details: { attempts: home.attempt } };
    }
}

module.exports = JourneyUX30PluctMockupParityVisual01Validation;

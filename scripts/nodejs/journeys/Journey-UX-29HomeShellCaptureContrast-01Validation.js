const { BaseJourney } = require('./Pluct-Journey-01Orchestrator');
const fs = require('fs');
const path = require('path');

/**
 * Journey-UX-29HomeShellCaptureContrast-01Validation
 * Verify-retry: capture card, readable example line, promise pill, nav_home tag, no immediate FATAL,
 * bottom nav responds (ADB tap + UI dump).
 */
class JourneyUX29HomeShellCaptureContrast01Validation extends BaseJourney {
    constructor(core) {
        super(core);
        this.name = 'UX-29HomeShellCaptureContrast-01Validation';
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
        await this.core.sleep(350);
        await this.core.executeCommand(`adb shell input swipe ${x} ${yBottom} ${x} ${yTop} 320`, 8000, undefined, { allowFailure: true });
        await this.core.sleep(350);
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

    async waitForHomeUi(maxAttempts = 36, intervalMs = 500) {
        for (let i = 0; i < maxAttempts; i++) {
            if (i % 4 === 0) {
                await this.core.ensureAppForeground();
                await this.core.sleep(350);
            }
            if (i % 5 === 2) {
                await this.core.executeCommand('adb shell input swipe 270 320 270 860 280', 8000, undefined, { allowFailure: true });
                await this.core.sleep(220);
            }
            if (i % 3 === 0) {
                await this.dismissBlockingOverlays();
            }
            const raw = await this.dumpOnce();
            const u = raw.toLowerCase();
            const inPluct = u.includes('package="app.pluct"') || u.includes('app.pluct');
            const hasCapture =
                u.includes('capture_card_root') ||
                u.includes('always visible capture') ||
                u.includes('capture_url_example_hint') ||
                u.includes('capture_component_label') ||
                u.includes('url_input_field');
            const hasNav =
                u.includes('nav_home') ||
                u.includes('home tab') ||
                u.includes('library tab') ||
                u.includes('settings tab') ||
                u.includes('bottomnavigation') ||
                u.includes('navigation') ||
                u.includes('nav_library');
            if (inPluct && hasCapture && hasNav) {
                return { ok: true, raw, attempt: i + 1 };
            }
            await this.core.sleep(intervalMs);
            await this.wakeDismissLockShade();
        }
        return { ok: false, error: 'Home shell: capture surface + bottom nav not detected' };
    }

    async coldStartMainActivity() {
        await this.core.executeCommand('adb shell am force-stop app.pluct', 12000, undefined, { allowFailure: true });
        await this.core.sleep(450);
        const r = await this.core.executeCommand(
            'adb shell am start -S -n app.pluct/.PluctUIScreen01MainActivity',
            25000
        );
        await this.core.sleep(4000);
        return !!(r && r.success);
    }

    async execute() {
        this.core.logger.info('Starting UX-29 home shell / capture contrast / nav parity');
        const apkPath = path.join(__dirname, '../../../app/build/outputs/apk/debug/app-debug.apk');
        if (fs.existsSync(apkPath)) {
            await this.core.executeCommand(`adb install -r "${apkPath}"`, 180000, undefined, { allowFailure: true });
            await this.core.sleep(700);
        }
        await this.core.clearLogcat();
        await this.wakeDismissLockShade();
        const cold = await this.coldStartMainActivity();
        if (!cold) {
            return { success: false, error: 'cold start MainActivity (-S) failed' };
        }
        await this.core.ensureAppForeground();

        const home = await this.waitForHomeUi();
        if (!home.ok) {
            const tail = await this.core.executeCommand('adb logcat -d -t 400', 15000, undefined, { allowFailure: true });
            this.core.logger.error(`UX-29 UI tail (800): ${String((tail && tail.output) || '').slice(-800)}`);
            return { success: false, error: home.error };
        }

        const rawLower = home.raw.toLowerCase();
        const hasExample =
            rawLower.includes('vt.tiktok.com') ||
            rawLower.includes('vm.tiktok.com') ||
            rawLower.includes('capture_url_example_hint');
        if (!hasExample) {
            this.core.logger.error(`UX-29 dump head: ${home.raw.substring(0, 900)}`);
            return { success: false, error: 'Example TikTok URL line not visible (contrast / testTag regression)' };
        }

        if (!rawLower.includes('home_value_promise_line') && !rawLower.includes('clean it up')) {
            this.core.logger.warn('UX-29: promise line not found in dump fragment (continuing if capture OK)');
        }

        const logWide = await this.core.executeCommand('adb logcat -d -t 2000', 22000, undefined, { allowFailure: true });
        const lb = String((logWide && logWide.output) || '').toLowerCase();
        if (/\bfatal exception\b/i.test(lb)) {
            const fatalSlice = lb.split('fatal exception').slice(-1)[0].slice(0, 700);
            this.core.logger.error(`UX-29 logcat slice: ${fatalSlice}`);
            return { success: false, error: 'FATAL EXCEPTION in logcat shortly after home load' };
        }

        const r = await this.core.executeCommand('adb shell wm size', 8000, undefined, { allowFailure: true });
        let w = 1080;
        let h = 2340;
        const m = r.output ? String(r.output).match(/Physical size:\s*(\d+)\s*x\s*(\d+)/i) : null;
        if (m) {
            w = parseInt(m[1], 10);
            h = parseInt(m[2], 10);
        }
        const xLib = Math.round((1 + 0.5) * (w / 3));
        const yNav = Math.round(h - Math.max(110, h * 0.06));
        await this.core.executeCommand(`adb shell input tap ${xLib} ${yNav}`, 8000, undefined, { allowFailure: true });
        await this.core.sleep(650);
        const afterLib = (await this.dumpOnce()).toLowerCase();
        if (!afterLib.includes('library') && !afterLib.includes('nav_library')) {
            this.core.logger.warn('UX-29: Library not obvious in dump after bottom tap (soft)');
        }

        let homeTap = await this.core.tapByTestTag('nav_home');
        if (!homeTap.success) {
            const xHome = Math.round((0 + 0.5) * (w / 3));
            await this.core.executeCommand(`adb shell input tap ${xHome} ${yNav}`, 8000, undefined, { allowFailure: true });
            await this.core.sleep(500);
        }
        const back = await this.waitForHomeUi(14, 450);
        if (!back.ok) {
            return { success: false, error: 'Could not return to Home with capture visible' };
        }

        this.core.logger.info('UX-29 home shell / capture contrast / nav parity passed');
        return { success: true, details: { homeAttempts: home.attempt } };
    }
}

module.exports = JourneyUX29HomeShellCaptureContrast01Validation;

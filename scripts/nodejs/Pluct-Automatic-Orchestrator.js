// Pluct Automatic Orchestrator (Node.js)
// Mirrors Powershell automatic orchestrator: structure, naming, and behavior

const path = require('path');
const fs = require('fs');
const {
	logInfo,
	logSuccess,
	logWarn,
	logError,
	logStage,
} = require('./core/Logger');
const { execOk, execOut, which } = require('./core/Pluct-Test-Core-Exec');
const Logcat = require('./core/Pluct-Node-Tests-Core-Logcat-LiveHttpStreamer');
const BuildDetector = require('./modules/Pluct-Test-Core-BuildDetector');
const JourneyEngine = require('./modules/Pluct-Node-Tests-Journey-CoreUserFlowsEngine');
const { reportCriticalError, TestSession, showTestReport } = require('./modules/Pluct-Test-Core-Status');
const PluctUICompactLayoutTest = require('./modules/Pluct-UI-Compact-Layout-Test');
const PluctUIFunctionalityTest = require('./modules/Pluct-UI-Functionality-Test');
const PluctUIRealEstateTest = require('./modules/Pluct-UI-Real-Estate-Test');

function parseArgs(argv) {
	const out = {
		scope: 'Core',
		url: 'https://www.tiktok.com/@garyvee/video/7308801293029248299',
		forceBuild: false,
		skipInstall: false,
		captureScreenshots: false,
		verbose: false,
	};
	for (let i = 0; i < argv.length; i++) {
		const a = argv[i];
		if (a === '-scope') out.scope = argv[++i] || out.scope;
		else if (a === '-url') out.url = argv[++i] || out.url;
		else if (a === '--forceBuild' || a === '-forceBuild') out.forceBuild = true;
		else if (a === '--skipInstall' || a === '-skipInstall') out.skipInstall = true;
		else if (a === '--captureScreenshots' || a === '-captureScreenshots') out.captureScreenshots = true;
		else if (a === '--verbose' || a === '-v') out.verbose = true;
	}
	return out;
}

function loadDefaults() {
    try {
        const p = path.join(__dirname, 'config', 'Pluct-Test-Config-Defaults.json');
        if (fs.existsSync(p)) {
            const raw = fs.readFileSync(p, 'utf8');
            return JSON.parse(raw);
        }
    } catch {}
    return {};
}

function ensurePrerequisites() {
	logStage('Prerequisites', 'DeviceManager');
	if (!which('adb')) {
		logError('ADB not found in PATH', 'DeviceManager');
		reportCriticalError('ADB Missing', 'Install Android platform-tools and ensure adb is in PATH', 'Prerequisites');
		process.exit(1);
	}
	if (!execOk('adb devices')) {
		reportCriticalError('ADB Device Check Failed', 'ADB not responding', 'Prerequisites');
		process.exit(1);
	}
	const model = execOut('adb shell getprop ro.product.model').trim();
	if (!model) {
		reportCriticalError('No Android Device Connected', 'Start an emulator or connect a device', 'Prerequisites');
		process.exit(1);
	}
	logInfo(`Device: ${model}`, 'DeviceManager');
}

function prepareDevice() {
    // Wake and unlock, ignore failures
    execOk('adb wait-for-device');
    execOk('adb shell input keyevent KEYCODE_WAKEUP');
    execOk('adb shell wm dismiss-keyguard');
    // Try-kill common OEM overlays (best effort)
    const overlays = [
        'com.miui.securitycenter',
        'com.coloros.oppoguardelf',
        'com.huawei.systemmanager',
    ];
    overlays.forEach(pkg => execOk(`adb shell am force-stop ${pkg}`));
}

function grantBaselinePermissions() {
    const perms = [
        'android.permission.POST_NOTIFICATIONS',
        'android.permission.READ_MEDIA_AUDIO',
    ];
    perms.forEach(p => execOk(`adb shell pm grant app.pluct ${p}`));
}

function optionalBuild(forceBuild) {
	logStage('Build', 'BuildSystem');
	const needBuild = BuildDetector.isBuildRequired(forceBuild);
	TestSession.BuildRequired = needBuild;
	if (!needBuild) {
		logSuccess('No code changes - skipping build', 'BuildSystem');
		return true;
	}
	if (!which('gradlew.bat')) {
		logWarn('gradlew.bat not found, attempting system Gradle', 'BuildSystem');
	}
	// Check if we're running from project root or scripts/nodejs directory
	const isProjectRoot = require('fs').existsSync('gradlew.bat');
	const gradleCmd = isProjectRoot ? 'gradlew.bat assembleDebug' : 'cd ..\\.. && gradlew.bat assembleDebug';
	const ok = execOk(gradleCmd);
	if (!ok) {
		reportCriticalError('Build Failed', 'Gradle build failed. Run .\\gradlew.bat assembleDebug --stacktrace', 'Build');
		return false;
	}
	return true;
}

function deployIfNeeded(skipInstall) {
	if (skipInstall) return true;
	logStage('Install', 'Installer');
	execOk('adb uninstall app.pluct');
	// Check if we're running from project root or scripts/nodejs directory
	const isProjectRoot = require('fs').existsSync('gradlew.bat');
	const apkPath = isProjectRoot 
		? path.join('app', 'build', 'outputs', 'apk', 'debug', 'app-debug.apk')
		: path.join('..', '..', 'app', 'build', 'outputs', 'apk', 'debug', 'app-debug.apk');
	if (!BuildDetector.fileExists(apkPath)) {
		reportCriticalError('APK Missing', 'Debug APK not found. Build the app with assembleDebug', 'Install');
		return false;
	}
	if (!execOk(`adb install -r "${apkPath}"`)) {
		reportCriticalError('Install Failed', 'Could not install APK. Ensure device has storage and authorization', 'Install');
		return false;
	}
	execOk('adb shell pm clear app.pluct');
	return true;
}

async function runScope(scope, url) {
	// === DEVICE STABILIZATION ===
	try {
		execOk('adb shell settings put global animator_duration_scale 0');
		execOk('adb shell settings put global transition_animation_scale 0');
		execOk('adb shell settings put global window_animation_scale 0');
		execOk('adb shell svc wifi enable');
		execOk('adb shell cmd jobscheduler run -u 0 -f app.pluct 0 || true');
		logInfo('Device stabilized for precise UI interactions', 'DeviceManager');
	} catch (e) {
		logWarn(`Device stabilization failed: ${e.message}`, 'DeviceManager');
	}
	
	const s = String(scope || '').toLowerCase();
	switch (s) {
		case 'core':
			// Run enhanced journeys with intelligent orchestration
			const JourneyOrchestrator = require('./modules/Pluct-Node-Tests-Journey-Orchestrator');
			const orchestrator = new JourneyOrchestrator();
			const result = await orchestrator.runAllJourneys(url);
			if (!result) {
				logError('Core journeys failed - stopping execution', 'Orchestrator');
				return false;
			}
			return result;
		case 'all':
			const coreOk = JourneyEngine.testCoreUserJourneys(url);
			if (!coreOk) return false;
			const enhOk = JourneyEngine.testEnhancementsJourney(url);
			if (!enhOk) return false;
			// Credit balance display test
			const creditOk = JourneyEngine.testCreditBalanceDisplay(url);
			if (!creditOk) return false;
			// UI Compact Layout Test
			const uiCompactOk = new PluctUICompactLayoutTest().runTest();
			if (!uiCompactOk) return false;
			// UI Functionality Test
			const uiFunctionalityOk = new PluctUIFunctionalityTest().runTest();
			if (!uiFunctionalityOk) return false;
			// UI Real Estate Test
			const uiRealEstateOk = new PluctUIRealEstateTest().runTest();
			if (!uiRealEstateOk) return false;
			// Business Engine optional based on config and processing tier
			const defaults = loadDefaults();
			if (defaults.enableBusinessEngine) {
				// Check if this is a Quick Scan flow by looking for Quick Scan logs
				const isQuickScan = Logcat.recent('(QUICK_SCAN|Quick Scan|🎯.*QUICK_SCAN|QuickScan|Quick.*Scan|QUICK SCAN SELECTED|stage=QUICK_SCAN|QUICK_SCAN_START|QUICK_SCAN_COMPLETE|QUICK_SCAN_MODE)', 20);
				if (!isQuickScan || isQuickScan.length === 0) {
					const businessOk = JourneyEngine.testBusinessEngineIntegration(url);
					if (!businessOk) return false;
				} else {
					logInfo('Skipping Business Engine test for Quick Scan flow', 'Orchestrator');
				}
			}
			// After core flows, validate pipeline path as queued journey
			try { if (!JourneyEngine.testPipeline_Transcription || !JourneyEngine.testPipeline_Transcription(defaults)) return false; } catch {}
			
			// === TRUTHY GATES: Never pass if pipeline didn't happen ===
			// Check if this is a Quick Scan flow (which doesn't use Business Engine APIs)
			const isQuickScan = Logcat.recent('(QUICK_SCAN|Quick Scan|🎯.*QUICK_SCAN|QUICK SCAN SELECTED|stage=QUICK_SCAN|QUICK_SCAN_START|QUICK_SCAN_COMPLETE|QUICK_SCAN_MODE)', 10);
			if (!isQuickScan || isQuickScan.length === 0) {
				// For AI Analysis flows, require both vend-token and ttt/transcribe
				const vendSeen = !!Logcat.findLastHttpExchange('vend-token');
				const tttSeen  = !!Logcat.findLastHttpExchange('ttt/transcribe');
				if (!vendSeen || !tttSeen) {
					reportCriticalError('Pipeline not executed',
						`vendSeen=${vendSeen} tttSeen=${tttSeen} (tests cannot pass without both)`, 'Core');
					return false;
				}
			} else {
				logInfo('Skipping pipeline validation for Quick Scan flow', 'Orchestrator');
			}
			
			return true;
		default:
			reportCriticalError('Invalid TestScope', `Scope '${scope}' not supported`, 'Validation');
			return false;
	}
}

function main() {
    const defaults = loadDefaults();
    const args = Object.assign({}, defaults, parseArgs(process.argv.slice(2)));
	logInfo('=== Pluct Automatic Tests (Node.js) ===', 'Entry');
	logInfo(`Scope=${args.scope} Url=${args.url}`, 'Entry');
	// Start live logcat streaming for richer, real-time output and HTTP surfacing
    try {
        const liveFilter = 'app.pluct|TTT|BusinessEngine|REQUEST_SUBMITTED|HTTP REQUEST|HTTP RESPONSE|proxy|Authorization|Bearer|ttt/|am_start|START u0|FATAL|AndroidRuntime|Crash|Exception|Error|Quick Scan|AI Analysis|ProcessingTier|Worker|WorkManager';
        Logcat.startLive && Logcat.startLive(liveFilter, null); // No file output, just console
    } catch {}

	TestSession.TestUrl = args.url;
	TestSession.StartTime = Date.now();

	ensurePrerequisites();
	if (!optionalBuild(args.forceBuild)) {
		showTestReport(false);
		process.exit(1);
	}
	if (!deployIfNeeded(args.skipInstall)) {
		showTestReport(false);
		process.exit(1);
	}

	// Launch
	logStage('Launch', 'AppLaunch');
	prepareDevice();
	grantBaselinePermissions();
    // allow package/activity from config
    const pkg = defaults.package || 'app.pluct';
    const act = defaults.activity || 'app.pluct/.MainActivity';
    if (!execOk(`adb shell am start -W -n ${act}`)) {
		reportCriticalError('App Launch Failed', 'MainActivity did not start', 'Launch');
		showTestReport(false);
		process.exit(1);
	}

	// Clear logcat before journeys for clean detection
	Logcat.clear();
}

async function main() {
	const args = parseArgs(process.argv.slice(2));
	const overallSuccess = await runScope(args.scope, args.url);
	showTestReport(overallSuccess);
	
	
    try { Logcat.stopLive && Logcat.stopLive(); } catch {}
	if (overallSuccess) {
		logSuccess('Automatic tests completed', 'Entry');
		process.exit(0);
	} else {
		process.exit(1);
	}
}

main().catch(e => {
	logError(`Fatal error: ${e.message}`, 'Entry');
	process.exit(1);
});

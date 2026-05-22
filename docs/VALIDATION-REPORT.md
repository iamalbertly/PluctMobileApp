# Validation Report

Date: 2026-05-12

## Passed

```powershell
.\gradlew :shared:allTests
.\gradlew :shared:compileKotlinAndroid
.\gradlew clean :app:assembleDebug :app:testDebugUnitTest :shared:allTests
.\scripts\windows\verify-repo-ready.ps1
.\scripts\windows\build-android-debug.ps1
```

Results:

- Shared KMP Android/common tests passed.
- `:shared:compileKotlinAndroid` passed through the compatibility alias task.
- Android debug APK built successfully.
- Android unit tests passed.
- Repo readiness checks passed.
- Debug APK exists at `app\build\outputs\apk\debug\app-debug.apk`.

APK observed on Windows:

```text
app\build\outputs\apk\debug\app-debug.apk
Size: 21080151 bytes
```

## Failed or Not Fully Validated

```powershell
.\scripts\windows\run-android-e2e.ps1
```

The script found `emulator-5554`, ran `adb devices`, `npm install`, and `npm run test:all`. The wrapper now exits non-zero when the harness reports failures. Observed failures:

```text
Journey-Intent-03TikTok-04BalanceRace-01Validation.js failed:
Auto-submit not triggered within ~66s (no submit and no post-balance skip; check lock shade / logcat buffer)

Second run:
Tests failed: home_ui: home shell not visible
```

This is recorded as a failed e2e validation item. The Gradle build and APK output are still valid.

iOS archive/sign/upload was not run locally because this is Windows. The iOS path must be validated in GitHub Actions on `macos-latest`.

## Required Accounts and Secrets

Apple/TestFlight GitHub secrets:

```text
APPLE_TEAM_ID
APPLE_BUNDLE_ID
APP_STORE_CONNECT_API_KEY_ID
APP_STORE_CONNECT_API_ISSUER_ID
APP_STORE_CONNECT_API_KEY_P8
IOS_DISTRIBUTION_CERTIFICATE_BASE64
IOS_DISTRIBUTION_CERTIFICATE_PASSWORD
IOS_PROVISIONING_PROFILE_BASE64
```

Backend runtime:

```text
ENGINE_JWT_SECRET
```

## Windows Limitation

Windows can validate Gradle, build the Android APK, and run Android/ADB tests. Windows cannot locally sign and upload a real iOS IPA for TestFlight. The permanent iOS path is the macOS GitHub Actions workflow.

## E2E harness mitigations (post-2026-05-12)

- **`run-android-e2e.ps1`**: adb wait-for-device, `sys.boot_completed` poll, wake (`keyevent 224`), `wm dismiss-keyguard`, swipe to reduce lock-shade interference before `npm run test:all`.
- **`validateEnvironment`**: requires `ENGINE_JWT_SECRET` unless `PLUCT_E2E_SKIP_ENGINE_JWT_CHECK=1` (smoke only).
- **`BaseJourney`**: shared `wakeDismissLockShade` / `nudgePluctForeground` for journeys; **Dedupe SSOT** uses them plus **activity-stack soft pass** when UIAutomator still shows SystemUI but `dumpsys` shows Pluct Main resumed.
- **Intent-03**: after logcat sweep, **`dumpsys activity activities`**; clearer error text; optional **`PLUCT_INTENT03_HARNESS_TAP=1`** to tap `extract_script_button` / **Start** for recovery.
- **UI tap map**: `extract_script_button` content-desc fallback aligned to **Start** (Compose semantics).
- **iOS checklist script**: `.\scripts\windows\verify-ios-ci-ready.ps1` prints required GitHub secret names and optional `gh secret list`.
- **NPM**: `npm run test:e2e-device-sensitive` for the two device-flaky journeys only.

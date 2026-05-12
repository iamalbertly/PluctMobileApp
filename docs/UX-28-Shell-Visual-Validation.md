# UX-28: App icon and shell visual validation

## Purpose (Customer, Speed and Trust)

This journey proves that the **same foreground artwork** used for the adaptive launcher icon appears in the **home header** (`pluct_brand_logo_mark`), that the **capture card** and **value promise banner** render on a cold launch, that **Settings** shows the **grouped card layout** (account, support, permissions, credits), and that **no fatal crash** appears in logcat during navigation. Retries on flaky `uiautomator dump` mirror real-device friction.

## Prerequisites

1. One Android device or emulator with ADB state **`device`** (not `offline` or `unauthorized`). Verify with `adb devices`.
2. Optional but recommended: run `.\gradlew.bat :app:assembleDebug` so `app/build/outputs/apk/debug/app-debug.apk` exists; the journey installs with `adb install -r` when the APK is present.

## How to run

```bash
npm run test:shell-visual
```

Or directly:

```bash
cross-env TEST_FILTER=Journey-UX-28AppIconAndShellVisual-01Validation.js node scripts/nodejs/Pluct-Main-01Orchestrator.js
```

## Assertions (high level)

| Stage | UIAutomator / behavior |
|-------|-------------------------|
| Home | `capture_card_root`, `pluct_brand_logo_mark`, `home_value_promise_banner` or `home_value_promise_line` |
| Logcat | No `FATAL EXCEPTION` in recent buffer |
| Settings | `settings_sheet_content` plus grouped markers `settings_section_account` / `settings_group_card_*` |
| Back | Tap `settings_top_bar_back` (fallback: relaunch + Home tab), then home assertions again |
| Icons | Adaptive XML includes **monochrome** (`ic_launcher_monochrome`); `setSmallIcon` must use **`ic_stat_pluct`**, not `@mipmap/ic_launcher`. |

## Rationale for edge cases

- **Offline ADB host**: The journey requires a line ending with tab + `device` so wireless-debugging `offline` rows do not produce false positives.
- **Install failure**: If the APK is missing or install fails, the journey continues with whatever build is on the device (still validates UI if the app is current enough).
- **Back affordance**: If `settings_top_bar_back` is not tappable in a vendor overlay, hardware back is attempted once.

## Related files

- Journey: [scripts/nodejs/journeys/Journey-UX-28AppIconAndShellVisual-01Validation.js](../scripts/nodejs/journeys/Journey-UX-28AppIconAndShellVisual-01Validation.js)
- Icon sync: [scripts/sync-icons.ps1](../scripts/sync-icons.ps1)
- Store graphics mirror: [docs/store-listing/playstore-icon.png](store-listing/playstore-icon.png)

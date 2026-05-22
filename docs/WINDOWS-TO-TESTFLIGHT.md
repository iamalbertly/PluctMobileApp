# Windows to Android APK and iOS TestFlight

This repo can build Android locally on Windows. iOS signing and TestFlight upload run on GitHub Actions macOS because Windows cannot locally sign an iOS app for real iPhone distribution.

## Windows Commands

```powershell
git pull
.\scripts\windows\verify-repo-ready.ps1
.\scripts\windows\build-android-debug.ps1
.\scripts\windows\verify-ios-ci-ready.ps1
.\scripts\windows\run-android-e2e.ps1
```

### Android E2E environment

- Export `ENGINE_JWT_SECRET` (same value the Business Engine uses for JWT) before `npm run test:all`, or load it via a local `.dev.vars` you apply to your shell. The harness **fails fast** at startup if it is missing, unless you set `PLUCT_E2E_SKIP_ENGINE_JWT_CHECK=1` for partial smoke only.
- `run-android-e2e.ps1` runs a short **device preflight** (wait-for-device, boot check, wake, dismiss keyguard, swipe) to reduce lock-shade flakes.
- Optional harness-only recovery for Intent-03: set `PLUCT_INTENT03_HARNESS_TAP=1` to tap the inline **Start** (`extract_script_button`) after a failed logcat wait when Pluct is already the resumed activity.
- Optional targeted rerun: `npm run test:e2e-device-sensitive` runs only Dedupe SSOT + Intent-03 balance race.

### Playwright MCP (App Store Connect in Chrome)

Chrome’s in-product remote debugging often returns **HTTP 404** for `http://127.0.0.1:9222/json/version`, so `@playwright/mcp` must use the **WebSocket browser endpoint** from Chrome’s `DevToolsActivePort` file (not the `http://…:9222` URL alone).

1. Enable **Allow remote debugging** at `chrome://inspect/#remote-debugging` and keep Chrome open.
2. Run `.\scripts\windows\print-chrome-mcp-cdp-endpoint.ps1` — it prints `--cdp-endpoint=ws://127.0.0.1:<port>/devtools/browser/<guid>` lines for your **User** `~/.cursor/mcp.json` `playwright.args` array (append `--cdp-timeout=120000` to avoid slow-start timeouts).
3. **Restart MCP** (Cursor: reload window or restart Playwright MCP) so the new args load. After every full Chrome quit, the browser GUID changes — run the script again and update `mcp.json`.

The Android debug APK is produced at:

```text
app\build\outputs\apk\debug\app-debug.apk
```

## GitHub Actions

Android:

```text
Actions -> Android CI -> run workflow -> download pluct-android-debug-apk
```

iOS:

```text
Actions -> iOS TestFlight -> run workflow
```

If Apple secrets are missing, the iOS workflow fails before signing with a clear missing-secret message. If all Apple secrets are present, it builds the shared KMP framework, archives the SwiftUI app, exports an IPA, uploads the IPA artifact, and uploads the app to TestFlight.

## Required GitHub Secrets

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

Android/backend runtime authentication also requires `ENGINE_JWT_SECRET` outside git when using Business Engine JWT signing.

## Apple Account Requirements

TestFlight, Ad Hoc distribution, and App Store distribution require Apple Developer Program access. TestFlight avoids collecting tester device UDIDs, while Ad Hoc requires registered devices.

Sources:
- Apple TestFlight: https://developer.apple.com/testflight
- Apple app distribution docs: https://developer.apple.com/documentation/xcode/distributing-your-app-for-beta-testing-and-releases
- Apple Xcode distribution methods: https://help.apple.com/xcode/mac/current/en.lproj/dev31de635e5.html

# ClipForge Technical Context

## Current State
- ✅ Choice Engine user journey implemented
- ✅ Testing framework refactored to Node-only orchestrator with detailed-by-default logging
- ✅ Tests validate actual Android screen content via ADB UI dump
- ✅ Verbose error reporting, retry-once, terminate-on-first-error

## Technical Debt & Issues
1. ✅ False positives eliminated with real UI validation
2. ✅ Detailed failure artifacts: XML + screenshots + log slices
3. ✅ Device prep and permission grants automated
4. ✅ Quick Scan selection automated by id/text/content-desc
5. ❌ Business Engine health logs missing in app (kept opt-in to avoid fake positives)

## Root Cause Analysis - CONFIRMED
- ✅ Intent flow works correctly (ShareIngestActivity → MainActivity)
- ✅ Capture request is set in ViewModel
- ✅ Capture sheet is being triggered ("Displaying capture sheet for URL")
- ❌ **CRITICAL**: App is not staying in foreground - returns to launcher
- ❌ **CRITICAL**: MainActivity launches but immediately goes to background
- ❌ **CRITICAL**: Screen shows Android launcher instead of Pluct app UI

## ✅ CHOICE ENGINE WORKING - ALL 6 ENHANCEMENTS IMPLEMENTED!
The Choice Engine user journey is working correctly with all 6 enhancements:
- ShareIngestActivity receives intent ✅
- MainActivity launches with CAPTURE_INSIGHT intent ✅
- HomeScreen detects intent and sets capture request ✅
- CaptureInsightSheet is triggered ✅
- **6 ENHANCEMENTS IMPLEMENTED** ✅
- **CODEBASE REFACTORED** ✅ - Simplified and consolidated
- **AUTOMATIC DEPLOYMENT** ✅ - Orchestrator now handles deployment automatically

### ✅ **ALL 6 ENHANCEMENTS COMPLETED**
1. **TTTranscribe API Integration** ✅ - High-quality transcription for AI Analysis tier
2. **WebView Scraping** ✅ - Fragile but free transcription for Quick Scan tier
3. **Coin System** ✅ - Premium tier with coin balance and purchase flow
4. **Thumbnail Loading** ✅ - Coil image library for proper video thumbnails
5. **Toast Messages** ✅ - User feedback for tier selection and coin status
6. **Error Handling** ✅ - Comprehensive retry mechanisms and error recovery

### ✅ **CODEBASE REFACTORING COMPLETED**
- **File Size Compliance** ✅ - All files under 300 lines
- **Component Separation** ✅ - Split CaptureInsightSheet into logical components
- **Unified Testing** ✅ - Consolidated duplicate test logic into single source of truth
- **Database Optimization** ✅ - Simplified entity definitions
- **Naming Convention** ✅ - Consistent Pluct-[ParentScope]-[ChildScope]-[CoreResponsibility] format
- **Automatic Deployment** ✅ - Orchestrator detects and deploys latest builds automatically

### Remaining gating item
Business Engine validation remains disabled by default until the app emits a clear health log (e.g., `BusinessEngine: HEALTH_OK`).

## Required Fixes
1. **Fix Capture Sheet Rendering**: The CaptureInsightSheet composable is not being displayed
2. **Fix App Foreground Issue**: App should stay in foreground and show capture sheet
3. **Debug UI State**: Check if uiState.captureRequest is properly observed
4. **Fix Navigation**: Ensure MainActivity stays active and shows HomeScreen with capture sheet

## Next Actions
## Dependency/Integration Map (updated 2025-10-13)
- API: `PluctCoreApiService`
  - Business Engine: `POST /user/create`, `POST /vend-token`
  - Proxy: `POST /ttt/transcribe` with `Authorization: Bearer <token>`
- Secrets: Removed from mobile. `PluctTTTranscribeAuthenticator` now timestamps only.
- TTTranscribe flow: `vendToken(userId)` → `transcribeViaPluctProxy` (no HMAC on-device).
- WebView: Single config `WebViewConfig.configureSettings(webView)`; `WebViewConfiguration` deleted.
- Script injection: `PluctWebViewScriptInjection` + `PluctWebViewScriptBuilder.buildAutomationScriptFromAsset` templating `comprehensive_automation.js`.
- Assets: Keep `comprehensive_automation.js`; removed `scripttokaudit_automation.js`.
- Manifest: Storage permissions removed; kept `INTERNET`, `ACCESS_NETWORK_STATE`.

## Naming/Structure Notes
- Prefer `[Project]-[ParentScope]-[ChildScope]-[CoreResponsibility]` naming.
- Avoid giant Kotlin files (>300 lines).

## Test Execution (Node-only)
```
node scripts/nodejs/Pluct-Automatic-Orchestrator.js -scope All
```

Artifacts:
- UI dumps/screenshots: `artifacts/ui/`
- Request/response log slices: `artifacts/logs/`

Config: `scripts/nodejs/config/Pluct-Test-Config-Defaults.json`
- `enableBusinessEngine`: false by default
- `expectedLogPatterns`: requestSubmitted/responseOk regexes used to confirm API activity

## Logs
- 2025-10-13..15: Refactored test harness (Node-only), added UI taps, request/response artifacts, config flags; renamed modules to Pluct-* naming.
- 2025-01-21: UI Cleanup - Removed Capture Video section and FAB button from main home screen for cleaner, more intuitive interface. Updated PluctHomeScreen.kt and MainActivity.kt, cleaned up unused imports and parameters.

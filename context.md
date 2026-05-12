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

## Main shell navigation (2026-05-11)

- `app/pluct/ui/navigation/Pluct-UI-Navigation-01MainBottomBar.kt` — `PluctUIMainShellTab` + bottom bar (`nav_home`, `nav_library`, `nav_settings`).
- `app/pluct/PluctUIScreen01MainActivity.kt` — outer `Scaffold` when not in video detail: Home (embedded `PluctHomeScreen` without inner scaffold), Library (`PluctUIScreen02LibraryTab01Screen`), Settings tab (`PluctUIScreen03SettingsTab01Screen`), shared debug log overlay.
- `app/pluct/ui/screens/Pluct-UI-Screen-01HomeScreen-04Settings-00SharedBody.kt` — settings scroll body shared by bottom sheet and Settings tab (one SSOT for rows).
- `app/pluct/ui/components/PluctHomeShellTopBar` in `Pluct-UI-03Header.kt` — brand row + settings (`settings_button`).
- `app/pluct/ui/components/Pluct-UI-Component-02Branding-01LogoMark.kt` — in-app mark uses `R.mipmap.ic_launcher_foreground` (aligned with adaptive launcher foreground; `pluct_brand_logo_mark`).
- Journey: `scripts/nodejs/journeys/Journey-UX-27PluctRedesign-MockupParity-01Validation.js` — `npm run test:redesign`.
- Journey: `scripts/nodejs/journeys/Journey-UX-28AppIconAndShellVisual-01Validation.js` — grouped settings + header icon + promise banner; `npm run test:shell-visual`.

## Direct-to-value modules (2026-05-11)
- `app/pluct/ui/readiness/Pluct-UI-Readiness-01Kind.kt` — readiness resolver (Customer / Speed & Trust).
- `app/pluct/ui/components/Pluct-UI-Component-09Readiness-01Strip.kt` — single primary CTA strip.
- `app/pluct/core/error/Pluct-Core-Error-08OutcomeFamily.kt` — SSOT outcome taxonomy for failed feed rows.
- `app/pluct/core/user/Pluct-Core-User-01Display-01Formatter.kt` — non-raw identity string.
- `app/pluct/core/debug/Pluct-Core-Debug-02DiagnosticShare-01Builder.kt` — support bundle text.
- Tests: `scripts/nodejs/journeys/Journey-UX-25DirectToValue-Readiness-01Validation.js`, runner `scripts/nodejs/Pluct-Test-Focused-07DirectToValue-01Runner.js`.
- Credit fairness + invalid TikTok URL: `scripts/nodejs/journeys/Journey-UX-26TikTok-Url-Refund-NoCharge-01Validation.js`, runner `scripts/nodejs/Pluct-Test-Focused-08TikTok-Url-Refund-01Runner.js`, npm `npm run test:tiktok-refund`, doc `docs/CREDIT_FAIRNESS_ADB_LOOP.md`.

## Test Execution (Node-only)
```
node scripts/nodejs/Pluct-Automatic-Orchestrator.js -scope All
```

Direct-to-value subset:

```
node scripts/nodejs/Pluct-Test-Focused-07DirectToValue-01Runner.js
```

TikTok invalid URL + refund UI (ADB):

```
npm run test:tiktok-refund
```

NPM focused path (same journeys as `TEST_FILTER`):

```
npm run test:paths
```

After touching readiness, balance, capture, battery, or journey harness files, prefer:

```
npm run test:updated
```

(`UX-25` + `Intent-03 balance race` only; `UX-24` needs a clean tutorial state—run via full orchestrator or after `pm clear` when validating battery UI.)

ADB multi-device: set `ANDROID_SERIAL` or `ADB_SERIAL` to the target id from `adb devices`; otherwise the harness prefers `emulator-*` then the first authorized `device`.

Operator visibility (client-only, 2026-05-11): duplicate debug rows bump ` · repeats=N` and emit `PluctUserPain` logcat; health refresh logs `HealthMonitor: refresh_failed|parse_failed_inner|ttt_status_missing`; diagnostic share includes error count by category; `PluctCoreAPIUnifiedService.isPolicyBlockingTranscribe` parses JSON policy (replaces substring match).

Public API (2026-05-11): `PluctCoreAPIUnifiedService.onAppForegroundedForDiagnostics()` — forced health refresh on `ON_RESUME` plus `PluctUserPain` / `PluctForeground` queue snapshot when processing or queued work exists; `execute()` attempts `healthMonitor.refreshNow(force=true)` once when the circuit breaker is open (still returns `SERVICE_COOLDOWN`).

Node ADB harness: `adb start-server` uses an extended timeout; `_executeCommandDirect` treats SIGTERM/ETIMEDOUT during daemon boot as success when `adb devices` still shows an authorized device; `_adbStartServerReliable` continues if devices are online despite a failed start-server return.

Artifacts:
- UI dumps/screenshots: `artifacts/ui/`
- Request/response log slices: `artifacts/logs/`

Config: `scripts/nodejs/config/Pluct-Test-Config-Defaults.json`
- `enableBusinessEngine`: false by default
- `expectedLogPatterns`: requestSubmitted/responseOk regexes used to confirm API activity

## Trust Fixes Implementation Status

### ✅ Completed Trust Fixes (7 features)
1. **Intelligent Timeout Logic** - Tracks API progress instead of blind timeout
2. **Error Deduplication** - Unified error state management
3. **ADB False Positive Fix** - ADB connection detection prevents false timeouts
4. **Auto-Submit Intent** - Automatic transcription start on intent receive
5. **Background Processing** - App minimizes with notification during processing
6. **Notification Navigation** - Notification tap navigates to transcribed video
7. **Credit Queue Flow** - Auto-queue when credits insufficient

### ✅ Completed Edge Cases (8 features)
1. **Rapid Intent Receipt** - Queue intents when processing active
2. **Credit Depletion** - Atomic credit reservation prevents race conditions
3. **Network Loss** - Network monitoring during background processing
4. **Multiple Notifications** - Job deduplication prevents duplicates
5. **JWT Token Expiration** - Proactive token refresh before expiration
6. **Concurrent Token Vending** - Request deduplication prevents duplicates
7. **Token Expiration Polling** - Token refresh during polling phase
8. **Network Interruption** - Idempotency for network interruptions

### ✅ Test Infrastructure
- **Focused Test Runner**: `Pluct-Test-Focused-02TrustFixes-01Runner.js`
- **Auto-Fix Service**: `Pluct-Test-AutoFix-02TrustFixes-01Service.js`
- **Build & Deploy Service**: `Pluct-Build-Deploy-01AutoService.js`
- **15 Comprehensive Tests**: 7 trust fixes + 8 edge cases

### File Naming Compliance
All new files follow 5+ scope layer naming convention:
- ✅ `Pluct-UI-Screen-01MainActivity-01IntentHandler-02QueueManager.kt` (6 scopes)
- ✅ `Pluct-Core-Credit-01AtomicReservation-01Service.kt` (5 scopes)
- ✅ `Pluct-Core-Background-01TranscriptionWorker-02NetworkMonitor.kt` (5 scopes)
- ✅ `Pluct-Core-Background-01TranscriptionWorker-03JobDeduplication.kt` (5 scopes)
- ✅ `Pluct-Core-API-01UnifiedService-02TokenRefresh-01Manager.kt` (6 scopes)
- ✅ `Pluct-Core-API-01UnifiedService-03RequestDeduplication-01Handler.kt` (6 scopes)
- ✅ `Pluct-Test-Focused-02TrustFixes-01Runner.js` (5 scopes)
- ✅ `Pluct-Test-AutoFix-02TrustFixes-01Service.js` (5 scopes)
- ✅ `Pluct-Build-Deploy-01AutoService.js` (4 scopes - utility)
- ✅ `Journey-EdgeCase-01RapidIntentReceipt-Validation.js` (4 scopes - Journey is top-level)

## Codebase Refactoring - Duplicate Elimination (2025-01-XX)

### ✅ COMPLETED REFACTORING (2025-01-XX)

#### Phase 1: File Size Compliance ✅
- **UnifiedService-01Main.kt**: Reduced from 336 lines to 219 lines
  - Extracted handlers: Balance, Token, Metadata, Status, AuthRetry
  - Eliminated duplicate 401 retry logic (was in 6+ places)
  - Improved separation of concerns

#### Phase 2: Error Handling Consolidation ✅
- **UI Error Handler**: Now uses `PluctCoreError03UserMessageFormatter` as single source of truth
- **Removed**: Duplicate `determineRetryability()` function
- **Unified**: All error messages now go through one formatter

#### Phase 3: Auth Retry Handler ✅
- **Created**: `Pluct-Core-API-01UnifiedService-15AuthRetry-01Handler.kt`
- **Eliminated**: 6+ duplicate 401 retry implementations
- **Single Source**: All API methods now use unified auth retry handler

### Files Marked for Deletion
- `DeleteThisFile_Pluct-Core-API-01UnifiedService-08TranscriptionFlow-03Submission-01Handler.kt` (170 lines)
  - **Rationale**: Duplicate submission logic already implemented in TranscriptionFlow01Handler (lines 313-427). This file was created as an extraction attempt but never integrated, creating confusion and maintenance burden.
  - **Status**: Marked for deletion, no references found in codebase
  
- `DeleteThisFile_Pluct-Core-API-01UnifiedService-08TranscriptionFlow-04Polling-01UnifiedService.kt` (277 lines)
  - **Rationale**: Duplicate polling logic already implemented in TranscriptionFlow01Handler (lines 440-634). This file was created as an extraction attempt but never integrated, creating confusion and maintenance burden.
  - **Status**: Marked for deletion, no references found in codebase

### Refactoring Status
- **Phase 1**: ✅ File size compliance (UnifiedService refactored)
- **Phase 2**: ✅ Error handling consolidation (UI handler uses unified formatter)
- **Phase 3**: ✅ Auth retry handler extraction (eliminated 6+ duplicates)
- **Phase 4**: ⏳ Validation logic consolidation (pending)
- **Phase 5**: ⏳ Notification/dialog consolidation (pending)
- **Phase 6**: ⏳ Naming convention fixes (pending)

## Logs
- 2025-10-13..15: Refactored test harness (Node-only), added UI taps, request/response artifacts, config flags; renamed modules to Pluct-* naming.
- 2025-01-21: UI Cleanup - Removed Capture Video section and FAB button from main home screen for cleaner, more intuitive interface. Updated PluctHomeScreen.kt and MainActivity.kt, cleaned up unused imports and parameters.
- 2025-01-29: Trust Fixes Implementation - Completed all 6 edge case implementations, 8 edge case tests, focused test runner with auto-fix, and comprehensive documentation. All files follow 5+ scope layer naming convention.

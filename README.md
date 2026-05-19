# 🚀 Pluct Mobile App - Advanced TikTok Transcription Platform

## 📱 **Overview**

Pluct is a cutting-edge mobile application that provides instant AI-powered transcription services for TikTok videos. Built with modern Android architecture and comprehensive API integration, Pluct delivers seamless video-to-text conversion with real-time processing and intelligent content analysis.

## ✨ **Key Features**

### 🎯 **Core Functionality**
- **⚡ Quick Scan**: Instant transcription with free tier processing
- **🤖 AI Analysis**: Premium deep insights with key takeaways
- **📊 Real-time Credit Management**: Live balance tracking and usage monitoring
- **🔄 Background Processing**: WorkManager-powered transcription pipeline
- **📱 Modern UI**: Jetpack Compose with Material 3 design

### 🌐 **API Integration**
- **Business Engine**: Complete integration with Pluct Business Engine API
- **TTTranscribe**: Advanced transcription service with status polling
- **JWT Authentication**: Secure token-based authentication system
- **Health Monitoring**: Real-time system health checks
- **Error Handling**: Comprehensive retry logic and fallback mechanisms

### 🧪 **Testing & Quality**
- **Automated Testing**: Node.js-based test orchestration
- **UI Validation**: Comprehensive UI component testing
- **API Testing**: End-to-end API integration validation
- **Logcat Monitoring**: Real-time log analysis and debugging
- **Artifact Capture**: Screenshot and XML dump collection

## 🏗️ **Architecture**

### **Modern Android Architecture**
```
📱 Presentation Layer (Jetpack Compose)
├── 🎨 UI Components (Material 3)
├── 🔄 ViewModels (MVVM Pattern)
└── 🧭 Navigation (Compose Navigation)

📊 Business Logic Layer
├── 🔧 Use Cases & Interactors
├── 🏪 Repository Pattern
└── 🔄 State Management (StateFlow)

🌐 Data Layer
├── 🗄️ Local Database (Room)
├── 🌍 Remote APIs (OkHttp + Retrofit)
└── 💾 Data Sources (Repository)

🔧 Infrastructure
├── 🏗️ Dependency Injection (Hilt)
├── ⚙️ Background Processing (WorkManager)
└── 🧪 Testing Framework (JUnit + Espresso)
```

### **API Integration Flow**
```
1. Health Check -> Business Engine Status
2. Client Policy -> update gate, APK URL, feature gates
3. Balance Check -> available and reserved wallet units
4. Quote -> price shown before work starts
5. Fulfill -> reserve only when the engine accepts work
6. Status Polling -> completed, charged, refunded, or ready/no charge
7. Result Processing -> transcript delivery and compact history row
```
## **API Integration Flow**
```
1. 🏥 Health Check → Business Engine Status
2. 🔐 JWT Generation → Authentication Token
3. 💰 Balance Check → Credit Validation
4. 🎫 Token Vending → Transcription Authorization
5. 🎬 Transcription Start → TTTranscribe Job
6. ⏳ Status Polling → Completion Monitoring
7. ✅ Result Processing → Transcript Delivery
```

## 🚀 **Getting Started**

### **Prerequisites**
- Android Studio Arctic Fox or later
- JDK 17+
- Android SDK 26+ (API Level 26)
- ADB (Android Debug Bridge)
- Node.js 16+ (for testing)

### **Quick Start**

1. **Clone the Repository**
   ```bash
   git clone https://github.com/iamalbertly/PluctMobileApp.git
   cd PluctMobileApp
   ```

2. **Build the Application**
   ```bash
   ./gradlew assembleDebug
   ```

3. **Install on Device**
   ```bash
   adb install app/build/outputs/apk/debug/app-debug.apk
   ```

4. **Run Automated Tests**
   ```bash
   # Run all tests with real-time output
   npm run test:all
   
   # Run in dev mode (stops on first error)
   cross-env DEV_MODE=1 npm run test:all
   
   # Run specific category
   npm run test:core
   npm run test:ui
   npm run test:transcription

   # Validate notification SSOT + dedupe wiring (ADB + logcat + UI)
   npm run test:dedupe-ssot
   The canonical test entrypoint is `npm run test:all`, backed by the Node journey orchestrator in `scripts/nodejs`. **Android UI validation is ADB + Node**, not browser automation. **Playwright MCP** (if enabled in your environment) is for **web** surfaces only, not the Android app. Test URLs include `https://vt.tiktok.com/ZS9bDyvc5/` for validation. See **[Testing](#-testing-framework)** below for complete testing documentation and options.

### **Three-tier system (evolution contract)**

The product spans **Android client**, **Business Engine** (policy, wallet, fulfillment, health), and **TTTranscribe** (transcription execution only). Today, client pain is surfaced via **logcat** (`PluctUserPain`, `HealthMonitor`), **Room debug logs**, and **Settings -> Send diagnostic**. When the app or engine moves ahead of the other, use **app `VERSION_CODE`**, **`GET /v1/public/client-policy`**, and **`GET /health/services`** as the coordination signals. `/v1/public/client-policy` is the single source for Android hard update, latest APK URL/SHA/version, future iOS URL, transcription disable, wallet fulfillment, free tier, and legacy vend-token compatibility gates. Playwright MCP applies only to **web** dashboards you validate separately from this repo. The runner fails fast on the first broken UI, API, or device step and prints focused UI/logcat diagnostics.

### **Quick Start**
```bash
npm run test:all
```

This runs registered journeys in customer-risk order: latest changed paths first, last failed tests next, policy/update, wallet settlement, queue count, history metadata, quick scan, TTTranscribe, API connectivity, and legacy compatibility paths. The runner prints one of these labels so skipped device coverage is never confused with a production pass: `PASS_FULL_DEVICE`, `PASS_LOCAL_COMPILE_ONLY`, `SKIPPED_MISSING_RELEASE_ENV`, or `FAIL_FIRST_ERROR`. Protected Business Engine calls use `BE_USER_JWT` or generated JWT auth when available; otherwise the harness uses the same release-style mobile request headers that the app uses, so missing `ENGINE_JWT_SECRET` no longer turns a connected device run into a fake skip.

### **Read Pluct adb logcat (operator / device)**

Use a real device or emulator with the app installed, then:

```bash
adb logcat -s PluctForeground:I PluctUserPain:I HealthMonitor:D HealthMonitor:W HealthMonitor:E CaptureCard:D MainActivity:D TranscriptionWorker:D PluctCoreAPIUnified:W DebugLogManager:D
```

On resume the app emits `PluctForeground` and `PluctUserPain` queue snapshots when work is stuck; the Node harness uses a **long timeout** for `adb start-server` and treats daemon-boot **SIGTERM** as success when `adb devices` still lists a device.

### **Common Test Commands**
```bash
# Run all tests
npm run test:all

# Run tests in specific categories
npm run test:core                  # 01-core tests only
npm run test:ui                    # 02-ui tests only
npm run test:transcription         # 03-transcription tests only

# Run specific focused tests
npm run test:ux-fixes              # UX improvements test suite

# Development mode (stops on first failure)
cross-env DEV_MODE=1 npm run test:all

# Validate updated TikTok and Business Engine paths first
npm run test:all -- Pluct-Test-Validation-10ErrorHandling Journey-QuickScan Journey-TTTranscribeIntegration Journey-APIConnectivity Journey-TokenVendingValidation
```

### **Testing Validation Checklist**
- ✅ **Core Flows**: App launch, share intent, TikTok URL handling
- ✅ **API Integration**: Business Engine connectivity, token vending, transcription submission
- ✅ **Status Polling**: Adaptive intervals (2s → 3s → 4.5s → 10s) and timeout detection
- ✅ **Error Categorization**: Consistent recovery actions (Network, Auth, Rate Limit, Credits, Validation)
- ✅ **Circuit Breaker Auto-Reset**: Opens after 5 failures, auto-resets after 30s silence
- ✅ **Adaptive Polling**: Backend load reduction from ~120 req/min to ~18 req/min
- ✅ **Credit Management**: Balance loading, insufficient credits, queue fallback

### **Focused Validation Tests** (UX/Reliability Improvements)
Current high-signal journeys live in `scripts/nodejs/journeys` and run through `npm run test:all`.

1. **`scripts/nodejs/journeys/Journey-TTTranscribeIntegration.js`**
   - Validates Business Engine to TTTranscribe connectivity
   - Confirms valid TikTok URLs return usable metadata
   - Fails fast before wasting credits or worker time

2. **`scripts/nodejs/journeys/Journey-UX-05RedundantVisuals-Validation.js`**
   - Validates the refreshed compact home journey
   - Confirms invalid or stale URLs do not pass as real jobs
   - Keeps senior-friendly visible text and primary actions in view

3. **`scripts/nodejs/journeys/Pluct-Test-Validation-10ErrorHandling.js`**
   - Tests network error categorization → "Check Connection" + "Retry"
   - Tests rate-limit categorization → "Retry Later"
   - Tests credits error categorization → "Add Credits"
   - Tests server error categorization → "Retry"
   - Validates consistency across the high-risk failure paths

### **Monitoring Test Execution**
Node/ADB journeys write artifacts to `artifacts/` including:
- `ui_dump_latest.xml` — Latest UI hierarchy
- `video_added_ui_dump.xml` — Post-transcription state
- `status_history.json` — Status transitions
- Node journey logs with request/response details

## 🛠️ **Architecture & Technical Improvements**

### **Implementation Summary** (5 UX/Reliability Fixes + 3 Tech Debt Cleanups)

#### **5 UX/Reliability Improvements Delivered**

| # | Fix | Implementation | Files Modified | Impact |
|---|-----|-----------------|-----------------|--------|
| 1 | **Stall Timeout** | Reduced timeout from 20s → 10s | `TranscriptionFlow02Handler.kt` | Faster error detection |
| 2 | **Retry Consolidation** | Centralized retry logic | `RetryabilityDecider.kt`, `UnifiedHandler.kt` | Single source of truth |
| 3 | **Error Categorization** | Unified error messaging | `ErrorClassifier.kt`, `ErrorDisplay.kt` | Consistent UX |
| 4 | **Circuit Breaker** | Auto-reset after 30s silence | `EnhancedBreaker.kt`, `UnifiedService.kt` | Prevents indefinite blocking |
| 5 | **Adaptive Polling** | 2s → 10s exponential backoff | `AdaptiveIntervalCalculator.kt`, `PollingHandler.kt` | 85% less backend load |

#### **3 Tech Debt Cleanups Completed**

1. **Retry Logic Duplication**: `PluctCoreChecks01RetryabilityDecider` consolidates:
   - `HTTPClient.isRetryable()` (deprecated)
   - `UnifiedService.isRetryableFailure()` (delegates)
   - `RetryHandler.isRetryable()` (delegates)

2. **Error Categorization Duplication**: `PluctCoreCategorization01ErrorClassifier` consolidates:
   - `ErrorDisplay.getRecoveryActions()` (integrated)
   - `UnifiedHandler` error messaging (refactored)
   - `UserMessageFormatter` categorization (consolidated)

3. **Legacy Test Orchestrators**: `npm run test:all` is the canonical runner for:
   - `scripts/nodejs/Pluct-Main-01Orchestrator.js` (removed)
   - All `Pluct-Test-Focused-*.js` runners (consolidated)
   - Entry point now: `scripts/nodejs/journeys/Pluct-Journey-01Orchestrator.js`

### **Recent Tech Debt Cleanups**

#### **1. Centralized Retry Decision Logic** (`Pluct-Core-Checks-01RetryabilityDecider`)

- Single source of truth for retry decisions
- Consolidates logic from HTTPClient, UnifiedService, and RetryHandler
- Ensures consistent retry behavior across all API calls
- Supports exponential backoff with configurable delays

#### **2. Centralized Error Categorization** (`Pluct-Core-Categorization-01ErrorClassifier`)
- Unified error classification across the app
- Consolidates logic from ErrorDisplay, UnifiedHandler, and UserMessageFormatter
- Provides consistent user-friendly messages for each error type
- Enables better analytics and error tracking

#### **3. Enhanced Circuit Breaker** (`Pluct-Core-Circuit-02EnhancedBreaker`)
- Auto-resets after 30 seconds of silence (no new failures)
- Prevents indefinite open state that blocks all requests
- Logs state transitions for debugging
- Thread-safe with coroutine mutex protection

#### **4. Adaptive Polling Intervals** (`Pluct-UI-Polling-01AdaptiveIntervalCalculator`)
- Starts at 2 seconds, scales by 1.5x every 3 attempts
- Reduces backend load during long-running transcription jobs
- Example: 2s → 2s → 2s → 3s → 3s → 3s → 4.5s → ... → 10s (max)
- Calculates total polling time for timeout thresholds

### **Legacy Code Consolidation**
The following older focused runners in `scripts/nodejs/` are marked for deletion (`DeleteThisFile_` prefix) when replaced by the canonical Node journey orchestrator:
- `Pluct-Main-01Orchestrator.js`
- `Pluct-Test-Focused-*.js` (all focused test runners)
- Individual test runner files

**Transition**: All test execution now routes through `npm run test:all`, which invokes `scripts/nodejs/Pluct-Main-01Orchestrator.js`.

## 🚀 **Building and Testing**

2. **Build the Application**
   ```bash
   ./gradlew assembleDebug
   ```

3. **Install on Device**
   ```bash
   adb install app/build/outputs/apk/debug/app-debug.apk
   ```

4. **Run Tests**
   ```bash
   npm run test:all
   ```
   Testing details are kept in this README. The canonical orchestrator is `scripts/nodejs/Pluct-Main-01Orchestrator.js`; `npm run test:all` invokes it.

### **Integration Guide**
For complete API integration instructions, authentication setup, and best practices, see the **[Mobile to Business Engine Integration Guide](MOBILE-to-BUSINESSengine-INTEGRATION-GUIDE.md)**.

## 🧪 **Testing Framework**

Focused validation uses **Node.js + ADB** (UIAutomator dump, logcat, shell intents). **Browser MCP** (Playwright-style) is for **web** targets only, not the Android app. **`maestro/` YAML is legacy** and is not invoked by `npm run` scripts—ignore it unless you explicitly run a Maestro runner.

### **Quick Start**
```bash
npm run test:all
```

Runs the full journey list in **customer-risk order** (recently touched paths first, then last failed, then high-priority API/transcription). For a **short** run after editing readiness, balance, capture, or the ADB harness:

```bash
npm run test:updated
```

(`Journey-UX-24BatteryOptimizationRefresh-Validation` is not in this filter: it needs the onboarding tutorial dismissed or cleared app data so the Settings sheet is reachable via UIAutomator; run it inside a full `npm run test:all` or after `adb shell pm clear app.pluct` when you explicitly need battery UI validation.)

### **Node Journey Coverage**
The current focused journeys validate the core user paths:

- **🎯 App Launch**: UI component validation and initialization
- **📤 Share Intent**: TikTok URL handling and capture sheet display
- **⚡ Quick Scan**: Button interaction and processing initiation
- **🔄 API Integration**: Real Business Engine and TTTranscribe connectivity
- **📊 Credit Management**: Live balance updates and API responses
- **📝 Processing Logs**: Logcat monitoring and status verification

### **Test Results**
```
✅ App Launch: PASSING - Main screen validation
✅ Share Intent: PASSING - TikTok URL processing
✅ Video Processing: PASSING - Complete flow navigation
✅ Quick Scan Click: PASSING - Button interaction
✅ Processing Logs: PASSING - Logcat monitoring
✅ Credit Balance: PASSING - Real API integration (10 credits)
⚠️ JWT Generation: EXPECTED - Requires real API endpoints
```

## 🔧 **Technical Implementation**

### **Core Services**

#### **PluctAPIIntegrationService**
Complete API integration service handling:
- Health checks and system monitoring
- JWT token generation and validation
- Credit balance management
- Token vending for transcription
- Transcription job management
- Status polling and completion handling

#### **PluctAuthJWTGenerator**
Secure JWT token generation with:
- Business Engine compatibility
- 15-minute token expiration
- `ttt:transcribe` scope validation
- HMAC256 algorithm implementation

#### **PluctNetworkHTTP01Logger**
Comprehensive HTTP logging with:
- Request/response interception
- JSON format logging for Node.js parsing
- Sensitive header redaction
- Performance monitoring

### **UI Components**

#### **Modern Recent Transcripts**
- Vertical scrolling with LazyColumn
- Status management with pills
- Video removal with SwipeToDismissBox
- Expand/collapse functionality
- Real-time status updates

#### **Quick Scan Integration**
- Clickable card with AndroidView
- UIAutomator compatibility
- Telemetry logging
- De-bounce protection
- Client request ID tracking

## 📊 **API Endpoints**

### **Business Engine Integration**
```
Base URL: https://pluct-business-engine.romeo-lya2.workers.dev

🏥 Health Check: GET /health
💰 Balance Check: GET /v1/credits/balance
🎫 Token Vending: POST /v1/vend-token
🎬 Transcription: POST /ttt/transcribe
⏳ Status Check: GET /ttt/status/{jobId}
```

### **Authentication Flow**
1. **JWT Generation**: User authentication with mobile scope
2. **Balance Validation**: Credit availability checking
3. **Token Vending**: Short-lived transcription tokens
4. **API Authorization**: Bearer token authentication

## 🔒 **Security Features**

- **JWT Authentication**: Secure token-based API access
- **Header Redaction**: Sensitive data protection in logs
- **Request Validation**: Input sanitization and validation
- **Error Handling**: Secure error message handling
- **Token Expiration**: Automatic token refresh mechanism

## 📱 **User Experience**

### **Modern UI Design**
- **Material 3**: Latest design system implementation
- **Responsive Layout**: Adaptive screen size handling
- **Accessibility**: Screen reader and navigation support
- **Performance**: Optimized rendering and memory management

### **Real-time Features**
- **Live Credit Balance**: Real-time API updates
- **Processing Status**: Background job monitoring
- **Error Feedback**: User-friendly error messages
- **Progress Tracking**: Visual processing indicators

## 🚀 **Performance Optimizations**

### **Build Optimizations**
- **KSP Migration**: Faster annotation processing
- **Configuration Cache**: Build time optimization
- **Resource Shrinking**: APK size reduction
- **ProGuard**: Code obfuscation and optimization

### **Runtime Optimizations**
- **Coroutines**: Asynchronous processing
- **StateFlow**: Reactive state management
- **Lazy Loading**: Efficient list rendering
- **Memory Management**: Optimized resource usage

## 🧪 **Testing & Quality Assurance**

### **Automated Testing**
- **Unit Tests**: JUnit-based component testing
- **Integration Tests**: API connectivity validation
- **UI Tests**: Espresso-based interaction testing
- **End-to-End Tests**: Complete user journey validation

### **Quality Metrics**
- **Code Coverage**: Comprehensive test coverage
- **Performance Monitoring**: Real-time metrics collection
- **Error Tracking**: Automated error detection and reporting
- **User Analytics**: Usage pattern analysis

## 📈 **Monitoring & Analytics**

### **Logcat Integration**
- **HTTP Telemetry**: Request/response logging
- **Processing Logs**: Background job monitoring
- **Error Tracking**: Exception logging and reporting
- **Performance Metrics**: Response time monitoring

### **Artifact Collection**
- **Screenshots**: UI state capture
- **XML Dumps**: UI hierarchy analysis
- **Log Files**: Comprehensive log collection
- **Test Reports**: Detailed test result analysis

## 🔄 **Continuous Integration**

### **Build Pipeline**
1. **Code Quality**: Lint checks and formatting
2. **Unit Testing**: Automated test execution
3. **Build Generation**: APK compilation and signing
4. **Deployment**: Automated device installation
5. **Testing**: End-to-end test execution

### **Quality Gates**
- **Build Success**: Compilation and packaging
- **Test Coverage**: Minimum coverage requirements
- **Performance**: Response time validation
- **Security**: Vulnerability scanning

## 📚 **Documentation**

### **Primary Integration Guide**
- **[Mobile to Business Engine Integration Guide](MOBILE-to-BUSINESSengine-INTEGRATION-GUIDE.md)**: Complete, production-verified API integration guide
  - Complete API reference for Business Engine
  - TTTranscribe service documentation
  - Authentication flow (JWT tokens)
  - Error handling and edge cases
  - Code examples and best practices
  - Testing and debugging guide

### **Development Guides**
- **Architecture**: System design and patterns (see Integration Guide)
- **Testing**: Test framework usage (see `scripts/nodejs/README-Smart-Testing.md`)
- **Deployment**: Build and release process
- **Troubleshooting**: Common issues and solutions (see Integration Guide)

### **Speed & Performance Features**
- **Optimistic UI**: Immediate progress feedback with smooth animations
- **Intelligent Polling**: Adaptive polling intervals (2s → 3s → 5s → 10s) based on job duration
- **Parallel Operations**: Metadata and token vending run simultaneously
- **Background Processing**: Process transcriptions in background with progress notifications
- **Enhanced Pre-warming**: Automatic metadata and token pre-warming when URL detected
- **Progressive Status Messages**: Phase-specific messages (Preparing → Downloading → Extracting → Transcribing → Finalizing)

## 🤝 **Contributing**

### **Development Setup**
1. Fork the repository
2. Create a feature branch
3. Implement changes with tests
4. Submit a pull request
5. Code review and approval

### **Code Standards**
- **Kotlin**: Modern language features
- **Compose**: Declarative UI patterns
- **Architecture**: MVVM with Repository pattern
- **Testing**: Comprehensive test coverage

## 📄 **License**

This project is licensed under the MIT License - see the [LICENSE](LICENSE) file for details.

## 🆘 **Support**

### **Documentation**
- **README**: This comprehensive guide
- **API Docs**: Business Engine integration
- **Test Reports**: Automated test results
- **Architecture**: System design documentation

### **Contact**
- **Issues**: GitHub issue tracker
- **Discussions**: Community forums
- **Email**: support@pluct.app
- **Documentation**: docs.pluct.app

---

## Recent Updates

### v2.10.1 - Request Identity + Update Gate Hardening (Current)

**UX/Reliability Improvements:**
1. Mobile now sends both `X-Request-ID` and canonical `X-Client-Request-Id` so retries resolve to the same backend job.
2. Mobile sends a distinct stable `X-Device-Id` instead of reusing the user id, improving abuse checks without extra user steps.
3. Android hard update now follows the simple production rule: block only when server minimum version code is greater than installed `VERSION_CODE`.
4. Client policy parsing now reads nested Android APK URLs from `platforms.android.apkUrl`.
5. Client policy parsing treats `features.transcriptionSubmit=false` as a submit disable signal.
6. Device identity logs no longer print the raw Android ID.

**Validation:**
- `:shared:allTests`, `:app:compileDebugKotlin`, and `:app:assembleDebug` passed.
- `npm run test:all` distinguishes connected-device, compile-only, missing-release-env, and first-failure modes without relying on privileged mobile secrets.

### v2.10.0 - Fulfillment Wallet, Policy Gate, Queue Trust

**UX/Reliability Improvements:**
1. The visible product path is now quote -> reserve -> status -> charged/refunded, while legacy vend-token remains compatibility plumbing.
2. Home/header balance copy can include waiting work, for example `Balance 3 · Waiting 2` or `2 waiting · Need balance`.
3. Wallet wording is consistent: balance, waiting, reserved, charged, not charged, and ready/no charge.
4. History rows reuse the existing row space for `@creator · Today 14:32` and `42s · Text · Done`.
5. Missing TikTok metadata falls back to `TikTok · saved time` instead of blank identity text.
6. Android hard update is driven only by server `minimumVersionCode > BuildConfig.VERSION_CODE`.
7. Cached/fresh client policy can protect submit paths without blocking saved transcripts during network failure.
8. Release builds no longer embed a local engine signing secret; Business Engine exposes a controlled mobile header-auth fallback gate.
9. The latest APK contract is visible through Business Engine policy and download redirects with versionCode, git SHA, and SHA256 fields.
10. The Node runner labels validation truthfully as `PASS_FULL_DEVICE`, `PASS_LOCAL_COMPILE_ONLY`, `SKIPPED_MISSING_RELEASE_ENV`, or `FAIL_FIRST_ERROR`.

**Edge Cases Covered:**
1. Outdated app policy blocks new submit only when the server minimum version exceeds the installed version code.
2. Policy fetch failure does not erase access to already saved transcripts.
3. APK delivery can be marked `SKIPPED_MISSING_RELEASE_ENV` locally when `MOBILE_APK_URL` is absent instead of being called a full pass.
4. Queued work remains visible when balance is zero.
5. Duplicate/retry paths keep using the same request/job identity instead of charging again.
6. Cached transcript reuse can settle as ready/no charge.
7. Mobile release auth avoids privileged secrets in the APK.

**Validation:**
- `npm run test:all` uses release-style mobile header auth when `BE_USER_JWT` and `ENGINE_JWT_SECRET` are absent; a real connected-device journey is labeled `PASS_FULL_DEVICE` only after journeys run.
- Android compile/build command: `.\gradlew.bat :shared:compileKotlinMetadata :app:compileDebugKotlin :app:assembleDebug`.

### v2.9.0 - Mobile/BE/TTT Linkage + Progress Trust

**UX/Reliability Improvements:**
1. Mobile now sends truthful `PluctMobile/<version> (android)` User-Agent and `X-Client-Version` headers.
2. App version is aligned from Gradle into `BuildConfig.VERSION_NAME` for debug and release.
3. Mobile fetches the Business Engine client policy and caches it for update/feature signaling.
4. Mobile blocks submit when server policy disables transcription, preventing wasted credits and worker load.
5. Mobile syncs a policy-safe device profile after server warmup so support/admin sees real app/device state.
6. Device profile sync skips unchanged payloads locally, reducing radio and Worker traffic.
7. Health checks now reuse fresh snapshots and back off longer while TTTranscribe is degraded.
8. Background auto-minimize is suppressed when notifications are disabled, keeping progress visible in-app.
9. Upgrade/credit recovery opens the policy-provided store/help URL instead of dead-end support copy.
10. Transcription notification code is split into focused modules under `app/pluct/notification` (public API unchanged: `PluctNotificationHelper`).
11. WorkManager foreground notification uses the same id as `KEY_NOTIFICATION_ID` so the shade card updates instead of spawning a duplicate entry.
12. Foreground CaptureCard flow consults `PluctCoreTranscription01Dedupe01Facade` before starting work when a background job already exists; heartbeat pacing is shared with the worker via `PluctCoreTranscription02ProgressHeartbeat01Policy`.

**Edge Cases Covered:**
1. Globally disabled app notifications no longer let background progress disappear silently.
2. TTTranscribe `error` health now maps to unhealthy on mobile instead of being ignored.
3. Server-side `disableTranscribeSubmit` can stop old clients before expensive vend/TTT work.
4. Stale profile payloads are not resent during normal startup.
5. Client policy fetch failure keeps the app usable with last-known behavior.

**Tech Debt Cleanups:**
1. `npm run test:all` latest-change ordering now includes health, API connectivity, TTTranscribe, and user identity linkage.
2. Deprecated PowerShell wrappers now point only to Node/ADB validation and no longer execute legacy flows.
3. README test SSOT now matches the actual `scripts/nodejs/Pluct-Main-01Orchestrator.js` package entrypoint.
4. Mobile health refresh coalescing removes duplicate `/health/services` callers from cold-start paths.

### v2.8.0 - Universal Notification Flow + Fast-Fail Validation

**UX/Reliability Improvements:**
1. Progress notifications now use universal arrow labels like `0% -> Text`, `Video -> Audio`, and `Audio -> Text`.
2. Foreground captures now post the same live Pluct notification as background captures, so users always see progress at the top.
3. Completion notifications now show `100% -> Text` with direct `Copy` and `TikTok` actions.
4. Error notifications now hide internal service wording and use short recovery cues such as `No internet -> Saved`.
5. The TikTok URL field now uses `TikTok link -> Text` and a compact wallet credit badge to reduce reading load.
6. Invalid TikTok text is blocked inline before any API, database, or worker cost is spent.
7. Manual capture progress has a heartbeat so the status does not freeze while Business Engine or TTTranscribe responds.
8. Background worker progress has the same heartbeat stages, keeping notification status believable during slower upstream calls.
9. QuickScan validation now recognizes the production completion notification when the user path returns to TikTok or launcher.

**Edge Cases Covered:**
1. Duplicate pasted TikTok fragments are rejected before processing.
2. Incomplete short links are rejected before queue/database creation.
3. TTTranscribe/service errors map to a saved-video retry message instead of a churn-inducing raw error.
4. Network loss keeps the video saved and surfaces a Wi-Fi recovery cue.
5. Multiple notification validation confirms one active Pluct notification and no duplicate processing start.

**Tech Debt Cleanups:**
1. `npm run test:all` ordering now starts with recently changed notification/UI journeys, then last failed paths, then high-priority service checks.
2. Notification journey checks were migrated to explicit Pluct/Puppeteer-style ADB evidence instead of stale broad log patterns.
3. Invalid URL validation now filters only real Pluct API-submit evidence and ignores unrelated Android scheduler logs.
4. QuickScan result detection shares the same production completion signal as the app notification path.

**Validation:**
- Built and installed `app-debug.apk` against `http://127.0.0.1:8789`.
- `npm run test:all -- Journey-UX-20NotificationConsolidation-Validation Journey-UX-13NotificationNavigation-Validation Journey-UX-12BackgroundProcessing-Validation Journey-UX-11AutoSubmitIntent-Validation Pluct-Test-Validation-10ErrorHandling Journey-UX-22VideoTitleFallback-Validation Journey-UX-05RedundantVisuals-Validation Journey-EdgeCase-04MultipleNotifications-Validation Journey-EdgeCase-03NetworkLoss-Validation Journey-QuickScan Journey-TTTranscribeIntegration Journey-APIConnectivity Journey-TokenVendingValidation`
- Playwright MCP validated `http://127.0.0.1:8789/admin/dashboard`: local health connected, users loaded, refresh worked, select-all/clear worked, and console warnings/errors were zero.
- Business Engine health confirmed `ttt: healthy` and circuit breaker `closed`.
- Hugging Face `hf` CLI was checked but is not installed on PATH in this Windows environment.

### v2.7.0 - Senior-Friendly Visual Flow + Notification Return Path

**UX/Reliability Improvements:**
1. Compact transcript cards now show thumbnail, status icon, author, duration, confidence, and preview in one scannable row.
2. Completed cards use `OK` and a copy icon instead of relying on long English labels.
3. Empty state is reduced to a single `Paste -> Text` value cue plus `Demo`.
4. Error recovery buttons now pair each action with a universal icon: wallet, Wi-Fi, clock, or retry.
5. Notification progress title now starts with the percentage, for example `42% Pluct`.
6. Progress notifications only alert once while updating, reducing fatigue.
7. Completion notifications use shorter `Done` copy and preserve the transcript preview.
8. Progress, completion, and error notifications include a `TikTok` action to return users to the original video.
9. Metadata handoff now preserves thumbnail, duration, author, and description when TTTranscribe/Business Engine returns them.

**Edge Cases Covered:**
1. Bad or missing thumbnail URLs fall back to the same status icon tile.
2. Progress is clamped to `0..99` until completion so notifications do not overpromise.
3. Completed transcript detection recognizes compact `OK` cards in Node/ADB journeys.
4. Existing videos keep previous thumbnail/description if new metadata is partial.
5. Notification `TikTok` action is skipped safely if the URL cannot be parsed.

**Validation:**
- Built and installed the debug APK after freeing generated cache space.
- `npm run test:all -- Journey-UX-05RedundantVisuals-Validation Journey-UX-22VideoTitleFallback-Validation Pluct-Test-Validation-10ErrorHandling Journey-QuickScan Journey-TTTranscribeIntegration Journey-APIConnectivity Journey-TokenVendingValidation`
- Playwright MCP validated the local Business Engine admin dashboard at `http://127.0.0.1:8789/admin/dashboard`: health connected, users loaded, refresh worked, and user details expanded.

### v2.6.0 - UX Improvements + Tech Debt Cleanup

**5 UX/Reliability Improvements:**
1. **Network Restoration Handler**: Wired `handleNetworkRestored` to queue processor - automatically processes queued videos when network is restored
2. **Low Balance Warning**: Added proactive warning when balance < 3 credits (threshold updated from <= 2 to < 3) with visual indicator in header
3. **Confidence Score Display**: Added confidence score percentage badge to transcript detail screen and video list items
4. **Cancel Notification Action**: Added cancel button to in-progress transcription notifications, allowing users to cancel background jobs
5. **Empty Transcript Error Message**: Enhanced error message with troubleshooting tips and retry suggestions when transcription completes but no text is found

**3 Tech Debt Cleanups:**
1. **Non-Null Assertions**: Fixed 17 unnecessary `!!` operators across codebase, replaced with safe calls and proper null handling
2. **Elvis Operators**: Reviewed 30 `?:` operators - all found to be necessary for proper fallback behavior
3. **Unused Parameter**: Removed unused `error` parameter from `ErrorClassifier.categorizeByMessage()` function

**Testing Updates:**
- Updated Node journey test URLs to include stable valid TikTok links for broader test coverage
- Fixed app launch issue with pre-launch fallback
- Fixed YAML syntax errors in test files
- Test runner now shows real-time output for better visibility

**Database Schema:**
- Added `confidence` field to `VideoItem` entity (database version 6)
- Migration added: `MIGRATION_5_6` adds confidence column

### v2.5.0 - Battery Optimization + 5xx Retry + First Sentence Preview
5 UX/Reliability Fixes:
1. **Shadowed Variable Fix (pollExistingJob)**: Renamed to `videoForCompletion`, `videoForFailure` in poll loop
2. **Battery Constraint**: Background WorkManager jobs deferred when battery is low (`setRequiresBatteryNotLow`)
3. **First Sentence Preview**: Notification collapsed text shows first sentence instead of truncated chars
4. **5xx Exponential Backoff**: Server errors trigger backoff retry (1s → 2s → 4s → ... 30s max)
5. **Migration Parameter Naming**: Fixed Lint warnings by renaming `database` to `db`

3 Tech Debt Cleanups:
1. **@Suppress handleNetworkRestored**: Reserved for future queue processor integration
2. **Safe Unwrap in Worker**: Replaced `!!` assertion with safe `?: run { return@repeat }`
3. **Removed Unused Import**: PluctNotificationHelper in NetworkMonitor

### v2.4.0 - Database Migration + Network Constraints + Foreground Detection
5 UX/Reliability Fixes:
1. **Database Migration v4→v5**: Added `transcriptCachedAt` column with proper Room migration
2. **Cache-Refresh Notification**: Shows "Refreshing transcript (cache expired)" for stale cache
3. **Network Constraint**: Background WorkManager jobs require network connectivity
4. **Centralized Notification ID Usage**: Status resumer uses `PluctNotificationHelper.generateNotificationId(url)`
5. **ProcessLifecycleOwner**: Foreground detection via `lifecycle-process` (replaces deprecated getRunningTasks)

3 Tech Debt Cleanups:
1. **@Suppress Unused Parameters**: `networkMonitor` in TranscriptionWorker properly suppressed
2. **Shadowed Variable Fix**: Renamed to `videoForJobUpdate`, `videoForFailUpdate` in worker
3. **Redundant Else Removal**: ProcessingTier when expression now exhaustive without else

### v2.3.0 - Pluct Logo Notification + Cache Management
5 UX/Reliability Fixes:
1. **Dedicated Notification Icon**: New ic_stat_pluct.xml monochrome vector (Pluct logo silhouette with plug connector)
2. **Progress % in Notification**: Shows "45% - Transcribing..." in notification text
3. **Word Count in Completion**: "Transcription Complete (127 words)" title
4. **Notification Channel Group**: Organized under "Transcription" in Settings
5. **24h Cache Invalidation**: Cached transcripts re-transcribed after 24 hours

3 Tech Debt Cleanups:
1. **isBackground Polling**: Background jobs now poll 50% slower (3s vs 2s initial)
2. **Centralized Notification ID**: `PluctNotificationHelper.generateNotificationId(url)`
3. **Deprecated API Removal**: Removed getRunningTasks usage (deprecated API 21)

### v2.2.0 - Notification Sound + Progress Visibility
5 UX/Reliability Fixes:
1. **Notification Sound+Vibration**: Completion notifications now play sound and vibrate (IMPORTANCE_HIGH channel with DEFAULT_ALL)
2. **Monochrome Icon**: Uses ic_launcher_foreground for Android 13+ status bar visibility
3. **Incremental Progress Logging**: PROGRESS[XX%] logs in logcat for debugging long waits
4. **Cached Transcript Instant Return**: Previously transcribed videos return immediately
5. **Toast Sound for Background**: Toast notifications play sound+vibrate when transcription completes

3 Tech Debt Cleanups:
1. Fixed ErrorDisplay enum references (INSUFFICIENT_CREDITS, NETWORK, RATE_LIMIT, AUTHENTICATION)
2. Fixed RetryHandler import for PluctCoreChecks01RetryabilityDecider
3. Fixed AdaptiveIntervalCalculator isBackground parameter order

### v2.1.0 - UX/International Improvements
- Dark/Light Mode: User-selectable theme (System/Light/Dark) in Settings
- International-Friendly UI: Minimal text, visual indicators, icons over emojis
- Improved Contrast: Better readability for disabled states
- Reduced Clutter: Consolidated duplicate messaging
- Code Consolidation: TikTok detection utility, deprecated file cleanup

### **v2.0.0 - Complete API Integration**
- ✅ **Real API Integration**: Business Engine and TTTranscribe connectivity
- ✅ **JWT Authentication**: Secure token-based authentication
- ✅ **Credit Management**: Live balance tracking with real API data
- ✅ **Modern UI**: Jetpack Compose with Material 3 design
- ✅ **Comprehensive Testing**: End-to-end test orchestration
- ✅ **Performance Optimization**: Build and runtime optimizations

### **Key Improvements**
- **Theme Toggle**: Settings > Appearance > Theme (System/Light/Dark)
- **Cleaner Empty State**: "Your transcripts appear here" + Try Demo button
- **Better Button Contrast**: 0.7 alpha for disabled text (was 0.38)
- **API Connectivity**: Real Business Engine integration
- **Transcription Pipeline**: Complete TTTranscribe workflow
- **Test Automation**: Node/ADB journey orchestration through `npm run test:all`

### App icons and Play listing

- **SSOT export**: `AppIcons/android/` (Android Studio asset output).
- **Sync into `app/src/main/res`**: `powershell -File scripts/sync-icons.ps1` from repo root.
- **Play Console**: `docs/store-listing/playstore-icon.png` for the high-res launcher slot.
- **ADB shell check**: `npm run test:shell-visual` (see `docs/UX-28-Shell-Visual-Validation.md`; requires a device line ending in `device`).
- **Themed / install UI (API 33+)**: Adaptive icons include `drawable/ic_launcher_monochrome.xml`; status bar uses `drawable/ic_stat_pluct.xml` (never `@mipmap/ic_launcher` for `setSmallIcon`).
- **Still seeing an old teal tile after sideload?** Uninstall `app.pluct` once, then reinstall — PackageManager caches launcher art. Re-export `mipmap-*/ic_launcher.png` from Android Studio if that file still shows legacy art (it is used on some OEM paths beside adaptive XML).

---

**🚀 Pluct Mobile App - Transforming TikTok videos into actionable insights with AI-powered transcription technology.**

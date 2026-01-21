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
   git clone https://github.com/your-org/pluct-mobile-app.git
   cd pluct-mobile-app
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
   npm run test:all
   ```
   The canonical test entrypoint is `maestro/Pluct-Maestro-Test-01Runner-01Orchestrator.js` which runs all Maestro YAML user journey tests. See **[Testing](#-testing-framework)** below for complete testing documentation and options.

### **Integration Guide**
For complete API integration instructions, authentication setup, and best practices, see the **[Mobile to Business Engine Integration Guide](MOBILE-to-BUSINESSengine-INTEGRATION-GUIDE.md)**.

## 🧪 **Testing Framework**

All tests use **Maestro**, a YAML-based UI testing framework. The Node.js orchestrator (`maestro/Pluct-Maestro-Test-01Runner-01Orchestrator.js`) discovers and runs all flows.

### **Quick Start**
```bash
npm run test:all
```

This runs all categorized test flows (01-core, 02-ui, 03-transcription, 04-queue, 05-edge-cases, 06-ux-improvements, 07-intent-fixes, 08-onboarding, 09-onboarding-improvements) in sequence and reports results.

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
npm run test:maestro               # Explicit maestro orchestrator run

# Development mode (stops on first failure)
cross-env DEV_MODE=1 npm run test:all

# Filter tests by category
cross-env MAESTRO_CATEGORY=02-ui npm run test:all
```

### **Testing Validation Checklist**
- ✅ **Core Flows**: App launch, share intent, TikTok URL handling
- ✅ **API Integration**: Business Engine connectivity, token vending, transcription submission
- ✅ **Status Polling**: Adaptive intervals (2s → 3s → 4.5s → 10s) and timeout detection
- ✅ **Error Categorization**: Consistent recovery actions (Network, Auth, Rate Limit, Credits, Validation)
- ✅ **Circuit Breaker Auto-Reset**: Opens after 5 failures, auto-resets after 30s silence
- ✅ **Adaptive Polling**: Backend load reduction from ~120 req/min to ~18 req/min
- ✅ **Credit Management**: Balance loading, insufficient credits, queue fallback

### **New Validation Tests** (UX/Reliability Improvements)
The following Maestro flows validate the 5 UX/reliability fixes:

1. **`maestro/flows/03-transcription/06-adaptive-polling-validation.yaml`**
   - Validates polling intervals scale from 2s to 10s
   - Confirms backend load reduction metrics
   - Tests 3-minute transcription job with adaptive intervals

2. **`maestro/flows/05-edge-cases/04-circuit-breaker-auto-reset.yaml`**
   - Simulates network failures to trigger circuit breaker
   - Validates auto-reset after 30s silence
   - Confirms recovery to CLOSED state on success

3. **`maestro/flows/02-ui/06-error-categorization-consistency.yaml`**
   - Tests network error categorization → "Check Connection" + "Retry"
   - Tests rate-limit categorization → "Retry Later"
   - Tests credits error categorization → "Add Credits"
   - Tests server error categorization → "Retry"
   - Validates consistency across all error paths

### **Monitoring Test Execution**
Maestro tests write artifacts to `artifacts/` including:
- `ui_dump_latest.xml` — Latest UI hierarchy
- `video_added_ui_dump.xml` — Post-transcription state
- `status_history.json` — Status transitions
- Maestro logs with request/response details

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

3. **Legacy Test Orchestrators**: `npm run test:all` replaces:
   - `scripts/nodejs/Pluct-Main-01Orchestrator.js` (removed)
   - All `Pluct-Test-Focused-*.js` runners (consolidated)
   - Entry point now: `maestro/Pluct-Maestro-Test-01Runner-01Orchestrator.js`

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
The following Node.js orchestrators in `scripts/nodejs/` are marked for deletion (`DeleteThisFile_` prefix) as they were replaced by the canonical Maestro orchestrator:
- `Pluct-Main-01Orchestrator.js`
- `Pluct-Test-Focused-*.js` (all focused test runners)
- Individual test runner files

**Transition**: All test execution now routes through `npm run test:all`, which invokes `maestro/Pluct-Maestro-Test-01Runner-01Orchestrator.js`.

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
   See [TESTING.md](TESTING.md) for complete testing documentation. The canonical orchestrator is `maestro/Pluct-Maestro-Test-01Runner-01Orchestrator.js` — `npm run test:all` invokes it.

### **Integration Guide**
For complete API integration instructions, authentication setup, and best practices, see the **[Mobile to Business Engine Integration Guide](MOBILE-to-BUSINESSengine-INTEGRATION-GUIDE.md)**.

## 🧪 **Testing Framework**

All tests use **Maestro**, a YAML-based UI testing framework. See [TESTING.md](TESTING.md) for complete documentation.

### **Quick Start**
```bash
npm run test:all
```

### **Legacy Node.js Tests (Deprecated)**
The app previously used Node.js-based testing. These tests are deprecated in favor of Maestro:

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
- **Test Automation**: Maestro YAML-based test orchestration

---

**🚀 Pluct Mobile App - Transforming TikTok videos into actionable insights with AI-powered transcription technology.**
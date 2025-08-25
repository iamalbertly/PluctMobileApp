# Pluct - Video-to-Data Pipeline for AI Builders

## Project Overview

Pluct is a professional video-to-data pipeline designed for AI builders, researchers, and prompt engineers. The app transforms any shared video link into AI-ready data outputs in under 20 seconds, eliminating the tedious multi-step manual process of finding content, transcribing it, and cleaning/reformatting text into usable structured assets.

### Target User
- **Primary**: AI builders, researchers, and prompt engineers
- **Pain Point**: High cost of manual data cleaning and critical importance of correctly formatted data
- **Use Cases**: Fine-tuning models, building RAG knowledge bases, programming autonomous agents
- **Value Proposition**: "Video to data, fast and clean"

### Core Functionality
- **Two-Tier Data Ingestion**: Default method via script.tokaudit.io + fallback to user API keys
- **Three Power Actions**: Format for Fine-Tuning, Generate Prompt, Chunk for Vectorization
- **Native Share Integration**: Direct video link processing from Android Share Sheet
- **Local Library**: Save and search processed transcripts
- **Professional Focus**: Source-agnostic pipeline (TikTok is one example, not the focus)

## Technology Stack

- **Language**: Kotlin (JVM target 17)
- **UI Framework**: Jetpack Compose with Material3
- **Architecture**: Single Activity with Compose Navigation
- **Dependency Injection**: Hilt
- **Local Storage**: Room + DataStore
- **Serialization**: Kotlinx Serialization
- **Min SDK**: 26 (Android 8.0) - covers older devices common in budget markets
- **Target/Compile SDK**: 34 (Android 14) - for policy currency
- **Permissions**: INTERNET only - strong privacy focus

## Build Instructions

### Prerequisites
1. **Android Studio Hedgehog (2023.1.1) or later**
   - Download from: https://developer.android.com/studio
   - Install with default settings

2. **Java Development Kit (JDK) 17**
   - Android Studio should include this automatically
   - Verify in Android Studio: File → Project Structure → SDK Location

3. **Android SDK**
   - API Level 34 (Android 14)
   - API Level 26 (Android 8.0)
   - Build Tools 34.0.0

### Build Steps

1. **Open Project**
   ```
   File → Open → Navigate to ClipForge folder → Select
   ```

2. **Sync Gradle**
   - Android Studio will automatically sync
   - Or manually: File → Sync Project with Gradle Files
   - Wait for all dependencies to download

3. **Build APK**
   ```
   Build → Build Bundle(s) / APK(s) → Build APK(s)
   ```
   
   **Alternative via Terminal:**
   ```bash
   # Navigate to project root
   cd ClipForge
   
   # Build debug APK
   ./gradlew assembleDebug
   ```

4. **Locate APK**
   ```
   app/build/outputs/apk/debug/app-debug.apk
   ```

### Testing

1. **Run on Emulator**
   - Tools → AVD Manager → Create Virtual Device
   - Select API 26 or higher device
   - Run → Run 'app'

2. **Run on Physical Device**
   - Enable Developer Options on device
   - Enable USB Debugging
   - Connect via USB
   - Run → Run 'app'

3. **ADB Sanity Checks**
   ```bash
   # Install APK
   adb install -r app/build/outputs/apk/debug/app-debug.apk
   
   # Verify package installation
   adb shell pm list packages | grep pluct
   
   # Test app launch
   adb shell monkey -p app.pluct -c android.intent.category.LAUNCHER 1
   
   # Test Share activity (if exists)
   adb shell cmd package query-intent-activities -a android.intent.action.SEND -t text/plain | grep pluct || true
   ```

## Automation Testing Framework

The project includes a comprehensive modular test framework designed for easy maintenance and extension.

### Test Structure
```
ClipForge/
├── master_test.ps1                # Main test runner
├── Master_test_common.ps1         # Common functions and utilities
├── Master_test_clipboard.ps1      # Clipboard functionality tests
├── Master_test_webview.ps1        # WebView automation tests
├── Master_test_transcript.ps1     # Transcript generation tests
├── Master_test_error.ps1          # Error handling tests
```

### Usage Examples
```powershell
# Run all tests with default settings
.\master_test.ps1

# Skip build and installation
.\master_test.ps1 -SkipBuild

# Run only clipboard tests
.\master_test.ps1 -ClipboardOnly

# Run only WebView tests
.\master_test.ps1 -WebViewOnly

# Run only transcript tests
.\master_test.ps1 -TranscriptOnly

# Run only error handling tests
.\master_test.ps1 -ErrorOnly

# Specify a custom test URL
.\master_test.ps1 -TestUrl "https://vm.tiktok.com/your-custom-url/"

# Set custom timeout
.\master_test.ps1 -TimeoutSeconds 300

# Run multiple reliability tests
.\master_test.ps1 -TestRuns 5
```

## Playwright Automation

The project includes Playwright tests for comprehensive web automation testing of the ScriptTokAudit.io integration.

### Key Features
- **Automatic modal closing** - Closes any popups or modals that might interfere
- **Smart URL handling** - Normalizes TikTok URLs for optimal processing
- **Robust input filling** - Reliably fills the URL input field
- **Intelligent result monitoring** - Waits for and detects transcript results
- **Comprehensive error handling** - Handles various error scenarios gracefully
- **Processing indicators** - Shows loading state during automation

### Running Playwright Tests
```bash
# Install Playwright
npm install -g playwright

# Run the complete workflow test
npx playwright test scripttokaudit_complete_workflow.spec.ts

# Run with UI mode for debugging
npx playwright test scripttokaudit_complete_workflow.spec.ts --ui

# Run with headed browser
npx playwright test scripttokaudit_complete_workflow.spec.ts --headed
```

## Project Architecture

### Design Decisions
1. **Single Activity**: Reduces memory footprint, faster startup, fewer lifecycle issues
2. **Compose Navigation**: Avoids XML overhead, type-safe routing
3. **Min SDK 26**: Covers Android 8.0+ devices common in budget markets
4. **INTERNET Only**: Strong privacy focus, minimal permissions
5. **Material3**: Modern design system with better performance

### File Structure
```
ClipForge/
├── app/
│   ├── src/main/
│   │   ├── java/app/pluct/
│   │   │   ├── PluctApplication.kt
│   │   │   ├── MainActivity.kt
│   │   │   ├── data/
│   │   │   │   ├── converter/
│   │   │   │   ├── dao/
│   │   │   │   ├── database/
│   │   │   │   ├── entity/
│   │   │   │   ├── repository/
│   │   │   │   └── service/
│   │   │   ├── share/
│   │   │   ├── ui/
│   │   │   │   ├── components/
│   │   │   │   ├── navigation/
│   │   │   │   ├── screens/
│   │   │   │   └── theme/
│   │   │   ├── viewmodel/
│   │   │   └── di/
│   │   ├── res/
│   │   └── AndroidManifest.xml
│   └── build.gradle.kts
├── build.gradle.kts
├── settings.gradle.kts
├── gradle.properties
├── playwright_tests/
└── scripts/
```

## Development Roadmap

### Phase 1: Core Infrastructure ✅
- Project scaffold and navigation
- Build verification and testing
- Basic UI components

### Phase 2: Data Ingestion Implementation ✅
- Video link sharing integration
- script.tokaudit.io integration (Standard Method)
- API key management in Settings (Reliable Method)
- Progress screen and error handling

### Phase 3: Power Actions Implementation 🔄
- Transcript display and review screen
- Format for Fine-Tuning action
- Generate Prompt action
- Chunk for Vectorization action
- Native Share Sheet integration

### Phase 4: Monetization & Library 🔄
- Trial system (3 free conversions)
- $25 Pilot Lifetime Deal implementation
- Local library with search functionality
- Usage tracking and analytics

### Phase 5: Polish & Launch 🔄
- Onboarding flow
- Privacy policy integration
- Professional branding and copy
- Performance optimization

## Success Metrics

- **Quantitative Goals**:
  - Median time-to-value: < 20 seconds
  - Pilot deal conversion rate: 8-15%
  - Week-four retention rate: > 35%
- **Ultimate Proof Point**: Users requesting API keys/webhooks for automation integration

## Troubleshooting

### Build Issues
1. **Gradle sync fails**: Check internet connection, invalidate caches (File → Invalidate Caches)
2. **SDK not found**: Install required SDK levels in SDK Manager
3. **JDK issues**: Verify JDK 17 is installed and configured

### Runtime Issues
1. **App crashes on startup**: Check logcat for specific error messages
2. **Navigation not working**: Verify Compose Navigation dependencies are included
3. **Theme not applying**: Check Material3 theme implementation

### Test Issues
1. **Test fails with "No device connected"**: Ensure an Android device is connected and visible to ADB
2. **Build failures**: Run `./gradlew clean` before building
3. **Timeout errors**: Increase the timeout value with `-TimeoutSeconds`
4. **Script errors**: Make sure all module files are in the same directory

## Build Status

- **Project Structure**: ✅ Complete
- **Basic Compilation**: ✅ Success
- **APK Generation**: ✅ Success (APK created)
- **Full Compilation**: ✅ Success (migrated from KAPT to KSP)
- **Device Testing**: ✅ **SUCCESSFUL** - App installed and running on connected device
- **App Launch**: ✅ **WORKING** - MainActivity launches correctly, app in focus
- **Package Info**: ✅ Version 1.0, Code 1, MinSDK 26, TargetSDK 34
- **Automated Testing**: ✅ All tests pass successfully

## License

This project is proprietary software. All rights reserved.

## Contributing

This is a private project. Please contact the maintainers for contribution guidelines.

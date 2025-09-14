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



## Troubleshooting

### Build Issues
1. **Gradle sync fails**: Check internet connection, invalidate caches (File → Invalidate Caches)
2. **SDK not found**: Install required SDK levels in SDK Manager
3. **JDK issues**: Verify JDK 17 is installed and configured

### Runtime Issues
1. **App crashes on startup**: Check logcat for specific error messages
2. **Navigation not working**: Verify Compose Navigation dependencies are included
3. **Theme not applying**: Check Material3 theme implementation


## License

This project is proprietary software. All rights reserved.

## Contributing

This is a private project. Please contact the maintainers for contribution guidelines.

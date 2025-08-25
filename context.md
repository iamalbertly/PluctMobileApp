# Pluct Android MVP - Project Context

## Project Overview
**Status**: MVP Implementation Complete  
**Last Updated**: 2025-08-23  
**Package**: app.pluct  
**Min SDK**: 26 (Android 8.0)  
**Target SDK**: 34 (Android 14)  

## Product Vision & Target User

### Core Purpose
Pluct is a video-to-data pipeline designed for professional use, specifically targeting AI builders, researchers, and prompt engineers. The app transforms any shared video link into AI-ready data outputs in under 20 seconds, eliminating the tedious multi-step manual process of finding content, transcribing it, and cleaning/reformatting text into usable structured assets.

### Target User Profile
- **Primary**: AI builders, researchers, and prompt engineers
- **Pain Point**: High cost of manual data cleaning and critical importance of correctly formatted data
- **Use Cases**: Fine-tuning models, building RAG knowledge bases, programming autonomous agents
- **Job to be Done**: Eliminate manual, error-prone process of cleaning and formatting video data

### Value Proposition
"Video to data, fast and clean" - Turn the entire chain of video processing into a single, near-instantaneous action.

## Core Functionality & Data Strategy

### Two-Tier Data Ingestion Strategy
1. **Standard Method (Default)**: Process through script.tokaudit.io public web service
   - Fast, zero-configuration, no-cost
   - Treated as best-effort, high-speed convenience
   - Acknowledged as unofficial dependency with potential fragility

2. **Reliable Method (Fallback)**: User-provided API key for professional transcription service
   - "Bring-your-own-key" model
   - User-controlled reliability and cost
   - Clear upgrade path when default method fails

### Three Power Actions (Core Value)
1. **Format for Fine-Tuning**: Produces perfectly structured JSONL pairs
2. **Generate Prompt**: Creates concise system prompt from content essence
3. **Chunk for Vectorization**: Key habit-forming feature for RAG knowledge bases

### User Experience Flow
1. User selects Pluct from Android Share Sheet
2. Progress screen during data ingestion
3. Single, clean result screen with full transcript
4. Power Actions prominently displayed below transcript
5. Standard utilities: Copy, Save to Library, Export
6. Native Share Sheet integration for "Last Mile Problem" solution

## Monetization Strategy

### Pilot Pricing Model
- **Free Trial**: 3 free conversions with full Power Actions access
- **Pilot Lifetime Deal**: $25 one-time payment for unlimited Power Actions
- **Strategic Intent**: $25 serves as "seriousness threshold" to filter casual users
- **Basic Transcription**: Remains available even without purchase

### Success Metrics
- **Quantitative Goals**:
  - Median time-to-value: < 20 seconds
  - Pilot deal conversion rate: 8-15%
  - Week-four retention rate: > 35%
- **Ultimate Proof Point**: Users requesting API keys/webhooks for automation integration

## Architecture Decisions

### Technology Stack
- **Language**: Kotlin (JVM 17) - Modern, concise, Android-first
- **UI Framework**: Jetpack Compose + Material3 - Declarative, performant, modern
- **Architecture**: Single Activity - Stability on low-end hardware
- **DI**: Hilt - Clean dependency injection, testable
- **Storage**: Room + DataStore - Local database and preferences
- **Serialization**: Kotlinx Serialization - Type-safe JSON handling

### Design Choices
1. **Single Activity**: Reduces memory footprint, faster startup, fewer lifecycle issues
2. **Compose Navigation**: Avoids XML overhead, type-safe routing
3. **Min SDK 26**: Covers Android 8.0+ devices common in budget markets
4. **INTERNET Only**: Strong privacy focus, minimal permissions
5. **Material3**: Modern design system with better performance

## Current Implementation

### Core Components
- ✅ `PluctApplication.kt` - Hilt setup with @HiltAndroidApp
- ✅ `MainActivity.kt` - Single activity with Compose setup
- ✅ Navigation system with four screens (Home, Ingest, Settings, Onboarding)
- ✅ `ShareIngestActivity.kt` - For handling Share intents from other apps
- ✅ `ScriptTokAuditWebView.kt` - WebView component for transcript extraction
- ✅ Material3 theme with light/dark support
- ✅ Room database with entities and DAOs
- ✅ ViewModels with Hilt integration
- ✅ Basic Hilt module structure

### Screens (Current Placeholder State)
1. **Home Screen**: Empty state with "Share a link from another app to start"
2. **Ingest Screen**: Processing screen for ingested video links
3. **Settings Screen**: Stub for future API key entry
4. **Onboarding Screen**: Placeholder for future implementation

### Dependencies
- **Compose BOM**: 2023.10.01
- **Navigation**: 2.7.5
- **Hilt**: 2.48
- **Room**: 2.6.1
- **DataStore**: 1.0.0
- **Kotlinx Serialization**: 1.6.0

## Build Configuration
- **Gradle**: 8.2
- **Android Gradle Plugin**: 8.2.0
- **Kotlin**: 1.9.10
- **Compose Compiler**: 1.5.3

## File Structure
```
Pluct/
├── app/
│   ├── src/main/
│   │   ├── java/app/pluct/
│   │   │   ├── PluctApplication.kt
│   │   │   ├── MainActivity.kt
│   │   │   ├── data/
│   │   │   │   ├── converter/
│   │   │   │   │   └── Converters.kt
│   │   │   │   ├── dao/
│   │   │   │   │   ├── OutputArtifactDao.kt
│   │   │   │   │   ├── TranscriptDao.kt
│   │   │   │   │   └── VideoItemDao.kt
│   │   │   │   ├── database/
│   │   │   │   │   └── PluctDatabase.kt
│   │   │   │   ├── entity/
│   │   │   │   │   ├── OutputArtifact.kt
│   │   │   │   │   ├── Transcript.kt
│   │   │   │   │   └── VideoItem.kt
│   │   │   │   ├── repository/
│   │   │   │   │   └── PluctRepository.kt
│   │   │   │   └── service/
│   │   │   │       └── VideoMetadataService.kt
│   │   │   ├── share/
│   │   │   │   └── ShareIngestActivity.kt
│   │   │   ├── ui/
│   │   │   │   ├── components/
│   │   │   │   │   └── ScriptTokAuditWebView.kt
│   │   │   │   ├── navigation/
│   │   │   │   │   ├── PluctNavigation.kt
│   │   │   │   │   └── Screen.kt
│   │   │   │   ├── screens/
│   │   │   │   │   ├── HomeScreen.kt
│   │   │   │   │   ├── IngestScreen.kt
│   │   │   │   │   ├── OnboardingScreen.kt
│   │   │   │   │   └── SettingsScreen.kt
│   │   │   │   └── theme/
│   │   │   │       ├── Color.kt
│   │   │   │       ├── Theme.kt
│   │   │   │       └── Type.kt
│   │   │   ├── viewmodel/
│   │   │   │   ├── HomeViewModel.kt
│   │   │   │   └── IngestViewModel.kt
│   │   │   └── di/
│   │   │       └── AppModule.kt
│   │   ├── res/
│   │   │   ├── drawable/
│   │   │   │   └── [drawable resources]
│   │   │   ├── mipmap-hdpi/
│   │   │   │   └── [app icons]
│   │   │   ├── values/
│   │   │   │   └── [value resources]
│   │   │   └── xml/
│   │   │       ├── backup_rules.xml
│   │   │       └── data_extraction_rules.xml
│   │   └── AndroidManifest.xml
│   ├── build.gradle.kts
│   └── src/test/
│       └── java/app/pluct/
│           └── PluctAppTest.kt
├── build.gradle.kts
├── settings.gradle.kts
├── gradle.properties
├── gradlew.bat
├── gradle/wrapper/
│   └── gradle-wrapper.properties
├── README_PILOT.md
├── context.md
├── automation_test_runner.ps1
└── playwright_tests/
    ├── README.md
    └── scripttokaudit_complete_workflow.spec.ts
```

## Development Roadmap

### Phase 1: Core Infrastructure
- ✅ Project scaffold and navigation
- ✅ Build verification and testing
- ✅ Basic UI components

### Phase 2: Data Ingestion Implementation (Current)
- ✅ Video link sharing integration
- ✅ script.tokaudit.io integration (Standard Method)
- ✅ API key management in Settings (Reliable Method)
- ✅ Progress screen and error handling

### Phase 3: Power Actions Implementation
- ✅ Transcript display and review screen
- 🔄 Format for Fine-Tuning action
- 🔄 Generate Prompt action
- 🔄 Chunk for Vectorization action
- ✅ Native Share Sheet integration

### Phase 4: Monetization & Library
- ✅ Trial system (3 free conversions)
- 🔄 $25 Pilot Lifetime Deal implementation
- ✅ Local library with search functionality
- 🔄 Usage tracking and analytics

### Phase 5: Polish & Launch
- ✅ Onboarding flow
- 🔄 Privacy policy integration
- ✅ Professional branding and copy
- ✅ Performance optimization

## Build Status
- **Project Structure**: ✅ Complete
- **Basic Compilation**: ✅ Success
- **APK Generation**: ✅ Success (APK created)
- **Full Compilation**: ✅ Success (migrated from KAPT to KSP)
- **Device Testing**: ✅ **SUCCESSFUL** - App installed and running on connected device
- **App Launch**: ✅ **WORKING** - MainActivity launches correctly, app in focus
- **Package Info**: ✅ Version 1.0, Code 1, MinSDK 26, TargetSDK 34
- **Automated Testing**: ✅ All tests pass successfully

### Build and Testing
- **Migration**: Successfully migrated from KAPT to KSP for better compatibility
- **APK Location**: `app/build/outputs/apk/debug/app-debug.apk`
- **Installation**: ✅ **SUCCESSFUL** - App installed on device via ADB
- **Core Functionality**: ✅ Compose UI, Navigation, Material3 theme all working
- **Automation**: ✅ Completed full automation workflow test with WebView integration
- **Performance**: ✅ WebView component optimized for better performance
- **Memory Usage**: ✅ Fixed potential memory leaks in WebView
- **Error Handling**: ✅ Enhanced error recovery in automation

## Notes
- All architectural decisions documented in code comments
- Project follows Material Design 3 guidelines
- Optimized for low-end Android devices
- Privacy-focused with minimal permissions
- Ready for Windows 11 development environment
- Professional positioning: "Video to data, fast and clean"
- Source-agnostic pipeline (TikTok, YouTube, Instagram, Twitter/X support)
- Comprehensive automation testing framework
- Single activity architecture with Compose Navigation
- WebView integration for script.tokaudit.io

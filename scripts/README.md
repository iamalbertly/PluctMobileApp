# Pluct Testing Framework

## Overview
Consolidated, intelligent testing framework for ClipForge with single source of truth and smart build detection.

## File Structure
```
scripts/
├── Pluct-Test-Orchestrator-Main.ps1    # Main entry point
├── Pluct-Test-Core-Utilities.ps1      # Common utilities
├── Pluct-Test-Core-Build.ps1          # Smart build management
├── Pluct-Test-Core-Device.ps1         # Device management
├── Pluct-Test-Core-Screenshots.ps1    # Screenshot capture
├── Pluct-Test-Journey-Intent.ps1      # Intent flow testing
├── Pluct-Test-Journey-Capture.ps1     # Capture sheet testing
├── Pluct-Test-Journey-Background.ps1  # Background processing testing
└── Pluct-Test-API-Services.ps1        # API connectivity testing
```

## Usage

### Run All Tests
```powershell
.\scripts\Pluct-Test-Orchestrator-Main.ps1
```

### Run Specific Test Scope
```powershell
.\scripts\Pluct-Test-Orchestrator-Main.ps1 -TestScope Journey
.\scripts\Pluct-Test-Orchestrator-Main.ps1 -TestScope Capture
.\scripts\Pluct-Test-Orchestrator-Main.ps1 -TestScope Background
.\scripts\Pluct-Test-Orchestrator-Main.ps1 -TestScope API
```

### With Screenshots
```powershell
.\scripts\Pluct-Test-Orchestrator-Main.ps1 -CaptureScreenshots
```

### Skip Build/Install
```powershell
.\scripts\Pluct-Test-Orchestrator-Main.ps1 -SkipBuild -SkipInstall
```

## Features

### Smart Build Detection
- Automatically detects when key Kotlin files have changed
- Only rebuilds when necessary
- Tracks file modification times vs last build

### Comprehensive Testing
- **Intent Journey**: Tests share intent flow and capture insight triggering
- **Capture Journey**: Tests capture sheet display and preliminary insights
- **Background Journey**: Tests background processing and worker execution
- **API Services**: Tests basic app functionality and connectivity

### Intelligent Logging
- Real-time log monitoring with pattern matching
- Automatic log capture and storage
- Screenshot capture on errors
- Comprehensive test reporting

### Single Source of Truth
- No duplicate testing logic
- Consistent naming conventions
- Modular, maintainable code structure
- Maximum 300 lines per file rule enforced

## Test Results
All core user journeys are working successfully:
- ✅ Intent Journey: PASS
- ✅ Capture Journey: PASS  
- ✅ Background Journey: PASS
- ✅ API Services: PASS
- ✅ Overall: PASS

The "Choice Engine" user journey is fully functional and ready for production use.

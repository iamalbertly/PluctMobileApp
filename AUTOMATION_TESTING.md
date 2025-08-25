# Pluct App Automation Testing

This document describes the comprehensive automation testing setup for the Pluct app, which tests the complete transcript generation workflow from TikTok share intent to transcript extraction.

## Overview

The automation testing framework provides:

- **Complete Workflow Testing** - Tests the entire journey from TikTok share to transcript
- **Performance Testing** - Measures speed and efficiency
- **Reliability Testing** - Multiple runs to ensure consistency
- **Error Handling Testing** - Tests various error scenarios
- **Device Testing** - Runs on real Android devices

## Test Architecture

### Test Components

1. **PluctAppAutomationTest.kt** - Main automation test suite
2. **TestConfig.kt** - Configuration and utilities
3. **run_automation_tests.ps1** - PowerShell test runner
4. **automation_test_runner.ps1** - Advanced test runner with detailed reporting

### Test Coverage

- ✅ Complete transcript workflow
- ✅ Multiple URL format handling
- ✅ Error scenario handling
- ✅ Performance benchmarking
- ✅ Reliability testing (multiple runs)
- ✅ WebView automation verification
- ✅ UI element detection and interaction

## Prerequisites

### Required Software

1. **Android SDK** with ADB tools
2. **Java 17** or higher
3. **PowerShell** (for Windows)
4. **Gradle** (included in project)

### Device Setup

1. **Enable Developer Options** on Android device
2. **Enable USB Debugging**
3. **Connect device** via USB
4. **Authorize** the computer for debugging

### Environment Variables

Ensure these are set in your environment:
```bash
ANDROID_HOME=/path/to/android/sdk
PATH=$PATH:$ANDROID_HOME/platform-tools
```

## Quick Start

### 1. Connect Device

```bash
# Check if device is connected
adb devices

# Should show something like:
# List of devices attached
# ABC123DEF456    device
```

### 2. Run Basic Test

```powershell
# Run the simplified test runner
.\run_automation_tests.ps1

# Or with specific parameters
.\run_automation_tests.ps1 -TestRuns 5 -Verbose
```

### 3. Run Advanced Test

```powershell
# Run the comprehensive test runner
.\automation_test_runner.ps1 -TestRuns 3 -Verbose
```

## Test Runner Options

### Basic Runner (`run_automation_tests.ps1`)

```powershell
.\run_automation_tests.ps1 [options]

Options:
  -DeviceId <string>    Specific device ID (if multiple devices)
  -BuildOnly            Only build the app, don't run tests
  -TestOnly             Only run tests, don't build
  -Verbose              Enable verbose logging
  -TestRuns <int>       Number of test runs (default: 3)
```

### Advanced Runner (`automation_test_runner.ps1`)

```powershell
.\automation_test_runner.ps1 [options]

Options:
  -DeviceId <string>    Specific device ID (if multiple devices)
  -BuildOnly            Only build the app, don't run tests
  -TestOnly             Only run tests, don't build
  -Verbose              Enable verbose logging
  -TestRuns <int>       Number of test runs (default: 3)
```

## Test Scenarios

### 1. Complete Workflow Test

**Purpose**: Tests the entire transcript generation journey

**Steps**:
1. Launch app with TikTok share intent
2. Process URL and open WebView
3. Automate ScriptTokAudit.io workflow
4. Extract transcript or handle "no transcript"
5. Return to app with result

**Expected Result**: Successfully complete the workflow within 60 seconds

### 2. Performance Test

**Purpose**: Measures speed and efficiency

**Metrics**:
- Total workflow time (target: <60 seconds)
- Page load time (target: <5 seconds)
- Automation time (target: <30 seconds)

**Expected Result**: All performance targets met

### 3. Reliability Test

**Purpose**: Ensures consistent performance across multiple runs

**Parameters**:
- Multiple test runs (default: 3)
- Success rate threshold (target: ≥80%)
- Performance consistency

**Expected Result**: ≥80% success rate across all runs

### 4. Error Handling Test

**Purpose**: Tests various error scenarios

**Test Cases**:
- Invalid URLs
- Malformed URLs
- Non-existent TikTok videos
- Network errors

**Expected Result**: Graceful error handling and user feedback

## Test Configuration

### TestConfig.kt Settings

```kotlin
// Test URLs
TestUrls.VALID_VM_TIKTOK = "https://vm.tiktok.com/ZMAF56hjK/"
TestUrls.VALID_FULL_TIKTOK = "https://www.tiktok.com/@chris219m/video/7539882214209686840"

// Timeouts
Timeouts.WEBVIEW_LOAD = 10000L // 10 seconds
Timeouts.AUTOMATION_COMPLETE = 30000L // 30 seconds
Timeouts.RESULT_DETECTION = 60000L // 60 seconds

// Performance thresholds
Performance.MAX_TOTAL_TIME = 60000L // 60 seconds
Performance.MIN_SUCCESS_RATE = 80.0 // 80%
```

## Running Tests Manually

### Using ADB

```bash
# Run specific test
adb shell am instrument -w -r -e class 'app.pluct.PluctAppAutomationTest#testCompleteTranscriptWorkflow' app.pluct.test/androidx.test.runner.AndroidJUnitRunner

# Run all tests
adb shell am instrument -w -r app.pluct.test/androidx.test.runner.AndroidJUnitRunner
```

### Using Gradle

```bash
# Run all Android tests
./gradlew connectedAndroidTest

# Run specific test class
./gradlew connectedAndroidTest -Pandroid.testInstrumentationRunnerArguments.class=app.pluct.PluctAppAutomationTest
```

## Test Results

### Success Criteria

- ✅ **Complete Workflow**: Successfully process TikTok URL and extract transcript
- ✅ **Performance**: Complete workflow within 60 seconds
- ✅ **Reliability**: ≥80% success rate across multiple runs
- ✅ **Error Handling**: Graceful handling of all error scenarios

### Sample Output

```
[2025-01-23 10:30:15] [INFO] Starting Pluct App Automation Test Runner
[2025-01-23 10:30:15] [INFO] Found 1 connected device(s)
[2025-01-23 10:30:15] [INFO] Device: ABC123DEF456    device
[2025-01-23 10:30:20] [INFO] Build completed successfully
[2025-01-23 10:30:25] [INFO] App installation completed
[2025-01-23 10:30:30] [INFO] Running test: app.pluct.PluctAppAutomationTest#testCompleteTranscriptWorkflow
[2025-01-23 10:31:00] [INFO] ✅ Test PASSED: app.pluct.PluctAppAutomationTest#testCompleteTranscriptWorkflow
[2025-01-23 10:31:05] [INFO] === TEST RESULTS SUMMARY ===
[2025-01-23 10:31:05] [INFO] Total tests: 3
[2025-01-23 10:31:05] [INFO] Passed: 3
[2025-01-23 10:31:05] [INFO] Failed: 0
[2025-01-23 10:31:05] [INFO] Success rate: 100.00%
[2025-01-23 10:31:05] [INFO] ✅ Overall test result: PASSED
```

## Troubleshooting

### Common Issues

#### 1. Device Not Detected

```bash
# Check ADB connection
adb devices

# Restart ADB server
adb kill-server
adb start-server

# Check USB debugging is enabled
```

#### 2. Build Failures

```bash
# Clean and rebuild
./gradlew clean
./gradlew assembleDebug
./gradlew assembleAndroidTest

# Check Java version
java -version
```

#### 3. Test Failures

**WebView Not Opening**:
- Check internet connection
- Verify ScriptTokAudit.io is accessible
- Check WebView permissions

**Automation Timeout**:
- Increase timeout values in TestConfig.kt
- Check device performance
- Verify network speed

**Element Not Found**:
- Check UI selectors in TestConfig.kt
- Verify app UI hasn't changed
- Check device screen resolution

### Debug Mode

Enable verbose logging:
```powershell
.\run_automation_tests.ps1 -Verbose
```

### Manual Testing

For manual verification:
1. Install the app on device
2. Share a TikTok URL to the app
3. Observe the automation process
4. Verify transcript extraction

## Performance Optimization

### Test Optimization

1. **Parallel Testing**: Run multiple test instances
2. **Device Pool**: Use multiple devices for faster testing
3. **Test Selection**: Run only critical tests for quick feedback

### App Optimization

1. **WebView Performance**: Optimize JavaScript injection
2. **Network Handling**: Implement retry mechanisms
3. **UI Responsiveness**: Ensure smooth transitions

## Continuous Integration

### GitHub Actions

```yaml
name: Automation Tests
on: [push, pull_request]
jobs:
  test:
    runs-on: ubuntu-latest
    steps:
      - uses: actions/checkout@v2
      - name: Set up JDK 17
        uses: actions/setup-java@v2
        with:
          java-version: '17'
      - name: Run automation tests
        run: |
          ./run_automation_tests.ps1 -TestRuns 1
```

### Local CI

```bash
# Run tests before commit
./run_automation_tests.ps1 -TestRuns 1

# Run full test suite
./automation_test_runner.ps1 -TestRuns 3
```

## Future Enhancements

### Planned Features

1. **Visual Testing**: Screenshot comparison
2. **Performance Profiling**: Detailed timing analysis
3. **Cross-Device Testing**: Multiple device support
4. **Cloud Testing**: Firebase Test Lab integration
5. **Test Reporting**: HTML reports with screenshots

### Technical Debt

1. **Test Data Management**: Centralized test data
2. **Mock Services**: Offline testing capabilities
3. **Test Parallelization**: Concurrent test execution
4. **Test Maintenance**: Automated test updates

## Support

For issues with automation testing:

1. Check the troubleshooting section
2. Review test logs for specific errors
3. Verify device and environment setup
4. Test manually to isolate issues

## Conclusion

The automation testing framework provides comprehensive coverage of the Pluct app's transcript generation workflow. It ensures:

- **Reliability**: Consistent performance across multiple runs
- **Performance**: Fast and efficient transcript generation
- **Quality**: Proper error handling and user feedback
- **Maintainability**: Easy to update and extend

The framework is designed to catch issues early and ensure the app provides a smooth, fast, and reliable transcript generation experience.

# Pluct Enhanced Test Orchestrator - Main Entry Point
# Single source of truth for all testing operations
# Handles build detection, test execution, and result aggregation with enhanced error reporting

param(
    [Parameter(Position=0)]
    [string]$TestUrl = "https://www.tiktok.com/@garyvee/video/7308801293029248299",

    [Parameter()]
    [switch]$SkipBuild,

    [Parameter()]
    [switch]$SkipInstall,

    [Parameter()]
    [switch]$CaptureScreenshots,

    [Parameter()]
    [string]$TestScope = "All"  # All, Journey, Capture, Background, API, Enhancements
)

# Import core modules
$script:FrameworkRoot = $PSScriptRoot
. "$script:FrameworkRoot\Pluct-Test-Core-Utilities.ps1"
. "$script:FrameworkRoot\Pluct-Test-Core-Build.ps1"
. "$script:FrameworkRoot\Pluct-Test-Core-Device.ps1"
. "$script:FrameworkRoot\Pluct-Test-Core-Screenshots.ps1"
. "$script:FrameworkRoot\Pluct-Test-Core-Unified.ps1"

# Initialize test session
$script:TestSession = @{
    StartTime = Get-Date
    TestResults = @{}
    BuildRequired = $false
    Screenshots = @()
    Logs = @()
    TestUrl = $TestUrl
    CriticalErrors = @()
    FailureDetails = @()
}

function Start-TestOrchestrator {
    Write-Log "=== Pluct Enhanced Test Orchestrator ===" "Cyan"
    Write-Log "Test Scope: $TestScope" "White"
    Write-Log "Test URL: $TestUrl" "White"

    # Check prerequisites
    if (-not (Test-AndroidDevice)) {
        Write-Log "❌ CRITICAL ERROR: No Android device connected" "Red"
        Write-Log "TERMINATING: Cannot proceed without device" "Red"
        exit 1
    }

    # Determine if build is needed
    if (-not $SkipBuild) {
        $script:TestSession.BuildRequired = Test-BuildRequired
        if ($script:TestSession.BuildRequired) {
            Write-Log "Code changes detected - build required" "Yellow"
            if (-not (Build-App)) {
                Write-Log "❌ CRITICAL ERROR: Enhanced build failed" "Red"
                Write-Log "TERMINATING: Build failure prevents testing" "Red"
                Show-BuildFailureDetails
                exit 1
            }
        } else {
            Write-Log "No code changes - skipping build" "Green"
        }
    }

    # Deploy to device if needed
    if (-not $SkipInstall) {
        $deploymentNeeded = Test-DeploymentNeeded
        if ($deploymentNeeded -or $script:TestSession.BuildRequired) {
            Write-Log "Deploying latest build to device..." "Yellow"
            $deploySuccess = Deploy-ToDevice
            if (-not $deploySuccess) {
                Write-Log "❌ CRITICAL ERROR: Deployment failed" "Red"
                Write-Log "TERMINATING: Deployment failure prevents testing" "Red"
                Show-DeploymentFailureDetails
                exit 1
            }
            Write-Log "Deployment successful" "Green"
        } else {
            Write-Log "Latest build already deployed" "Green"
        }
    }

    # Execute tests based on scope
    $overallSuccess = $true

    switch ($TestScope.ToLower()) {
        "all" {
            $intentResult = Test-IntentJourney
            $captureResult = Test-CaptureJourney
            $completeResult = Test-CompleteJourney
            $enhancementsResult = Test-EnhancementsJourney
            $overallSuccess = [bool]($intentResult -and $captureResult -and $completeResult -and $enhancementsResult)
        }
        "journey" {
            $intentResult = Test-IntentJourney
            $captureResult = Test-CaptureJourney
            $overallSuccess = [bool]($intentResult -and $captureResult)
        }
        "capture" {
            $overallSuccess = [bool](Test-CaptureJourney)
        }
        "complete" {
            $overallSuccess = [bool](Test-CompleteJourney)
        }
        "enhancements" {
            $overallSuccess = [bool](Test-EnhancementsJourney)
        }
        "api" {
            $overallSuccess = [bool](Test-IntentJourney) # API tests can be part of intent journey or separate
        }
        default {
            Write-Log "❌ CRITICAL ERROR: Invalid TestScope specified: $TestScope" "Red"
            Write-Log "TERMINATING: Invalid test scope" "Red"
            exit 1
        }
    }

    # Generate final report
    Show-TestReport -OverallSuccess $overallSuccess

    if ($overallSuccess) {
        Write-Log "✅ All tests passed successfully" "Green"
        exit 0
    } else {
        Write-Log "❌ Some tests failed" "Red"
        Show-FailureDetails
        exit 1
    }
}

function Test-IntentJourney {
    Write-Log "=== Testing Intent Journey ===" "Cyan"
    
    try {
        # Test app launch
        Write-Log "Testing app launch..." "Yellow"
        adb shell am start -n "$script:AppPackage/$script:MainActivity"
        Start-Sleep -Seconds 3
        
        # Check if app is running
        $appRunning = adb shell ps | Select-String $script:AppPackage
        if (-not $appRunning) {
            Write-Log "❌ CRITICAL ERROR: App failed to launch" "Red"
            Write-Log "TERMINATING: App launch failure" "Red"
            Show-AppLaunchFailureDetails
            return $false
        }
        
        Write-Log "✅ App launched successfully" "Green"
        
        # Test intent handling
        Write-Log "Testing intent handling..." "Yellow"
        adb shell am start -a android.intent.action.SEND -t "text/plain" --es android.intent.extra.TEXT $TestUrl -n "$script:AppPackage/$script:ShareActivity"
        Start-Sleep -Seconds 2
        
        # Check if share activity is running
        $shareActivityRunning = adb shell ps | Select-String "ShareIngestActivity"
        if (-not $shareActivityRunning) {
            Write-Log "❌ CRITICAL ERROR: Share activity failed to launch" "Red"
            Write-Log "TERMINATING: Share activity launch failure" "Red"
            Show-ShareActivityFailureDetails
            return $false
        }
        
        Write-Log "✅ Intent handling successful" "Green"
        return $true
        
    } catch {
        Write-Log "❌ CRITICAL ERROR: Intent journey failed with exception: $($_.Exception.Message)" "Red"
        Write-Log "TERMINATING: Intent journey failure" "Red"
        return $false
    }
}

function Test-CaptureJourney {
    Write-Log "=== Testing Capture Journey ===" "Cyan"
    
    try {
        # Test URL processing
        Write-Log "Testing URL processing..." "Yellow"
        adb shell input text $TestUrl
        Start-Sleep -Seconds 1
        adb shell input keyevent 66  # Enter key
        
        # Wait for processing
        Start-Sleep -Seconds 5
        
        # Check for processing logs
        $processingLogs = adb logcat -d | Select-String "Processing|Transcription|Metadata"
        if (-not $processingLogs) {
            Write-Log "❌ CRITICAL ERROR: No processing logs found" "Red"
            Write-Log "TERMINATING: Processing failure" "Red"
            Show-ProcessingFailureDetails
            return $false
        }
        
        Write-Log "✅ URL processing successful" "Green"
        return $true
        
    } catch {
        Write-Log "❌ CRITICAL ERROR: Capture journey failed with exception: $($_.Exception.Message)" "Red"
        Write-Log "TERMINATING: Capture journey failure" "Red"
        return $false
    }
}

function Test-CompleteJourney {
    Write-Log "=== Testing Complete Journey ===" "Cyan"
    
    try {
        # Test full workflow
        Write-Log "Testing complete workflow..." "Yellow"
        
        # Simulate user interactions
        adb shell input tap 500 800  # Tap on process button
        Start-Sleep -Seconds 2
        
        # Wait for completion
        $timeout = 60
        $elapsed = 0
        while ($elapsed -lt $timeout) {
            $completionLogs = adb logcat -d | Select-String "Success|Complete|Finished"
            if ($completionLogs) {
                Write-Log "✅ Complete journey successful" "Green"
                return $true
            }
            Start-Sleep -Seconds 2
            $elapsed += 2
        }
        
        Write-Log "❌ CRITICAL ERROR: Complete journey timed out" "Red"
        Write-Log "TERMINATING: Complete journey timeout" "Red"
        Show-TimeoutFailureDetails
        return $false
        
    } catch {
        Write-Log "❌ CRITICAL ERROR: Complete journey failed with exception: $($_.Exception.Message)" "Red"
        Write-Log "TERMINATING: Complete journey failure" "Red"
        return $false
    }
}

function Test-EnhancementsJourney {
    Write-Log "=== Testing Enhancements Journey ===" "Cyan"
    
    try {
        # Test all 6 enhancements
        $enhancements = @(
            "AI-Powered Video Metadata Analysis",
            "Intelligent Transcript Processing", 
            "Smart Caching & Offline Capabilities",
            "Advanced Search & AI Recommendations",
            "Real-Time Collaboration Features",
            "Analytics Dashboard & Performance Insights"
        )
        
        foreach ($enhancement in $enhancements) {
            Write-Log "Testing: $enhancement" "Yellow"
            
            # Simulate enhancement testing
            adb shell input tap 300 600  # Tap on enhancement button
            Start-Sleep -Seconds 2
            
            # Check for enhancement logs
            $enhancementLogs = adb logcat -d | Select-String $enhancement.Split(' ')[0]
            if (-not $enhancementLogs) {
                Write-Log "❌ CRITICAL ERROR: Enhancement '$enhancement' failed" "Red"
                Write-Log "TERMINATING: Enhancement failure" "Red"
                Show-EnhancementFailureDetails -Enhancement $enhancement
                return $false
            }
        }
        
        Write-Log "✅ All enhancements successful" "Green"
        return $true
        
    } catch {
        Write-Log "❌ CRITICAL ERROR: Enhancements journey failed with exception: $($_.Exception.Message)" "Red"
        Write-Log "TERMINATING: Enhancements journey failure" "Red"
        return $false
    }
}

function Show-BuildFailureDetails {
    Write-Log "=== BUILD FAILURE DETAILS ===" "Red"
    Write-Log "Build failed - possible causes:" "Yellow"
    Write-Log "1. Compilation errors in Kotlin/Java code" "White"
    Write-Log "2. Missing dependencies in build.gradle" "White"
    Write-Log "3. Insufficient disk space" "White"
    Write-Log "4. Gradle daemon issues" "White"
    Write-Log "5. Android SDK/NDK issues" "White"
    Write-Log "Check build logs above for specific errors" "Yellow"
}

function Show-DeploymentFailureDetails {
    Write-Log "=== DEPLOYMENT FAILURE DETAILS ===" "Red"
    Write-Log "Deployment failed - possible causes:" "Yellow"
    Write-Log "1. Device not connected or unauthorized" "White"
    Write-Log "2. APK not found or corrupted" "White"
    Write-Log "3. Insufficient device storage" "White"
    Write-Log "4. Device compatibility issues" "White"
    Write-Log "5. ADB connection problems" "White"
    Write-Log "Check device connection and try again" "Yellow"
}

function Show-AppLaunchFailureDetails {
    Write-Log "=== APP LAUNCH FAILURE DETAILS ===" "Red"
    Write-Log "App launch failed - possible causes:" "Yellow"
    Write-Log "1. App not installed on device" "White"
    Write-Log "2. MainActivity not found or misconfigured" "White"
    Write-Log "3. App crashes on startup" "White"
    Write-Log "4. Device compatibility issues" "White"
    Write-Log "5. Insufficient device resources" "White"
    Write-Log "Check app installation and device logs" "Yellow"
}

function Show-ShareActivityFailureDetails {
    Write-Log "=== SHARE ACTIVITY FAILURE DETAILS ===" "Red"
    Write-Log "Share activity failed - possible causes:" "Yellow"
    Write-Log "1. ShareIngestActivity not found" "White"
    Write-Log "2. Intent handling not configured" "White"
    Write-Log "3. Activity crashes on launch" "White"
    Write-Log "4. Missing permissions" "White"
    Write-Log "5. URL processing errors" "White"
    Write-Log "Check activity configuration and intent handling" "Yellow"
}

function Show-ProcessingFailureDetails {
    Write-Log "=== PROCESSING FAILURE DETAILS ===" "Red"
    Write-Log "Processing failed - possible causes:" "Yellow"
    Write-Log "1. URL validation errors" "White"
    Write-Log "2. Network connectivity issues" "White"
    Write-Log "3. Transcription service unavailable" "White"
    Write-Log "4. API rate limiting" "White"
    Write-Log "5. Service configuration errors" "White"
    Write-Log "Check network connection and service status" "Yellow"
}

function Show-TimeoutFailureDetails {
    Write-Log "=== TIMEOUT FAILURE DETAILS ===" "Red"
    Write-Log "Operation timed out - possible causes:" "Yellow"
    Write-Log "1. Slow network connection" "White"
    Write-Log "2. Heavy server load" "White"
    Write-Log "3. Large file processing" "White"
    Write-Log "4. Service unavailability" "White"
    Write-Log "5. Device performance issues" "White"
    Write-Log "Check network speed and service status" "Yellow"
}

function Show-EnhancementFailureDetails {
    param([string]$Enhancement)
    Write-Log "=== ENHANCEMENT FAILURE DETAILS ===" "Red"
    Write-Log "Enhancement '$Enhancement' failed - possible causes:" "Yellow"
    Write-Log "1. Service not properly configured" "White"
    Write-Log "2. Missing dependencies" "White"
    Write-Log "3. API service unavailable" "White"
    Write-Log "4. Configuration errors" "White"
    Write-Log "5. Resource constraints" "White"
    Write-Log "Check service configuration and dependencies" "Yellow"
}

function Show-FailureDetails {
    Write-Log "=== FAILURE SUMMARY ===" "Red"
    Write-Log "Test failures detected:" "Yellow"
    Write-Log "1. Check logs above for specific error details" "White"
    Write-Log "2. Verify device connection and app installation" "White"
    Write-Log "3. Check network connectivity" "White"
    Write-Log "4. Verify service configurations" "White"
    Write-Log "5. Review build and deployment logs" "White"
    Write-Log "Fix issues and re-run tests" "Yellow"
}

function Show-TestReport {
    param([bool]$OverallSuccess)
    
    $duration = (Get-Date) - $script:TestSession.StartTime
    Write-Log "=== TEST REPORT ===" "Cyan"
    Write-Log "Duration: $($duration.TotalMinutes.ToString('F2')) minutes" "White"
    Write-Log "Overall Success: $OverallSuccess" $(if ($OverallSuccess) { "Green" } else { "Red" })
    Write-Log "Test URL: $TestUrl" "White"
    Write-Log "Test Scope: $TestScope" "White"
    
    if (-not $OverallSuccess) {
        Write-Log "❌ Some tests failed - check details above" "Red"
    } else {
        Write-Log "✅ All tests passed successfully" "Green"
    }
}

# Main execution
Start-TestOrchestrator
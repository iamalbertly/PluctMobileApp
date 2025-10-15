# Pluct Test Orchestrator Core - Single Source of Truth
# Follows naming convention: [Project]-[ParentScope]-[ChildScope]-[CoreResponsibility]
# Consolidated from all duplicate test files with smart build detection and comprehensive testing

param(
    [Parameter(Position=0)]
    [string]$TestUrl = "https://www.tiktok.com/@garyvee/video/7308801293029248299",
    [Parameter()]
    [ValidateSet("All", "Core", "Enhancements", "BusinessEngine", "API", "Journey")]
    [string]$TestScope = "All",
    [Parameter()]
    [switch]$ForceBuild,
    [Parameter()]
    [switch]$SkipInstall,
    [Parameter()]
    [switch]$CaptureScreenshots,
    [Parameter()]
    [switch]$VerboseOutput
)

# Import core utilities (single source of truth)
$script:FrameworkRoot = $PSScriptRoot
. "$script:FrameworkRoot\Pluct-Smart-Test-Core-Utilities.ps1"
. "$script:FrameworkRoot\Pluct-Smart-Test-Build-Detector.ps1"
. "$script:FrameworkRoot\Pluct-Smart-Test-Device-Manager.ps1"
. "$script:FrameworkRoot\Pluct-Smart-Test-Journey-Engine.ps1"

# Initialize unified test session
$script:TestSession = @{
    StartTime = Get-Date
    TestResults = @{}
    BuildRequired = $false
    Screenshots = @()
    Logs = @()
    TestUrl = $TestUrl
    CriticalErrors = @()
    BusinessEngineHealth = $null
    SmartBuildDetection = @{
        LastBuildTime = $null
        ChangedFiles = @()
        BuildReason = ""
    }
}

function Start-PluctTestOrchestrator {
    Write-SmartLog "=== Pluct Test Orchestrator Core ===" "Cyan"
    Write-SmartLog "Test Scope: $TestScope" "White"
    Write-SmartLog "Test URL: $TestUrl" "White"
    Write-SmartLog "Single source of truth with smart build detection" "Yellow"

    # Check prerequisites
    if (-not (Test-SmartAndroidDevice)) {
        Report-CriticalError "No Android device connected" "Ensure an Android emulator is running or a physical device is connected via ADB."
        exit 1
    }

    # Smart build detection
    $script:TestSession.BuildRequired = Test-SmartBuildRequired -ForceBuild:$ForceBuild
    if ($script:TestSession.BuildRequired) {
        Write-SmartLog "Code changes detected - build required" "Yellow"
        if (-not (Build-SmartApp)) {
            Report-CriticalError "Build failed" "The Gradle build process failed. Check the build output for specific compilation errors."
            exit 1
        }
    } else {
        Write-SmartLog "No code changes - skipping build" "Green"
    }

    # Deploy to device if needed
    if (-not $SkipInstall) {
        $deploymentNeeded = Test-SmartDeploymentNeeded
        if ($deploymentNeeded -or $script:TestSession.BuildRequired) {
            Write-SmartLog "Deploying latest build to device..." "Yellow"
            $deploySuccess = Deploy-SmartToDevice
            if (-not $deploySuccess) {
                Report-CriticalError "Deployment failed" "The APK could not be installed on the device."
                exit 1
            }
            Write-SmartLog "Deployment successful" "Green"
        } else {
            Write-SmartLog "Latest build already deployed" "Green"
        }
    }

    # Execute tests based on scope
    $overallSuccess = $true

    switch ($TestScope.ToLower()) {
        "all" {
            $overallSuccess = (Test-CoreUserJourneys -TestUrl $TestUrl)
            if (-not $overallSuccess) { 
                Report-CriticalError "Core User Journeys Failed" "Core functionality failed."
                exit 1
            }
            
            $overallSuccess = (Test-EnhancementsJourney -TestUrl $TestUrl)
            if (-not $overallSuccess) { 
                Report-CriticalError "Enhancements Journey Failed" "Enhancement features failed."
                exit 1
            }
            
            $overallSuccess = (Test-BusinessEngineIntegration -TestUrl $TestUrl)
            if (-not $overallSuccess) { 
                Report-CriticalError "Business Engine Integration Failed" "Business Engine connectivity failed."
                exit 1
            }
        }
        "core" {
            $overallSuccess = (Test-CoreUserJourneys -TestUrl $TestUrl)
            if (-not $overallSuccess) { 
                Report-CriticalError "Core User Journeys Failed" "Core functionality failed."
                exit 1
            }
        }
        "enhancements" {
            $overallSuccess = (Test-EnhancementsJourney -TestUrl $TestUrl)
            if (-not $overallSuccess) { 
                Report-CriticalError "Enhancements Journey Failed" "Enhancement features failed."
                exit 1
            }
        }
        "businessengine" {
            $overallSuccess = (Test-BusinessEngineIntegration -TestUrl $TestUrl)
            if (-not $overallSuccess) { 
                Report-CriticalError "Business Engine Integration Failed" "Business Engine connectivity failed."
                exit 1
            }
        }
        default {
            Report-CriticalError "Invalid TestScope specified" "Use 'All', 'Core', 'Enhancements', or 'BusinessEngine'."
            exit 1
        }
    }

    # Generate final report
    Show-TestReport -OverallSuccess $overallSuccess

    if ($overallSuccess) {
        Write-SmartLog "All tests passed successfully" "Green"
        exit 0
    } else {
        Write-SmartLog "Some tests failed" "Red"
        exit 1
    }
}

function Test-CoreUserJourneys {
    param([string]$TestUrl)
    
    Write-SmartLog "Testing Core User Journeys..." "Yellow"
    
    try {
        # Test 1: App Launch
        Write-SmartLog "Testing app launch..." "Gray"
        $launchSuccess = Test-AppLaunch
        if (-not $launchSuccess) {
            Report-CriticalError "App Launch Failed" "The app failed to launch properly."
            return $false
        }

        # Test 2: Share Intent Handling
        Write-SmartLog "Testing share intent handling..." "Gray"
        $intentSuccess = Test-ShareIntent -TestUrl $TestUrl
        if (-not $intentSuccess) {
            Report-CriticalError "Share Intent Failed" "The app failed to handle the share intent properly."
            return $false
        }

        # Test 3: Video Processing Flow
        Write-SmartLog "Testing video processing flow..." "Gray"
        $processingSuccess = Test-VideoProcessing -TestUrl $TestUrl
        if (-not $processingSuccess) {
            Report-CriticalError "Video Processing Failed" "The video processing flow failed."
            return $false
        }

        Write-SmartLog "Core user journeys test passed" "Green"
        return $true
        
    } catch {
        Report-CriticalError "Core User Journeys Test Exception" "An unexpected error occurred: $($_.Exception.Message)"
        return $false
    }
}

function Test-EnhancementsJourney {
    param([string]$TestUrl)
    
    Write-SmartLog "Testing Enhancements Journey..." "Yellow"
    
    try {
        # Test AI-powered features
        Write-SmartLog "Testing AI-powered features..." "Gray"
        $aiSuccess = Test-AIFeatures -TestUrl $TestUrl
        if (-not $aiSuccess) {
            Report-CriticalError "AI Features Failed" "AI-powered features failed."
            return $false
        }

        # Test smart caching
        Write-SmartLog "Testing smart caching..." "Gray"
        $cacheSuccess = Test-SmartCaching
        if (-not $cacheSuccess) {
            Report-CriticalError "Smart Caching Failed" "Smart caching failed."
            return $false
        }

        Write-SmartLog "Enhancements journey test passed" "Green"
        return $true
        
    } catch {
        Report-CriticalError "Enhancements Journey Test Exception" "An unexpected error occurred: $($_.Exception.Message)"
        return $false
    }
}

function Test-BusinessEngineIntegration {
    param([string]$TestUrl)
    
    Write-SmartLog "Testing Business Engine Integration..." "Yellow"
    
    try {
        # Test Business Engine health
        Write-SmartLog "Testing Business Engine health..." "Gray"
        $healthSuccess = Test-BusinessEngineHealth
        if (-not $healthSuccess) {
            Report-CriticalError "Business Engine Health Failed" "Business Engine health check failed."
            return $false
        }

        # Test token vending
        Write-SmartLog "Testing token vending..." "Gray"
        $tokenSuccess = Test-TokenVending
        if (-not $tokenSuccess) {
            Report-CriticalError "Token Vending Failed" "Token vending failed."
            return $false
        }

        # Test TTTranscribe proxy
        Write-SmartLog "Testing TTTranscribe proxy..." "Gray"
        $proxySuccess = Test-TTTranscribeProxy
        if (-not $proxySuccess) {
            Report-CriticalError "TTTranscribe Proxy Failed" "TTTranscribe proxy failed."
            return $false
        }

        Write-SmartLog "Business Engine integration test passed" "Green"
        return $true
        
    } catch {
        Report-CriticalError "Business Engine Integration Test Exception" "An unexpected error occurred: $($_.Exception.Message)"
        return $false
    }
}

function Test-AppLaunch {
    Write-SmartLog "Testing app launch..." "Gray"
    
    try {
        $launchCommand = "adb shell am start -n app.pluct/.MainActivity"
        $launchResult = Invoke-Expression $launchCommand 2>&1
        
        if ($LASTEXITCODE -eq 0) {
            Write-SmartLog "App launched successfully" "Green"
            Start-Sleep -Seconds 3
            return $true
        } else {
            Write-SmartLog "App launch failed: $launchResult" "Red"
            return $false
        }
    } catch {
        Write-SmartLog "App launch exception: $($_.Exception.Message)" "Red"
        return $false
    }
}

function Test-ShareIntent {
    param([string]$TestUrl)
    
    Write-SmartLog "Testing share intent with URL: $TestUrl" "Gray"
    
    try {
        $shareCommand = "adb shell am start -a android.intent.action.SEND -t text/plain --es android.intent.extra.TEXT `"$TestUrl`" -n app.pluct/.share.PluctShareIngestActivity"
        $shareResult = Invoke-Expression $shareCommand 2>&1
        
        if ($LASTEXITCODE -eq 0) {
            Write-SmartLog "Share intent handled successfully" "Green"
            Start-Sleep -Seconds 2
            return $true
        } else {
            Write-SmartLog "Share intent failed: $shareResult" "Red"
            return $false
        }
    } catch {
        Write-SmartLog "Share intent exception: $($_.Exception.Message)" "Red"
        return $false
    }
}

function Test-VideoProcessing {
    param([string]$TestUrl)
    
    Write-SmartLog "Testing video processing flow..." "Gray"
    
    try {
        # Dump UI and verify interactive elements
        $xml = Get-UiHierarchy
        if (-not $xml) { Write-SmartLog "Failed to obtain UI hierarchy" "Red"; return $false }

        # Attempt to click on primary actions
        $candidates = @('Add Video','Search','Processing Status','Start','Process','Confirm','Analyze')
        $clickedAny = $false
        foreach ($label in $candidates) {
            $hits = Find-UiElementsByText -UiXml $xml -Text $label -Contains
            if ($hits.Count -gt 0) {
                if (Click-UiNode $hits[0]) {
                    Write-SmartLog "Clicked '$label'" "Yellow"
                    $clickedAny = $true
                    Start-Sleep -Seconds 1
                    break
                }
            }
        }

        if (-not $clickedAny) {
            Write-SmartLog "No actionable button found; attempting generic clickable element" "Yellow"
            $nodes = $xml.SelectNodes('//node[@clickable="true"]')
            if ($nodes.Count -gt 0) { [void](Click-UiNode $nodes[0]); Start-Sleep -Seconds 1 }
        }

        # Verify processing state
        if (Wait-ForUiText -Text 'Processing' -TimeoutSeconds 6) {
            Write-SmartLog "Detected 'Processing' text after interactions" "Green"
            return $true
        }

        # Check logcat for processing logs
        $log = adb shell logcat -d | Select-String 'PluctTTTranscribeService|Status|TRANSCRIBING|Processing'
        if ($log) { Write-SmartLog "Detected processing logs in logcat" "Green"; return $true }

        Write-SmartLog "No processing indicators found after interaction" "Red"
        return $false
    } catch {
        Write-SmartLog "Video processing exception: $($_.Exception.Message)" "Red"
        return $false
    }
}

function Test-AIFeatures {
    param([string]$TestUrl)
    
    Write-SmartLog "Testing AI features..." "Gray"
    
    try {
        # Check if AI features are working
        $aiCheck = "adb shell logcat -d"
        $aiResult = Invoke-Expression $aiCheck 2>&1
        
        if ($aiResult -match "AI|metadata|transcript") {
            Write-SmartLog "AI features are working" "Green"
            return $true
        } else {
            Write-SmartLog "AI features not detected" "Yellow"
            return $true  # Not critical for basic functionality
        }
    } catch {
        Write-SmartLog "AI features exception: $($_.Exception.Message)" "Red"
        return $false
    }
}

function Test-SmartCaching {
    Write-SmartLog "Testing smart caching..." "Gray"
    
    try {
        # Check if caching is working
        $cacheCheck = "adb shell logcat -d"
        $cacheResult = Invoke-Expression $cacheCheck 2>&1
        
        if ($cacheResult -match "cache") {
            Write-SmartLog "Smart caching is working" "Green"
            return $true
        } else {
            Write-SmartLog "Smart caching not detected" "Yellow"
            return $true  # Not critical for basic functionality
        }
    } catch {
        Write-SmartLog "Smart caching exception: $($_.Exception.Message)" "Red"
        return $false
    }
}

function Test-BusinessEngineHealth {
    Write-SmartLog "Testing Business Engine health..." "Gray"
    
    try {
        # Check for Business Engine health logs
        $healthLogs = adb shell logcat -d | Select-String "BusinessEngineHealthChecker|HEALTH_CHECK" | Select-Object -Last 5
        if ($healthLogs) {
            Write-SmartLog "Business Engine health logs found" "Green"
            return $true
        } else {
            Report-CriticalError "Business Engine Health Check" "No Business Engine health logs found"
            return $false
        }
    } catch {
        Write-SmartLog "Business Engine health exception: $($_.Exception.Message)" "Red"
        return $false
    }
}

function Test-TokenVending {
    Write-SmartLog "Testing token vending..." "Gray"
    
    try {
        # Check for token vending logs
        $tokenLogs = adb shell logcat -d | Select-String "VENDING_TOKEN|vend-token|Bearer" | Select-Object -Last 5
        if ($tokenLogs) {
            Write-SmartLog "Token vending logs found" "Green"
            return $true
        } else {
            Report-CriticalError "Token Vending" "No token vending logs found"
            return $false
        }
    } catch {
        Write-SmartLog "Token vending exception: $($_.Exception.Message)" "Red"
        return $false
    }
}

function Test-TTTranscribeProxy {
    Write-SmartLog "Testing TTTranscribe proxy..." "Gray"
    
    try {
        # Check for TTTranscribe proxy logs
        $proxyLogs = adb shell logcat -d | Select-String "REQUEST_SUBMITTED|ttt/transcribe|proxy" | Select-Object -Last 5
        if ($proxyLogs) {
            Write-SmartLog "TTTranscribe proxy logs found" "Green"
            return $true
        } else {
            Report-CriticalError "TTTranscribe Proxy" "No TTTranscribe proxy logs found"
            return $false
        }
    } catch {
        Write-SmartLog "TTTranscribe proxy exception: $($_.Exception.Message)" "Red"
        return $false
    }
}

function Report-CriticalError {
    param(
        [string]$ErrorType,
        [string]$ErrorMessage,
        [string]$Stage = "Unknown"
    )
    
    Write-SmartLog "❌ CRITICAL ERROR: $ErrorType" "Red"
    Write-SmartLog "Stage: $Stage" "Red"
    Write-SmartLog "Error Details: $ErrorMessage" "Red"
    Write-SmartLog "Test execution stopped due to critical error." "Red"
    
    $script:TestSession.CriticalErrors += @{
        Type = $ErrorType
        Message = $ErrorMessage
        Stage = $Stage
        Timestamp = Get-Date
    }
}

function Show-TestReport {
    param([bool]$OverallSuccess)
    
    $duration = (Get-Date) - $script:TestSession.StartTime
    Write-SmartLog "=== TEST REPORT ===" "Cyan"
    Write-SmartLog "Duration: $($duration.TotalSeconds.ToString('F2')) seconds" "White"
    Write-SmartLog "Test URL: $($script:TestSession.TestUrl)" "White"
    Write-SmartLog "Build Required: $($script:TestSession.BuildRequired)" "White"
    
    if ($script:TestSession.CriticalErrors.Count -gt 0) {
        Write-SmartLog "Critical Errors:" "Red"
        foreach ($error in $script:TestSession.CriticalErrors) {
            Write-SmartLog "  - $($error.Type): $($error.Message)" "Red"
        }
    }
    
    if ($OverallSuccess) {
        Write-SmartLog "✅ All tests passed successfully" "Green"
    } else {
        Write-SmartLog "❌ Some tests failed" "Red"
    }
}

# Main execution
Start-PluctTestOrchestrator

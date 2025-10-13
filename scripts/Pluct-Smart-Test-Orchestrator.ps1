# Pluct Smart Test Orchestrator - Intelligent Testing Framework
# Single source of truth for all testing with smart build detection and comprehensive journey testing
# Follows naming convention: [Project]-[ParentScope]-[ChildScope]-[CoreResponsibility]

param(
    [Parameter(Position=0)]
    [string]$TestUrl = "https://www.tiktok.com/@garyvee/video/7308801293029248299",
    
    [Parameter()]
    [ValidateSet("All", "Intent", "Capture", "Background", "TTTranscribe", "Settings", "Status")]
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

# Import core testing modules
$script:FrameworkRoot = $PSScriptRoot
. "$script:FrameworkRoot\Pluct-Smart-Test-Core-Utilities.ps1"
. "$script:FrameworkRoot\Pluct-Smart-Test-Build-Detector.ps1"
. "$script:FrameworkRoot\Pluct-Smart-Test-Device-Manager.ps1"
. "$script:FrameworkRoot\Pluct-Smart-Test-Journey-Engine.ps1"
. "$script:FrameworkRoot\Pluct-Smart-Test-Status-Tracker.ps1"

# Initialize smart test session
$script:SmartTestSession = @{
    StartTime = Get-Date
    TestResults = @{}
    BuildRequired = $false
    Screenshots = @()
    Logs = @()
    TestUrl = $TestUrl
    JourneyResults = @{}
    StatusTracking = @{}
    FailureDetails = @()
    CriticalErrors = @()
    SmartBuildDetection = @{
        LastBuildTime = $null
        ChangedFiles = @()
        BuildReason = ""
    }
}

function Start-SmartTestOrchestrator {
    Write-SmartLog "=== Pluct Smart Test Orchestrator ===" "Cyan"
    Write-SmartLog "Test Scope: $TestScope" "White"
    Write-SmartLog "Test URL: $TestUrl" "White"
    Write-SmartLog "Intelligent testing with smart build detection and comprehensive journey validation" "Yellow"
    
    # Check prerequisites
    if (-not (Test-SmartAndroidDevice)) {
        Report-SmartCriticalError "No Android device connected" "Ensure an Android emulator is running or a physical device is connected via ADB."
        exit 1
    }
    
    # Smart build detection
    if (-not $SkipInstall) {
        $script:SmartTestSession.BuildRequired = Test-SmartBuildRequired -ForceBuild:$ForceBuild
        if ($script:SmartTestSession.BuildRequired) {
            Write-SmartLog "Smart build detection: Code changes detected - build required" "Yellow"
            Write-SmartLog "Build reason: $($script:SmartTestSession.SmartBuildDetection.BuildReason)" "Gray"
            if (-not (Build-SmartApp)) {
                Report-SmartCriticalError "Smart build failed" "The intelligent build process failed. Check the build output for specific compilation errors."
                exit 1
            }
        } else {
            Write-SmartLog "Smart build detection: No changes detected - skipping build" "Green"
        }
    }
    
    # Smart deployment
    if (-not $SkipInstall) {
        $deploymentNeeded = Test-SmartDeploymentNeeded
        if ($deploymentNeeded -or $script:SmartTestSession.BuildRequired) {
            Write-SmartLog "Deploying latest build to device..." "Yellow"
            $deploySuccess = Deploy-SmartToDevice
            if (-not $deploySuccess) {
                Report-SmartCriticalError "Smart deployment failed" "The APK could not be installed on the device. Check device connection and storage."
                exit 1
            }
            Write-SmartLog "Smart deployment successful" "Green"
        } else {
            Write-SmartLog "Latest build already deployed" "Green"
        }
    }
    
    # Execute smart tests with immediate termination on failure
    $overallSuccess = $true
    
    switch ($TestScope.ToLower()) {
        "all" {
            $overallSuccess = (Test-SmartIntentJourney -TestUrl $TestUrl)
            if (-not $overallSuccess) { 
                Report-SmartCriticalError "Intent Journey Failed" "The intent handling journey failed. This is critical for app functionality."
                Write-SmartLog "TERMINATING ON FIRST FAILURE: Intent Journey" "Red"
                exit 1
            }
            Write-SmartLog "Intent Journey test passed" "Green"
            
            $overallSuccess = (Test-SmartCaptureJourney -TestUrl $TestUrl)
            if (-not $overallSuccess) { 
                Report-SmartCriticalError "Capture Journey Failed" "The capture preliminary insights journey failed. This affects user experience."
                Write-SmartLog "TERMINATING ON FIRST FAILURE: Capture Journey" "Red"
                exit 1
            }
            Write-SmartLog "Capture Journey test passed" "Green"
            
            $overallSuccess = (Test-SmartBackgroundJourney -TestUrl $TestUrl)
            if (-not $overallSuccess) { 
                Report-SmartCriticalError "Background Journey Failed" "The background processing journey failed. This affects data processing."
                Write-SmartLog "TERMINATING ON FIRST FAILURE: Background Journey" "Red"
                exit 1
            }
            Write-SmartLog "Background Journey test passed" "Green"
            
            $overallSuccess = (Test-SmartTTTranscribeJourney -TestUrl $TestUrl)
            if (-not $overallSuccess) { 
                Report-SmartCriticalError "TTTranscribe Journey Failed" "The TTTranscribe integration journey failed. This affects transcription quality."
                Write-SmartLog "TERMINATING ON FIRST FAILURE: TTTranscribe Journey" "Red"
                exit 1
            }
            Write-SmartLog "TTTranscribe Journey test passed" "Green"
            
            $overallSuccess = (Test-SmartSettingsJourney)
            if (-not $overallSuccess) { 
                Report-SmartCriticalError "Settings Journey Failed" "The settings page journey failed. This affects user configuration."
                Write-SmartLog "TERMINATING ON FIRST FAILURE: Settings Journey" "Red"
                exit 1
            }
            Write-SmartLog "Settings Journey test passed" "Green"
            
            $overallSuccess = (Test-SmartStatusTrackingJourney -TestUrl $TestUrl)
            if (-not $overallSuccess) { 
                Report-SmartCriticalError "Status Tracking Journey Failed" "The status tracking journey failed. This affects user feedback."
                Write-SmartLog "TERMINATING ON FIRST FAILURE: Status Tracking Journey" "Red"
                exit 1
            }
            Write-SmartLog "Status Tracking Journey test passed" "Green"
        }
        "intent" {
            Write-SmartLog "Testing Intent Journey..." "Cyan"
            $overallSuccess = (Test-SmartIntentJourney -TestUrl $TestUrl)
            if (-not $overallSuccess) { 
                Report-SmartCriticalError "Intent Journey Failed" "The intent handling journey failed. This is critical for app functionality."
                Write-SmartLog "TERMINATING ON FIRST FAILURE: Intent Journey" "Red"
                exit 1
            }
            Write-SmartLog "Intent Journey test passed" "Green"
        }
        "capture" {
            Write-SmartLog "Testing Capture Journey..." "Cyan"
            $overallSuccess = (Test-SmartCaptureJourney -TestUrl $TestUrl)
            if (-not $overallSuccess) { 
                Report-SmartCriticalError "Capture Journey Failed" "The capture preliminary insights journey failed. This affects user experience."
                Write-SmartLog "TERMINATING ON FIRST FAILURE: Capture Journey" "Red"
                exit 1
            }
            Write-SmartLog "Capture Journey test passed" "Green"
        }
        "background" {
            Write-SmartLog "Testing Background Journey..." "Cyan"
            $overallSuccess = (Test-SmartBackgroundJourney -TestUrl $TestUrl)
            if (-not $overallSuccess) { 
                Report-SmartCriticalError "Background Journey Failed" "The background processing journey failed. This affects data processing."
                Write-SmartLog "TERMINATING ON FIRST FAILURE: Background Journey" "Red"
                exit 1
            }
            Write-SmartLog "Background Journey test passed" "Green"
        }
        "tttranscribe" {
            Write-SmartLog "Testing TTTranscribe Journey..." "Cyan"
            $overallSuccess = (Test-SmartTTTranscribeJourney -TestUrl $TestUrl)
            if (-not $overallSuccess) { 
                Report-SmartCriticalError "TTTranscribe Journey Failed" "The TTTranscribe integration journey failed. This affects transcription quality."
                Write-SmartLog "TERMINATING ON FIRST FAILURE: TTTranscribe Journey" "Red"
                exit 1
            }
            Write-SmartLog "TTTranscribe Journey test passed" "Green"
        }
        "settings" {
            Write-SmartLog "Testing Settings Journey..." "Cyan"
            $overallSuccess = (Test-SmartSettingsJourney)
            if (-not $overallSuccess) { 
                Report-SmartCriticalError "Settings Journey Failed" "The settings page journey failed. This affects user configuration."
                Write-SmartLog "TERMINATING ON FIRST FAILURE: Settings Journey" "Red"
                exit 1
            }
            Write-SmartLog "Settings Journey test passed" "Green"
        }
        "status" {
            Write-SmartLog "Testing Status Tracking Journey..." "Cyan"
            $overallSuccess = (Test-SmartStatusTrackingJourney -TestUrl $TestUrl)
            if (-not $overallSuccess) { 
                Report-SmartCriticalError "Status Tracking Journey Failed" "The status tracking journey failed. This affects user feedback."
                Write-SmartLog "TERMINATING ON FIRST FAILURE: Status Tracking Journey" "Red"
                exit 1
            }
            Write-SmartLog "Status Tracking Journey test passed" "Green"
        }
        default {
            Report-SmartCriticalError "Invalid TestScope specified" "The provided TestScope '$TestScope' is not recognized. Please use 'All', 'Intent', 'Capture', 'Background', 'TTTranscribe', 'Settings', or 'Status'."
            exit 1
        }
    }
    
    # Generate smart test report
    Show-SmartTestReport -OverallSuccess $overallSuccess
}

# Start the smart test orchestrator
Start-SmartTestOrchestrator

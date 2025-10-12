# Pluct Test Orchestrator - Main Entry Point
# Single source of truth for all testing operations
# Handles build detection, test execution, and result aggregation

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
    [string]$TestScope = "All"  # All, Journey, Capture, Background, API
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
}

function Start-TestOrchestrator {
    Write-Log "=== Pluct Test Orchestrator ===" "Cyan"
    Write-Log "Test Scope: $TestScope" "White"
    Write-Log "Test URL: $TestUrl" "White"
    
    # Check prerequisites
    if (-not (Test-AndroidDevice)) {
        Write-Log "No Android device connected" "Red"
        exit 1
    }
    
    # Determine if build is needed
    if (-not $SkipBuild) {
        $script:TestSession.BuildRequired = Test-BuildRequired
        if ($script:TestSession.BuildRequired) {
            Write-Log "Code changes detected - build required" "Yellow"
        } else {
            Write-Log "No code changes - skipping build" "Green"
        }
    }
    
    # Execute tests based on scope
    $overallSuccess = $true
    
    switch ($TestScope.ToLower()) {
        "all" {
            $overallSuccess = (Test-IntentJourney) -and (Test-CaptureJourney) -and (Test-CompleteJourney) -and (Test-EnhancementsJourney)
        }
        "journey" {
            $overallSuccess = (Test-IntentJourney) -and (Test-CaptureJourney)
        }
        "capture" {
            $overallSuccess = Test-CaptureJourney
        }
        "complete" {
            $overallSuccess = Test-CompleteJourney
        }
        "enhancements" {
            $overallSuccess = Test-EnhancementsJourney
        }
        "api" {
            $overallSuccess = Test-IntentJourney
        }
    }
    
    # Generate final report
    Show-TestReport -OverallSuccess $overallSuccess
    
    if ($overallSuccess) {
        Write-Log "All tests passed successfully" "Green"
        exit 0
    } else {
        Write-Log "Some tests failed" "Red"
        exit 1
    }
}

# Main execution
Start-TestOrchestrator

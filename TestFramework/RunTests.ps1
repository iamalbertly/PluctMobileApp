# ClipForge Test Framework - Main Test Runner
# This script orchestrates the execution of all tests

param(
    [Parameter(Position=0)]
    [string]$TestUrl = $null,
    
    
    [Parameter()]
    [switch]$CaptureHTML,
    
    [Parameter()]
    [switch]$SkipBuild,
    
    [Parameter()]
    [switch]$SkipInstall,
    
    [Parameter()]
    [switch]$RunTikTokTest,
    
    [Parameter()]
    [switch]$RunErrorTests,
    
    [Parameter()]
    [switch]$RunWebViewTests,
    
    [Parameter()]
    [switch]$RunAll
)

# Set script scope variables
$script:StartTime = Get-Date
$script:TotalTests = 0
$script:PassedTests = 0
$script:FailedTests = 0
$script:TestResults = @{}
$script:CurrentTestSuite = ""
$script:CurrentTestCase = ""
$script:CurrentTestStep = ""

# Import modules
$frameworkRoot = $PSScriptRoot

# Import configuration
. "$frameworkRoot\Config\TestConfig.ps1"

# Import core modules
. "$frameworkRoot\Core\Logging.ps1"
. "$frameworkRoot\Core\Utilities.ps1"
. "$frameworkRoot\Core\TestExecution.ps1"

# Import test modules
. "$frameworkRoot\Modules\TikTokTests.ps1"

# Set default test URL if not provided
if ([string]::IsNullOrWhiteSpace($TestUrl)) {
    $TestUrl = $script:DefaultTestUrl
    Write-Log "Using default test URL: $TestUrl" "Yellow"
}

# Validate URL
$validatedUrl = Test-ValidUrl -Url $TestUrl
if (-not $validatedUrl) {
    Write-Log "Invalid URL format: $TestUrl" "Red"
    exit 1
}
$TestUrl = $validatedUrl

# Check prerequisites
if (-not (Test-AndroidDevice)) {
    Write-Log "No Android device connected, cannot run tests" "Red"
    exit 1
}

# Start real-time logs
Show-RealtimeLogs

# Build and install app if needed
$buildSuccess = Build-App -SkipBuild:$SkipBuild
if (-not $buildSuccess) {
    Write-Log "Failed to build app" "Red"
    Stop-RealtimeLogs
    exit 1
}

$installSuccess = Install-App -SkipInstall:$SkipInstall
if (-not $installSuccess) {
    Write-Log "Failed to install app" "Red"
    Stop-RealtimeLogs
    exit 1
}

# Determine which tests to run
$runTikTok = $RunTikTokTest -or $RunAll
$runError = $RunErrorTests -or $RunAll
$runWebView = $RunWebViewTests -or $RunAll

# If no specific tests are selected, run TikTok test by default
if (-not ($runTikTok -or $runError -or $runWebView)) {
    $runTikTok = $true
}

# Run tests
$overallSuccess = $true

if ($runTikTok) {
    Write-Log "\n===== RUNNING TIKTOK URL PROCESSING TESTS =====" "Blue"
    $tikTokSuccess = Test-TokAuditE2E -TestUrl $TestUrl -CaptureHTML:$CaptureHTML
    if (-not $tikTokSuccess) {
        $overallSuccess = $false
    }
}

if ($runError) {
    Write-Log "\n===== RUNNING ERROR HANDLING TESTS =====" "Blue"
    $errorSuccess = Test-ErrorHandling -CaptureHTML:$CaptureHTML
    if (-not $errorSuccess) {
        $overallSuccess = $false
    }
}

if ($runWebView) {
    Write-Log "\n===== RUNNING WEBVIEW FUNCTIONALITY TESTS =====" "Blue"
    $webViewSuccess = Test-WebViewFunctionality -TestUrl $TestUrl -CaptureHTML:$CaptureHTML
    if (-not $webViewSuccess) {
        $overallSuccess = $false
    }
    
    $transcriptSuccess = Test-TranscriptExtraction -TestUrl $TestUrl -CaptureHTML:$CaptureHTML
    if (-not $transcriptSuccess) {
        $overallSuccess = $false
    }
}

# Generate test summary
Write-Log "\n===== TEST SUMMARY =====" "Blue"
Write-Log "Total Tests: $script:TotalTests" "White"
Write-Log "Passed: $script:PassedTests" "Green"
Write-Log "Failed: $script:FailedTests" "Red"

if ($script:TotalTests -gt 0) {
    $successRate = [math]::Round(($script:PassedTests / $script:TotalTests) * 100, 2)
    $color = if ($successRate -ge 80) { "Green" } elseif ($successRate -ge 50) { "Yellow" } else { "Red" }
    Write-Log "Success Rate: ${successRate}%" $color
}

# Log feature test results
Write-Log "\nFeature Test Results:" "White"
if ($runTikTok) {
    $result = if ($tikTokSuccess) { "PASS" } else { "FAIL" }
    $color = if ($tikTokSuccess) { "Green" } else { "Red" }
    Write-Log "  TikTok URL Processing: $result" $color
}
if ($runError) {
    $result = if ($errorSuccess) { "PASS" } else { "FAIL" }
    $color = if ($errorSuccess) { "Green" } else { "Red" }
    Write-Log "  Error Handling: $result" $color
}
if ($runWebView) {
    $webViewResult = if ($webViewSuccess) { "PASS" } else { "FAIL" }
    $webViewColor = if ($webViewSuccess) { "Green" } else { "Red" }
    Write-Log "  WebView Functionality: $webViewResult" $webViewColor
    
    $transcriptResult = if ($transcriptSuccess) { "PASS" } else { "FAIL" }
    $transcriptColor = if ($transcriptSuccess) { "Green" } else { "Red" }
    Write-Log "  Transcript Extraction: $transcriptResult" $transcriptColor
}

# Get recent logs
Write-Log "\nRecent Logs:" "White"
$recentLogs = adb logcat -d -t 20 | Select-String -Pattern "WebViewUtils|WVConsole|ScriptTokAudit|WebTranscriptActivity|Ingest|MainActivity"
if ($recentLogs) {
    $recentLogs | ForEach-Object { Write-Log "  $_" "Gray" }
} else {
    Write-Log "  No relevant logs found" "Yellow"
}

# Stop real-time logs
Stop-RealtimeLogs

# Calculate overall result
if ($overallSuccess) {
    Write-Log "\nOVERALL RESULT: PASS" "Green"
    exit 0
} else {
    Write-Log "\nOVERALL RESULT: FAIL" "Red"
    exit 1
}
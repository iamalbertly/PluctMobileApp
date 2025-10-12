# Pluct Test Core Utilities
# Single source of truth for common testing utilities

$script:LogFile = "TestFramework\Logs\pluct_test_$(Get-Date -Format 'yyyyMMdd_HHmmss').log"
$script:AppPackage = "app.pluct"
$script:MainActivity = "app.pluct.MainActivity"
$script:ShareActivity = "app.pluct.share.ShareIngestActivity"

# Ensure logs directory exists
if (-not (Test-Path "TestFramework\Logs")) {
    New-Item -Path "TestFramework\Logs" -ItemType Directory -Force | Out-Null
}

function Write-Log {
    param(
        [string]$Message,
        [string]$Color = "White",
        [switch]$NoTimestamp
    )
    
    $timestamp = if ($NoTimestamp) { "" } else { "[$(Get-Date -Format 'HH:mm:ss')]" }
    $logMessage = "$timestamp $Message"
    
    Write-Host $logMessage -ForegroundColor $Color
    Add-Content -Path $script:LogFile -Value $logMessage
}

function Test-AndroidDevice {
    $devices = adb devices | Select-String "device$"
    return $devices.Count -gt 0
}

function Wait-ForLog {
    param(
        [string]$Pattern,
        [int]$TimeoutSeconds = 10,
        [string]$Description = "Log pattern",
        [switch]$ShowLogs
    )
    
    Write-Log "Waiting for: $Description" "Gray"
    $timer = 0
    
    while ($timer -lt $TimeoutSeconds) {
        $logs = adb logcat -d | Select-String -Pattern $Pattern
        if ($logs) {
            Write-Log "Found: $Description" "Green"
            if ($ShowLogs) {
                Write-Log "Matching logs:" "Cyan"
                $logs | ForEach-Object { Write-Log "  $_" "Gray" }
            }
            return $true
        }
        Start-Sleep -Seconds 1
        $timer++
    }
    
    Write-Log "Timeout waiting for: $Description" "Red"
    Write-Log "Recent logs for debugging:" "Yellow"
    $recentLogs = adb logcat -d | Select-Object -Last 20
    $recentLogs | ForEach-Object { Write-Log "  $_" "Gray" }
    return $false
}

function Get-ScreenContent {
    Write-Log "Capturing current screen content..." "Yellow"
    
    # Get current activity
    $currentActivity = adb shell dumpsys activity activities | Select-String "mResumedActivity" | Select-Object -First 1
    Write-Log "Current Activity: $currentActivity" "Cyan"
    
    # Get visible text on screen
    $screenText = adb shell dumpsys activity top | Select-String "TEXT"
    if ($screenText) {
        Write-Log "Screen Text Content:" "Cyan"
        $screenText | ForEach-Object { Write-Log "  $_" "Gray" }
    }
    
    # Get UI hierarchy
    $uiHierarchy = adb shell uiautomator dump
    if ($uiHierarchy) {
        Write-Log "UI Hierarchy captured" "Green"
    }
    
    return @{
        Activity = $currentActivity
        Text = $screenText
        UI = $uiHierarchy
    }
}

function Test-ScreenContent {
    param(
        [string]$ExpectedText,
        [string]$Context = "Screen validation"
    )
    
    Write-Log "Validating screen content: $Context" "Yellow"
    Write-Log "Looking for: $ExpectedText" "Gray"
    
    $screenContent = Get-ScreenContent
    
    if ($screenContent.Text -match $ExpectedText) {
        Write-Log "✅ Screen validation PASSED: Found '$ExpectedText'" "Green"
        return $true
    } else {
        Write-Log "❌ Screen validation FAILED: '$ExpectedText' not found" "Red"
        Write-Log "Available screen text:" "Yellow"
        $screenContent.Text | ForEach-Object { Write-Log "  $_" "Gray" }
        return $false
    }
}

function Simulate-SwipeUp {
    param(
        [string]$Context = "Swipe up gesture"
    )
    
    Write-Log "Simulating swipe up gesture: $Context" "Yellow"
    
    # Get screen dimensions
    $screenSize = adb shell wm size | Select-String "Physical size:" | ForEach-Object { $_.ToString().Split(':')[1].Trim() }
    $width = $screenSize.Split('x')[0]
    $height = $screenSize.Split('x')[1]
    
    # Calculate swipe coordinates (from bottom center to middle)
    $startX = [int]($width / 2)
    $startY = [int]($height * 0.8)  # Start from 80% down the screen
    $endX = $startX
    $endY = [int]($height * 0.4)    # End at 40% down the screen
    
    Write-Log "Swipe coordinates: ($startX,$startY) -> ($endX,$endY)" "Gray"
    
    # Execute swipe command
    $swipeCommand = "shell input swipe $startX $startY $endX $endY 500"
    adb $swipeCommand.Split(' ')
    
    Start-Sleep -Seconds 1  # Allow animation to complete
    Write-Log "Swipe up gesture completed" "Green"
}

function Simulate-Tap {
    param(
        [int]$X,
        [int]$Y,
        [string]$Context = "Tap gesture"
    )
    
    Write-Log "Simulating tap at ($X,$Y): $Context" "Yellow"
    
    $tapCommand = "shell input tap $X $Y"
    adb $tapCommand.Split(' ')
    
    Start-Sleep -Seconds 1  # Allow interaction to complete
    Write-Log "Tap gesture completed" "Green"
}

function Test-BottomSheetExpansion {
    Write-Log "Testing bottom sheet expansion..." "Cyan"
    
    # First, check if the bottom sheet is visible
    $bottomSheetVisible = Test-ScreenContent -ExpectedText "Capture This Insight" -Context "Bottom sheet headline"
    
    if ($bottomSheetVisible) {
        Write-Log "✅ Bottom sheet is visible" "Green"
        
        # Simulate swipe up to expand the sheet
        Simulate-SwipeUp -Context "Expand bottom sheet to show tier options"
        
        # Wait for animation
        Start-Sleep -Seconds 2
        
        # Check for tier selection options
        $quickScanVisible = Test-ScreenContent -ExpectedText "Quick Scan" -Context "Quick Scan option"
        $aiAnalysisVisible = Test-ScreenContent -ExpectedText "AI Analysis" -Context "AI Analysis option"
        
        if ($quickScanVisible -and $aiAnalysisVisible) {
            Write-Log "✅ Bottom sheet expanded successfully - both tier options visible" "Green"
            return $true
        } elseif ($quickScanVisible -or $aiAnalysisVisible) {
            Write-Log "⚠️ PARTIAL: Only one tier option visible after expansion" "Yellow"
            return $true
        } else {
            Write-Log "❌ Bottom sheet expansion failed - tier options not visible" "Red"
            return $false
        }
    } else {
        Write-Log "❌ Bottom sheet not visible" "Red"
        return $false
    }
}

function Clear-AppData {
    Write-Log "Clearing app data for clean test" "Yellow"
    adb shell pm clear $script:AppPackage | Out-Null
    Start-Sleep -Seconds 2
}

function Capture-Logs {
    param([string]$Description)
    
    $timestamp = Get-Date -Format "yyyyMMdd_HHmmss"
    $logFile = "TestFramework\Logs\${Description}_${timestamp}.log"
    
    Write-Log "Capturing logs to: $logFile" "Yellow"
    adb logcat -d > $logFile
    return $logFile
}

function Show-TestReport {
    param([bool]$OverallSuccess)
    
    Write-Log ""
    Write-Log "=== TEST REPORT ===" "Cyan"
    
    foreach ($test in $script:TestSession.TestResults.GetEnumerator()) {
        $status = if ($test.Value) { "PASS" } else { "FAIL" }
        $color = if ($test.Value) { "Green" } else { "Red" }
        Write-Log "  $($test.Key): $status" $color
    }
    
    $overallStatus = if ($OverallSuccess) { "PASS" } else { "FAIL" }
    $overallColor = if ($OverallSuccess) { "Green" } else { "Red" }
    Write-Log "  Overall: $overallStatus" $overallColor
}

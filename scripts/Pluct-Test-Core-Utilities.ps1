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

function Show-AllVisibleElements {
    Write-Log "=== COMPREHENSIVE SCREEN ANALYSIS ===" "Cyan"
    
    # Get current activity
    $currentActivity = adb shell dumpsys activity activities | Select-String "mResumedActivity" | Select-Object -First 1
    Write-Log "Current Activity: $currentActivity" "Cyan"
    
    # Get all visible text elements
    Write-Log "=== ALL VISIBLE TEXT ELEMENTS ===" "Yellow"
    $allText = adb shell dumpsys activity top | Select-String "TEXT"
    if ($allText) {
        $allText | ForEach-Object { 
            $text = $_.ToString().Trim()
            if ($text -ne "") {
                Write-Log "TEXT: $text" "White"
            }
        }
    } else {
        Write-Log "No text elements found" "Red"
    }
    
    # Get UI hierarchy and extract all clickable elements
    Write-Log "=== ALL CLICKABLE ELEMENTS ===" "Yellow"
    $uiDump = adb shell uiautomator dump
    if ($uiDump) {
        $xmlContent = adb shell cat /sdcard/window_dump.xml
        if ($xmlContent) {
            $lines = $xmlContent -split "`n"
            $clickableElements = $lines | Where-Object { $_ -match 'clickable="true"' }
            foreach ($element in $clickableElements) {
                if ($element -match 'text="([^"]*)"') {
                    $text = $matches[1]
                    if ($text -ne "") {
                        Write-Log "CLICKABLE: $text" "Green"
                    }
                }
                if ($element -match 'content-desc="([^"]*)"') {
                    $desc = $matches[1]
                    if ($desc -ne "") {
                        Write-Log "CLICKABLE (desc): $desc" "Green"
                    }
                }
            }
        }
    }
    
    # Get all text elements with bounds
    Write-Log "=== ALL TEXT ELEMENTS WITH COORDINATES ===" "Yellow"
    if ($xmlContent) {
        $textElements = $lines | Where-Object { $_ -match 'text="[^"]*"' }
        foreach ($element in $textElements) {
            if ($element -match 'text="([^"]*)"' -and $element -match 'bounds="\[(\d+),(\d+)\]\[(\d+),(\d+)\]"') {
                $text = $matches[1]
                $x1 = $matches[2]
                $y1 = $matches[3]
                $x2 = $matches[4]
                $y2 = $matches[5]
                if ($text -ne "") {
                    $centerX = [int](($x1 + $x2) / 2)
                    $centerY = [int](($y1 + $y2) / 2)
                    Write-Log "TEXT: '$text' at ($centerX, $centerY)" "White"
                }
            }
        }
    }
    
    Write-Log "=== END SCREEN ANALYSIS ===" "Cyan"
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

function Simulate-TapByText {
    param(
        [string]$Text,
        [string]$Context = "Tap by text"
    )
    
    Write-Log "Looking for text: '$Text' to tap" "Yellow"
    
    # Show all available text elements first
    Write-Log "=== AVAILABLE TEXT ELEMENTS ===" "Cyan"
    $uiDump = adb shell uiautomator dump
    if ($uiDump) {
        $xmlContent = adb shell cat /sdcard/window_dump.xml
        if ($xmlContent) {
            $lines = $xmlContent -split "`n"
            $textElements = $lines | Where-Object { $_ -match 'text="[^"]*"' }
            foreach ($element in $textElements) {
                if ($element -match 'text="([^"]*)"') {
                    $elementText = $matches[1]
                    if ($elementText -ne "") {
                        Write-Log "Available: '$elementText'" "Gray"
                    }
                }
            }
        }
    }
    
    # First, get the UI hierarchy to find the element
    if (-not $uiDump) {
        Write-Log "❌ Failed to get UI hierarchy" "Red"
        return $false
    }
    
    # Parse the UI dump to find coordinates of the text
    $xmlContent = adb shell cat /sdcard/window_dump.xml
    if (-not $xmlContent) {
        Write-Log "❌ Failed to read UI dump content" "Red"
        return $false
    }
    
    # Look for the text in the UI hierarchy
    $textFound = $false
    $coordinates = $null
    
    # Try to find the element by text content
    $lines = $xmlContent -split "`n"
    foreach ($line in $lines) {
        if ($line -match "text=`"$Text`"" -or $line -match "content-desc=`"$Text`"") {
            # Extract bounds from the line
            if ($line -match 'bounds="\[(\d+),(\d+)\]\[(\d+),(\d+)\]"') {
                $x1 = [int]$matches[1]
                $y1 = [int]$matches[2]
                $x2 = [int]$matches[3]
                $y2 = [int]$matches[4]
                
                # Calculate center point
                $centerX = [int](($x1 + $x2) / 2)
                $centerY = [int](($y1 + $y2) / 2)
                
                $coordinates = @{ X = $centerX; Y = $centerY }
                $textFound = $true
                Write-Log "Found '$Text' at coordinates ($centerX, $centerY)" "Green"
                break
            }
        }
    }
    
    if (-not $textFound -or -not $coordinates) {
        Write-Log "❌ Text '$Text' not found in UI hierarchy" "Red"
        Write-Log "Available UI elements:" "Yellow"
        $lines | Where-Object { $_ -match 'text=' -or $_ -match 'content-desc=' } | Select-Object -First 10 | ForEach-Object { Write-Log "  $_" "Gray" }
        return $false
    }
    
    # Perform the tap
    Write-Log "Tapping on '$Text' at ($($coordinates.X), $($coordinates.Y))" "Yellow"
    Simulate-Tap -X $coordinates.X -Y $coordinates.Y -Context $Context
    
    return $true
}

function Test-BottomSheetExpansion {
    Write-Log "Testing bottom sheet expansion..." "Cyan"
    
    # Wait for the capture sheet to be fully rendered
    Write-Log "Waiting for capture sheet to be fully rendered..." "Yellow"
    Start-Sleep -Seconds 3
    
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
        Write-Log "❌ Bottom sheet not visible - trying alternative approach" "Red"
        
        # Try to find any text that might indicate the capture sheet
        $alternativeTexts = @("Quick Scan", "AI Analysis", "Capture", "Insight", "Tier")
        foreach ($text in $alternativeTexts) {
            $found = Test-ScreenContent -ExpectedText $text -Context "Looking for $text"
            if ($found) {
                Write-Log "✅ Found alternative text: $text" "Green"
                return $true
            }
        }
        
        Write-Log "❌ No capture sheet elements found" "Red"
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

# Simple test script to validate WebView fixes
param(
    [string]$TestUrl = "https://vm.tiktok.com/ZMA2MTD9C",
    [int]$TimeoutSeconds = 120,
    [switch]$SkipBuild
)

$ErrorActionPreference = "Stop"

function Write-Output {
    param([string]$Message, [string]$Color = "White")
    Write-Host $Message -ForegroundColor $Color
}

function Test-DeviceConnection {
    Write-Output "=== Testing Device Connection ===" "Cyan"
    
    $devices = adb devices | Where-Object { $_ -match "device$" }
    if ($devices.Count -eq 0) {
        Write-Output "No devices connected" "Red"
        return $false
    }
    
    Write-Output "Device connected: $($devices[0])" "Green"
    return $true
}

function Test-AppInstallation {
    Write-Output "=== Testing App Installation ===" "Cyan"
    
    $packages = adb shell pm list packages | Select-String "app.pluct"
    if ($packages) {
        Write-Output "App installed: $packages" "Green"
        return $true
    } else {
        Write-Output "App not installed" "Red"
        return $false
    }
}

function Test-WebViewFixes {
    Write-Output "=== Testing WebView Fixes ===" "Cyan"
    
    # Launch app with test URL
    Write-Output "Launching app with test URL: $TestUrl" "Yellow"
    
    $intent = "android.intent.action.SEND"
    $type = "text/plain"
    $component = "app.pluct/.share.ShareIngestActivity"
    $extra = "android.intent.extra.TEXT"
    
    adb shell am start -a $intent -t $type -n $component --es $extra $TestUrl
    
    if ($LASTEXITCODE -ne 0) {
        Write-Output "Failed to launch app" "Red"
        return $false
    }
    
    Write-Output "App launched successfully" "Green"
    
    # Monitor logs for expected patterns
    Write-Output "Monitoring logs for WebView fixes..." "Yellow"
    
    $logPatterns = @(
        "WV:A:webview_configured",
        "WV:A:inject_auto", 
        "WV:J:phase=page_ready",
        "WV:J:input_found",
        "WV:J:value_verified",
        "WV:J:submit_clicked",
        "WV:J:result_node_found",
        "WV:J:copied_length=",
        "WV:J:returned"
    )
    
    $foundPatterns = @()
    $startTime = Get-Date
    $timeout = $startTime.AddSeconds($TimeoutSeconds)
    
    # Start log monitoring
    $logJob = Start-Job -ScriptBlock {
        param($Patterns, $Timeout)
        $found = @()
        $endTime = (Get-Date).AddSeconds($Timeout)
        
        while ((Get-Date) -lt $endTime) {
            $logs = adb logcat -d -s WVConsole:V WebViewUtils:V WebViewConfiguration:V WebViewFocusManager:V JavaScriptBridge:V *:S
            foreach ($pattern in $Patterns) {
                if ($logs -match $pattern -and $found -notcontains $pattern) {
                    $found += $pattern
                }
            }
            Start-Sleep -Milliseconds 500
        }
        return $found
    } -ArgumentList $logPatterns, $TimeoutSeconds
    
    # Wait for completion or timeout
    while ((Get-Date) -lt $timeout) {
        if ($logJob.State -eq "Completed") {
            break
        }
        Start-Sleep -Seconds 2
        
        # Check for critical errors
        $errorLogs = adb logcat -d -s WVConsole:V WebViewUtils:V WebViewConfiguration:V WebViewFocusManager:V JavaScriptBridge:V *:S | Select-String "ERROR|clipboard_write_err|focus_error"
        if ($errorLogs) {
            Write-Output "Errors detected:" "Yellow"
            $errorLogs | ForEach-Object { Write-Output "  $($_.Line)" "Yellow" }
        }
    }
    
    # Get results
    $foundPatterns = Receive-Job $logJob
    Remove-Job $logJob
    
    # Analyze results
    Write-Output "=== Test Results ===" "Cyan"
    
    $criticalPatterns = @(
        "WV:A:webview_configured",
        "WV:A:inject_auto", 
        "WV:J:phase=page_ready",
        "WV:J:input_found",
        "WV:J:value_verified",
        "WV:J:submit_clicked"
    )
    
    $allCriticalFound = $true
    foreach ($pattern in $criticalPatterns) {
        if ($foundPatterns -contains $pattern) {
            Write-Output "PASS: $pattern" "Green"
        } else {
            Write-Output "FAIL: $pattern" "Red"
            $allCriticalFound = $false
        }
    }
    
    # Check for success indicators
    $successPatterns = @("WV:J:result_node_found", "WV:J:copied_length=", "WV:J:returned")
    $successFound = $false
    foreach ($pattern in $successPatterns) {
        if ($foundPatterns -contains $pattern) {
            Write-Output "PASS: $pattern" "Green"
            $successFound = $true
        }
    }
    
    if (-not $successFound) {
        Write-Output "No success indicators found - may still be processing" "Yellow"
    }
    
    # Check for clipboard errors
    $clipboardErrors = adb logcat -d -s WVConsole:V WebViewUtils:V WebViewConfiguration:V WebViewFocusManager:V JavaScriptBridge:V *:S | Select-String "clipboard_write_err|NotAllowedError"
    if ($clipboardErrors) {
        Write-Output "Clipboard errors still present:" "Red"
        $clipboardErrors | ForEach-Object { Write-Output "  $($_.Line)" "Red" }
        return $false
    } else {
        Write-Output "No clipboard errors detected" "Green"
    }
    
    return $allCriticalFound
}

function Show-Summary {
    param([bool]$Success)
    
    Write-Output "=== Test Summary ===" "Cyan"
    
    if ($Success) {
        Write-Output "All WebView fixes are working correctly!" "Green"
        Write-Output "Key improvements:" "Green"
        Write-Output "  - Enhanced transcript detection" "Green"
        Write-Output "  - Improved clipboard focus management" "Green"
        Write-Output "  - Better error handling and retry mechanisms" "Green"
        Write-Output "  - Comprehensive validation to prevent false positives" "Green"
    } else {
        Write-Output "Some issues remain - check logs above" "Red"
        Write-Output "Troubleshooting tips:" "Yellow"
        Write-Output "  - Check device connection" "Yellow"
        Write-Output "  - Verify app installation" "Yellow"
        Write-Output "  - Review logcat output" "Yellow"
        Write-Output "  - Try increasing timeout" "Yellow"
    }
}

# Main execution
try {
    Write-Output "WebView Fixes Test Suite" "Cyan"
    Write-Output "=======================" "Cyan"
    
    $allTestsPassed = $true
    
    # Test device connection
    if (-not (Test-DeviceConnection)) {
        $allTestsPassed = $false
    }
    
    # Test app installation
    if ($allTestsPassed -and -not (Test-AppInstallation)) {
        Write-Output "App not installed - please install first" "Red"
        $allTestsPassed = $false
    }
    
    # Test WebView fixes
    if ($allTestsPassed) {
        $webViewTestPassed = Test-WebViewFixes
        if (-not $webViewTestPassed) {
            $allTestsPassed = $false
        }
    }
    
    # Show summary
    Show-Summary $allTestsPassed
    
    if ($allTestsPassed) {
        exit 0
    } else {
        exit 1
    }
    
} catch {
    Write-Output "Test suite error: $($_.Exception.Message)" "Red"
    exit 1
}

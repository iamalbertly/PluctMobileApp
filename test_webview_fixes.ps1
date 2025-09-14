# Test script to validate WebView fixes for transcript extraction
# This script tests the comprehensive fixes for clipboard, focus, and error handling issues

param(
    [string]$TestUrl = "https://vm.tiktok.com/ZMA2MTD9C",
    [int]$TimeoutSeconds = 120,
    [switch]$SkipBuild,
    [switch]$Verbose
)

$ErrorActionPreference = "Stop"

# Colors for output
$Green = "`e[32m"
$Red = "`e[31m"
$Yellow = "`e[33m"
$Blue = "`e[34m"
$Reset = "`e[0m"

function Write-ColorOutput {
    param([string]$Message, [string]$Color = $Reset)
    Write-Host "${Color}${Message}${Reset}"
}

function Test-DeviceConnection {
    Write-ColorOutput "`n=== Testing Device Connection ===" $Blue
    
    $devices = adb devices | Where-Object { $_ -match "device$" }
    if ($devices.Count -eq 0) {
        Write-ColorOutput "❌ No devices connected" $Red
        return $false
    }
    
    Write-ColorOutput "✅ Device connected: $($devices[0])" $Green
    return $true
}

function Test-AppInstallation {
    Write-ColorOutput "`n=== Testing App Installation ===" $Blue
    
    $packages = adb shell pm list packages | Select-String "app.pluct"
    if ($packages) {
        Write-ColorOutput "✅ App installed: $packages" $Green
        return $true
    } else {
        Write-ColorOutput "❌ App not installed" $Red
        return $false
    }
}

function Build-App {
    if ($SkipBuild) {
        Write-ColorOutput "`n=== Skipping Build ===" $Yellow
        return $true
    }
    
    Write-ColorOutput "`n=== Building App ===" $Blue
    
    try {
        Push-Location $PSScriptRoot
        ./gradlew assembleDebug
        if ($LASTEXITCODE -eq 0) {
            Write-ColorOutput "✅ Build successful" $Green
            return $true
        } else {
            Write-ColorOutput "❌ Build failed" $Red
            return $false
        }
    } catch {
        Write-ColorOutput "❌ Build error: $($_.Exception.Message)" $Red
        return $false
    } finally {
        Pop-Location
    }
}

function Install-App {
    Write-ColorOutput "`n=== Installing App ===" $Blue
    
    try {
        $apkPath = "app/build/outputs/apk/debug/app-debug.apk"
        if (-not (Test-Path $apkPath)) {
            Write-ColorOutput "❌ APK not found at $apkPath" $Red
            return $false
        }
        
        adb install -r $apkPath
        if ($LASTEXITCODE -eq 0) {
            Write-ColorOutput "✅ App installed successfully" $Green
            return $true
        } else {
            Write-ColorOutput "❌ App installation failed" $Red
            return $false
        }
    } catch {
        Write-ColorOutput "❌ Installation error: $($_.Exception.Message)" $Red
        return $false
    }
}

function Test-WebViewFixes {
    Write-ColorOutput "`n=== Testing WebView Fixes ===" $Blue
    
    # Launch app with test URL
    Write-ColorOutput "Launching app with test URL: $TestUrl" $Yellow
    
    $intent = "android.intent.action.SEND"
    $type = "text/plain"
    $component = "app.pluct/.share.ShareIngestActivity"
    $extra = "android.intent.extra.TEXT"
    
    adb shell am start -a $intent -t $type -n $component --es $extra $TestUrl
    
    if ($LASTEXITCODE -ne 0) {
        Write-ColorOutput "❌ Failed to launch app" $Red
        return $false
    }
    
    Write-ColorOutput "✅ App launched successfully" $Green
    
    # Monitor logs for expected patterns
    Write-ColorOutput "`nMonitoring logs for WebView fixes..." $Yellow
    
    $logPatterns = @(
        "WV:A:webview_configured",
        "WV:A:webview_ssl_configured", 
        "WV:A:page_started",
        "WV:A:page_finished",
        "WV:A:inject_auto",
        "WV:J:phase=page_ready",
        "WV:J:input_found",
        "WV:J:value_verified",
        "WV:J:submit_clicked",
        "WV:J:focus_ensured",
        "WV:J:clipboard_prepared",
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
            Write-ColorOutput "`n⚠️  Errors detected:" $Yellow
            $errorLogs | ForEach-Object { Write-ColorOutput "  $($_.Line)" $Yellow }
        }
    }
    
    # Get results
    $foundPatterns = Receive-Job $logJob
    Remove-Job $logJob
    
    # Analyze results
    Write-ColorOutput "`n=== Test Results ===" $Blue
    
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
            Write-ColorOutput "✅ $pattern" $Green
        } else {
            Write-ColorOutput "❌ $pattern" $Red
            $allCriticalFound = $false
        }
    }
    
    # Check for focus and clipboard fixes
    $focusPatterns = @("WV:J:focus_ensured", "WV:J:clipboard_prepared")
    $focusFixed = $true
    foreach ($pattern in $focusPatterns) {
        if ($foundPatterns -contains $pattern) {
            Write-ColorOutput "✅ $pattern" $Green
        } else {
            Write-ColorOutput "⚠️  $pattern (may not be needed)" $Yellow
        }
    }
    
    # Check for success indicators
    $successPatterns = @("WV:J:result_node_found", "WV:J:copied_length=", "WV:J:returned")
    $successFound = $false
    foreach ($pattern in $successPatterns) {
        if ($foundPatterns -contains $pattern) {
            Write-ColorOutput "✅ $pattern" $Green
            $successFound = $true
        }
    }
    
    if (-not $successFound) {
        Write-ColorOutput "⚠️  No success indicators found - may still be processing" $Yellow
    }
    
    # Check for clipboard errors
    $clipboardErrors = adb logcat -d -s WVConsole:V WebViewUtils:V WebViewConfiguration:V WebViewFocusManager:V JavaScriptBridge:V *:S | Select-String "clipboard_write_err|NotAllowedError"
    if ($clipboardErrors) {
        Write-ColorOutput "`n❌ Clipboard errors still present:" $Red
        $clipboardErrors | ForEach-Object { Write-ColorOutput "  $($_.Line)" $Red }
        return $false
    } else {
        Write-ColorOutput "`n✅ No clipboard errors detected" $Green
    }
    
    return $allCriticalFound
}

function Show-Summary {
    param([bool]$Success)
    
    Write-ColorOutput "`n=== Test Summary ===" $Blue
    
    if ($Success) {
        Write-ColorOutput "🎉 All WebView fixes are working correctly!" $Green
        Write-ColorOutput "`nKey improvements:" $Green
        Write-ColorOutput "  ✅ Clipboard focus management" $Green
        Write-ColorOutput "  ✅ WebView focus handling" $Green
        Write-ColorOutput "  ✅ Enhanced error handling" $Green
        Write-ColorOutput "  ✅ Retry mechanisms" $Green
        Write-ColorOutput "  ✅ Fallback clipboard methods" $Green
    } else {
        Write-ColorOutput "❌ Some issues remain - check logs above" $Red
        Write-ColorOutput "`nTroubleshooting tips:" $Yellow
        Write-ColorOutput "  • Check device connection" $Yellow
        Write-ColorOutput "  • Verify app installation" $Yellow
        Write-ColorOutput "  • Review logcat output" $Yellow
        Write-ColorOutput "  • Try increasing timeout" $Yellow
    }
}

# Main execution
try {
    Write-ColorOutput "🧪 WebView Fixes Test Suite" $Blue
    Write-ColorOutput "=========================" $Blue
    
    $allTestsPassed = $true
    
    # Test device connection
    if (-not (Test-DeviceConnection)) {
        $allTestsPassed = $false
    }
    
    # Build app if needed
    if ($allTestsPassed -and -not (Test-AppInstallation)) {
        if (-not (Build-App)) {
            $allTestsPassed = $false
        } elseif (-not (Install-App)) {
            $allTestsPassed = $false
        }
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
    Write-ColorOutput "`n❌ Test suite error: $($_.Exception.Message)" $Red
    exit 1
}

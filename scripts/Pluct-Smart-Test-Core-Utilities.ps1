# Pluct Smart Test Core Utilities - Single source of truth for testing utilities
# Follows naming convention: [Project]-[ParentScope]-[ChildScope]-[CoreResponsibility]

function Write-SmartLog {
    param(
        [string]$Message,
        [string]$Color = "White"
    )
    
    $timestamp = Get-Date -Format "HH:mm:ss"
    $colorMap = @{
        "Red" = "Red"
        "Green" = "Green"
        "Yellow" = "Yellow"
        "Cyan" = "Cyan"
        "Gray" = "Gray"
        "White" = "White"
    }
    
    $actualColor = $colorMap[$Color]
    if (-not $actualColor) { $actualColor = "White" }
    
    Write-Host "[$timestamp] $Message" -ForegroundColor $actualColor
}

function Test-SmartAndroidDevice {
    try {
        $deviceInfo = adb devices 2>&1
        $deviceCount = ($deviceInfo | Select-String "device").Count
        return $deviceCount -gt 0
    } catch {
        return $false
    }
}

function Report-SmartCriticalError {
    param(
        [string]$ErrorType,
        [string]$ErrorMessage
    )
    
    $script:SmartTestSession.CriticalErrors += @{
        Type = $ErrorType
        Message = $ErrorMessage
        Timestamp = Get-Date
    }
    
    Write-SmartLog "CRITICAL ERROR: $ErrorType" "Red"
    Write-SmartLog "Details: $ErrorMessage" "Red"
}

function Show-SmartTestReport {
    param([bool]$OverallSuccess)
    
    $duration = (Get-Date) - $script:SmartTestSession.StartTime
    $durationFormatted = "{0:F2}" -f $duration.TotalSeconds
    
    Write-SmartLog "=== SMART TEST REPORT ===" "Cyan"
    Write-SmartLog "Duration: $durationFormatted seconds" "White"
    Write-SmartLog "Test URL: $($script:SmartTestSession.TestUrl)" "White"
    Write-SmartLog "Build Required: $($script:SmartTestSession.BuildRequired)" "White"
    
    if ($script:SmartTestSession.SmartBuildDetection.BuildReason) {
        Write-SmartLog "Build Reason: $($script:SmartTestSession.SmartBuildDetection.BuildReason)" "Gray"
    }
    
    if ($script:SmartTestSession.CriticalErrors.Count -gt 0) {
        Write-SmartLog "Critical Errors: $($script:SmartTestSession.CriticalErrors.Count)" "Red"
        foreach ($error in $script:SmartTestSession.CriticalErrors) {
            Write-SmartLog "  - $($error.Type): $($error.Message)" "Red"
        }
    }
    
    if ($OverallSuccess) {
        Write-SmartLog "✅ All smart tests passed successfully" "Green"
    } else {
        Write-SmartLog "❌ Smart tests failed" "Red"
    }
    
    Write-SmartLog "Smart testing completed" "Cyan"
}

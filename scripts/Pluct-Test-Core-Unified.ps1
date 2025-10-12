# Pluct Test Core Unified
# Single source of truth for all testing logic

function Test-UnifiedJourney {
    param(
        [string]$TestName,
        [string[]]$LogPatterns,
        [string[]]$ScreenValidations,
        [scriptblock]$UserActions,
        [int]$TimeoutSeconds = 30
    )
    
    Write-Log "=== Testing $TestName ===" "Cyan"
    
    $success = $true
    $step = 1
    
    try {
        # Step 1: Clear and prepare
        Write-Log "Step ${step}: Preparing test environment..." "Yellow"
        Clear-AppData
        adb logcat -c
        $logcatProcess = Start-LogcatMonitor
        $step++
        
        # Step 2: Send share intent
        Write-Log "Step ${step}: Sending share intent..." "Yellow"
        Send-ShareIntent -Url $script:TestUrl
        $step++
        
        # Step 3: Wait for log patterns
        foreach ($pattern in $LogPatterns) {
            Write-Log "Step ${step}: Waiting for pattern: $pattern" "Yellow"
            $found = Wait-ForLog -Pattern $pattern -TimeoutSeconds $TimeoutSeconds -Description $pattern -ShowLogs
            if (-not $found) {
                Write-Log "❌ FAIL: Pattern '$pattern' not found" "Red"
                $success = $false
                break
            }
            $step++
        }
        
        # Step 4: Execute user actions
        if ($success -and $UserActions) {
            Write-Log "Step ${step}: Executing user actions..." "Yellow"
            & $UserActions
            $step++
        }
        
        # Step 5: Validate screen content
        foreach ($validation in $ScreenValidations) {
            Write-Log "Step ${step}: Validating screen: $validation" "Yellow"
            $valid = Test-ScreenContent -ExpectedText $validation -Context $validation
            if (-not $valid) {
                Write-Log "❌ FAIL: Screen validation '$validation' failed" "Red"
                $success = $false
                break
            }
            $step++
        }
        
        if ($success) {
            Write-Log "✅ $TestName SUCCESSFUL" "Green"
        } else {
            Write-Log "❌ $TestName FAILED" "Red"
        }
        
        return $success
        
    } finally {
        Stop-LogcatMonitor -Process $logcatProcess
    }
}

function Test-IntentJourney {
    return Test-UnifiedJourney -TestName "Intent Journey" -LogPatterns @(
        "ShareIngestActivity.*onCreate",
        "CAPTURE_INSIGHT detected",
        "Capture request set in ViewModel"
    ) -ScreenValidations @("MainActivity")
}

function Test-CaptureJourney {
    return Test-UnifiedJourney -TestName "Capture Journey" -LogPatterns @(
        "Displaying capture sheet for URL",
        "setCaptureRequest"
    ) -ScreenValidations @("Capture This Insight") -UserActions {
        Test-BottomSheetExpansion
    }
}

function Test-CompleteJourney {
    return Test-UnifiedJourney -TestName "Complete Journey" -LogPatterns @(
        "Displaying capture sheet for URL",
        "Tier selected",
        "enqueueTranscriptionWork"
    ) -ScreenValidations @("Capture This Insight", "Quick Scan", "AI Analysis") -UserActions {
        Test-BottomSheetExpansion
        Simulate-TierSelection -Tier "Quick Scan"
    }
}

function Test-EnhancementsJourney {
    return Test-UnifiedJourney -TestName "Enhancements Journey" -LogPatterns @(
        "Displaying capture sheet for URL",
        "CoinManager|Pluct Coins",
        "AsyncImage|Coil",
        "Toast|toast message",
        "ErrorHandler|executeWithRetry"
    ) -ScreenValidations @("Capture This Insight", "Pluct Coins") -UserActions {
        Test-BottomSheetExpansion
        Simulate-TierSelection -Tier "Quick Scan"
    }
}

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
    $errorDetails = @()
    $logcatProcess = $null
    
    try {
        # Step 1: Clear and prepare
        Write-Log "Step ${step}: Preparing test environment..." "Yellow"
        try {
            Clear-AppData
            adb logcat -c
            $logcatProcess = Start-LogcatMonitor
        } catch {
            $errorDetails += "Failed to prepare test environment: $($_.Exception.Message)"
            Write-Log "❌ ERROR: Failed to prepare test environment: $($_.Exception.Message)" "Red"
            $success = $false
            throw
        }
        $step++
        
        # Step 2: Send share intent
        Write-Log "Step ${step}: Sending share intent..." "Yellow"
        try {
            Send-ShareIntent -Url $script:TestSession.TestUrl
        } catch {
            $errorDetails += "Failed to send share intent: $($_.Exception.Message)"
            Write-Log "❌ ERROR: Failed to send share intent: $($_.Exception.Message)" "Red"
            $success = $false
            throw
        }
        $step++
        
        # Step 3: Wait for log patterns
        foreach ($pattern in $LogPatterns) {
            Write-Log "Step ${step}: Waiting for pattern: $pattern" "Yellow"
            try {
                $found = Wait-ForLog -Pattern $pattern -TimeoutSeconds $TimeoutSeconds -Description $pattern -ShowLogs
                if (-not $found) {
                    $errorDetails += "Pattern '$pattern' not found within $TimeoutSeconds seconds"
                    Write-Log "❌ FAIL: Pattern '$pattern' not found" "Red"
                    $success = $false
                    break
                }
            } catch {
                $errorDetails += "Error waiting for pattern '$pattern': $($_.Exception.Message)"
                Write-Log "❌ ERROR: Failed to wait for pattern '$pattern': $($_.Exception.Message)" "Red"
                $success = $false
                break
            }
            $step++
        }
        
        # Step 4: Execute user actions
        if ($success -and $UserActions) {
            Write-Log "Step ${step}: Executing user actions..." "Yellow"
            try {
                & $UserActions
            } catch {
                $errorDetails += "Failed to execute user actions: $($_.Exception.Message)"
                Write-Log "❌ ERROR: User actions failed: $($_.Exception.Message)" "Red"
                $success = $false
            }
            $step++
        }
        
        # Step 5: Validate screen content
        if ($success -and $ScreenValidations.Count -gt 0) {
            foreach ($validation in $ScreenValidations) {
                Write-Log "Step ${step}: Validating screen: $validation" "Yellow"
                try {
                    $valid = Test-ScreenContent -ExpectedText $validation -Context $validation
                    if (-not $valid) {
                        $errorDetails += "Screen validation '$validation' failed - expected text not found"
                        Write-Log "❌ FAIL: Screen validation '$validation' failed" "Red"
                        $success = $false
                        break
                    }
                } catch {
                    $errorDetails += "Error validating screen '$validation': $($_.Exception.Message)"
                    Write-Log "❌ ERROR: Screen validation '$validation' failed: $($_.Exception.Message)" "Red"
                    $success = $false
                    break
                }
                $step++
            }
        }
        
    } catch {
        $errorDetails += "Critical error during $TestName`: $($_.Exception.Message)"
        Write-Log "❌ CRITICAL ERROR: $($_.Exception.Message)" "Red"
        $success = $false
    } finally {
        if ($logcatProcess) {
            try {
                Stop-LogcatMonitor -Process $logcatProcess
            } catch {
                Write-Log "Warning: Failed to stop logcat monitor" "Yellow"
            }
        }
    }

    # Detailed error reporting and termination
    if (-not $success) {
        Write-Log "❌ $TestName FAILED" "Red"
        Write-Log "Error Details:" "Red"
        foreach ($error in $errorDetails) {
            Write-Log "  - $error" "Red"
        }
        
        # Terminate on first error as requested
        Write-Log "TERMINATING TEST EXECUTION DUE TO FAILURE" "Red"
        Write-Log "Please fix the issues above and re-run the test" "Red"
        exit 1
    } else {
        Write-Log "✅ $TestName SUCCESSFUL" "Green"
    }
    
    $script:TestSession.TestResults[$TestName] = $success
    return $success
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
        "Displaying capture sheet for URL"
    ) -ScreenValidations @() -UserActions {
        # Wait for the capture sheet to be fully displayed
        Start-Sleep -Seconds 3
        
        # Show all visible elements before attempting interaction
        Write-Log "Analyzing screen before interaction..." "Yellow"
        Show-AllVisibleElements
        
        # First, expand the bottom sheet to show tier options
        Write-Log "Expanding bottom sheet to show tier options..." "Yellow"
        Test-BottomSheetExpansion
        
        # Wait for the sheet to fully expand
        Start-Sleep -Seconds 2
        
        # Simulate a tap on the "Quick Scan" button
        Write-Log "Simulating tap on 'Quick Scan' button..." "Yellow"
        $tapSuccess = Simulate-TapByText -Text "Quick Scan" -Context "Select Quick Scan tier"
        if (-not $tapSuccess) {
            Write-Log "❌ Failed to tap Quick Scan button" "Red"
            throw "Failed to interact with Quick Scan button"
        }
        
        Start-Sleep -Seconds 3 # Give time for action to process and sheet to dismiss
        
        # Now wait for the tier selection log
        Write-Log "Waiting for tier selection confirmation..." "Yellow"
        $tierSelected = Wait-ForLog -Pattern "Tier selected" -TimeoutSeconds 10 -Description "Tier selection confirmation"
        if (-not $tierSelected) {
            Write-Log "❌ Tier selection not confirmed" "Red"
            throw "Tier selection not confirmed"
        }
        
        # Wait for video creation
        Write-Log "Waiting for video creation..." "Yellow"
        $videoCreated = Wait-ForLog -Pattern "Video created with ID" -TimeoutSeconds 10 -Description "Video creation confirmation"
        if (-not $videoCreated) {
            Write-Log "❌ Video creation not confirmed" "Red"
            throw "Video creation not confirmed"
        }
    }
}

function Test-CompleteJourney {
    return Test-UnifiedJourney -TestName "Complete Journey" -LogPatterns @(
        "Displaying capture sheet for URL",
        "Tier selected",
        "enqueueTranscriptionWork"
    ) -ScreenValidations @() -UserActions {
        # First, expand the bottom sheet to show tier options
        Write-Log "Expanding bottom sheet to show tier options..." "Yellow"
        Test-BottomSheetExpansion
        
        # Wait for the sheet to fully expand
        Start-Sleep -Seconds 2
        
        # Simulate a tap on the "Quick Scan" button
        Write-Log "Simulating tap on 'Quick Scan' button..." "Yellow"
        $tapSuccess = Simulate-TapByText -Text "Quick Scan" -Context "Select Quick Scan tier"
        if (-not $tapSuccess) {
            Write-Log "❌ Failed to tap Quick Scan button" "Red"
            throw "Failed to interact with Quick Scan button"
        }
        
        Start-Sleep -Seconds 3 # Give time for action to process and sheet to dismiss
    }
}

function Test-EnhancementsJourney {
    return Test-UnifiedJourney -TestName "Enhancements Journey" -LogPatterns @(
        "Displaying capture sheet for URL",
        "Tier selected",
        "Video created with ID"
    ) -ScreenValidations @() -UserActions {
        # First, expand the bottom sheet to show tier options
        Write-Log "Expanding bottom sheet to show tier options..." "Yellow"
        Test-BottomSheetExpansion
        
        # Wait for the sheet to fully expand
        Start-Sleep -Seconds 2
        
        # Simulate a tap on the "Quick Scan" button
        Write-Log "Simulating tap on 'Quick Scan' button..." "Yellow"
        $tapSuccess = Simulate-TapByText -Text "Quick Scan" -Context "Select Quick Scan tier"
        if (-not $tapSuccess) {
            Write-Log "❌ Failed to tap Quick Scan button" "Red"
            throw "Failed to interact with Quick Scan button"
        }
        
        Start-Sleep -Seconds 3 # Give time for action to process and sheet to dismiss
    }
}

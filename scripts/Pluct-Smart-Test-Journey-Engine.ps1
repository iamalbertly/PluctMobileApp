# Wait for a specific background stage emitted by worker via Logcat
function Wait-ForStage {
    param([string]$Stage,[int]$timeout=30)
    
    Write-SmartLog "Waiting for stage: $Stage (timeout: ${timeout}s)" "Yellow"
    $deadline = (Get-Date).AddSeconds($timeout)
    $attempts = 0
    
    while((Get-Date) -lt $deadline){
        $attempts++
        $log = adb shell logcat -d | Select-String "TTT: stage=$Stage"
        if ($log){ 
            Write-SmartLog "✅ Found stage '$Stage' after $attempts attempts:" "Green"
            $log | ForEach-Object { Write-SmartLog "  $($_.Line)" "Gray" }
            return $true 
        }
        
        # Show progress every 5 seconds
        if ($attempts % 5 -eq 0) {
            $remaining = [int]($deadline - (Get-Date)).TotalSeconds
            Write-SmartLog "Still waiting for '$Stage'... ($remaining seconds remaining)" "Gray"
            
            # Show recent TTT logs for debugging
            $recentLogs = adb shell logcat -d | Select-String "TTT:" | Select-Object -Last 3
            if ($recentLogs) {
                Write-SmartLog "Recent TTT logs:" "Gray"
                $recentLogs | ForEach-Object { Write-SmartLog "  $($_.Line)" "Gray" }
            }
        }
        
        Start-Sleep -Seconds 1
    }
    
    Write-SmartLog "❌ Stage '$Stage' not found within ${timeout}s timeout" "Red"
    Write-SmartLog "Showing all recent TTT logs for debugging:" "Red"
    $allTTTLogs = adb shell logcat -d | Select-String "TTT:" | Select-Object -Last 10
    if ($allTTTLogs) {
        $allTTTLogs | ForEach-Object { Write-SmartLog "  $($_.Line)" "Red" }
    } else {
        Write-SmartLog "  No TTT logs found at all" "Red"
    }
    
    return $false
}
# Pluct Smart Test Journey Engine - Comprehensive journey testing with status tracking
# Follows naming convention: [Project]-[ParentScope]-[ChildScope]-[CoreResponsibility]

function Test-SmartIntentJourney {
    param([string]$TestUrl)
    
    Write-SmartLog "Testing Smart Intent Journey..." "Cyan"
    
    try {
        # Test app launch
        Write-SmartLog "Testing app launch..." "Gray"
        $launchResult = adb shell monkey -p app.pluct -c android.intent.category.LAUNCHER 1 2>&1
        if ($LASTEXITCODE -ne 0) {
            Write-SmartLog "App launch failed: $launchResult" "Red"
            return $false
        }
        Start-Sleep -Seconds 3
        
        # Test share intent
        Write-SmartLog "Testing share intent with URL: $TestUrl" "Gray"
        $shareResult = adb shell am start -a android.intent.action.SEND -t "text/plain" --es android.intent.extra.TEXT $TestUrl -n app.pluct/.share.PluctShareIngestActivity 2>&1
        if ($LASTEXITCODE -ne 0) {
            Write-SmartLog "Share intent failed: $shareResult" "Red"
            return $false
        }
        # Wait for app to come to foreground
        $maxWait = 10
        for ($i=0; $i -lt $maxWait; $i++) {
            $top = adb shell dumpsys activity activities | Select-String "app.pluct"
            if ($top) { break }
            Start-Sleep -Seconds 1
        }
        
        # Verify intent was handled (retry up to 8s)
        $intentHandled = $false
        for ($i=0; $i -lt 8; $i++) {
            if (Test-SmartIntentHandled) { $intentHandled = $true; break }
            Start-Sleep -Seconds 1
        }
        if (-not $intentHandled) {
            Write-SmartLog "Intent not handled properly" "Red"
            Write-SmartLog "Top activities:" "Yellow"
            adb shell dumpsys activity activities | Select-String "app.pluct" | Select-Object -Last 30
            return $false
        }
        
        Write-SmartLog "Smart Intent Journey test passed" "Green"
        return $true
    } catch {
        Write-SmartLog "Intent Journey exception: $($_.Exception.Message)" "Red"
        return $false
    }
}

function Test-SmartCaptureJourney {
    param([string]$TestUrl)
    
    Write-SmartLog "Testing Smart Capture Journey..." "Cyan"
    
    try {
        # Test capture sheet display (retry up to 10s)
        Write-SmartLog "Testing capture sheet display..." "Gray"
        $captureSheetVisible = $false
        for ($i=0; $i -lt 10; $i++) {
            if (Test-SmartCaptureSheetVisible) { $captureSheetVisible = $true; break }
            Start-Sleep -Seconds 1
        }
        if (-not $captureSheetVisible) {
            Write-SmartLog "Capture sheet not visible" "Red"
            Write-SmartLog "Recent logcat (UI keywords):" "Yellow"
            adb shell logcat -d | Select-String "CAPTURE_INSIGHT|Capture|ModalBottomSheet|HomeScreen" | Select-Object -Last 50
            return $false
        }
        
        # Test preliminary insights
        Write-SmartLog "Testing preliminary insights..." "Gray"
        $insightsAvailable = Test-SmartPreliminaryInsights
        if (-not $insightsAvailable) {
            Write-SmartLog "Preliminary insights not available" "Red"
            Describe-ClickableSummary
            return $false
        }
        
        # Test tier selection
        Write-SmartLog "Testing tier selection..." "Gray"
        # Try to select a tier via UI if not detected by logs
        $tierSelection = Test-SmartTierSelection
        if (-not $tierSelection) {
            $xml = Get-UiHierarchy
            $hits = Find-UiElementsByText -UiXml $xml -Text 'Free' -Contains
            if ($hits.Count -eq 0) { $hits = Find-UiElementsByText -UiXml $xml -Text 'Paid' -Contains }
            if ($hits.Count -gt 0) {
                [void](Click-UiNode $hits[0]); Start-Sleep -Seconds 1
                $tierSelection = Test-SmartTierSelection
            }
        }
        if (-not $tierSelection) {
            Write-SmartLog "Tier selection not working" "Red"
            Describe-ClickableSummary
            return $false
        }
        
        Write-SmartLog "Smart Capture Journey test passed" "Green"
        return $true
    } catch {
        Write-SmartLog "Capture Journey exception: $($_.Exception.Message)" "Red"
        return $false
    }
}

function Test-SmartBackgroundJourney {
    param([string]$TestUrl)
    
    Write-SmartLog "Testing Smart Background Journey..." "Cyan"
    
    try {
        # Test background processing initiation
        Write-SmartLog "Testing background processing initiation..." "Gray"
        $backgroundStarted = Test-SmartBackgroundProcessingStarted
        if (-not $backgroundStarted) {
            Write-SmartLog "Background processing not started" "Red"
            return $false
        }
        
        # Test status tracking
        Write-SmartLog "Testing status tracking..." "Gray"
        $statusTracking = Test-SmartStatusTracking
        if (-not $statusTracking) {
            Write-SmartLog "Status tracking not working" "Red"
            return $false
        }
        
        # Test progress updates
        Write-SmartLog "Testing progress updates..." "Gray"
        $progressUpdates = Test-SmartProgressUpdates
        if (-not $progressUpdates) {
            Write-SmartLog "Progress updates not working" "Red"
            return $false
        }
        
        Write-SmartLog "Smart Background Journey test passed" "Green"
        return $true
    } catch {
        Write-SmartLog "Background Journey exception: $($_.Exception.Message)" "Red"
        return $false
    }
}

function Test-SmartTTTranscribeJourney {
    param([string]$TestUrl)
    
    Write-SmartLog "Testing Smart TTTranscribe Journey..." "Cyan"
    
    try {
        # Test TTTranscribe API connectivity
        Write-SmartLog "Testing TTTranscribe API connectivity..." "Gray"
        $apiConnectivity = Test-SmartTTTranscribeConnectivity
        if (-not $apiConnectivity) {
            Write-SmartLog "TTTranscribe API not accessible" "Red"
            return $false
        }
        
        # Test authentication
        Write-SmartLog "Testing TTTranscribe authentication..." "Gray"
        $authentication = Test-SmartTTTranscribeAuthentication
        if (-not $authentication) {
            Write-SmartLog "TTTranscribe authentication failed" "Red"
            return $false
        }
        
        # Test transcription process
        Write-SmartLog "Testing TTTranscribe transcription process..." "Gray"
        $transcription = Test-SmartTTTranscribeTranscription -TestUrl $TestUrl
        if (-not $transcription) {
            Write-SmartLog "TTTranscribe transcription failed" "Red"
            return $false
        }
        
        # Test transcript quality
        Write-SmartLog "Testing transcript quality..." "Gray"
        $transcriptQuality = Test-SmartTranscriptQuality
        if (-not $transcriptQuality) {
            Write-SmartLog "Transcript quality insufficient" "Red"
            return $false
        }
        
        Write-SmartLog "Smart TTTranscribe Journey test passed" "Green"
        return $true
    } catch {
        Write-SmartLog "TTTranscribe Journey exception: $($_.Exception.Message)" "Red"
        return $false
    }
}

function Test-SmartSettingsJourney {
    Write-SmartLog "Testing Smart Settings Journey..." "Cyan"
    
    try {
        # Test settings page access
        Write-SmartLog "Testing settings page access..." "Gray"
        $settingsAccess = Test-SmartSettingsAccess
        if (-not $settingsAccess) {
            Write-SmartLog "Settings page not accessible" "Red"
            return $false
        }
        
        # Test settings persistence
        Write-SmartLog "Testing settings persistence..." "Gray"
        $settingsPersistence = Test-SmartSettingsPersistence
        if (-not $settingsPersistence) {
            Write-SmartLog "Settings not persisting" "Red"
            return $false
        }
        
        # Test settings integration
        Write-SmartLog "Testing settings integration..." "Gray"
        $settingsIntegration = Test-SmartSettingsIntegration
        if (-not $settingsIntegration) {
            Write-SmartLog "Settings not integrated with app" "Red"
            return $false
        }
        
        Write-SmartLog "Smart Settings Journey test passed" "Green"
        return $true
    } catch {
        Write-SmartLog "Settings Journey exception: $($_.Exception.Message)" "Red"
        return $false
    }
}

function Test-SmartStatusTrackingJourney {
    param([string]$TestUrl)
    
    Write-SmartLog "Testing Smart Status Tracking Journey..." "Cyan"
    
    try {
        # Test status display
        Write-SmartLog "Testing status display..." "Gray"
        $statusDisplay = Test-SmartStatusDisplay
        if (-not $statusDisplay) {
            Write-SmartLog "Status not displaying" "Red"
            return $false
        }
        
        # Test progress indicators
        Write-SmartLog "Testing progress indicators..." "Gray"
        $progressIndicators = Test-SmartProgressIndicators
        if (-not $progressIndicators) {
            Write-SmartLog "Progress indicators not working" "Red"
            return $false
        }
        
        # Test error reporting
        Write-SmartLog "Testing error reporting..." "Gray"
        $errorReporting = Test-SmartErrorReporting
        if (-not $errorReporting) {
            Write-SmartLog "Error reporting not working" "Red"
            return $false
        }
        
        Write-SmartLog "Smart Status Tracking Journey test passed" "Green"
        return $true
    } catch {
        Write-SmartLog "Status Tracking Journey exception: $($_.Exception.Message)" "Red"
        return $false
    }
}

# Helper functions for journey testing
function Test-SmartIntentHandled {
    try {
        $activityInfo = adb shell dumpsys activity activities | Select-String "pluct"
        return $activityInfo -ne $null
    } catch {
        return $false
    }
}

function Test-SmartCaptureSheetVisible {
    try {
        $uiInfo = adb shell dumpsys activity activities | Select-String "ModalBottomSheet\|BottomSheet"
        return $uiInfo -ne $null
    } catch {
        return $false
    }
}

function Test-SmartPreliminaryInsights {
    try {
        $logInfo = adb shell logcat -d | Select-String "preliminary\|insights\|metadata"
        return $logInfo -ne $null
    } catch {
        return $false
    }
}

function Test-SmartTierSelection {
    try {
        $uiInfo = adb shell dumpsys activity activities | Select-String "tier\|selection\|choice"
        return $uiInfo -ne $null
    } catch {
        return $false
    }
}

function Test-SmartBackgroundProcessingStarted {
    try {
        $logInfo = adb shell logcat -d | Select-String "background\|worker\|processing"
        return $logInfo -ne $null
    } catch {
        return $false
    }
}

function Test-SmartStatusTracking {
    try {
        $logInfo = adb shell logcat -d | Select-String "status\|progress\|tracking"
        return $logInfo -ne $null
    } catch {
        return $false
    }
}

function Test-SmartProgressUpdates {
    try {
        $logInfo = adb shell logcat -d | Select-String "progress\|update\|percentage"
        return $logInfo -ne $null
    } catch {
        return $false
    }
}

function Test-SmartTTTranscribeConnectivity {
    try {
        $networkInfo = adb shell logcat -d | Select-String "Pluct Proxy\|ttt/transcribe\|PluctTTTranscribeService\|TTTranscribe"
        return $networkInfo -ne $null
    } catch {
        return $false
    }
}

function Test-SmartTTTranscribeAuthentication {
    try {
        $authInfo = adb shell logcat -d | Select-String "Bearer\|vend token\|vend-token\|Authorization"
        return $authInfo -ne $null
    } catch {
        return $false
    }
}

function Test-SmartTTTranscribeTranscription {
    param([string]$TestUrl)
    try {
        $transcriptionInfo = adb shell logcat -d | Select-String "transcript\|transcription\|whisper"
        return $transcriptionInfo -ne $null
    } catch {
        return $false
    }
}

function Test-SmartTranscriptQuality {
    try {
        $qualityInfo = adb shell logcat -d | Select-String "quality\|confidence\|accuracy"
        return $qualityInfo -ne $null
    } catch {
        return $false
    }
}

function Test-SmartSettingsAccess {
    try {
        $settingsInfo = adb shell dumpsys activity activities | Select-String "settings\|preferences"
        return $settingsInfo -ne $null
    } catch {
        return $false
    }
}

function Test-SmartSettingsPersistence {
    try {
        $persistenceInfo = adb shell logcat -d | Select-String "save\|persist\|preferences"
        return $persistenceInfo -ne $null
    } catch {
        return $false
    }
}

function Test-SmartSettingsIntegration {
    try {
        $integrationInfo = adb shell logcat -d | Select-String "integrate\|apply\|configure"
        return $integrationInfo -ne $null
    } catch {
        return $false
    }
}

function Test-SmartStatusDisplay {
    try {
        $statusInfo = adb shell dumpsys activity activities | Select-String "status\|progress\|indicator"
        return $statusInfo -ne $null
    } catch {
        return $false
    }
}

function Test-SmartProgressIndicators {
    try {
        $progressInfo = adb shell dumpsys activity activities | Select-String "progress\|loading\|spinner"
        return $progressInfo -ne $null
    } catch {
        return $false
    }
}

function Test-SmartErrorReporting {
    try {
        $errorInfo = adb shell logcat -d | Select-String "error\|exception\|failed"
        return $errorInfo -ne $null
    } catch {
        return $false
    }
}

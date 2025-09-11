# ClipForge Test Framework - TikTok Tests Module
# Contains tests specific to TikTok URL processing

# Function to test TikTok URL processing end-to-end
function Test-TokAuditE2E {
    param(
        [string]$TestUrl,
        [switch]$CaptureHTML
    )
    
    # Validate device connection
    if (-not (Test-AndroidDevice)) {
        Write-Log "No Android device connected, cannot run test" "Red"
        return $false
    }
    
    # Clear logs before starting
    Clear-Logs
    
    # Validate URL
    $validatedUrl = Test-ValidUrl -Url $TestUrl
    if (-not $validatedUrl) {
        Write-Log "Invalid URL format: $TestUrl" "Red"
        return $false
    }
    $TestUrl = $validatedUrl
    
    # Launch the app with the URL
    $launchSuccess = Launch-AppWithUrl -Url $TestUrl
    if (-not $launchSuccess) {
        Write-Log "Failed to launch app with URL: $TestUrl" "Red"
        return $false
    }
    
    # Wait for transcript extraction marker in logs
    $transcriptSuccess = Wait-ForLog -Pattern $script:TERMINAL_OUTCOMES.Success -TimeoutSeconds $script:TIMEOUTS.Long -Description "Transcript extraction success" -Verbose
    
    # If we didn't find success marker, check for error markers
    if (-not $transcriptSuccess) {
        # Check for known error patterns
        foreach ($errorType in $script:TERMINAL_OUTCOMES.Keys | Where-Object { $_ -ne "Success" }) {
            $errorPattern = $script:TERMINAL_OUTCOMES[$errorType]
            $errorFound = Wait-ForLog -Pattern $errorPattern -TimeoutSeconds 5 -Description "Error: $errorType" -Verbose
            
            if ($errorFound) {
                Write-Log "Test failed due to error: $errorType" "Red"
                return $false
            }
        }
        
        # If no specific error was found, report generic failure
        Write-Log "Test failed: Transcript extraction not detected" "Red"
        return $false
    }
    
    # Capture HTML if requested
    if ($CaptureHTML) {
        Capture-WebViewHtml -CaptureHTML -TestName "TikTok_E2E"
    }
    
    Write-Log "TikTok URL processing test completed successfully" "Green"
    return $true
}

# Function to test error handling with invalid URL
function Test-ErrorHandling {
    param(
        [string]$InvalidUrl = "https://invalid.url",
        [switch]$CaptureHTML
    )
    
    # Test step: Error - Invalid URL
    return Test-Step -Name "Error - Invalid URL" -Action {
        # Launch the app with an invalid URL
        Launch-AppWithUrl -Url $InvalidUrl -ValidateUrl:$false
        
        # Wait for error dialog or message in logs
        $errorFound = Wait-ForLog -Pattern $script:TERMINAL_OUTCOMES.Error -TimeoutSeconds $script:TIMEOUTS.Medium -Description "Error dialog or message" -Verbose
        
        # Check specifically for invalid URL error
        $invalidUrlError = Wait-ForLog -Pattern $script:TERMINAL_OUTCOMES.InvalidUrl -TimeoutSeconds 5 -Description "Invalid URL error" -Verbose
        
        return $errorFound -or $invalidUrlError
    } -CaptureHTML:$CaptureHTML -Critical
}

# Function to test WebView functionality
function Test-WebViewFunctionality {
    param(
        [string]$TestUrl,
        [switch]$CaptureHTML
    )
    
    # Test WebView page load
    $pageLoadSuccess = Test-Step -Name "WebView - Page Load" -Action {
        # Launch the app with the URL
        Launch-AppWithUrl -Url $TestUrl
        
        # Wait for page load indicator in logs
        return Wait-ForLog -Pattern "Page loaded successfully" -TimeoutSeconds $script:TIMEOUTS.Medium -Description "WebView page load" -Verbose
    } -CaptureHTML:$CaptureHTML -Critical
    
    if (-not $pageLoadSuccess) {
        return $false
    }
    
    # Test WebView JavaScript injection
    return Test-Step -Name "WebView - JavaScript Injection" -Action {
        # Wait for script injection indicator in logs
        return Wait-ForLog -Pattern "Script injected successfully" -TimeoutSeconds $script:TIMEOUTS.Medium -Description "JavaScript injection" -Verbose
    } -CaptureHTML:$CaptureHTML
}

# Function to test transcript extraction
function Test-TranscriptExtraction {
    param(
        [string]$TestUrl,
        [switch]$CaptureHTML
    )
    
    return Test-Step -Name "Transcript Extraction" -Action {
        # Launch the app with the URL
        Launch-AppWithUrl -Url $TestUrl
        
        # Wait for transcript extraction marker in logs
        $extractionSuccess = Wait-ForLog -Pattern $script:TERMINAL_OUTCOMES.Success -TimeoutSeconds $script:TIMEOUTS.Long -Description "Transcript extraction" -Verbose
        
        if (-not $extractionSuccess) {
            return $false
        }
        
        # Verify that transcript data is present in logs
        $transcriptData = Wait-ForLog -Pattern "Transcript data:" -TimeoutSeconds 5 -Description "Transcript data in logs" -Verbose
        
        return $transcriptData
    } -CaptureHTML:$CaptureHTML -Critical
}

# Export functions
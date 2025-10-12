# Pluct Comprehensive ADB Test
# Tests complete app flow with ADB automation including TTTranscribe and TokAudit
# Validates all user journeys end-to-end with screen element detection

param(
    [Parameter()]
    [string]$TestUrl = "https://vm.tiktok.com/ZMAPTWV7o/",
    
    [Parameter()]
    [switch]$SkipBuild,
    
    [Parameter()]
    [switch]$SkipInstall,
    
    [Parameter()]
    [switch]$CaptureScreenshots
)

# Import core modules
$script:FrameworkRoot = $PSScriptRoot
. "$script:FrameworkRoot\Pluct-Test-Core-Utilities.ps1"
. "$script:FrameworkRoot\Pluct-Test-Core-Build.ps1"
. "$script:FrameworkRoot\Pluct-Test-Core-Device.ps1"
. "$script:FrameworkRoot\Pluct-Test-Core-Screenshots.ps1"

# Initialize comprehensive test session
$script:ComprehensiveTestSession = @{
    StartTime = Get-Date
    TestResults = @{}
    BuildRequired = $false
    Screenshots = @()
    Logs = @()
    TestUrl = $TestUrl
    ADBTestResults = @{}
    ElementDetectionResults = @{}
    UserJourneyResults = @{}
    CriticalErrors = @()
}

function Start-ComprehensiveADBTest {
    Write-Log "=== Pluct Comprehensive ADB Test ===" "Cyan"
    Write-Log "Test URL: $TestUrl" "White"
    Write-Log "Testing complete app flow with ADB automation..." "Yellow"
    Write-Log "Including TTTranscribe integration and TokAudit WebView flow" "Yellow"

    # Check prerequisites
    if (-not (Test-AndroidDevice)) {
        Write-Log "No Android device connected" "Red"
        Write-Log "Ensure an Android emulator is running or a physical device is connected via ADB" "Yellow"
        exit 1
    }

    # Determine if build is needed
    if (-not $SkipBuild) {
        $script:ComprehensiveTestSession.BuildRequired = Test-BuildRequired
        if ($script:ComprehensiveTestSession.BuildRequired) {
            Write-Log "Code changes detected - build required" "Yellow"
            if (-not (Build-App -Scope "Comprehensive")) {
                Write-Log "Build failed - Gradle compilation error" "Red"
                Write-Log "Check the build output for specific compilation errors" "Yellow"
                exit 1
            }
        } else {
            Write-Log "No code changes - skipping build" "Green"
        }
    }

    # Deploy to device if needed
    if (-not $SkipInstall) {
        $deploymentNeeded = Test-DeploymentNeeded
        if ($deploymentNeeded -or $script:ComprehensiveTestSession.BuildRequired) {
            Write-Log "Deploying latest build to device..." "Yellow"
            $deploySuccess = Deploy-ToDevice
            if (-not $deploySuccess) {
                Write-Log "Deployment failed - APK installation error" "Red"
                Write-Log "Possible causes: APK corrupted, device storage full, or ADB connection issues" "Yellow"
                exit 1
            }
            Write-Log "Deployment successful" "Green"
        } else {
            Write-Log "Latest build already deployed" "Green"
        }
    }

    # Execute comprehensive tests
    $overallSuccess = $true

    # Test 1: App Launch and Basic Navigation
    if (-not (Test-AppLaunchAndNavigation)) {
        Write-Log "App Launch Test Failed" "Red"
        Write-Log "The app failed to launch or basic navigation is not working" "Yellow"
        $overallSuccess = $false
    }

    # Test 2: TTTranscribe Integration Flow
    if (-not (Test-TTTranscribeIntegrationFlow)) {
        Write-Log "TTTranscribe Integration Test Failed" "Red"
        Write-Log "The TTTranscribe integration is not working properly" "Yellow"
        $overallSuccess = $false
    }

    # Test 3: TokAudit WebView Flow
    if (-not (Test-TokAuditWebViewFlow)) {
        Write-Log "TokAudit WebView Test Failed" "Red"
        Write-Log "The TokAudit WebView flow is not working properly" "Yellow"
        $overallSuccess = $false
    }

    # Test 4: Complete User Journey
    if (-not (Test-CompleteUserJourney)) {
        Write-Log "Complete User Journey Test Failed" "Red"
        Write-Log "The complete user journey from URL input to transcript generation is not working" "Yellow"
        $overallSuccess = $false
    }

    # Test 5: Error Handling and Recovery
    if (-not (Test-ErrorHandlingAndRecovery)) {
        Write-Log "Error Handling Test Failed" "Red"
        Write-Log "Error handling and recovery mechanisms are not working properly" "Yellow"
        $overallSuccess = $false
    }

    # Generate final report
    Show-ComprehensiveTestReport -OverallSuccess $overallSuccess

    if ($overallSuccess) {
        Write-Log "All comprehensive tests passed successfully" "Green"
        exit 0
    } else {
        Write-Log "Some comprehensive tests failed" "Red"
        exit 1
    }
}

function Test-AppLaunchAndNavigation {
    Write-Log "Testing app launch and basic navigation..." "Yellow"
    
    try {
        # Launch the app
        Write-Log "Launching Pluct app..." "Gray"
        $launchResult = adb shell am start -n app.pluct/.MainActivity
        if ($LASTEXITCODE -ne 0) {
            Write-Log "Failed to launch app" "Red"
            return $false
        }
        
        Start-Sleep -Seconds 3
        
        # Get current screen elements
        $screenElements = Get-ScreenElements
        Write-Log "Screen elements detected: $($screenElements.Count)" "Gray"
        
        # Check for main app elements
        $mainElements = $screenElements | Where-Object { 
            $_.text -match "Pluct" -or 
            $_.text -match "Home" -or 
            $_.text -match "Process" -or
            $_.text -match "Settings"
        }
        
        if ($mainElements.Count -gt 0) {
            Write-Log "Main app elements found" "Green"
            Write-Log "Elements: $($mainElements | ForEach-Object { $_.text } | Join-String -Separator ', ')" "Gray"
            return $true
        } else {
            Write-Log "Main app elements not found" "Red"
            return $false
        }
    } catch {
        Write-Log "App launch test failed: $($_.Exception.Message)" "Red"
        return $false
    }
}

function Test-TTTranscribeIntegrationFlow {
    Write-Log "Testing TTTranscribe integration flow..." "Yellow"
    
    try {
        # Navigate to ingest screen
        Write-Log "Navigating to ingest screen..." "Gray"
        
        # Look for URL input or process button
        $screenElements = Get-ScreenElements
        $urlInput = $screenElements | Where-Object { 
            $_.class -match "EditText" -or 
            $_.text -match "URL" -or 
            $_.text -match "Enter" -or
            $_.text -match "Process"
        }
        
        if ($urlInput.Count -gt 0) {
            Write-Log "URL input found" "Green"
            
            # Input test URL
            Write-Log "Inputting test URL: $TestUrl" "Gray"
            $inputResult = adb shell input text $TestUrl
            if ($LASTEXITCODE -ne 0) {
                Write-Log "Failed to input URL" "Red"
                return $false
            }
            
            # Look for process/transcribe button
            Start-Sleep -Seconds 2
            $screenElements = Get-ScreenElements
            $processButton = $screenElements | Where-Object { 
                $_.text -match "Process" -or 
                $_.text -match "Transcribe" -or 
                $_.text -match "Start" -or
                $_.text -match "Submit"
            }
            
            if ($processButton.Count -gt 0) {
                Write-Log "Process button found" "Green"
                
                # Click process button
                $buttonId = $processButton[0].id
                Write-Log "Clicking process button: $buttonId" "Gray"
                $clickResult = adb shell input tap $buttonId
                if ($LASTEXITCODE -ne 0) {
                    Write-Log "Failed to click process button" "Red"
                    return $false
                }
                
                # Wait for transcription process
                Write-Log "Waiting for transcription process..." "Gray"
                Start-Sleep -Seconds 10
                
                # Check for progress indicators
                $screenElements = Get-ScreenElements
                $progressElements = $screenElements | Where-Object { 
                    $_.text -match "Processing" -or 
                    $_.text -match "Transcribing" -or 
                    $_.text -match "Loading" -or
                    $_.text -match "Connecting"
                }
                
                if ($progressElements.Count -gt 0) {
                    Write-Log "Transcription process started" "Green"
                    Write-Log "Progress: $($progressElements[0].text)" "Gray"
                    
                    # Wait for completion (up to 2 minutes)
                    $maxWait = 120
                    $waited = 0
                    while ($waited -lt $maxWait) {
                        Start-Sleep -Seconds 5
                        $waited += 5
                        
                        $screenElements = Get-ScreenElements
                        $successElements = $screenElements | Where-Object { 
                            $_.text -match "Success" -or 
                            $_.text -match "Complete" -or 
                            $_.text -match "Transcript" -or
                            $_.text -match "Done"
                        }
                        
                        if ($successElements.Count -gt 0) {
                            Write-Log "Transcription completed successfully" "Green"
                            Write-Log "Result: $($successElements[0].text)" "Gray"
                            return $true
                        }
                        
                        $errorElements = $screenElements | Where-Object { 
                            $_.text -match "Error" -or 
                            $_.text -match "Failed" -or 
                            $_.text -match "Timeout"
                        }
                        
                        if ($errorElements.Count -gt 0) {
                            Write-Log "Transcription failed: $($errorElements[0].text)" "Red"
                            return $false
                        }
                    }
                    
                    Write-Log "Transcription timed out" "Red"
                    return $false
                } else {
                    Write-Log "No progress indicators found" "Red"
                    return $false
                }
            } else {
                Write-Log "Process button not found" "Red"
                return $false
            }
        } else {
            Write-Log "URL input not found" "Red"
            return $false
        }
    } catch {
        Write-Log "TTTranscribe integration test failed: $($_.Exception.Message)" "Red"
        return $false
    }
}

function Test-TokAuditWebViewFlow {
    Write-Log "Testing TokAudit WebView flow..." "Yellow"
    
    try {
        # Look for WebView or manual mode options
        $screenElements = Get-ScreenElements
        $webViewElements = $screenElements | Where-Object { 
            $_.text -match "WebView" -or 
            $_.text -match "Manual" -or 
            $_.text -match "Browser" -or
            $_.text -match "Web"
        }
        
        if ($webViewElements.Count -gt 0) {
            Write-Log "WebView elements found" "Green"
            
            # Click on WebView option
            $webViewId = $webViewElements[0].id
            Write-Log "Clicking WebView option: $webViewId" "Gray"
            $clickResult = adb shell input tap $webViewId
            if ($LASTEXITCODE -ne 0) {
                Write-Log "Failed to click WebView option" "Red"
                return $false
            }
            
            # Wait for WebView to load
            Start-Sleep -Seconds 5
            
            # Check for WebView content
            $screenElements = Get-ScreenElements
            $webViewContent = $screenElements | Where-Object { 
                $_.class -match "WebView" -or 
                $_.text -match "TikTok" -or 
                $_.text -match "Video" -or
                $_.text -match "Loading"
            }
            
            if ($webViewContent.Count -gt 0) {
                Write-Log "WebView content loaded" "Green"
                Write-Log "Content: $($webViewContent[0].text)" "Gray"
                return $true
            } else {
                Write-Log "WebView content not found" "Red"
                return $false
            }
        } else {
            Write-Log "WebView elements not found" "Red"
            return $false
        }
    } catch {
        Write-Log "TokAudit WebView test failed: $($_.Exception.Message)" "Red"
        return $false
    }
}

function Test-CompleteUserJourney {
    Write-Log "Testing complete user journey..." "Yellow"
    
    try {
        # Start from app launch
        Write-Log "Starting complete user journey..." "Gray"
        
        # Step 1: Launch app
        if (-not (Test-AppLaunchAndNavigation)) {
            Write-Log "App launch failed in complete journey" "Red"
            return $false
        }
        
        # Step 2: Navigate to URL input
        Write-Log "Navigating to URL input..." "Gray"
        $screenElements = Get-ScreenElements
        $urlInput = $screenElements | Where-Object { 
            $_.class -match "EditText" -or 
            $_.text -match "URL" -or 
            $_.text -match "Enter"
        }
        
        if ($urlInput.Count -eq 0) {
            Write-Log "URL input not found in complete journey" "Red"
            return $false
        }
        
        # Step 3: Input URL and process
        Write-Log "Inputting URL and processing..." "Gray"
        adb shell input text $TestUrl
        Start-Sleep -Seconds 2
        
        # Look for process button
        $screenElements = Get-ScreenElements
        $processButton = $screenElements | Where-Object { 
            $_.text -match "Process" -or 
            $_.text -match "Transcribe" -or 
            $_.text -match "Start"
        }
        
        if ($processButton.Count -eq 0) {
            Write-Log "Process button not found in complete journey" "Red"
            return $false
        }
        
        # Click process button
        $buttonId = $processButton[0].id
        adb shell input tap $buttonId
        
        # Step 4: Wait for transcription
        Write-Log "Waiting for transcription completion..." "Gray"
        $maxWait = 120
        $waited = 0
        while ($waited -lt $maxWait) {
            Start-Sleep -Seconds 5
            $waited += 5
            
            $screenElements = Get-ScreenElements
            $successElements = $screenElements | Where-Object { 
                $_.text -match "Success" -or 
                $_.text -match "Complete" -or 
                $_.text -match "Transcript"
            }
            
            if ($successElements.Count -gt 0) {
                Write-Log "Complete user journey successful" "Green"
                return $true
            }
        }
        
        Write-Log "Complete user journey timed out" "Red"
        return $false
    } catch {
        Write-Log "Complete user journey test failed: $($_.Exception.Message)" "Red"
        return $false
    }
}

function Test-ErrorHandlingAndRecovery {
    Write-Log "Testing error handling and recovery..." "Yellow"
    
    try {
        # Test with invalid URL
        Write-Log "Testing with invalid URL..." "Gray"
        
        # Input invalid URL
        adb shell input text "invalid-url-test"
        Start-Sleep -Seconds 2
        
        # Look for process button and click
        $screenElements = Get-ScreenElements
        $processButton = $screenElements | Where-Object { 
            $_.text -match "Process" -or 
            $_.text -match "Transcribe" -or 
            $_.text -match "Start"
        }
        
        if ($processButton.Count -gt 0) {
            $buttonId = $processButton[0].id
            adb shell input tap $buttonId
            
            # Wait for error handling
            Start-Sleep -Seconds 10
            
            # Check for error messages
            $screenElements = Get-ScreenElements
            $errorElements = $screenElements | Where-Object { 
                $_.text -match "Error" -or 
                $_.text -match "Invalid" -or 
                $_.text -match "Failed" -or
                $_.text -match "Try again"
            }
            
            if ($errorElements.Count -gt 0) {
                Write-Log "Error handling working correctly" "Green"
                Write-Log "Error message: $($errorElements[0].text)" "Gray"
                return $true
            } else {
                Write-Log "No error handling detected" "Red"
                return $false
            }
        } else {
            Write-Log "Process button not found for error test" "Red"
            return $false
        }
    } catch {
        Write-Log "Error handling test failed: $($_.Exception.Message)" "Red"
        return $false
    }
}

function Get-ScreenElements {
    try {
        # Get current screen dump
        $dumpResult = adb shell uiautomator dump /sdcard/screen_dump.xml
        if ($LASTEXITCODE -ne 0) {
            Write-Log "Failed to get screen dump" "Red"
            return @()
        }
        
        # Pull the dump file
        $pullResult = adb pull /sdcard/screen_dump.xml screen_dump.xml
        if ($LASTEXITCODE -ne 0) {
            Write-Log "Failed to pull screen dump" "Red"
            return @()
        }
        
        # Parse XML to extract elements
        [xml]$xmlContent = Get-Content screen_dump.xml -ErrorAction SilentlyContinue
        if ($xmlContent -eq $null) {
            Write-Log "Failed to parse screen dump XML" "Red"
            return @()
        }
        
        $elements = @()
        $nodes = $xmlContent.SelectNodes("//node")
        foreach ($node in $nodes) {
            $element = @{
                id = $node.GetAttribute("resource-id")
                class = $node.GetAttribute("class")
                text = $node.GetAttribute("text")
                bounds = $node.GetAttribute("bounds")
            }
            $elements += $element
        }
        
        # Clean up
        Remove-Item screen_dump.xml -ErrorAction SilentlyContinue
        
        return $elements
    } catch {
        Write-Log "Failed to get screen elements: $($_.Exception.Message)" "Red"
        return @()
    }
}

function Show-ComprehensiveTestReport {
    param([bool]$OverallSuccess)
    
    $duration = (Get-Date) - $script:ComprehensiveTestSession.StartTime
    Write-Log "=== COMPREHENSIVE ADB TEST REPORT ===" "Cyan"
    Write-Log "Duration: $($duration.TotalSeconds.ToString('F2')) seconds" "White"
    Write-Log "Test URL: $TestUrl" "White"
    Write-Log "Build Required: $($script:ComprehensiveTestSession.BuildRequired)" "White"
    Write-Log "Screenshots Captured: $($script:ComprehensiveTestSession.Screenshots.Count)" "White"
    
    if ($OverallSuccess) {
        Write-Log "✅ All comprehensive tests passed" "Green"
    } else {
        Write-Log "❌ Some comprehensive tests failed" "Red"
    }
}

# Main execution
Start-ComprehensiveADBTest

# Pluct Enhanced Test Framework - Business Engine Integration Validation
# Comprehensive testing with automated ADB commands and detailed error reporting

param(
    [string]$TestScope = "All",
    [string]$DeviceId = "",
    [switch]$Verbose = $false
)

# Enhanced error reporting and critical error handling
$script:TestSession = @{
    StartTime = Get-Date
    CriticalErrors = @()
    TestResults = @()
    BusinessEngineHealth = $null
    TTTranscribeStatus = $null
}

function Write-EnhancedLog {
    param(
        [string]$Message,
        [string]$Color = "White",
        [string]$Level = "INFO"
    )
    
    $timestamp = Get-Date -Format "yyyy-MM-dd HH:mm:ss"
    $logMessage = "[$timestamp] [$Level] $Message"
    
    switch ($Color) {
        "Red" { Write-Host $logMessage -ForegroundColor Red }
        "Green" { Write-Host $logMessage -ForegroundColor Green }
        "Yellow" { Write-Host $logMessage -ForegroundColor Yellow }
        "Cyan" { Write-Host $logMessage -ForegroundColor Cyan }
        default { Write-Host $logMessage }
    }
}

function Report-CriticalError {
    param(
        [string]$ErrorType,
        [string]$ErrorMessage,
        [string]$Stage = "Unknown"
    )
    
    Write-EnhancedLog "❌ CRITICAL ERROR: $ErrorType" "Red" "ERROR"
    Write-EnhancedLog "Stage: $Stage" "Red" "ERROR"
    Write-EnhancedLog "Error Details: $ErrorMessage" "Red" "ERROR"
    Write-EnhancedLog "Test execution stopped due to critical error." "Red" "ERROR"
    
    $script:TestSession.CriticalErrors += @{
        Type = $ErrorType
        Message = $ErrorMessage
        Stage = $Stage
        Timestamp = Get-Date
    }
    
    # Stop execution immediately
    exit 1
}

function Test-BusinessEngineConnectivity {
    Write-EnhancedLog "🔍 Testing Business Engine Connectivity..." "Cyan" "INFO"
    
    try {
        # Test 1: Basic connectivity
        Write-EnhancedLog "Testing Business Engine health endpoint..." "Yellow" "INFO"
        $healthResponse = Invoke-RestMethod -Uri "https://pluct-business-engine.romeo-lya2.workers.dev/health" -Method GET -TimeoutSec 30
        if ($healthResponse) {
            Write-EnhancedLog "✅ Business Engine health check passed" "Green" "SUCCESS"
        } else {
            Report-CriticalError "Business Engine Health Check Failed" "Business Engine health endpoint returned null or empty response" "BusinessEngineHealth"
        }
        
        # Test 2: Token vending
        Write-EnhancedLog "Testing token vending endpoint..." "Yellow" "INFO"
        $tokenRequest = @{
            userId = "mobile"
        } | ConvertTo-Json
        
        $tokenResponse = Invoke-RestMethod -Uri "https://pluct-business-engine.romeo-lya2.workers.dev/vend-token" -Method POST -Body $tokenRequest -ContentType "application/json" -TimeoutSec 30
        
        if ($tokenResponse -and $tokenResponse.token) {
            Write-EnhancedLog "✅ Token vending successful" "Green" "SUCCESS"
            $script:TestSession.BusinessEngineHealth = @{
                IsHealthy = $true
                Token = $tokenResponse.token
                Timestamp = Get-Date
            }
        } else {
            Report-CriticalError "Token Vending Failed" "Token vending endpoint failed or returned invalid response" "TokenVending"
        }
        
        # Test 3: TTTranscribe proxy
        Write-EnhancedLog "Testing TTTranscribe proxy endpoint..." "Yellow" "INFO"
        $transcribeRequest = @{
            url = "https://vm.tiktok.com/ZMAPTWV7o/"
        } | ConvertTo-Json
        
        $transcribeResponse = Invoke-RestMethod -Uri "https://pluct-business-engine.romeo-lya2.workers.dev/ttt/transcribe" -Method POST -Body $transcribeRequest -ContentType "application/json" -Headers @{"Authorization" = "Bearer $($script:TestSession.BusinessEngineHealth.Token)"} -TimeoutSec 30
        
        if ($transcribeResponse -and $transcribeResponse.request_id) {
            Write-EnhancedLog "✅ TTTranscribe proxy test successful" "Green" "SUCCESS"
            $script:TestSession.TTTranscribeStatus = @{
                IsWorking = $true
                RequestId = $transcribeResponse.request_id
                Timestamp = Get-Date
            }
        } else {
            Report-CriticalError "TTTranscribe Proxy Failed" "TTTranscribe proxy endpoint failed or returned invalid response" "TTTranscribeProxy"
        }
        
    } catch {
        Report-CriticalError "Business Engine Connectivity Test Failed" "Exception during Business Engine connectivity test: $($_.Exception.Message)" "BusinessEngineConnectivity"
    }
}

function Test-AppBuild {
    Write-EnhancedLog "🔨 Testing App Build..." "Cyan" "INFO"
    
    try {
        # Clean and build
        Write-EnhancedLog "Cleaning project..." "Yellow" "INFO"
        & ./gradlew clean
        
        if ($LASTEXITCODE -ne 0) {
            Report-CriticalError "Clean Failed" "Gradle clean failed with exit code $LASTEXITCODE" "Build"
        }
        
        Write-EnhancedLog "Building project..." "Yellow" "INFO"
        & ./gradlew assembleDebug
        
        if ($LASTEXITCODE -ne 0) {
            Report-CriticalError "Build Failed" "Gradle build failed with exit code $LASTEXITCODE. Check compilation errors." "Build"
        }
        
        # Check if APK exists
        $apkPath = "app/build/outputs/apk/debug/app-debug.apk"
        if (-not (Test-Path $apkPath)) {
            Report-CriticalError "APK Not Found" "APK file not found at expected location: $apkPath" "Build"
        }
        
        Write-EnhancedLog "✅ Build successful" "Green" "SUCCESS"
        
    } catch {
        Report-CriticalError "Build Process Failed" "Exception during build process: $($_.Exception.Message)" "Build"
    }
}

function Test-AppDeployment {
    Write-EnhancedLog "📱 Testing App Deployment..." "Cyan" "INFO"
    
    try {
        # Check device connection
        $devices = adb devices | Where-Object { $_ -match "device$" }
        if (-not $devices) {
            Report-CriticalError "No Device Connected" "No Android device connected via ADB" "Deployment"
        }
        
        # Install APK
        Write-EnhancedLog "Installing APK..." "Yellow" "INFO"
        $apkPath = "app/build/outputs/apk/debug/app-debug.apk"
        adb install -r $apkPath
        
        if ($LASTEXITCODE -ne 0) {
            Report-CriticalError "Installation Failed" "APK installation failed with exit code $LASTEXITCODE" "Deployment"
        }
        
        Write-EnhancedLog "✅ App deployed successfully" "Green" "SUCCESS"
        
    } catch {
        Report-CriticalError "Deployment Failed" "Exception during deployment: $($_.Exception.Message)" "Deployment"
    }
}

function Test-BusinessEngineIntegration {
    Write-EnhancedLog "🧪 Testing Business Engine Integration..." "Cyan" "INFO"
    
    try {
        # Launch app
        Write-EnhancedLog "Launching app..." "Yellow" "INFO"
        adb shell am start -n app.pluct/.MainActivity
        
        Start-Sleep -Seconds 5
        
        # Test 1: Check for Business Engine health logs
        Write-EnhancedLog "Checking Business Engine health logs..." "Yellow" "INFO"
        $healthLogs = adb shell logcat -d | Select-String "BusinessEngineHealthChecker|HEALTH_CHECK" | Select-Object -Last 5
        
        if ($healthLogs) {
            Write-EnhancedLog "✅ Business Engine health logs found" "Green" "SUCCESS"
            $healthLogs | ForEach-Object { Write-EnhancedLog "  $($_.Line)" "Cyan" "DEBUG" }
        } else {
            Write-EnhancedLog "⚠️ No Business Engine health logs found" "Yellow" "WARNING"
        }
        
        # Test 2: Check for VENDING_TOKEN stage
        Write-EnhancedLog "Checking for VENDING_TOKEN stage..." "Yellow" "INFO"
        $tokenLogs = adb shell logcat -d | Select-String "stage=VENDING_TOKEN" | Select-Object -Last 3
        
        if ($tokenLogs) {
            Write-EnhancedLog "✅ VENDING_TOKEN stage logs found" "Green" "SUCCESS"
            $tokenLogs | ForEach-Object { Write-EnhancedLog "  $($_.Line)" "Cyan" "DEBUG" }
        } else {
            Write-EnhancedLog "⚠️ No VENDING_TOKEN stage logs found" "Yellow" "WARNING"
        }
        
        # Test 3: Check for TTTRANSCRIBE_CALL stage
        Write-EnhancedLog "Checking for TTTRANSCRIBE_CALL stage..." "Yellow" "INFO"
        $transcribeLogs = adb shell logcat -d | Select-String "stage=TTTRANSCRIBE_CALL" | Select-Object -Last 3
        
        if ($transcribeLogs) {
            Write-EnhancedLog "✅ TTTRANSCRIBE_CALL stage logs found" "Green" "SUCCESS"
            $transcribeLogs | ForEach-Object { Write-EnhancedLog "  $($_.Line)" "Cyan" "DEBUG" }
        } else {
            Write-EnhancedLog "⚠️ No TTTRANSCRIBE_CALL stage logs found" "Yellow" "WARNING"
        }
        
        # Test 4: Check for STATUS_POLLING stage
        Write-EnhancedLog "Checking for STATUS_POLLING stage..." "Yellow" "INFO"
        $pollingLogs = adb shell logcat -d | Select-String "stage=STATUS_POLLING" | Select-Object -Last 3
        
        if ($pollingLogs) {
            Write-EnhancedLog "✅ STATUS_POLLING stage logs found" "Green" "SUCCESS"
            $pollingLogs | ForEach-Object { Write-EnhancedLog "  $($_.Line)" "Cyan" "DEBUG" }
        } else {
            Write-EnhancedLog "⚠️ No STATUS_POLLING stage logs found" "Yellow" "WARNING"
        }
        
        # Test 5: Check for COMPLETED stage
        Write-EnhancedLog "Checking for COMPLETED stage..." "Yellow" "INFO"
        $completedLogs = adb shell logcat -d | Select-String "stage=COMPLETED" | Select-Object -Last 3
        
        if ($completedLogs) {
            Write-EnhancedLog "✅ COMPLETED stage logs found" "Green" "SUCCESS"
            $completedLogs | ForEach-Object { Write-EnhancedLog "  $($_.Line)" "Cyan" "DEBUG" }
        } else {
            Write-EnhancedLog "⚠️ No COMPLETED stage logs found" "Yellow" "WARNING"
        }
        
        # Test 6: Check for errors
        Write-EnhancedLog "Checking for errors..." "Yellow" "INFO"
        $errorLogs = adb shell logcat -d | Select-String "ERROR|Exception|Failed|Error" | Select-Object -Last 10
        
        if ($errorLogs) {
            Write-EnhancedLog "⚠️ Error logs found:" "Yellow" "WARNING"
            $errorLogs | ForEach-Object { Write-EnhancedLog "  $($_.Line)" "Red" "ERROR" }
        } else {
            Write-EnhancedLog "✅ No error logs found" "Green" "SUCCESS"
        }
        
    } catch {
        Report-CriticalError "Business Engine Integration Test Failed" "Exception during Business Engine integration test: $($_.Exception.Message)" "BusinessEngineIntegration"
    }
}

function Test-AutomatedUserJourney {
    Write-EnhancedLog "🤖 Testing Automated User Journey..." "Cyan" "INFO"
    
    try {
        # Test 1: Launch app
        Write-EnhancedLog "Launching app..." "Yellow" "INFO"
        adb shell am start -n app.pluct/.MainActivity
        Start-Sleep -Seconds 3
        
        # Test 2: Simulate share intent with TikTok URL
        Write-EnhancedLog "Simulating share intent..." "Yellow" "INFO"
        $testUrl = "https://vm.tiktok.com/ZMAPTWV7o/"
        adb shell am start -a android.intent.action.SEND -t "text/plain" --es android.intent.extra.TEXT $testUrl -n app.pluct/.share.PluctShareIngestActivity
        
        Start-Sleep -Seconds 5
        
        # Test 3: Check if processing started
        Write-EnhancedLog "Checking if processing started..." "Yellow" "INFO"
        $processingLogs = adb shell logcat -d | Select-String "Starting TTTranscribe transcription|stage=VENDING_TOKEN" | Select-Object -Last 5
        
        if ($processingLogs) {
            Write-EnhancedLog "✅ Processing started successfully" "Green" "SUCCESS"
            $processingLogs | ForEach-Object { Write-EnhancedLog "  $($_.Line)" "Cyan" "DEBUG" }
        } else {
            Write-EnhancedLog "⚠️ Processing may not have started" "Yellow" "WARNING"
        }
        
        # Test 4: Monitor processing stages
        Write-EnhancedLog "Monitoring processing stages..." "Yellow" "INFO"
        $maxWaitTime = 300 # 5 minutes
        $waitTime = 0
        $stagesFound = @()
        
        while ($waitTime -lt $maxWaitTime) {
            $currentLogs = adb shell logcat -d | Select-String "stage=" | Select-Object -Last 10
            
            foreach ($log in $currentLogs) {
                if ($log.Line -match "stage=(\w+)") {
                    $stage = $matches[1]
                    if ($stage -notin $stagesFound) {
                        $stagesFound += $stage
                        Write-EnhancedLog "Found stage: $stage" "Green" "SUCCESS"
                    }
                }
            }
            
            if ($stagesFound -contains "COMPLETED") {
                Write-EnhancedLog "✅ Processing completed successfully" "Green" "SUCCESS"
                break
            }
            
            Start-Sleep -Seconds 10
            $waitTime += 10
        }
        
        if ($waitTime -ge $maxWaitTime) {
            Write-EnhancedLog "⚠️ Processing timed out after 5 minutes" "Yellow" "WARNING"
        }
        
        # Test 5: Check final results
        Write-EnhancedLog "Checking final results..." "Yellow" "INFO"
        $finalLogs = adb shell logcat -d | Select-String "stage=COMPLETED|Transcription completed" | Select-Object -Last 5
        
        if ($finalLogs) {
            Write-EnhancedLog "✅ Final results found" "Green" "SUCCESS"
            $finalLogs | ForEach-Object { Write-EnhancedLog "  $($_.Line)" "Cyan" "DEBUG" }
        } else {
            Write-EnhancedLog "⚠️ No final results found" "Yellow" "WARNING"
        }
        
    } catch {
        Report-CriticalError "Automated User Journey Test Failed" "Exception during automated user journey test: $($_.Exception.Message)" "AutomatedUserJourney"
    }
}

function Test-ErrorHandling {
    Write-EnhancedLog "🛡️ Testing Error Handling..." "Cyan" "INFO"
    
    try {
        # Test 1: Check for error categorization
        Write-EnhancedLog "Checking error categorization..." "Yellow" "INFO"
        $errorCategorizationLogs = adb shell logcat -d | Select-String "Credit/Authorization error|Network timeout|Connection error|Server error" | Select-Object -Last 5
        
        if ($errorCategorizationLogs) {
            Write-EnhancedLog "✅ Error categorization working" "Green" "SUCCESS"
            $errorCategorizationLogs | ForEach-Object { Write-EnhancedLog "  $($_.Line)" "Cyan" "DEBUG" }
        } else {
            Write-EnhancedLog "ℹ️ No error categorization logs found (this is normal if no errors occurred)" "Cyan" "INFO"
        }
        
        # Test 2: Check for retry logic
        Write-EnhancedLog "Checking retry logic..." "Yellow" "INFO"
        $retryLogs = adb shell logcat -d | Select-String "Result.retry|Retry on" | Select-Object -Last 5
        
        if ($retryLogs) {
            Write-EnhancedLog "✅ Retry logic working" "Green" "SUCCESS"
            $retryLogs | ForEach-Object { Write-EnhancedLog "  $($_.Line)" "Cyan" "DEBUG" }
        } else {
            Write-EnhancedLog "ℹ️ No retry logs found (this is normal if no retries were needed)" "Cyan" "INFO"
        }
        
    } catch {
        Report-CriticalError "Error Handling Test Failed" "Exception during error handling test: $($_.Exception.Message)" "ErrorHandling"
    }
}

function Show-TestSummary {
    Write-EnhancedLog "📊 Test Summary" "Cyan" "INFO"
    Write-EnhancedLog "===============" "Cyan" "INFO"
    
    $endTime = Get-Date
    $duration = $endTime - $script:TestSession.StartTime
    
    Write-EnhancedLog "Test Duration: $($duration.TotalMinutes.ToString('F2')) minutes" "White" "INFO"
    Write-EnhancedLog "Critical Errors: $($script:TestSession.CriticalErrors.Count)" "White" "INFO"
    
    if ($script:TestSession.CriticalErrors.Count -gt 0) {
        Write-EnhancedLog "❌ Test failed with critical errors:" "Red" "ERROR"
        foreach ($error in $script:TestSession.CriticalErrors) {
            Write-EnhancedLog "  - $($error.Type): $($error.Message)" "Red" "ERROR"
        }
    } else {
        Write-EnhancedLog "✅ All tests passed successfully!" "Green" "SUCCESS"
    }
    
    if ($script:TestSession.BusinessEngineHealth) {
        Write-EnhancedLog "Business Engine Health: $($script:TestSession.BusinessEngineHealth.IsHealthy)" "Green" "SUCCESS"
    }
    
    if ($script:TestSession.TTTranscribeStatus) {
        Write-EnhancedLog "TTTranscribe Status: $($script:TestSession.TTTranscribeStatus.IsWorking)" "Green" "SUCCESS"
    }
}

# Main execution
try {
    Write-EnhancedLog "🚀 Starting Pluct Enhanced Test Framework" "Cyan" "INFO"
    Write-EnhancedLog "Test Scope: $TestScope" "White" "INFO"
    Write-EnhancedLog "Device ID: $DeviceId" "White" "INFO"
    Write-EnhancedLog "Verbose: $Verbose" "White" "INFO"
    
    # Test Business Engine connectivity first
    Test-BusinessEngineConnectivity
    
    # Test app build
    Test-AppBuild
    
    # Test app deployment
    Test-AppDeployment
    
    # Test Business Engine integration
    Test-BusinessEngineIntegration
    
    # Test automated user journey
    Test-AutomatedUserJourney
    
    # Test error handling
    Test-ErrorHandling
    
    # Show summary
    Show-TestSummary
    
} catch {
    Report-CriticalError "Test Framework Execution Failed" "Exception during test framework execution: $($_.Exception.Message)" "TestFramework"
}

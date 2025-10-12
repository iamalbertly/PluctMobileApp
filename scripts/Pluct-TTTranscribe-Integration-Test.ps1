# Pluct TTTranscribe Integration Test
# Tests TTTranscribe API integration and TokAudit WebView flow
# Validates that transcription services are properly connected

param(
    [Parameter()]
    [string]$TestUrl = "https://vm.tiktok.com/ZMAPTWV7o/",
    
    [Parameter()]
    [switch]$SkipBuild,
    
    [Parameter()]
    [switch]$TestAPIOnly
)

# Import core modules
$script:FrameworkRoot = $PSScriptRoot
. "$script:FrameworkRoot\Pluct-Test-Core-Utilities.ps1"

# Initialize test session
$script:TTTranscribeTestSession = @{
    StartTime = Get-Date
    TestResults = @{}
    CriticalErrors = @()
    Warnings = @()
    SuccessCount = 0
    FailureCount = 0
}

function Start-TTTranscribeIntegrationTest {
    Write-Log "=== Pluct TTTranscribe Integration Test ===" "Cyan"
    Write-Log "Test URL: $TestUrl" "White"
    Write-Log "Testing TTTranscribe API integration and TokAudit WebView flow..." "Yellow"

    # Test 1: TTTranscribe API Connectivity
    if (Test-TTTranscribeAPIConnectivity) {
        Write-Log "✅ TTTranscribe API connectivity successful" "Green"
        $script:TTTranscribeTestSession.SuccessCount++
    } else {
        Write-Log "❌ CRITICAL ERROR: TTTranscribe API connectivity failed" "Red"
        $script:TTTranscribeTestSession.FailureCount++
        Show-TTTranscribeAPIFailureDetails
        return $false
    }

    # Test 2: TTTranscribe Authentication
    if (Test-TTTranscribeAuthentication) {
        Write-Log "✅ TTTranscribe authentication successful" "Green"
        $script:TTTranscribeTestSession.SuccessCount++
    } else {
        Write-Log "❌ CRITICAL ERROR: TTTranscribe authentication failed" "Red"
        $script:TTTranscribeTestSession.FailureCount++
        Show-TTTranscribeAuthFailureDetails
        return $false
    }

    # Test 3: TTTranscribe Transcription
    if (Test-TTTranscribeTranscription) {
        Write-Log "✅ TTTranscribe transcription successful" "Green"
        $script:TTTranscribeTestSession.SuccessCount++
    } else {
        Write-Log "❌ CRITICAL ERROR: TTTranscribe transcription failed" "Red"
        $script:TTTranscribeTestSession.FailureCount++
        Show-TTTranscribeTranscriptionFailureDetails
        return $false
    }

    # Test 4: TokAudit WebView Flow (if not API only)
    if (-not $TestAPIOnly) {
        if (Test-TokAuditWebViewFlow) {
            Write-Log "✅ TokAudit WebView flow successful" "Green"
            $script:TTTranscribeTestSession.SuccessCount++
        } else {
            Write-Log "⚠️ WARNING: TokAudit WebView flow issues found" "Yellow"
            $script:TTTranscribeTestSession.Warnings += "TokAudit WebView flow issues"
        }
    }

    # Test 5: Code Integration Validation
    if (Test-CodeIntegration) {
        Write-Log "✅ Code integration validation successful" "Green"
        $script:TTTranscribeTestSession.SuccessCount++
    } else {
        Write-Log "❌ CRITICAL ERROR: Code integration validation failed" "Red"
        $script:TTTranscribeTestSession.FailureCount++
        Show-CodeIntegrationFailureDetails
        return $false
    }

    # Generate final report
    Show-TTTranscribeTestReport

    if ($script:TTTranscribeTestSession.FailureCount -eq 0) {
        Write-Log "✅ All TTTranscribe integration tests passed" "Green"
        return $true
    } else {
        Write-Log "❌ TTTranscribe integration tests failed" "Red"
        return $false
    }
}

function Test-TTTranscribeAPIConnectivity {
    Write-Log "Testing TTTranscribe API connectivity..." "Yellow"
    
    try {
        $baseUrl = "https://iamromeoly-tttranscibe.hf.space"
        $healthUrl = "$baseUrl/health"
        
        Write-Log "Checking TTTranscribe health endpoint: $healthUrl" "Gray"
        
        $response = Invoke-WebRequest -Uri $healthUrl -Method GET -TimeoutSec 30 -ErrorAction Stop
        
        if ($response.StatusCode -eq 200) {
            Write-Log "TTTranscribe API is accessible" "Green"
            Write-Log "Response: $($response.Content)" "Gray"
            return $true
        } else {
            Write-Log "TTTranscribe API returned status: $($response.StatusCode)" "Red"
            return $false
        }
    } catch {
        Write-Log "TTTranscribe API connectivity failed: $($_.Exception.Message)" "Red"
        return $false
    }
}

function Test-TTTranscribeAuthentication {
    Write-Log "Testing TTTranscribe authentication..." "Yellow"
    
    try {
        # Test authentication using PowerShell implementation from README
        $BaseUrl = "https://iamromeoly-tttranscibe.hf.space"
        $ApiKey = "key_live_89f590e1f8cd3e4b19cfcf14"
        $Secret = "b0b5638935304b247195ff2cece8ed3bb307e1728397fce07bd2158866c73fa6"
        $Ts = [int64]((Get-Date).ToUniversalTime() - [datetime]'1970-01-01').TotalMilliseconds
        $Body = '{"url":"https://vm.tiktok.com/ZMAPTWV7o/"}'
        $String = "POST`n/api/transcribe`n$Body`n$Ts"

        $hmac = New-Object System.Security.Cryptography.HMACSHA256
        $hmac.Key = [Text.Encoding]::UTF8.GetBytes($Secret)
        $Sig = ($hmac.ComputeHash([Text.Encoding]::UTF8.GetBytes($String)) | ForEach-Object ToString x2) -join ""
        $hmac.Dispose()

        Write-Log "Generated signature: $Sig" "Gray"
        Write-Log "Timestamp: $Ts" "Gray"
        
        # Test the actual API call
        $headers = @{
            "X-API-Key" = $ApiKey
            "X-Timestamp" = $Ts
            "X-Signature" = $Sig
            "Content-Type" = "application/json"
        }
        
        $response = Invoke-WebRequest -Uri "$BaseUrl/api/transcribe" -Method POST -Headers $headers -Body $Body -TimeoutSec 60 -ErrorAction Stop
        
        if ($response.StatusCode -eq 200) {
            Write-Log "TTTranscribe authentication successful" "Green"
            $responseData = $response.Content | ConvertFrom-Json
            Write-Log "Response status: $($responseData.status)" "Gray"
            Write-Log "Transcript length: $($responseData.transcript.Length) characters" "Gray"
            return $true
        } else {
            Write-Log "TTTranscribe authentication failed with status: $($response.StatusCode)" "Red"
            Write-Log "Response: $($response.Content)" "Red"
            return $false
        }
    } catch {
        Write-Log "TTTranscribe authentication failed: $($_.Exception.Message)" "Red"
        return $false
    }
}

function Test-TTTranscribeTranscription {
    Write-Log "Testing TTTranscribe transcription..." "Yellow"
    
    try {
        # Use the same authentication setup as above
        $BaseUrl = "https://iamromeoly-tttranscibe.hf.space"
        $ApiKey = "key_live_89f590e1f8cd3e4b19cfcf14"
        $Secret = "b0b5638935304b247195ff2cece8ed3bb307e1728397fce07bd2158866c73fa6"
        $Ts = [int64]((Get-Date).ToUniversalTime() - [datetime]'1970-01-01').TotalMilliseconds
        $Body = "{`"url`":`"$TestUrl`"}"
        $String = "POST`n/api/transcribe`n$Body`n$Ts"

        $hmac = New-Object System.Security.Cryptography.HMACSHA256
        $hmac.Key = [Text.Encoding]::UTF8.GetBytes($Secret)
        $Sig = ($hmac.ComputeHash([Text.Encoding]::UTF8.GetBytes($String)) | ForEach-Object ToString x2) -join ""
        $hmac.Dispose()

        $headers = @{
            "X-API-Key" = $ApiKey
            "X-Timestamp" = $Ts
            "X-Signature" = $Sig
            "Content-Type" = "application/json"
        }
        
        Write-Log "Testing transcription with URL: $TestUrl" "Gray"
        $response = Invoke-WebRequest -Uri "$BaseUrl/api/transcribe" -Method POST -Headers $headers -Body $Body -TimeoutSec 120 -ErrorAction Stop
        
        if ($response.StatusCode -eq 200) {
            $responseData = $response.Content | ConvertFrom-Json
            Write-Log "TTTranscribe transcription successful" "Green"
            Write-Log "Status: $($responseData.status)" "Gray"
            Write-Log "Language: $($responseData.lang)" "Gray"
            Write-Log "Duration: $($responseData.duration_sec) seconds" "Gray"
            Write-Log "Transcript length: $($responseData.transcript.Length) characters" "Gray"
            Write-Log "Video ID: $($responseData.source.video_id)" "Gray"
            
            if ($responseData.transcript.Length -gt 0) {
                Write-Log "Transcript preview: $($responseData.transcript.Substring(0, [Math]::Min(100, $responseData.transcript.Length)))..." "Gray"
                return $true
            } else {
                Write-Log "Empty transcript received" "Red"
                return $false
            }
        } else {
            Write-Log "TTTranscribe transcription failed with status: $($response.StatusCode)" "Red"
            Write-Log "Response: $($response.Content)" "Red"
            return $false
        }
    } catch {
        Write-Log "TTTranscribe transcription failed: $($_.Exception.Message)" "Red"
        return $false
    }
}

function Test-TokAuditWebViewFlow {
    Write-Log "Testing TokAudit WebView flow..." "Yellow"
    
    try {
        # Check if TokAudit WebView components exist
        $tokAuditFiles = @(
            "app\src\main\java\app\pluct\ui\components\ScriptTokAuditWebView.kt",
            "app\src\main\java\app\pluct\ui\utils\scripts\Pluct-TokAudit-Automation.kt"
        )
        
        $missingFiles = @()
        foreach ($file in $tokAuditFiles) {
            if (-not (Test-Path $file)) {
                $missingFiles += $file
            }
        }
        
        if ($missingFiles.Count -gt 0) {
            Write-Log "Missing TokAudit WebView files:" "Red"
            $missingFiles | ForEach-Object { Write-Log "  $_" "Red" }
            return $false
        }
        
        Write-Log "TokAudit WebView components found" "Green"
        return $true
    } catch {
        Write-Log "TokAudit WebView flow test failed: $($_.Exception.Message)" "Red"
        return $false
    }
}

function Test-CodeIntegration {
    Write-Log "Testing code integration..." "Yellow"
    
    try {
        # Check if new TTTranscribe services exist
        $newServices = @(
            "app\src\main\java\app\pluct\api\Pluct-TTTranscribe-Authenticator.kt",
            "app\src\main\java\app\pluct\api\Pluct-TTTranscribe-Service.kt"
        )
        
        $missingServices = @()
        foreach ($service in $newServices) {
            if (-not (Test-Path $service)) {
                $missingServices += $service
            }
        }
        
        if ($missingServices.Count -gt 0) {
            Write-Log "Missing TTTranscribe services:" "Red"
            $missingServices | ForEach-Object { Write-Log "  $_" "Red" }
            return $false
        }
        
        # Check if DI module includes new services
        $diModule = "app\src\main\java\app\pluct\di\Pluct-DI-Core-Module.kt"
        if (Test-Path $diModule) {
            $diContent = Get-Content $diModule -Raw
            if ($diContent -match "PluctTTTranscribeService" -and $diContent -match "PluctTTTranscribeAuthenticator") {
                Write-Log "DI module includes TTTranscribe services" "Green"
            } else {
                Write-Log "DI module missing TTTranscribe services" "Red"
                return $false
            }
        } else {
            Write-Log "DI module not found" "Red"
            return $false
        }
        
        Write-Log "Code integration validation successful" "Green"
        return $true
    } catch {
        Write-Log "Code integration test failed: $($_.Exception.Message)" "Red"
        return $false
    }
}

function Show-TTTranscribeAPIFailureDetails {
    Write-Log "=== TTTranscribe API FAILURE DETAILS ===" "Red"
    Write-Log "API connectivity failed - possible causes:" "Yellow"
    Write-Log "1. TTTranscribe service is down" "White"
    Write-Log "2. Network connectivity issues" "White"
    Write-Log "3. Firewall blocking requests" "White"
    Write-Log "4. Incorrect API endpoint" "White"
    Write-Log "5. Service maintenance" "White"
    Write-Log "Check TTTranscribe service status and network connection" "Yellow"
}

function Show-TTTranscribeAuthFailureDetails {
    Write-Log "=== TTTranscribe AUTH FAILURE DETAILS ===" "Red"
    Write-Log "Authentication failed - possible causes:" "Yellow"
    Write-Log "1. Invalid API key or secret" "White"
    Write-Log "2. Incorrect signature generation" "White"
    Write-Log "3. Clock skew issues" "White"
    Write-Log "4. API key expired" "White"
    Write-Log "5. Rate limiting" "White"
    Write-Log "Check API credentials and signature generation" "Yellow"
}

function Show-TTTranscribeTranscriptionFailureDetails {
    Write-Log "=== TTTranscribe TRANSCRIPTION FAILURE DETAILS ===" "Red"
    Write-Log "Transcription failed - possible causes:" "Yellow"
    Write-Log "1. Invalid TikTok URL" "White"
    Write-Log "2. Video not accessible" "White"
    Write-Log "3. Audio extraction failed" "White"
    Write-Log "4. Whisper model issues" "White"
    Write-Log "5. Rate limiting" "White"
    Write-Log "Check video URL and TTTranscribe service status" "Yellow"
}

function Show-CodeIntegrationFailureDetails {
    Write-Log "=== CODE INTEGRATION FAILURE DETAILS ===" "Red"
    Write-Log "Code integration failed - possible causes:" "Yellow"
    Write-Log "1. Missing service files" "White"
    Write-Log "2. DI module not updated" "White"
    Write-Log "3. Import statements missing" "White"
    Write-Log "4. Service dependencies not resolved" "White"
    Write-Log "5. Compilation errors" "White"
    Write-Log "Check service files and dependency injection" "Yellow"
}

function Show-TTTranscribeTestReport {
    $duration = (Get-Date) - $script:TTTranscribeTestSession.StartTime
    Write-Log "=== TTTranscribe INTEGRATION TEST REPORT ===" "Cyan"
    Write-Log "Duration: $($duration.TotalSeconds.ToString('F2')) seconds" "White"
    Write-Log "Success Count: $($script:TTTranscribeTestSession.SuccessCount)" "Green"
    Write-Log "Failure Count: $($script:TTTranscribeTestSession.FailureCount)" $(if ($script:TTTranscribeTestSession.FailureCount -eq 0) { "Green" } else { "Red" })
    Write-Log "Warning Count: $($script:TTTranscribeTestSession.Warnings.Count)" $(if ($script:TTTranscribeTestSession.Warnings.Count -eq 0) { "Green" } else { "Yellow" })
    
    if ($script:TTTranscribeTestSession.Warnings.Count -gt 0) {
        Write-Log "Warnings:" "Yellow"
        $script:TTTranscribeTestSession.Warnings | ForEach-Object { Write-Log "  - $_" "Yellow" }
    }
    
    if ($script:TTTranscribeTestSession.FailureCount -eq 0) {
        Write-Log "✅ All TTTranscribe integration tests passed" "Green"
    } else {
        Write-Log "❌ Some TTTranscribe integration tests failed" "Red"
    }
}

# Main execution
Start-TTTranscribeIntegrationTest

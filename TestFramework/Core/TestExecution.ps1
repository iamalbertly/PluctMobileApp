# ClipForge Test Framework - Test Execution Module
# Contains functions for executing test steps and test cases

# Function to execute a test step
function Test-Step {
    param(
        [string]$Name,
        [scriptblock]$Action,
        [switch]$CaptureHTML,
        [switch]$Critical,
        [int]$TimeoutSeconds = 30
    )
    
    Write-Log "===== TEST STEP: $Name ====="  "Cyan"
    $script:CurrentTestStep = $Name
    $script:TotalTests++
    
    $stopwatch = [System.Diagnostics.Stopwatch]::StartNew()
    $success = $false
    
    try {
        # Execute the test action with timeout
        $job = Start-Job -ScriptBlock { param($action) & $action } -ArgumentList $Action
        $completed = Wait-Job -Job $job -Timeout $TimeoutSeconds
        
        if ($completed -eq $null) {
            Write-Log "Test step timed out after $TimeoutSeconds seconds" "Red"
            Stop-Job -Job $job
            throw "Timeout after $TimeoutSeconds seconds"
        }
        
        $result = Receive-Job -Job $job
        Remove-Job -Job $job
        
        # If the action returns a boolean, use that as success indicator
        if ($result -is [bool]) {
            $success = $result
        } else {
            # Otherwise, assume success if no exception was thrown
            $success = $true
        }
    } catch {
        $success = $false
        Write-Log "Test step failed: $_" "Red"
    } finally {
        $stopwatch.Stop()
    }
    
    # Capture HTML if requested
    if ($CaptureHTML) {
        Capture-WebViewHtml -CaptureHTML -TestName $Name.Replace(" ", "_")
    }
    
    # Log the result
    $duration = $stopwatch.Elapsed.TotalSeconds.ToString("0.00")
    if ($success) {
        Write-Log "PASS: $Name (${duration}s)" "Green"
        $script:PassedTests++
        $script:TestResults[$Name] = "PASS"
    } else {
        Write-Log "FAIL: $Name (${duration}s)" "Red"
        $script:FailedTests++
        $script:TestResults[$Name] = "FAIL"
        
        # If this is a critical test, exit the script
        if ($Critical) {
            Write-Log "Critical test failed, stopping execution" "Red"
            throw "Critical test failed: $Name"
        }
    }
    
    return $success
}

# Function to run a test case
function Test-Case {
    param(
        [string]$Name,
        [scriptblock]$Setup,
        [scriptblock]$Test,
        [scriptblock]$Cleanup,
        [switch]$CaptureHTML,
        [switch]$Critical
    )
    
    Write-Log "\n===== TEST CASE: $Name =====" "Magenta"
    $script:CurrentTestCase = $Name
    
    $success = $true
    
    try {
        # Run setup if provided
        if ($Setup) {
            Write-Log "Running test setup..." "Gray"
            & $Setup
        }
        
        # Run the test
        $testResult = & $Test
        if ($testResult -is [bool] -and -not $testResult) {
            $success = $false
        }
    } catch {
        $success = $false
        Write-Log "Test case failed with exception: $_" "Red"
    } finally {
        # Run cleanup if provided
        if ($Cleanup) {
            Write-Log "Running test cleanup..." "Gray"
            try {
                & $Cleanup
            } catch {
                Write-Log "Cleanup failed: $_" "Yellow"
            }
        }
    }
    
    # Log the result
    if ($success) {
        Write-Log "TEST CASE PASSED: $Name" "Green"
    } else {
        Write-Log "TEST CASE FAILED: $Name" "Red"
        
        # If this is a critical test case, exit the script
        if ($Critical) {
            Write-Log "Critical test case failed, stopping execution" "Red"
            throw "Critical test case failed: $Name"
        }
    }
    
    return $success
}

# Function to run a test suite
function Test-Suite {
    param(
        [string]$Name,
        [scriptblock]$Tests,
        [switch]$ContinueOnFailure
    )
    
    Write-Log "\n===== TEST SUITE: $Name =====" "Blue"
    $script:CurrentTestSuite = $Name
    
    $startTime = Get-Date
    $success = $true
    
    try {
        # Initialize test counters
        $script:TotalTests = 0
        $script:PassedTests = 0
        $script:FailedTests = 0
        $script:TestResults = @{}
        
        # Run the tests
        & $Tests
        
        # Check if any tests failed
        if ($script:FailedTests -gt 0) {
            $success = $false
        }
    } catch {
        $success = $false
        Write-Log "Test suite failed with exception: $_" "Red"
        
        # If we should continue on failure, swallow the exception
        if (-not $ContinueOnFailure) {
            throw
        }
    } finally {
        # Calculate duration
        $endTime = Get-Date
        $duration = ($endTime - $startTime).TotalSeconds.ToString("0.00")
        
        # Log summary
        Write-Log "\n===== TEST SUITE SUMMARY: $Name =====" "Blue"
        Write-Log "Duration: ${duration}s" "White"
        Write-Log "Total Tests: $script:TotalTests" "White"
        Write-Log "Passed: $script:PassedTests" "Green"
        Write-Log "Failed: $script:FailedTests" "Red"
        
        if ($script:TotalTests -gt 0) {
            $successRate = [math]::Round(($script:PassedTests / $script:TotalTests) * 100, 2)
            $color = if ($successRate -ge 80) { "Green" } elseif ($successRate -ge 50) { "Yellow" } else { "Red" }
            Write-Log "Success Rate: ${successRate}%" $color
        }
        
        # Log failed tests
        if ($script:FailedTests -gt 0) {
            Write-Log "\nFailed Tests:" "Red"
            $script:TestResults.GetEnumerator() | Where-Object { $_.Value -eq "FAIL" } | ForEach-Object {
                Write-Log "  - $($_.Key)" "Red"
            }
        }
    }
    
    return $success
}

# Export functions
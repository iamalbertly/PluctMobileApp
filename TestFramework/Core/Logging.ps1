# ClipForge Test Framework - Logging Module
# Contains all logging-related functionality

# Function to write log messages with timestamps and colors
function Write-Log {
    param(
        [string]$Message, 
        [string]$Color = "White",
        [string]$LogFile = $script:LogFile
    )
    
    $timestamp = Get-Date -Format "HH:mm:ss"
    Write-Host "[$timestamp] $Message" -ForegroundColor $Color
    
    # Write to log file if specified
    if ($LogFile) {
        $fileTimestamp = Get-Date -Format "yyyy-MM-dd HH:mm:ss.fff"
        "[$fileTimestamp] $Message" | Out-File -FilePath $LogFile -Append -Encoding utf8
    }
}

# Function to show real-time logs from device
function Show-RealtimeLogs {
    param(
        [switch]$StopOnExit,
        [int]$BufferSizeInMB = 16
    )
    
    # Resize logcat buffer to ensure we capture enough logs
    try {
        Write-Log "Resizing logcat buffer to ${BufferSizeInMB}MB..." "Gray"
        adb logcat -G ${BufferSizeInMB}M
        Start-Sleep -Seconds 1
    } catch {
        Write-Log "Warning: Failed to resize logcat buffer: $($_.Exception.Message)" "Yellow"
    }
    
    # Clear existing logs to start fresh
    try {
        Write-Log "Clearing existing logs..." "Gray"
        adb logcat -c
        Start-Sleep -Seconds 1
    } catch {
        Write-Log "Warning: Failed to clear logs: $($_.Exception.Message)" "Yellow"
    }
    
    # Create a string with all the log tags for filtering
    # Include ActivityManager and AndroidRuntime to catch app crashes and launch errors
    $logTagsString = "WVConsole:V WebViewUtils:V WebViewClientFactory:V WebViewConfiguration:V WebViewSettings:V WebTranscriptActivity:V Ingest:V ScriptTokAudit:V JavaScriptBridge:V WebViewScripts:V chromium:V cr_Console:V ActivityManager:E AndroidRuntime:E *:S"
    
    Write-Log "Starting real-time logs with filter: $logTagsString" "Cyan"
    
    # Create a temporary file to capture logs
    $tempLogFile = "$script:LogsDirectory\temp_logcat_$([Guid]::NewGuid().ToString()).log"
    
    # Start the logcat process with output redirected to file
    $logcatProcess = Start-Process -FilePath "adb" -ArgumentList "logcat", "-v", "threadtime", $logTagsString -NoNewWindow -PassThru -RedirectStandardOutput $tempLogFile
    
    # Store process ID and temp file in script scope so they can be accessed later
    $script:logcatProcessId = $logcatProcess.Id
    $script:logcatTempFile = $tempLogFile
    
    Write-Log "Started real-time log display (Process ID: $($logcatProcess.Id))" "Gray"
    Write-Log "Logs being saved to: $tempLogFile" "Gray"
    
    # Start a background job to periodically check for errors in the log file
    $script:logMonitorJob = Start-Job -ScriptBlock {
        param($logFile)
        
        $lastCheckTime = Get-Date
        $lastSize = 0
        
        while ($true) {
            if (Test-Path $logFile) {
                $fileInfo = Get-Item $logFile
                
                # Check for new content every 5 seconds
                if (((Get-Date) - $lastCheckTime).TotalSeconds -ge 5 -or $fileInfo.Length -gt $lastSize) {
                    $errorLogs = Get-Content $logFile | Select-String -Pattern "Error|Exception|Invalid|Failed|E/ActivityManager|E/AndroidRuntime"
                    if ($errorLogs -and $errorLogs.Count -gt 0) {
                        # Only output new errors
                        $newErrors = $errorLogs | Select-Object -Last 5
                        foreach ($error in $newErrors) {
                            Write-Output "[ERROR] $error"
                        }
                    }
                    
                    $lastCheckTime = Get-Date
                    $lastSize = $fileInfo.Length
                }
            }
            
            Start-Sleep -Seconds 2
        }
    } -ArgumentList $tempLogFile
    
    return $logcatProcess
}

# Function to stop real-time log display
function Stop-RealtimeLogs {
    # Stop the logcat process
    if ($script:logcatProcessId) {
        try {
            $process = Get-Process -Id $script:logcatProcessId -ErrorAction SilentlyContinue
            if ($process) {
                $process.Kill()
                Write-Log "Stopped real-time log display (Process ID: $($script:logcatProcessId))" "Gray"
            }
        } catch {
            Write-Log "Error stopping logcat process: $_" "Yellow"
        }
        $script:logcatProcessId = $null
    }
    
    # Stop the log monitor job
    if ($script:logMonitorJob) {
        try {
            Stop-Job -Job $script:logMonitorJob -ErrorAction SilentlyContinue
            Remove-Job -Job $script:logMonitorJob -Force -ErrorAction SilentlyContinue
            Write-Log "Stopped log monitor job" "Gray"
        } catch {
            Write-Log "Error stopping log monitor job: $_" "Yellow"
        }
        $script:logMonitorJob = $null
    }
    
    # Save the temporary log file to a permanent location with timestamp
    if ($script:logcatTempFile -and (Test-Path $script:logcatTempFile)) {
        try {
            $timestamp = Get-Date -Format "yyyyMMdd_HHmmss"
            $permanentLogFile = "$script:LogsDirectory\logcat_$timestamp.log"
            Copy-Item -Path $script:logcatTempFile -Destination $permanentLogFile -Force
            Write-Log "Saved logs to: $permanentLogFile" "Green"
            
            # Extract and display any error messages from the log
            $errorLogs = Get-Content $script:logcatTempFile | Select-String -Pattern "Error|Exception|Invalid|Failed|E/ActivityManager|E/AndroidRuntime"
            if ($errorLogs -and $errorLogs.Count -gt 0) {
                Write-Log "Found $($errorLogs.Count) error messages in logs:" "Red"
                $errorLogs | Select-Object -First 10 | ForEach-Object {
                    Write-Log "  $_" "Red"
                }
                if ($errorLogs.Count -gt 10) {
                    Write-Log "  ... and $($errorLogs.Count - 10) more errors. See $permanentLogFile for details." "Red"
                }
            }
            
            # Clean up the temporary file
            Remove-Item -Path $script:logcatTempFile -Force -ErrorAction SilentlyContinue
        } catch {
            Write-Log "Error saving log file: $_" "Yellow"
        }
        $script:logcatTempFile = $null
    }
}

# Function to wait for specific log patterns
function Wait-ForLog {
    param(
        [string]$Pattern, 
        [int]$TimeoutSeconds = 30, 
        [string]$Description = "Log pattern", 
    )
    
    Write-Log "Waiting for: $Description" "Yellow"
    $startTime = Get-Date
    $endTime = $startTime.AddSeconds($TimeoutSeconds)
    
    # Since we're already showing real-time logs, we just need to check periodically if the pattern appears
    # without duplicating the log output
    while ((Get-Date) -lt $endTime) {
        try {
            # Use proper Select-String with multiple patterns
            $logs = adb logcat -d | Select-String -Pattern "WebViewUtils|WVConsole|ScriptTokAudit|WebTranscriptActivity|Ingest|MainActivity" | Select-String -Pattern $Pattern
            if ($logs) {
                Write-Log "Found: $Description" "Green"
                if ($Verbose) {
                    Write-Log "Match details:" "Gray"
                    Write-Log "  $($logs[-1])" "Gray"
                }
                return $true
            } else {
                # Show periodic updates when in verbose mode
                if ($Verbose) {
                    $elapsedSeconds = [math]::Round(((Get-Date) - $startTime).TotalSeconds, 1)
                    $remainingSeconds = [math]::Round(($endTime - (Get-Date)).TotalSeconds, 1)
                    if ($elapsedSeconds % 5 -lt 0.5) { # Show update roughly every 5 seconds
                        Write-Log "Still waiting for: $Description (${elapsedSeconds}s elapsed, ${remainingSeconds}s remaining)" "Yellow"
                    }
                }
            }
        } catch {
            # Catch any errors silently
        }
        Start-Sleep -Seconds 1
    }
    
    Write-Log "Timeout waiting for: $Description" "Red"
    return $false
}

# Function to clear logs
function Clear-Logs {
    try {
        # Resize log buffer to 16M
        adb logcat -G 16M
        Start-Sleep -Seconds 1
        
        # Clear logs
        adb logcat -c
        Start-Sleep -Seconds 2
        
        Write-Log "Logs cleared and buffer resized" "Green"
    } catch {
        Write-Log "Failed to clear logs: $_" "Yellow"
    }
}

# Export functions
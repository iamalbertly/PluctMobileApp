# Pluct Test Core Device Management
# Device connection and app lifecycle management

function Test-DeviceConnection {
    $devices = adb devices | Select-String "device$"
    if ($devices.Count -eq 0) {
        Write-Log "No Android device connected" "Red"
        return $false
    }
    
    $deviceName = $devices[0].ToString().Split()[0]
    Write-Log "Device connected: $deviceName" "Green"
    return $true
}

function Start-App {
    Write-Log "Launching app..." "Yellow"
    adb shell am start -n "$script:AppPackage/$script:MainActivity"
    Start-Sleep -Seconds 3
}

function Send-ShareIntent {
    param([string]$Url)
    
    Write-Log "Sending share intent with URL: $Url" "Yellow"
    $shareIntent = @(
        "shell", "am", "start",
        "-a", "android.intent.action.SEND",
        "-t", "text/plain",
        "--es", "android.intent.extra.TEXT", $Url,
        "-n", "$script:AppPackage/$script:ShareActivity"
    )
    
    & adb $shareIntent
    Start-Sleep -Seconds 2
}

function Start-LogcatMonitor {
    param([string]$Tags = "MainActivity:D,ShareIngestActivity:D,HomeViewModel:D,HomeScreen:D")
    
    $logcatProcess = Start-Process -FilePath "adb" -ArgumentList "logcat", "-s", $Tags -NoNewWindow -PassThru
    return $logcatProcess
}

function Stop-LogcatMonitor {
    param([System.Diagnostics.Process]$Process)
    
    if ($Process -and -not $Process.HasExited) {
        Stop-Process -Id $Process.Id -Force -ErrorAction SilentlyContinue
    }
}

function Test-DeploymentNeeded {
    Write-Log "Checking if deployment is needed..." "Yellow"
    
    # Check if app is installed
    $installed = adb shell pm list packages | Select-String $script:AppPackage
    if (-not $installed) {
        Write-Log "App not installed - deployment needed" "Yellow"
        return $true
    }
    
    # Check if APK is newer than installed version
    $apkPath = "app\build\outputs\apk\debug\app-debug.apk"
    if (-not (Test-Path $apkPath)) {
        Write-Log "APK not found - build required first" "Yellow"
        return $true
    }
    
    $apkTime = (Get-Item $apkPath).LastWriteTime
    $deviceTime = Get-DeviceAppTimestamp
    
    if ($apkTime -gt $deviceTime) {
        Write-Log "APK is newer than installed version - deployment needed" "Yellow"
        return $true
    }
    
    Write-Log "App is up to date - no deployment needed" "Green"
    return $false
}

function Get-DeviceAppTimestamp {
    try {
        $timestamp = adb shell dumpsys package $script:AppPackage | Select-String "timeStamp" | Select-Object -First 1
        if ($timestamp) {
            # Parse timestamp from dumpsys output
            $timeString = $timestamp.ToString().Split("=")[1].Trim()
            return [DateTime]::Parse($timeString)
        }
    } catch {
        Write-Log "Could not get device app timestamp: $($_.Exception.Message)" "Yellow"
    }
    
    # Fallback to current time minus 1 hour to force deployment
    return (Get-Date).AddHours(-1)
}

function Deploy-ToDevice {
    Write-Log "Starting deployment to Android device..." "Yellow"
    
    try {
        # Uninstall existing app
        Write-Log "Uninstalling existing app..." "Yellow"
        adb uninstall $script:AppPackage | Out-Null
        
        # Install new APK
        Write-Log "Installing new APK..." "Yellow"
        $apkPath = "app\build\outputs\apk\debug\app-debug.apk"
        
        if (-not (Test-Path $apkPath)) {
            Write-Log "APK not found at $apkPath - build required first" "Red"
            return $false
        }
        
        $installResult = adb install -r $apkPath
        if ($LASTEXITCODE -ne 0) {
            Write-Log "Installation failed: $installResult" "Red"
            return $false
        }
        
        Write-Log "Installation successful" "Green"
        
        # Clear app data for clean test
        Write-Log "Clearing app data for clean test..." "Yellow"
        adb shell pm clear $script:AppPackage
        
        # Verify installation
        $installed = adb shell pm list packages | Select-String $script:AppPackage
        if ($installed) {
            Write-Log "Deployment completed successfully" "Green"
            return $true
        } else {
            Write-Log "Deployment verification failed" "Red"
            return $false
        }
        
    } catch {
        Write-Log "Deployment failed: $($_.Exception.Message)" "Red"
        return $false
    }
}

function Clear-AppData {
    Write-Log "Clearing app data for clean test" "Yellow"
    adb shell pm clear $script:AppPackage
    Start-Sleep -Seconds 2
}

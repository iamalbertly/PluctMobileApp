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

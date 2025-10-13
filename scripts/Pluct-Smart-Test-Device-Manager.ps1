# Pluct Smart Test Device Manager - Intelligent device management and monitoring
# Follows naming convention: [Project]-[ParentScope]-[ChildScope]-[CoreResponsibility]

function Test-SmartAndroidDevice {
    try {
        $deviceInfo = adb devices 2>&1
        $deviceCount = ($deviceInfo | Select-String "device").Count
        return $deviceCount -gt 0
    } catch {
        return $false
    }
}

function Get-SmartDeviceInfo {
    try {
        $deviceInfo = @{
            Model = adb shell getprop ro.product.model 2>$null
            Version = adb shell getprop ro.build.version.release 2>$null
            API = adb shell getprop ro.build.version.sdk 2>$null
            Serial = adb shell getprop ro.serialno 2>$null
        }
        return $deviceInfo
    } catch {
        return $null
    }
}

function Test-SmartAppInstalled {
    try {
        $packageInfo = adb shell pm list packages | Select-String "app.pluct"
        return $packageInfo -ne $null
    } catch {
        return $false
    }
}

function Get-SmartAppVersion {
    try {
        $versionInfo = adb shell dumpsys package app.pluct | Select-String "versionCode"
        if ($versionInfo) {
            return $versionInfo.ToString().Split("=")[1].Trim()
        }
        return $null
    } catch {
        return $null
    }
}

function Clear-SmartAppData {
    try {
        $clearResult = adb shell pm clear app.pluct 2>&1
        return $LASTEXITCODE -eq 0
    } catch {
        return $false
    }
}

function Start-SmartApp {
    try {
        $launchResult = adb shell monkey -p app.pluct -c android.intent.category.LAUNCHER 1 2>&1
        return $LASTEXITCODE -eq 0
    } catch {
        return $false
    }
}

function Stop-SmartApp {
    try {
        $stopResult = adb shell am force-stop app.pluct 2>&1
        return $LASTEXITCODE -eq 0
    } catch {
        return $false
    }
}

function Get-SmartAppLogs {
    param(
        [int]$Lines = 100
    )
    
    try {
        $logs = adb shell logcat -d -t $Lines 2>&1
        return $logs
    } catch {
        return $null
    }
}

function Monitor-SmartAppActivity {
    param(
        [int]$DurationSeconds = 10
    )
    
    try {
        $startTime = Get-Date
        $activities = @()
        
        while ((Get-Date) - $startTime -lt [TimeSpan]::FromSeconds($DurationSeconds)) {
            $currentActivity = adb shell dumpsys activity activities | Select-String "pluct"
            if ($currentActivity) {
                $activities += $currentActivity
            }
            Start-Sleep -Milliseconds 500
        }
        
        return $activities
    } catch {
        return $null
    }
}

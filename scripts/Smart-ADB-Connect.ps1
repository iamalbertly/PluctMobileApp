# Smart ADB Connection Script
param(
    [switch]$AutoReconnect = $false
)

function Get-NetworkDevices {
    $devices = @()
    $arpOutput = arp -a
    foreach ($line in $arpOutput -split "`n") {
        if ($line -match "\s+(\d+\.\d+\.\d+\.\d+)\s+") {
            $devices += $matches[1]
        }
    }
    return $devices
}

function Test-AdbDevice {
    param($ip)
    try {
        $result = adb connect "$($ip):5555" 2>&1
        if ($result -match "connected") {
            return $true
        }
    }
    catch {}
    return $false
}

function Find-AndroidDevice {
    Write-Host "Scanning network for Android device..." -ForegroundColor Cyan
    $devices = Get-NetworkDevices
    $totalDevices = $devices.Count
    $current = 0
    
    foreach ($ip in $devices) {
        $current++
        Write-Progress -Activity "Scanning for Android device" -Status "Checking IP: $ip" -PercentComplete (($current / $totalDevices) * 100)
        
        if (Test-AdbDevice $ip) {
            Write-Progress -Activity "Scanning for Android device" -Completed
            return $ip
        }
    }
    Write-Progress -Activity "Scanning for Android device" -Completed
    return $null
}

function Monitor-Connection {
    param($ip)
    while ($true) {
        $devices = adb devices
        if ($devices -notmatch "$($ip):5555") {
            Write-Host "Connection lost. Attempting to reconnect..." -ForegroundColor Yellow
            Test-AdbDevice $ip | Out-Null
        }
        Start-Sleep -Seconds 5
    }
}

# Main script
$lastDeviceFile = "$PSScriptRoot\last_device_ip.txt"

# Try to connect to last known device first
if (Test-Path $lastDeviceFile) {
    $lastIP = Get-Content $lastDeviceFile
    Write-Host "Trying last known device: $lastIP" -ForegroundColor Cyan
    if (Test-AdbDevice $lastIP) {
        Write-Host "Successfully connected to last known device!" -ForegroundColor Green
        if ($AutoReconnect) {
            Monitor-Connection $lastIP
        }
        exit 0
    }
}

# Scan network for device
$deviceIP = Find-AndroidDevice
if ($deviceIP) {
    Write-Host "Found and connected to Android device at $deviceIP" -ForegroundColor Green
    $deviceIP | Set-Content $lastDeviceFile
    if ($AutoReconnect) {
        Monitor-Connection $deviceIP
    }
}
else {
    Write-Host "No Android device found on the network. Make sure wireless ADB is enabled on your device." -ForegroundColor Red
    exit 1
}

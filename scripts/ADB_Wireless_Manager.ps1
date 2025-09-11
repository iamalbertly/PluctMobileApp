# ADB Wireless Manager - Enhanced Version
# Handles network changes and automatic device discovery

param(
    [switch]$AutoReconnect = $false,
    [switch]$ForceScan = $false,
    [string]$CustomIP = ""
)

function Write-ColorOutput {
    param(
        [string]$Message,
        [string]$Color = "White"
    )
    Write-Host $Message -ForegroundColor $Color
}

function Get-DeviceIP {
    Write-ColorOutput "🔍 Getting device IP address..." "Cyan"
    try {
        $ip = adb shell "ip addr show wlan0 | grep -w inet | awk '{print \$2}' | cut -d/ -f1"
        if ($ip -and $ip -match '\d+\.\d+\.\d+\.\d+') {
            return $ip.Trim()
        }
    }
    catch {
        Write-ColorOutput "❌ Failed to get device IP via ADB" "Red"
    }
    return $null
}

function Enable-WirelessADB {
    Write-ColorOutput "🚀 Enabling wireless ADB..." "Yellow"
    
    # Enable TCP/IP mode
    adb tcpip 5555
    Start-Sleep -Seconds 3
    
    # Get device IP
    $deviceIP = Get-DeviceIP
    if ($deviceIP) {
        Write-ColorOutput "✅ Device IP: $deviceIP" "Green"
        
        # Save IP to file for HTML interface
        $deviceIP | Out-File -FilePath "$PSScriptRoot\current_device_ip.txt" -Encoding UTF8
        
        # Try to connect
        $result = adb connect "$deviceIP`:5555"
        if ($result -match "connected") {
            Write-ColorOutput "✅ Wireless ADB enabled successfully!" "Green"
            return $deviceIP
        } else {
            Write-ColorOutput "❌ Failed to connect wirelessly" "Red"
        }
    } else {
        Write-ColorOutput "❌ Could not determine device IP" "Red"
    }
    return $null
}

function Test-NetworkConnectivity {
    param($ip)
    try {
        $ping = Test-Connection -ComputerName $ip -Count 1 -Quiet
        return $ping
    }
    catch {
        return $false
    }
}

function Find-AndroidDevice {
    Write-ColorOutput "🔍 Scanning network for Android devices..." "Cyan"
    
    # Get local network range
    $localIP = (Get-NetIPAddress -AddressFamily IPv4 | Where-Object {$_.IPAddress -like "192.168.*" -or $_.IPAddress -like "10.*" -or $_.IPAddress -like "172.*"} | Select-Object -First 1).IPAddress
    if ($localIP) {
        $networkPrefix = $localIP -replace '\.\d+$', ''
        Write-ColorOutput "📡 Scanning network: $networkPrefix.0/24" "Yellow"
        
        # Scan common ranges
        $ranges = @(
            "$networkPrefix.1-254",
            "192.168.1.1-254",
            "192.168.0.1-254",
            "10.0.0.1-254"
        )
        
        foreach ($range in $ranges) {
            Write-ColorOutput "🔍 Checking range: $range" "Cyan"
            for ($i = 1; $i -le 254; $i++) {
                $testIP = $range -replace '1-254', $i
                if (Test-NetworkConnectivity $testIP) {
                    Write-Progress -Activity "Scanning for Android device" -Status "Testing $testIP" -PercentComplete (($i / 254) * 100)
                    
                    try {
                        $result = adb connect "$testIP`:5555" 2>&1
                        if ($result -match "connected") {
                            Write-Progress -Activity "Scanning for Android device" -Completed
                            Write-ColorOutput "✅ Found Android device at $testIP" "Green"
                            return $testIP
                        }
                    }
                    catch {}
                }
            }
        }
    }
    
    Write-Progress -Activity "Scanning for Android device" -Completed
    return $null
}

function Monitor-Connection {
    param($ip)
    Write-ColorOutput "🔄 Starting connection monitor..." "Yellow"
    while ($true) {
        $devices = adb devices
        if ($devices -notmatch "$($ip):5555") {
            Write-ColorOutput "⚠️ Connection lost. Attempting to reconnect..." "Yellow"
            adb connect "$($ip):5555" | Out-Null
            Start-Sleep -Seconds 2
        }
        Start-Sleep -Seconds 10
    }
}

# Main execution
Write-ColorOutput "🔗 ADB Wireless Manager" "Magenta"
Write-ColorOutput "================================" "Magenta"

# Check if ADB is available
try {
    $adbVersion = adb version
    Write-ColorOutput "✅ ADB is available" "Green"
}
catch {
    Write-ColorOutput "❌ ADB is not available in PATH" "Red"
    Write-ColorOutput "Please install Android SDK Platform Tools" "Yellow"
    exit 1
}

# Check for USB connected devices
$devices = adb devices
if ($devices -match "device$") {
    Write-ColorOutput "📱 USB device found. Setting up wireless connection..." "Green"
    
    # Enable wireless ADB
    $deviceIP = Enable-WirelessADB
    if ($deviceIP) {
        Write-ColorOutput "✅ You can now unplug the USB cable!" "Green"
        
        if ($AutoReconnect) {
            Monitor-Connection $deviceIP
        }
    }
}
else {
    Write-ColorOutput "📱 No USB device found. Attempting wireless connection..." "Yellow"
    
    # Try custom IP if provided
    if ($CustomIP) {
        Write-ColorOutput "🔗 Trying custom IP: $CustomIP" "Cyan"
        $result = adb connect "$CustomIP`:5555"
        if ($result -match "connected") {
            Write-ColorOutput "✅ Connected to custom IP!" "Green"
            if ($AutoReconnect) {
                Monitor-Connection $CustomIP
            }
            exit 0
        }
    }
    
    # Try last known IP
    $lastIPFile = "$PSScriptRoot\current_device_ip.txt"
    if (Test-Path $lastIPFile -and -not $ForceScan) {
        $lastIP = Get-Content $lastIPFile
        Write-ColorOutput "🔄 Trying last known IP: $lastIP" "Cyan"
        $result = adb connect "$lastIP`:5555"
        if ($result -match "connected") {
            Write-ColorOutput "✅ Connected to last known device!" "Green"
            if ($AutoReconnect) {
                Monitor-Connection $lastIP
            }
            exit 0
        }
    }
    
    # Scan network for devices
    if ($ForceScan -or -not (Test-Path $lastIPFile)) {
        $foundIP = Find-AndroidDevice
        if ($foundIP) {
            Write-ColorOutput "✅ Found and connected to device at $foundIP" "Green"
            $foundIP | Out-File -FilePath $lastIPFile -Encoding UTF8
            if ($AutoReconnect) {
                Monitor-Connection $foundIP
            }
        } else {
            Write-ColorOutput "❌ No Android device found on network" "Red"
            Write-ColorOutput "💡 Make sure:" "Yellow"
            Write-ColorOutput "   - Device has wireless ADB enabled" "Yellow"
            Write-ColorOutput "   - Both devices are on same network" "Yellow"
            Write-ColorOutput "   - Try connecting via USB first" "Yellow"
        }
    }
}

Write-ColorOutput "✅ Script completed!" "Green"

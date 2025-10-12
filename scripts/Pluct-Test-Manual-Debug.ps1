# Manual Debug Test for Pluct Choice Engine
# Tests the actual user experience step by step

param(
    [string]$TestUrl = "https://www.tiktok.com/@garyvee/video/7308801293029248299"
)

function Write-Log {
    param([string]$Message, [string]$Color = "White")
    Write-Host "[$(Get-Date -Format 'HH:mm:ss')] $Message" -ForegroundColor $Color
}

Write-Log "=== Manual Debug Test for Pluct Choice Engine ===" "Cyan"
Write-Log "This test will help us understand what's happening on the Android screen" "White"
Write-Log ""

# Check device
$devices = adb devices | Select-String "device$"
if ($devices.Count -eq 0) {
    Write-Log "No Android device connected!" "Red"
    exit 1
}

Write-Log "Device connected: $($devices[0].ToString().Split()[0])" "Green"

# Clear app data
Write-Log "Clearing app data..." "Yellow"
adb shell pm clear app.pluct

# Clear logcat
Write-Log "Clearing logcat..." "Yellow"
adb logcat -c

# Start logcat monitoring
Write-Log "Starting logcat monitoring..." "Yellow"
$logcatProcess = Start-Process -FilePath "adb" -ArgumentList "logcat", "-s", "MainActivity:D,ShareIngestActivity:D,HomeViewModel:D,HomeScreen:D" -NoNewWindow -PassThru

Start-Sleep -Seconds 2

# Launch the app first
Write-Log "Step 1: Launching Pluct app..." "Cyan"
adb shell am start -n app.pluct/app.pluct.MainActivity
Start-Sleep -Seconds 3

Write-Log "Step 2: Checking if app is in foreground..." "Cyan"
$currentActivity = adb shell dumpsys activity activities | Select-String "mResumedActivity" | Select-Object -First 1
Write-Log "Current Activity: $currentActivity" "White"

if ($currentActivity -match "MainActivity") {
    Write-Log "✅ App is in foreground" "Green"
} else {
    Write-Log "❌ App is not in foreground" "Red"
}

# Send share intent
Write-Log "Step 3: Sending share intent..." "Cyan"
$shareIntent = @(
    "shell", "am", "start",
    "-a", "android.intent.action.SEND",
    "-t", "text/plain",
    "--es", "android.intent.extra.TEXT", $TestUrl,
    "-n", "app.pluct/app.pluct.share.ShareIngestActivity"
)

& adb $shareIntent
Start-Sleep -Seconds 2

Write-Log "Step 4: Checking activity after share intent..." "Cyan"
$currentActivity = adb shell dumpsys activity activities | Select-String "mResumedActivity" | Select-Object -First 1
Write-Log "Current Activity: $currentActivity" "White"

if ($currentActivity -match "MainActivity") {
    Write-Log "✅ MainActivity is in foreground" "Green"
} else {
    Write-Log "❌ MainActivity is not in foreground" "Red"
}

# Wait for logs
Write-Log "Step 5: Waiting for capture sheet logs..." "Cyan"
Start-Sleep -Seconds 5

# Check for capture sheet logs
$captureLogs = adb logcat -d | Select-String "Displaying capture sheet|CaptureInsightSheet"
if ($captureLogs) {
    Write-Log "✅ Capture sheet logs found:" "Green"
    $captureLogs | ForEach-Object { Write-Log "  $_" "Gray" }
} else {
    Write-Log "❌ No capture sheet logs found" "Red"
}

# Check screen content
Write-Log "Step 6: Checking screen content..." "Cyan"
$screenText = adb shell dumpsys activity top | Select-String "TEXT"
if ($screenText) {
    Write-Log "Screen contains:" "Yellow"
    $screenText | Select-Object -First 10 | ForEach-Object { Write-Log "  $_" "Gray" }
} else {
    Write-Log "No screen text found" "Yellow"
}

# Stop logcat
Stop-Process -Id $logcatProcess.Id -Force -ErrorAction SilentlyContinue

Write-Log ""
Write-Log "=== Manual Debug Complete ===" "Cyan"
Write-Log "Check the output above to see what's happening on the Android screen" "White"

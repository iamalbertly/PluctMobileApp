# Build and Deploy Script for Transcription Flow Fixes
# Builds debug APK, uninstalls old app, installs new APK, and clears app data
# Follows naming: [Project]-[Build]-[Deploy]-[TranscriptionFixes]-[PowerShell]

param(
    [switch]$SkipBuild,
    [switch]$SkipDeploy,
    [switch]$SkipClearData
)

$ErrorActionPreference = "Stop"

Write-Host "🔨 Building and Deploying Transcription Flow Fixes" -ForegroundColor Cyan
Write-Host ""

# Step 1: Clean previous build artifacts
if (-not $SkipBuild) {
    Write-Host "Step 1: Cleaning previous build artifacts..." -ForegroundColor Yellow
    Push-Location $PSScriptRoot\..
    try {
        & .\gradlew.bat clean --no-daemon
        if ($LASTEXITCODE -ne 0) {
            Write-Host "   [WARNING] Clean failed, continuing anyway" -ForegroundColor Yellow
        } else {
            Write-Host "   [OK] Clean successful" -ForegroundColor Green
        }
    } finally {
        Pop-Location
    }
}

# Step 2: Build debug APK
if (-not $SkipBuild) {
    Write-Host ""
    Write-Host "Step 2: Building debug APK..." -ForegroundColor Yellow
    Push-Location $PSScriptRoot\..
    try {
        & .\gradlew.bat assembleDebug --no-daemon
        if ($LASTEXITCODE -ne 0) {
            Write-Host "   [ERROR] Build failed" -ForegroundColor Red
            exit 1
        }
        Write-Host "   [OK] Build successful" -ForegroundColor Green
    } finally {
        Pop-Location
    }
}

# Step 3: Verify APK created
$apkPath = Join-Path $PSScriptRoot "..\app\build\outputs\apk\debug\app-debug.apk"
if (-not (Test-Path $apkPath)) {
    Write-Host ""
    Write-Host "   [ERROR] APK not found: $apkPath" -ForegroundColor Red
    exit 1
}
Write-Host "   [OK] APK verified: $apkPath" -ForegroundColor Green

# Step 4: Check ADB connectivity
Write-Host ""
Write-Host "Step 4: Checking ADB connectivity..." -ForegroundColor Yellow
$deviceCheck = adb devices
if ($deviceCheck -notmatch "device$") {
    Write-Host "   [ERROR] No ADB device connected" -ForegroundColor Red
    Write-Host "   Please connect a device via ADB and try again." -ForegroundColor Yellow
    exit 1
}
Write-Host "   [OK] ADB device connected" -ForegroundColor Green

# Step 5: Uninstall existing app (if deploying)
if (-not $SkipDeploy) {
    Write-Host ""
    Write-Host "Step 5: Uninstalling existing app..." -ForegroundColor Yellow
    adb uninstall app.pluct
    if ($LASTEXITCODE -eq 0) {
        Write-Host "   [OK] App uninstalled" -ForegroundColor Green
    } else {
        Write-Host "   [WARNING] Uninstall failed (app may not have been installed)" -ForegroundColor Yellow
    }
}

# Step 6: Install new APK
if (-not $SkipDeploy) {
    Write-Host ""
    Write-Host "Step 6: Installing new APK..." -ForegroundColor Yellow
    adb install -r $apkPath
    if ($LASTEXITCODE -ne 0) {
        Write-Host "   [ERROR] Installation failed" -ForegroundColor Red
        Write-Host "   Check device storage and permissions" -ForegroundColor Yellow
        exit 1
    }
    Write-Host "   [OK] Installation successful" -ForegroundColor Green
}

# Step 7: Verify installation
Write-Host ""
Write-Host "Step 7: Verifying installation..." -ForegroundColor Yellow
$packageCheck = adb shell pm list packages | findstr pluct
if ($LASTEXITCODE -ne 0 -or -not $packageCheck) {
    Write-Host "   [ERROR] Installation verification failed" -ForegroundColor Red
    exit 1
}
Write-Host "   [OK] Installation verified: $packageCheck" -ForegroundColor Green

# Step 8: Clear app data for clean test (optional)
if (-not $SkipDeploy -and -not $SkipClearData) {
    Write-Host ""
    Write-Host "Step 8: Clearing app data for clean test..." -ForegroundColor Yellow
    adb shell pm clear app.pluct
    if ($LASTEXITCODE -eq 0) {
        Write-Host "   [OK] App data cleared" -ForegroundColor Green
    } else {
        Write-Host "   [WARNING] App data clear failed (may be intentional for some tests)" -ForegroundColor Yellow
    }
}

# Step 9: Smoke test - launch app and check for startup errors
Write-Host ""
Write-Host "Step 9: Running smoke test..." -ForegroundColor Yellow
adb shell am start -n app.pluct/.PluctUIScreen01MainActivity
Start-Sleep -Seconds 3

# Check logcat for startup errors
$startupErrors = adb logcat -d | findstr /i "FATAL\|Crash\|Exception.*MainActivity"
if ($startupErrors) {
    Write-Host "   [WARNING] Startup errors detected:" -ForegroundColor Yellow
    $startupErrors | Select-Object -First 3 | ForEach-Object {
        Write-Host "      $_" -ForegroundColor Yellow
    }
} else {
    Write-Host "   [OK] No startup errors detected" -ForegroundColor Green
}

# Verify main activity loaded
$activityCheck = adb shell dumpsys window windows | findstr /i "PluctUIScreen01MainActivity"
if ($activityCheck) {
    Write-Host "   [OK] Main activity loaded" -ForegroundColor Green
} else {
    Write-Host "   [WARNING] Main activity may not have loaded" -ForegroundColor Yellow
}

Write-Host ""
Write-Host "Build and deployment completed successfully!" -ForegroundColor Green
Write-Host "   APK: $apkPath" -ForegroundColor Gray
Write-Host '   Ready for testing' -ForegroundColor Gray

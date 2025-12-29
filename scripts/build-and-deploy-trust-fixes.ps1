# Build and Deploy Script for Trust Fixes
# Builds debug APK and installs to connected ADB device

Write-Host "🔨 Building debug APK..." -ForegroundColor Cyan

# Build debug APK
.\gradlew assembleDebug

# Verify build success
if ($LASTEXITCODE -ne 0) {
    Write-Host "❌ Build failed!" -ForegroundColor Red
    exit 1
}

Write-Host "✅ Build successful!" -ForegroundColor Green

# Check if device is connected
Write-Host "📱 Checking for connected device..." -ForegroundColor Cyan
$deviceCheck = adb devices
if ($deviceCheck -notmatch "device$") {
    Write-Host "❌ No device connected!" -ForegroundColor Red
    Write-Host "Please connect a device via ADB and try again." -ForegroundColor Yellow
    exit 1
}

Write-Host "✅ Device connected" -ForegroundColor Green

# Install to connected device
Write-Host "📦 Installing APK to device..." -ForegroundColor Cyan
adb install -r app/build/outputs/apk/debug/app-debug.apk

if ($LASTEXITCODE -ne 0) {
    Write-Host "❌ Installation failed!" -ForegroundColor Red
    exit 1
}

# Verify installation
Write-Host "✅ Verifying installation..." -ForegroundColor Cyan
$packageCheck = adb shell pm list packages | findstr pluct
if ($LASTEXITCODE -ne 0) {
    Write-Host "❌ Installation verification failed!" -ForegroundColor Red
    exit 1
}

Write-Host "✅ Installation verified: $packageCheck" -ForegroundColor Green
Write-Host "🎉 Build and deployment successful!" -ForegroundColor Green


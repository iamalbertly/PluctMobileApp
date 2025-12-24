# Pluct UX Fixes Test Runner Script
# Builds APK, deploys to device, and runs all UX fix tests

Write-Host "🚀 Starting Pluct UX Fixes Test Suite" -ForegroundColor Cyan
Write-Host ""

# Step 1: Verify ADB device connected
Write-Host "Step 1: Verifying ADB device connection..." -ForegroundColor Yellow
$adbDevices = adb devices
if ($LASTEXITCODE -ne 0) {
    Write-Host "❌ ADB command failed. Is ADB installed and in PATH?" -ForegroundColor Red
    exit 1
}

$deviceCount = ($adbDevices | Select-String "device$" | Measure-Object).Count
if ($deviceCount -eq 0) {
    Write-Host "❌ No ADB devices connected. Please connect a device and enable USB debugging." -ForegroundColor Red
    exit 1
}

Write-Host "✅ ADB device connected ($deviceCount device(s))" -ForegroundColor Green
Write-Host ""

# Step 2: Build debug APK
Write-Host "Step 2: Building debug APK..." -ForegroundColor Yellow
Set-Location app
$buildResult = & .\gradlew.bat assembleDebug 2>&1
if ($LASTEXITCODE -ne 0) {
    Write-Host "❌ Build failed!" -ForegroundColor Red
    Write-Host $buildResult
    Set-Location ..
    exit 1
}

$apkPath = "build\outputs\apk\debug\app-debug.apk"
if (-not (Test-Path $apkPath)) {
    Write-Host "❌ APK not found at expected path: $apkPath" -ForegroundColor Red
    Set-Location ..
    exit 1
}

Write-Host "✅ APK built successfully: $apkPath" -ForegroundColor Green
Set-Location ..
Write-Host ""

# Step 3: Install APK on device
Write-Host "Step 3: Installing APK on device..." -ForegroundColor Yellow
$installResult = adb install -r "app\$apkPath" 2>&1
if ($LASTEXITCODE -ne 0) {
    Write-Host "❌ APK installation failed!" -ForegroundColor Red
    Write-Host $installResult
    exit 1
}

Write-Host "✅ APK installed successfully" -ForegroundColor Green
Write-Host ""

# Step 4: Clear app data (optional, for fresh state)
Write-Host "Step 4: Clearing app data for fresh test state..." -ForegroundColor Yellow
adb shell pm clear app.pluct 2>&1 | Out-Null
Write-Host "✅ App data cleared" -ForegroundColor Green
Write-Host ""

# Step 5: Run test orchestrator with UX fix tests only
Write-Host "Step 5: Running UX fix test suite..." -ForegroundColor Yellow
Set-Location scripts\nodejs

$testList = @(
    "Journey-UX-01CreditsIcon-Validation",
    "Journey-UX-02CreditsLoading-Validation",
    "Journey-UX-03CreditRequestLogging-Validation",
    "Journey-UX-04ErrorPersistence-Validation",
    "Journey-UX-05RedundantVisuals-Validation",
    "Journey-UX-06CorrelationIds-Validation",
    "Journey-UX-07DebugLogsSearch-Validation",
    "Journey-UX-08CreditRequestFeedback-Validation",
    "Journey-UX-09ErrorRecoveryActions-Validation"
)

$testListString = $testList -join ","

$testResult = node Pluct-Main-01Orchestrator.js --tests $testListString 2>&1
$testExitCode = $LASTEXITCODE

Set-Location ..\..

Write-Host ""
if ($testExitCode -eq 0) {
    Write-Host "✅ All UX fix tests passed!" -ForegroundColor Green
} else {
    Write-Host "❌ Some tests failed. Exit code: $testExitCode" -ForegroundColor Red
    Write-Host $testResult
    exit $testExitCode
}

Write-Host ""
Write-Host "🎉 UX Fixes Test Suite Complete!" -ForegroundColor Cyan
Write-Host ""

# Step 6: Generate test report (handled by orchestrator)
Write-Host "Test reports saved to: scripts/nodejs/artifacts/" -ForegroundColor Cyan

exit 0


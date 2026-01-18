# Pluct-Test-Focused-03TranscriptionFixes-01PowerShell.ps1
# Focused test runner for transcription fixes with auto-fix and retry
# Follows naming: [Project]-[Test]-[Focused]-[TranscriptionFixes]-[PowerShell]
# 5 scope layers: Project, Test, Focused, TranscriptionFixes, PowerShell

param(
    [switch]$SkipBuild,
    [switch]$SkipDeploy,
    [switch]$DevMode
)

$ErrorActionPreference = "Stop"

# Configuration
$ProjectRoot = $PSScriptRoot
$TestRunner = Join-Path $ProjectRoot "nodejs\Pluct-Test-Focused-03TranscriptionFixes-01Runner.js"
$BuildScript = Join-Path $ProjectRoot "build-and-deploy-transcription-fixes.ps1"

Write-Host "🎯 Focused Transcription Fixes Test Runner" -ForegroundColor Cyan
Write-Host "   Project Root: $ProjectRoot" -ForegroundColor Gray
Write-Host "   Mode: $(if ($DevMode) { 'DEV (terminates on first error)' } else { 'PROD (continues on errors)' })" -ForegroundColor Gray
Write-Host ""

# Step 1: Build and deploy (if not skipped)
if (-not $SkipBuild -and -not $SkipDeploy) {
    Write-Host "Step 1: Building and deploying..." -ForegroundColor Yellow
    Push-Location $ProjectRoot
    try {
        & $BuildScript -SkipClearData
        if ($LASTEXITCODE -ne 0) {
            Write-Host "   [ERROR] Build/deploy failed" -ForegroundColor Red
            exit 1
        }
        Write-Host "   [OK] Build and deploy successful" -ForegroundColor Green
    } finally {
        Pop-Location
    }
}

# Step 2: Clear logcat for clean test
Write-Host ""
Write-Host "Step 2: Clearing logcat..." -ForegroundColor Yellow
adb logcat -c
if ($LASTEXITCODE -eq 0) {
    Write-Host "   [OK] Logcat cleared" -ForegroundColor Green
} else {
    Write-Host "   [WARNING] Failed to clear logcat, continuing anyway" -ForegroundColor Yellow
}

# Step 3: Run focused tests
Write-Host ""
Write-Host "Step 3: Running focused transcription fixes tests..." -ForegroundColor Yellow
Write-Host ""

# Set environment variables
$env:DEV_MODE = if ($DevMode) { "1" } else { "0" }

# Run Node.js focused test runner
Push-Location $ProjectRoot
try {
    node $TestRunner
    $testExitCode = $LASTEXITCODE
} finally {
    Pop-Location
}

# Step 4: Report results
Write-Host ""
if ($testExitCode -eq 0) {
    Write-Host "[OK] All focused tests passed!" -ForegroundColor Green
    exit 0
} else {
    Write-Host "[ERROR] Some focused tests failed" -ForegroundColor Red
    if ($DevMode) {
        Write-Host "   Dev mode: Terminating on first error" -ForegroundColor Yellow
    }
    exit $testExitCode
}

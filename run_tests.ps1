<#
Deprecated PowerShell runner.
This repository now uses the Node-based orchestrator as the canonical test entrypoint.
Use `npm run test:all` which invokes `maestro/Pluct-Maestro-Test-01Runner-01Orchestrator.js`.

The original PowerShell runner has been retired to avoid duplicate/conflicting test entrypoints.
#>

Write-Host "This PowerShell test runner is deprecated. Use 'npm run test:all' instead." -ForegroundColor Yellow
Exit 1

Write-Host "Pluct Maestro Test Runner" -ForegroundColor Cyan
Write-Host "   Project Root: $ProjectRoot" -ForegroundColor Gray
Write-Host "   Mode: $(if ($DevMode) { 'DEV (terminates on first error)' } else { 'PROD (continues on errors)' })" -ForegroundColor Gray
Write-Host ""

# Step 1: Validate environment
Write-Host "Step 1: Validating environment..." -ForegroundColor Yellow

# Check Maestro installation
$maestroPath = $null
try {
    $maestroVersion = maestro --version 2>&1
    $maestroPath = "maestro"
    Write-Host "   [OK] Maestro installed: $maestroVersion" -ForegroundColor Green
} catch {
    # Try default installation location
    $defaultMaestroPath = Join-Path $env:USERPROFILE ".maestro\bin\maestro.bat"
    if (Test-Path $defaultMaestroPath) {
        $maestroPath = $defaultMaestroPath
        $maestroVersion = & $maestroPath --version 2>&1
        Write-Host "   [OK] Maestro installed (from default location): $maestroVersion" -ForegroundColor Green
    } else {
        Write-Host "   [ERROR] Maestro not found. Install with: curl -Ls 'https://get.maestro.mobile.dev' | bash" -ForegroundColor Red
        exit 1
    }
}

# Check ADB connection
try {
    $adbDevices = adb devices 2>&1 | Select-String "device$"
    if ($adbDevices) {
        Write-Host "   [OK] ADB device connected" -ForegroundColor Green
    } else {
        Write-Host "   [ERROR] No ADB device connected" -ForegroundColor Red
        exit 1
    }
} catch {
    Write-Host "   [ERROR] ADB not found in PATH" -ForegroundColor Red
    exit 1
}

# Check Node.js
try {
    $nodeVersion = node --version 2>&1
    Write-Host "   [OK] Node.js installed: $nodeVersion" -ForegroundColor Green
} catch {
    Write-Host "   [ERROR] Node.js not found" -ForegroundColor Red
    exit 1
}

# Step 2: Build APK (if not skipped)
if (-not $SkipBuild) {
    Write-Host ""
    Write-Host "Step 2: Building APK..." -ForegroundColor Yellow
    Push-Location $ProjectRoot
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
} else {
    Write-Host ""
    Write-Host "Step 2: Skipping build (SkipBuild flag)" -ForegroundColor Gray
}

# Step 3: Deploy APK (if not skipped)
if (-not $SkipDeploy) {
    Write-Host ""
    Write-Host "Step 3: Deploying APK to device..." -ForegroundColor Yellow
    $apkPath = Join-Path $ProjectRoot "app\build\outputs\apk\debug\app-debug.apk"
    if (Test-Path $apkPath) {
        adb install -r $apkPath
        if ($LASTEXITCODE -ne 0) {
            Write-Host "   [ERROR] Deployment failed" -ForegroundColor Red
            exit 1
        }
        Write-Host "   [OK] Deployment successful" -ForegroundColor Green
    } else {
        Write-Host "   [ERROR] APK not found: $apkPath" -ForegroundColor Red
        exit 1
    }
} else {
    Write-Host ""
    Write-Host "Step 3: Skipping deployment (SkipDeploy flag)" -ForegroundColor Gray
}

# Step 4: Run Maestro tests
Write-Host ""
Write-Host "Step 4: Running Maestro tests..." -ForegroundColor Yellow

# Set environment variables for Node.js runner
$env:DEV_MODE = if ($DevMode) { "1" } else { "0" }
if ($Filter) {
    $env:MAESTRO_FILTER = $Filter
}
if ($Category) {
    $env:MAESTRO_CATEGORY = $Category
}

# Run Node.js orchestrator
Push-Location $ProjectRoot
try {
    node $NodeRunner
    $testExitCode = $LASTEXITCODE
} finally {
    Pop-Location
}

# Step 5: Report results
Write-Host ""
if ($testExitCode -eq 0) {
    Write-Host "[OK] All tests passed!" -ForegroundColor Green
    exit 0
} else {
    Write-Host "[ERROR] Some tests failed" -ForegroundColor Red
    exit $testExitCode
}

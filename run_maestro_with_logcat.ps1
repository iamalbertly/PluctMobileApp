# Pluct-Maestro-Test-01Runner-01WithLogcat-01PowerShell.ps1
# Runs Maestro tests with real-time logcat monitoring
# Follows naming: [Project]-[Maestro]-[Test]-[01Runner]-[01WithLogcat]-[01PowerShell]

param(
    [switch]$SkipBuild,
    [switch]$SkipDeploy,
    [switch]$DevMode,
    [string]$Filter = "",
    [string]$Category = ""
)

$ErrorActionPreference = "Stop"

# Configuration
$ProjectRoot = $PSScriptRoot
$MaestroDir = Join-Path $ProjectRoot "maestro"
$NodeRunner = Join-Path $MaestroDir "Pluct-Maestro-Test-01Runner-01Orchestrator.js"
$LogcatFile = Join-Path $ProjectRoot "artifacts\logs\maestro_test_logcat.log"
$FilteredLogcatFile = Join-Path $ProjectRoot "artifacts\logs\maestro_test_logcat_filtered.log"

Write-Host "Pluct Maestro Test Runner with Logcat Monitoring" -ForegroundColor Cyan
Write-Host "   Project Root: $ProjectRoot" -ForegroundColor Gray
Write-Host "   Logcat Output: $LogcatFile" -ForegroundColor Gray
Write-Host "   Filtered Logcat Output: $FilteredLogcatFile" -ForegroundColor Gray
Write-Host ""

# Ensure artifacts directory exists
$artifactsDir = Join-Path $ProjectRoot "artifacts\logs"
if (-not (Test-Path $artifactsDir)) {
    New-Item -ItemType Directory -Path $artifactsDir -Force | Out-Null
}

# Step 1: Clear logcat
Write-Host "Step 1: Clearing logcat..." -ForegroundColor Yellow
adb logcat -c
if ($LASTEXITCODE -eq 0) {
    Write-Host "   [OK] Logcat cleared" -ForegroundColor Green
} else {
    Write-Host "   [WARNING] Failed to clear logcat, continuing anyway" -ForegroundColor Yellow
}

# Step 2: Start logcat monitoring in background
Write-Host "Step 2: Starting logcat monitoring..." -ForegroundColor Yellow
$logcatJob = Start-Job -ScriptBlock {
    param($logFile)
    adb logcat -v time *:V > $logFile 2>&1
} -ArgumentList $LogcatFile

Write-Host "   [OK] Logcat monitoring started (PID: $($logcatJob.Id))" -ForegroundColor Green

# Step 2b: Start filtered logcat monitoring in background for real-time error detection
Write-Host "Step 2b: Starting filtered logcat monitoring..." -ForegroundColor Yellow
$filteredLogcatJob = Start-Job -ScriptBlock {
    param($logFile)
    # Filter for PluctCoreAPIUnified, TranscriptionFlowHandler, CaptureCard, and all errors
    adb logcat -v time PluctCoreAPIUnified:* TranscriptionFlowHandler:* CaptureCard:* TranscriptionWorker:* IntentHandler:* MainActivity:* *:E > $logFile 2>&1
} -ArgumentList $FilteredLogcatFile

Write-Host "   [OK] Filtered logcat monitoring started (PID: $($filteredLogcatJob.Id))" -ForegroundColor Green

# Step 3: Build and deploy (if not skipped)
if (-not $SkipBuild) {
    Write-Host ""
    Write-Host "Step 3: Building APK..." -ForegroundColor Yellow
    Push-Location $ProjectRoot
    try {
        & .\gradlew.bat assembleDebug --no-daemon
        if ($LASTEXITCODE -ne 0) {
            Write-Host "   [ERROR] Build failed" -ForegroundColor Red
            Stop-Job $logcatJob
            Stop-Job $filteredLogcatJob
            Remove-Job $logcatJob
            Remove-Job $filteredLogcatJob
            exit 1
        }
        Write-Host "   [OK] Build successful" -ForegroundColor Green
    } finally {
        Pop-Location
    }
}

if (-not $SkipDeploy) {
    Write-Host ""
    Write-Host "Step 4: Deploying APK..." -ForegroundColor Yellow
    $apkPath = Join-Path $ProjectRoot "app\build\outputs\apk\debug\app-debug.apk"
    if (Test-Path $apkPath) {
        adb install -r $apkPath
        if ($LASTEXITCODE -ne 0) {
            Write-Host "   [ERROR] Deployment failed" -ForegroundColor Red
            Stop-Job $logcatJob
            Stop-Job $filteredLogcatJob
            Remove-Job $logcatJob
            Remove-Job $filteredLogcatJob
            exit 1
        }
        Write-Host "   [OK] Deployment successful" -ForegroundColor Green
    } else {
        Write-Host "   [ERROR] APK not found: $apkPath" -ForegroundColor Red
        Stop-Job $logcatJob
        Stop-Job $filteredLogcatJob
        Remove-Job $logcatJob
        Remove-Job $filteredLogcatJob
        exit 1
    }
}

# Step 5: Run Maestro tests with real-time error monitoring
Write-Host ""
Write-Host "Step 5: Running Maestro tests with logcat monitoring..." -ForegroundColor Yellow
Write-Host "   (Full logcat is being captured to: $LogcatFile)" -ForegroundColor Gray
Write-Host "   (Filtered logcat is being captured to: $FilteredLogcatFile)" -ForegroundColor Gray
Write-Host ""

# Function to check for critical errors in filtered logcat
function Check-FilteredLogcatErrors {
    if (Test-Path $FilteredLogcatFile) {
        $recentErrors = Get-Content $FilteredLogcatFile -Tail 50 | Select-String -Pattern "E/|ERROR|FATAL|Exception|Crash" -CaseSensitive:$false
        if ($recentErrors) {
            Write-Host "   [WARNING] Recent errors detected in filtered logcat:" -ForegroundColor Yellow
            $recentErrors | Select-Object -First 5 | ForEach-Object {
                Write-Host "      $($_.Line)" -ForegroundColor Yellow
            }
        }
    }
}

# Start background job to monitor filtered logcat for errors during test execution
$errorMonitorJob = Start-Job -ScriptBlock {
    param($logFile)
    $lastCheck = Get-Date
    while ($true) {
        Start-Sleep -Seconds 10
        if (Test-Path $logFile) {
            $content = Get-Content $logFile -Tail 20 -ErrorAction SilentlyContinue
            $criticalErrors = $content | Select-String -Pattern "FATAL|Crash|unclosed|Exception.*TranscriptionFlowHandler" -CaseSensitive:$false
            if ($criticalErrors) {
                Write-Output "CRITICAL_ERROR_DETECTED"
                break
            }
        }
    }
} -ArgumentList $FilteredLogcatFile

Write-Host ""

# Set environment variables
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
    # Check for errors periodically during test execution
    $testProcess = Start-Process -FilePath "node" -ArgumentList $NodeRunner -PassThru -NoNewWindow -Wait
    $testExitCode = $testProcess.ExitCode
    
    # Check filtered logcat for errors after test completion
    Check-FilteredLogcatErrors
} finally {
    Pop-Location
}

# Stop error monitor job
Stop-Job $errorMonitorJob -ErrorAction SilentlyContinue
Remove-Job $errorMonitorJob -ErrorAction SilentlyContinue

# Step 6: Stop logcat monitoring
Write-Host ""
Write-Host "Step 6: Stopping logcat monitoring..." -ForegroundColor Yellow
Stop-Job $logcatJob
Stop-Job $filteredLogcatJob
Wait-Job $logcatJob | Out-Null
Wait-Job $filteredLogcatJob | Out-Null
$logcatOutput = Receive-Job $logcatJob
$filteredLogcatOutput = Receive-Job $filteredLogcatJob
Remove-Job $logcatJob
Remove-Job $filteredLogcatJob

Write-Host "   [OK] Logcat monitoring stopped" -ForegroundColor Green
Write-Host "   Full logcat saved to: $LogcatFile" -ForegroundColor Gray
Write-Host "   Filtered logcat saved to: $FilteredLogcatFile" -ForegroundColor Gray

# Step 7: Analyze logcat for errors
Write-Host ""
Write-Host "Step 7: Analyzing logcat for errors..." -ForegroundColor Yellow

# Analyze filtered logcat first (more relevant)
if (Test-Path $FilteredLogcatFile) {
    $filteredErrorCount = (Select-String -Path $FilteredLogcatFile -Pattern "E/|ERROR|FATAL|Exception" -CaseSensitive:$false | Measure-Object).Count
    $filteredWarningCount = (Select-String -Path $FilteredLogcatFile -Pattern "W/|WARN" -CaseSensitive:$false | Measure-Object).Count
    
    Write-Host "   Filtered logcat errors: $filteredErrorCount" -ForegroundColor $(if ($filteredErrorCount -gt 0) { "Red" } else { "Green" })
    Write-Host "   Filtered logcat warnings: $filteredWarningCount" -ForegroundColor $(if ($filteredWarningCount -gt 50) { "Yellow" } else { "Gray" })
    
    if ($filteredErrorCount -gt 0) {
        Write-Host ""
        Write-Host "   Sample errors from filtered logcat:" -ForegroundColor Yellow
        Select-String -Path $FilteredLogcatFile -Pattern "E/|ERROR|FATAL|Exception" -CaseSensitive:$false | Select-Object -First 10 | ForEach-Object {
            Write-Host "      $($_.Line)" -ForegroundColor Red
        }
    }
}

# Also analyze full logcat
if (Test-Path $LogcatFile) {
    $errorCount = (Select-String -Path $LogcatFile -Pattern "E/|ERROR|FATAL|Exception" -CaseSensitive:$false | Measure-Object).Count
    $warningCount = (Select-String -Path $LogcatFile -Pattern "W/|WARN" -CaseSensitive:$false | Measure-Object).Count
    
    Write-Host "   Full logcat errors: $errorCount" -ForegroundColor $(if ($errorCount -gt 0) { "Red" } else { "Green" })
    Write-Host "   Full logcat warnings: $warningCount" -ForegroundColor $(if ($warningCount -gt 100) { "Yellow" } else { "Gray" })
}

# Step 8: Report results
Write-Host ""
if ($testExitCode -eq 0) {
    Write-Host "[OK] All tests passed!" -ForegroundColor Green
    exit 0
} else {
    Write-Host "[ERROR] Some tests failed" -ForegroundColor Red
    Write-Host "   Check filtered logcat file for details: $FilteredLogcatFile" -ForegroundColor Yellow
    Write-Host "   Check full logcat file for details: $LogcatFile" -ForegroundColor Yellow
    exit $testExitCode
}

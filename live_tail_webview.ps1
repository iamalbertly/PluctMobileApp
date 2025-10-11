# Live Tail WebView - Development Monitoring Script
# Enhanced version with comprehensive logging for debugging intent handling

param(
    [string]$TestUrl = "https://vm.tiktok.com/ZMASgAxR2",
    [int]$DuplicateThreshold = 40,
    [string]$OutputDir = "artifacts"
)

Write-Host "=== WebView Live Tail Development Monitor (Enhanced) ===" -ForegroundColor Cyan
Write-Host "Target URL: $TestUrl" -ForegroundColor Yellow
Write-Host "Starting comprehensive live tail monitoring..." -ForegroundColor Green

# Clear logcat and set up enhanced logging
Write-Host "Setting up comprehensive diagnostic logging..." -ForegroundColor Cyan
adb logcat -G 32M
adb logcat -c
adb shell setprop log.tag.WVConsole VERBOSE
adb shell setprop log.tag.WebViewUtils VERBOSE
adb shell setprop log.tag.ShareIngestActivity VERBOSE
adb shell setprop log.tag.MainActivity VERBOSE
adb shell setprop log.tag.PluctNavigation VERBOSE
adb shell setprop log.tag.IngestScreen VERBOSE

# Prepare output directories
$screenshotsDir = Join-Path $OutputDir "screenshots"
$logsDir = Join-Path $OutputDir "logs"
New-Item -ItemType Directory -Path $screenshotsDir -Force | Out-Null
New-Item -ItemType Directory -Path $logsDir -Force | Out-Null

# Check if app is already running
Write-Host "Checking if Pluct app is already running..." -ForegroundColor Cyan
$runningApps = adb shell "ps | grep app.pluct"
if ($runningApps -match "app.pluct") {
    Write-Host "Pluct app is already running" -ForegroundColor Green
} else {
    Write-Host "Pluct app is not running" -ForegroundColor Yellow
}

# Launch app with test URL
Write-Host "Launching app with test URL..." -ForegroundColor Cyan
$launchResult = adb shell am start -W -a android.intent.action.SEND -t text/plain --es android.intent.extra.TEXT $TestUrl -n app.pluct/.share.ShareIngestActivity

Write-Host "Launch result:" -ForegroundColor Cyan
Write-Host $launchResult -ForegroundColor White

if ($LASTEXITCODE -eq 0) {
    Write-Host "App launched successfully" -ForegroundColor Green
} else {
    Write-Host "Failed to launch app" -ForegroundColor Red
    exit 1
}

# Start comprehensive live tail monitoring
Write-Host "Starting comprehensive live tail monitoring..." -ForegroundColor Cyan
Write-Host "Monitoring all relevant components:" -ForegroundColor Yellow
Write-Host "  - ShareIngestActivity (intent handling)" -ForegroundColor White
Write-Host "  - MainActivity (deep link processing)" -ForegroundColor White
Write-Host "  - WebViewUtils (webview configuration)" -ForegroundColor White
Write-Host "  - WVConsole (automation scripts)" -ForegroundColor White
Write-Host "  - PluctNavigation (navigation handling)" -ForegroundColor White
Write-Host "  - IngestScreen (UI processing)" -ForegroundColor White
Write-Host "Press Ctrl+C to stop monitoring" -ForegroundColor Yellow
Write-Host ""

# Rolling buffer and duplication/error detection
$buffer = New-Object System.Collections.Generic.List[string]
$lineCounts = @{}
$captured = $false
$shouldTerminate = $false
$triggerLine = $null
$sessionTs = Get-Date -Format "yyyyMMdd_HHmmss"
$sessionLog = Join-Path $logsDir ("session_" + $sessionTs + ".log")

function Normalize-Key([string]$line) {
    return ($line -replace "\d{1,10}", "#" -replace "run=[a-f0-9\-]+", "run=<run>" -replace "len=\d+", "len=<n>")
}

function Capture-Artifacts([string]$reason, [string]$trigger) {
    if ($captured) { return }
    $captured = $true
    $ts = Get-Date -Format "yyyyMMdd_HHmmss"
    $png = Join-Path $screenshotsDir ("pluct_" + $ts + ".png")
    $htmlOut = Join-Path $logsDir ("webview_html_" + $ts + ".txt")
    $ctxOut = Join-Path $logsDir ("context_" + $ts + ".log")

    Write-Host "Triggering capture due to: $reason" -ForegroundColor Red
    Write-Host "Taking device screenshot..." -ForegroundColor Cyan
    try {
        adb exec-out screencap -p > $png
        Write-Host "Saved screenshot: $png" -ForegroundColor Green
    } catch { Write-Host "Failed screenshot: $_" -ForegroundColor Red }

    Write-Host "Saving recent WebView HTML logs..." -ForegroundColor Cyan
    try {
        $htmlLines = $buffer | Where-Object { $_ -match "WVConsole:html_(dump|head)" }
        if ($htmlLines.Count -gt 0) { $htmlLines | Out-File -FilePath $htmlOut -Encoding UTF8 } else { "<no html_dump lines captured yet>" | Out-File -FilePath $htmlOut -Encoding UTF8 }
        Write-Host "Saved HTML log: $htmlOut" -ForegroundColor Green
    } catch { Write-Host "Failed saving HTML log: $_" -ForegroundColor Red }

    # Build diagnostic context
    $lastState = ($buffer | Where-Object { $_ -match 'state_change_to=' } | Select-Object -Last 1)
    $lastAction = ($buffer | Where-Object { $_ -match 'tokaudit_' } | Select-Object -Last 1)
    $lastUrl = ($buffer | Where-Object { $_ -match 'WVConsole(Log)?:WV:J:url=' -or $_ -match 'WVConsole:url=' } | Select-Object -Last 1)
    $lastHtml = ($buffer | Where-Object { $_ -match 'WVConsole:html_(dump|head)=' } | Select-Object -Last 1)
    $recent = $buffer | Select-Object -Last 150
    $diagnostic = @(
        "==== FAILURE DIAGNOSTICS ====",
        "Reason: $reason",
        "Trigger: $trigger",
        "Last URL: $lastUrl",
        "Last State: $lastState",
        "Last Action: $lastAction",
        "Latest html_dump (truncated):",
        ($lastHtml | ForEach-Object { if ($_ -ne $null) { $_.Substring(0, [Math]::Min($_.Length, 400)) } else { "<none>" } }),
        "-- Recent log context (last 150 lines) --"
    )
    $diagnostic | Out-File -FilePath $ctxOut -Encoding UTF8
    $recent | Out-File -FilePath $ctxOut -Append -Encoding UTF8

    # Print summary to console for quick triage
    $diagnostic | ForEach-Object { Write-Host $_ -ForegroundColor Yellow }
    $recent | ForEach-Object { Write-Host $_ }

    Write-Host "Artifacts saved under: $OutputDir" -ForegroundColor Yellow

    # Signal termination
    $script:shouldTerminate = $true
}

# Start logcat, tee to session log, and live-process lines for errors/duplication
adb logcat -v brief `
    ShareIngestActivity:V `
    MainActivity:V `
    WebViewUtils:V `
    WVConsole:V `
    WVConsoleLog:V `
    WVConsoleErr:V `
    WVConsoleWarn:V `
    WVConsoleDbg:V `
    PluctNavigation:V `
    IngestScreen:V `
    Ingest:V `
    JavaScriptBridge:V `
    cr_Console:V `
    ActivityManager:I `
    *:S | Tee-Object -FilePath $sessionLog | ForEach-Object {
        $line = $_
        # Maintain rolling buffer (last 2000 lines)
        $buffer.Add($line)
        if ($buffer.Count -gt 2000) { $buffer.RemoveAt(0) }

        # Emit to console
        Write-Host $line

        # Error triggers (exclude known critical warnings)
        $isCriticalWarning = ($line -match "tokaudit_critical_warning_invalid_data") -or ($line -match "automation_completed_with_warning=invalid_data") -or ($line -match "state_change_to=WARNING") -or ($line -match "WV:A:js_warning=invalid_data")
        if (($line -match "WV:A:js_error=" -or $line -match "automation_failed" -or $line -match "state_change_to=FAILED") -and -not $isCriticalWarning) {
            $triggerLine = $line
            Capture-Artifacts "error_detected" $triggerLine
            if ($shouldTerminate) {
                $lastState = ($buffer | Where-Object { $_ -match 'state_change_to=' } | Select-Object -Last 1)
                $lastAction = ($buffer | Where-Object { $_ -match 'tokaudit_' } | Select-Object -Last 1)
                Write-Host ("Terminating due to error while state='{0}' action='{1}'. Trigger line:" -f $lastState, $lastAction) -ForegroundColor Red
                Write-Host $triggerLine -ForegroundColor Red
                exit 1
            }
        }

        # Duplication detection
        $key = Normalize-Key $line
        if ($lineCounts.ContainsKey($key)) { $lineCounts[$key]++ } else { $lineCounts[$key] = 1 }
        if (-not $captured -and $lineCounts[$key] -ge $DuplicateThreshold -and -not $isCriticalWarning) {
            $triggerLine = $line
            $dupReason = "duplication x" + $lineCounts[$key]
            Capture-Artifacts $dupReason $triggerLine
            if ($shouldTerminate) {
                $lastState = ($buffer | Where-Object { $_ -match 'state_change_to=' } | Select-Object -Last 1)
                $lastAction = ($buffer | Where-Object { $_ -match 'tokaudit_' } | Select-Object -Last 1)
                Write-Host ("Terminating due to {0} while state='{1}' action='{2}'. Trigger line:" -f $dupReason, $lastState, $lastAction) -ForegroundColor Red
                Write-Host $triggerLine -ForegroundColor Red
                exit 2
            }
        }
    }

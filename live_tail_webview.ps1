# Live Tail WebView - Development Monitoring Script
# Use this exact command to watch the run during development

param(
    [string]$TestUrl = "https://vm.tiktok.com/ZMA2MTD9C"
)

Write-Host "=== WebView Live Tail Development Monitor ===" -ForegroundColor Cyan
Write-Host "Target URL: $TestUrl" -ForegroundColor Yellow
Write-Host "Starting live tail monitoring..." -ForegroundColor Green

# Clear logcat and set up logging
Write-Host "Setting up diagnostic logging..." -ForegroundColor Cyan
adb logcat -G 16M
adb logcat -c
adb shell setprop log.tag.WVConsole VERBOSE
adb shell setprop log.tag.WebViewUtils VERBOSE

# Launch app with test URL
Write-Host "Launching app with test URL..." -ForegroundColor Cyan
adb shell am start -W -a android.intent.action.SEND -t text/plain --es android.intent.extra.TEXT $TestUrl -n app.pluct/.share.ShareIngestActivity

if ($LASTEXITCODE -eq 0) {
    Write-Host "App launched successfully" -ForegroundColor Green
} else {
    Write-Host "Failed to launch app" -ForegroundColor Red
    exit 1
}

# Start live tail monitoring
Write-Host "Starting live tail monitoring..." -ForegroundColor Cyan
Write-Host "Press Ctrl+C to stop monitoring" -ForegroundColor Yellow
Write-Host ""

# Execute the exact command from requirements
adb logcat -v brief WVConsole:V WebViewUtils:V Ingest:V cr_Console:V *:S

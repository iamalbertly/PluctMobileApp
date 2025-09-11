# ClipForge Test Framework - Configuration Module
# Contains all configuration settings for the test framework

# Default test URL
$script:DefaultTestUrl = "https://vm.tiktok.com/ZMA2MTD9C"

# Log tags for filtering logcat output
$script:LOG_TAGS = @{
    WebView = "WVConsole:V WebViewUtils:V WebViewClientFactory:V WebViewConfiguration:V WebViewSettings:V WebTranscriptActivity:V"
    App = "Ingest:V ScriptTokAudit:V JavaScriptBridge:V WebViewScripts:V"
    System = "ActivityManager:E AndroidRuntime:E"
    Chrome = "chromium:V cr_Console:V"
    Default = "*:S"
}

# HTML markers to look for in WebView content
$script:HTML_MARKERS = @{
    TikTokPage = "<title>TikTok</title>"
    TranscriptExtracted = "Transcript extracted"
    ErrorDialog = "Error dialog shown"
    LoadingIndicator = "Loading..."
}

# Terminal outcomes to look for in logs
$script:TERMINAL_OUTCOMES = @{
    Success = "SUCCESS: Transcript extracted"
    Error = "ERROR:"
    InvalidUrl = "Invalid URL format"
    NetworkError = "Network error"
    Timeout = "Operation timed out"
}

# App package and activity names
$script:APP_PACKAGE = "app.pluct"
$script:APP_ACTIVITY = "$script:APP_PACKAGE.MainActivity"

# Timeouts
$script:TIMEOUTS = @{
    Short = 5    # 5 seconds
    Medium = 15  # 15 seconds
    Long = 30    # 30 seconds
    VeryLong = 60 # 60 seconds
}

# Paths
$script:ProjectRoot = Split-Path -Parent (Split-Path -Parent $PSScriptRoot)
$script:LogsDirectory = "$script:ProjectRoot\TestFramework\Logs"
$script:HtmlCapturesDirectory = "$script:ProjectRoot\html_captures"
$script:ApkPath = "$script:ProjectRoot\app\build\outputs\apk\debug\app-debug.apk"

# Create directories if they don't exist
if (-not (Test-Path $script:LogsDirectory)) {
    New-Item -Path $script:LogsDirectory -ItemType Directory -Force | Out-Null
}

if (-not (Test-Path $script:HtmlCapturesDirectory)) {
    New-Item -Path $script:HtmlCapturesDirectory -ItemType Directory -Force | Out-Null
}

# Log file path with timestamp
$timestamp = Get-Date -Format "yyyyMMdd_HHmmss"
$script:LogFile = "$script:LogsDirectory\test_run_$timestamp.log"

# Export variables
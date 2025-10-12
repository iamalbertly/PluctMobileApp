# Pluct Test Core Screenshot Management
# Automated screenshot capture for debugging

function Capture-Screenshot {
    param(
        [string]$Description,
        [switch]$OnError
    )
    
    $timestamp = Get-Date -Format "yyyyMMdd_HHmmss"
    $screenshotDir = "TestFramework\Screenshots"
    
    if (-not (Test-Path $screenshotDir)) {
        New-Item -Path $screenshotDir -ItemType Directory -Force | Out-Null
    }
    
    $filename = "${Description}_${timestamp}.png"
    $filepath = "$screenshotDir\$filename"
    
    Write-Log "Capturing screenshot: $filename" "Yellow"
    adb shell screencap -p | Set-Content -Path $filepath -Encoding Byte
    
    $script:TestSession.Screenshots += $filepath
    return $filepath
}

function Capture-ScreenshotOnError {
    param([string]$Context)
    
    $screenshot = Capture-Screenshot -Description "ERROR_$Context" -OnError
    Write-Log "Error screenshot captured: $screenshot" "Red"
    return $screenshot
}

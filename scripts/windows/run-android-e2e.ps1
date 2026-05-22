Set-StrictMode -Version Latest
$ErrorActionPreference = "Stop"

function Invoke-PluctE2EPreflight {
    Write-Host "Pluct E2E preflight: adb wait-for-device, boot check, wake, keyguard, swipe..."
    adb wait-for-device
    $bootOk = $false
    for ($i = 0; $i -lt 36; $i++) {
        $prop = adb shell getprop sys.boot_completed 2>$null
        if ($prop -match "1") { $bootOk = $true; break }
        Start-Sleep -Seconds 2
    }
    if (-not $bootOk) { Write-Warning "sys.boot_completed not 1 yet; continuing anyway." }
    adb shell input keyevent 224 2>$null
    adb shell wm dismiss-keyguard 2>$null
    adb shell input swipe 520 1850 520 600 320 2>$null
    Start-Sleep -Seconds 1
}

Push-Location (Resolve-Path "$PSScriptRoot\..\..")
try {
    Invoke-PluctE2EPreflight
    adb devices
    npm install
    $output = npm run test:all 2>&1
    $output | ForEach-Object { Write-Host $_ }
    if ($LASTEXITCODE -ne 0) {
        exit $LASTEXITCODE
    }
    $joinedOutput = $output -join "`n"
    if ($joinedOutput -match "Tests failed|FAILED WITH EXCEPTION|FAILED \(") {
        Write-Error "Android e2e harness reported failures."
        exit 1
    }
}
finally {
    Pop-Location
}

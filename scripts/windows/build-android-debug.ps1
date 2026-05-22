Set-StrictMode -Version Latest
$ErrorActionPreference = "Stop"

Push-Location (Resolve-Path "$PSScriptRoot\..\..")
try {
    .\gradlew clean :shared:allTests :app:testDebugUnitTest :app:assembleDebug
    Write-Host "Android debug APK built at app\build\outputs\apk\debug\app-debug.apk"
}
finally {
    Pop-Location
}

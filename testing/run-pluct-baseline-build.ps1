$ErrorActionPreference = 'Stop'
Set-Location $PSScriptRoot\..
$outDir = 'C:\Shared\Projects\output\pluct-baseline'
New-Item -ItemType Directory -Path $outDir -Force | Out-Null
$log = Join-Path $outDir ('build-{0:yyyy-MM-dd}.log' -f (Get-Date))
# --no-configuration-cache avoids Kotlin incremental / configuration-cache ClassNotFound on some Gradle 8.13 + Kotlin combos
& .\gradlew.bat assembleDebug --no-configuration-cache 2>&1 | Tee-Object -FilePath $log
if ($LASTEXITCODE -ne 0) { throw "BUILD FAILED - see $log" }
$apk = Join-Path $PWD 'app\build\outputs\apk\debug\app-debug.apk'
if (-not (Test-Path $apk)) { throw "APK missing after build: $apk" }
$meta = @{
  issue     = 'ALB-58'
  project   = 'PluctMobileApp'
  path      = $PWD.Path
  apk       = $apk
  builtAt   = (Get-Date).ToString('o')
  gradleLog = $log
}
$metaPath = Join-Path $outDir ('baseline-{0:yyyy-MM-dd}.json' -f (Get-Date))
$meta | ConvertTo-Json | Set-Content -Encoding UTF8 $metaPath
Write-Host 'PLUCT_BASELINE_BUILD: PASSED'
Write-Host "LOG: $log"
Write-Host "META: $metaPath"

Set-StrictMode -Version Latest
$ErrorActionPreference = "Stop"

function Fail($Message) {
    Write-Error $Message
    exit 1
}

function Require-Path($Path, $Message) {
    if (-not (Test-Path $Path)) {
        Fail $Message
    }
}

Push-Location (Resolve-Path "$PSScriptRoot\..\..")
try {
    Require-Path ".\gradlew.bat" "Gradle wrapper is missing."

    if (-not (Get-Command node -ErrorAction SilentlyContinue)) {
        Fail "Node.js is not available on PATH."
    }

    if (-not (Get-Command adb -ErrorAction SilentlyContinue)) {
        Fail "ADB is not available on PATH."
    }

    $hasAndroidHome = -not [string]::IsNullOrWhiteSpace($env:ANDROID_HOME) -or -not [string]::IsNullOrWhiteSpace($env:ANDROID_SDK_ROOT)
    $hasLocalSdk = (Test-Path ".\local.properties") -and ((Get-Content ".\local.properties" -Raw) -match "sdk\.dir\s*=")
    if (-not ($hasAndroidHome -or $hasLocalSdk)) {
        Fail "Android SDK not found. Set ANDROID_HOME/ANDROID_SDK_ROOT or local.properties sdk.dir."
    }

    $settings = Get-Content ".\settings.gradle.kts" -Raw
    if ($settings -notmatch 'include\(":shared"\)') {
        Fail "settings.gradle.kts does not include :shared."
    }

    Require-Path ".\.github\workflows\android-ci.yml" "Android CI workflow is missing."
    Require-Path ".\.github\workflows\ios-testflight.yml" "iOS TestFlight workflow is missing."
    Require-Path ".\docs\WINDOWS-TO-TESTFLIGHT.md" "WINDOWS-TO-TESTFLIGHT.md is missing."
    Require-Path ".\docs\TESTER-ROLLOUT.md" "TESTER-ROLLOUT.md is missing."
    Require-Path ".\docs\KMP-MIGRATION-NOTES.md" "KMP-MIGRATION-NOTES.md is missing."
    Require-Path ".\scripts\windows\verify-ios-ci-ready.ps1" "verify-ios-ci-ready.ps1 is missing."

    $trackedFiles = git ls-files
    $forbiddenRawPatterns = @(
        ("prod" + "-jwt-secret"),
        "hf_[A-Za-z0-9_]{20,}",
        "ENGINE_JWT_SECRET\s*=\s*['""][^'""]+['""]",
        "TTT_SHARED_SECRET\s*=\s*['""][^'""]+['""]",
        "APP_STORE_CONNECT_API_KEY\s*=\s*['""][^'""]+['""]",
        "IOS_DISTRIBUTION_CERTIFICATE\s*=\s*['""][^'""]+['""]"
    )

    $secretHits = @()
    foreach ($file in $trackedFiles) {
        if (-not (Test-Path $file)) { continue }
        if ((Get-Item $file).PSIsContainer) { continue }
        $content = Get-Content $file -Raw -ErrorAction SilentlyContinue
        foreach ($pattern in $forbiddenRawPatterns) {
            if ($content -match $pattern) {
                $secretHits += $file
                break
            }
        }
    }

    if ($secretHits.Count -gt 0) {
        $uniqueHits = $secretHits | Sort-Object -Unique
        Write-Host "Potential committed secret material found in:"
        $uniqueHits | ForEach-Object { Write-Host " - $_" }
        Fail "Remove committed secret values before shipping. Secret values were not printed."
    }

    Write-Host "Repo readiness checks passed."
}
finally {
    Pop-Location
}

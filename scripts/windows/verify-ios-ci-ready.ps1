Set-StrictMode -Version Latest
$ErrorActionPreference = "Stop"

Write-Host "iOS TestFlight CI verification (no secret values printed)."
Write-Host "Required GitHub Actions secret NAMES (values must be set in repo Settings):"
Write-Host "  APPLE_TEAM_ID"
Write-Host "  APPLE_BUNDLE_ID"
Write-Host "  APP_STORE_CONNECT_API_KEY_ID"
Write-Host "  APP_STORE_CONNECT_API_ISSUER_ID"
Write-Host "  APP_STORE_CONNECT_API_KEY_P8"
Write-Host "  IOS_DISTRIBUTION_CERTIFICATE_BASE64"
Write-Host "  IOS_DISTRIBUTION_CERTIFICATE_PASSWORD"
Write-Host "  IOS_PROVISIONING_PROFILE_BASE64"
Write-Host ""
Write-Host "Dispatch: GitHub Actions -> iOS TestFlight -> Run workflow (or push tag ios-* per workflow triggers)."
Write-Host "Validate on macOS runner logs: Verify Apple secrets -> shared tests -> archive -> export -> upload."
Write-Host ""

if (Get-Command gh -ErrorAction SilentlyContinue) {
    Write-Host "gh CLI found. If authenticated (gh auth status), list configured secret names (not values):"
    gh secret list 2>$null
    if ($LASTEXITCODE -ne 0) {
        Write-Host "(gh secret list failed or not logged in - run gh auth login from repo root)"
    }
} else {
    Write-Host "Install GitHub CLI (gh) to list secret names from the terminal."
}

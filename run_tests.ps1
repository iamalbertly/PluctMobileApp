<#
Deprecated PowerShell runner.
Canonical validation now runs through the Node/ADB orchestrator:
  npm run test:all
#>

Write-Host "This runner is deprecated. Use 'npm run test:all'." -ForegroundColor Yellow
Write-Host "The Node/ADB runner fails fast and prints UI/logcat diagnostics." -ForegroundColor Gray
Exit 1

<#
Deprecated compatibility wrapper.
Use the canonical Node/ADB validation path instead:
  npm run test:all
#>

Write-Host "This legacy wrapper is deprecated. Use 'npm run test:all'." -ForegroundColor Yellow
Write-Host "The current runner captures focused UI/logcat diagnostics and stops on first failure." -ForegroundColor Gray
Exit 1

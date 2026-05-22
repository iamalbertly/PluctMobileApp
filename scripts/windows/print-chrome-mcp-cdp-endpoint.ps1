Set-StrictMode -Version Latest
$ErrorActionPreference = "Stop"
$p = Join-Path $env:LOCALAPPDATA "Google\Chrome\User Data\DevToolsActivePort"
if (-not (Test-Path $p)) {
    Write-Error "Chrome DevToolsActivePort not found. Start Chrome with remote debugging (chrome://inspect/#remote-debugging) or --remote-debugging-port=9222."
    exit 1
}
$lines = Get-Content $p | Where-Object { $_.Trim().Length -gt 0 }
if ($lines.Count -lt 2) {
    Write-Error "DevToolsActivePort file format unexpected."
    exit 1
}
$port = $lines[0].Trim()
$wsPath = $lines[1].Trim()
if (-not $wsPath.StartsWith("/")) { $wsPath = "/" + $wsPath }
$ws = "ws://127.0.0.1:$port$wsPath"
Write-Host "Paste this full line into Cursor User mcp.json -> playwright -> args (after @playwright/mcp@latest):"
Write-Host ""
Write-Host ('                        "--cdp-endpoint=' + $ws + '",')
Write-Host ('                        "--cdp-timeout=120000"')
Write-Host ""
Write-Host "Then restart MCP servers (Cursor: reload window or MCP restart). Chrome restart changes the browser GUID; re-run this script."

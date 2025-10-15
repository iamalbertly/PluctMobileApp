# Pluct-Test-Orchestrator-Main
Param(
    [string]$Scope = "All",
    [string]$Url = "https://www.tiktok.com/@garyvee/video/7308801293029248299",
    [switch]$ForceBuild,
    [switch]$SkipInstall
)

$ErrorActionPreference = 'Stop'

Write-Host "[ORCH] Running scope=$Scope url=$Url" -ForegroundColor Cyan

$root = Split-Path -Parent $MyInvocation.MyCommand.Path
Set-Location (Resolve-Path "$root\..")

$node = "node"

$flags = @("scripts/nodejs/Pluct-Automatic-Orchestrator.js", "-scope", $Scope, "-url", $Url)
if ($ForceBuild) { $flags += "--forceBuild" }
if ($SkipInstall) { $flags += "--skipInstall" }

$proc = Start-Process -FilePath $node -ArgumentList $flags -NoNewWindow -PassThru -Wait

if ($proc.ExitCode -ne 0) {
    Write-Host "[ORCH] Orchestrator failed with exit code $($proc.ExitCode)" -ForegroundColor Red
    exit $proc.ExitCode
}

Write-Host "[ORCH] Orchestrator completed successfully" -ForegroundColor Green

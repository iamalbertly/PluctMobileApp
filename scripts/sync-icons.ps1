param(
    [string]$SourceIconsPath = "AppIcons/android",
    [string]$DestResPath = "app/src/main/res"
)

Write-Host "Syncing Android launcher assets from $SourceIconsPath to $DestResPath..."

$root = Split-Path -Parent $MyInvocation.MyCommand.Path
$src = Join-Path $root "..\$SourceIconsPath" | Resolve-Path
$dst = Join-Path $root "..\$DestResPath" | Resolve-Path

if (-not (Test-Path $src)) {
    Write-Error "Source not found: $src"
    exit 1
}

$dirs = Get-ChildItem -Path $src -Directory | Where-Object {
    $_.Name -like "mipmap-*" -or $_.Name -eq "values"
}

foreach ($d in $dirs) {
    $target = Join-Path $dst $d.Name
    if (-not (Test-Path $target)) {
        New-Item -ItemType Directory -Path $target | Out-Null
    }
    Copy-Item -Path (Join-Path $d.FullName "*") -Destination $target -Recurse -Force
    Write-Host "Mirrored $($d.Name)"
}

Write-Host "Icon sync complete. Rebuild the app to refresh launcher and notification large-icon art."

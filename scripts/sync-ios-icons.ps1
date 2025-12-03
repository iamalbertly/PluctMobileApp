param(
    [string]$SourceImage = "AppIcons/appstore.png",
    [string]$AssetFolder = "AppIcons/Assets.xcassets/AppIcon.appiconset"
)

Write-Host "Syncing iOS AppIcon files from $SourceImage to $AssetFolder..."

if (-not (Test-Path $SourceImage)) { Write-Host "Source image not found: $SourceImage" ; exit 1 }
if (-not (Test-Path $AssetFolder)) { Write-Host "Asset folder not found: $AssetFolder" ; exit 1 }

$files = Get-ChildItem -Path $AssetFolder -Include '*.png' -File
foreach ($f in $files) {
    Copy-Item -Force -Path $SourceImage -Destination $f.FullName
    Write-Host "Replaced $($f.Name) with $SourceImage"
}

Write-Host "iOS icons sync complete. Note: sizes not adjusted; ensure images are resized for accurate display." 

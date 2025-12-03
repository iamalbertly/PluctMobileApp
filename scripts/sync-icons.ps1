param(
    [string]$SourceIconsPath = "AppIcons/android",
    [string]$DestResPath = "app/src/main/res"
)

Write-Host "Syncing icon files from $SourceIconsPath to $DestResPath..."

function copy-icon($src, $dst) {
    if (Test-Path $src) {
        Copy-Item -Force -Path $src -Destination $dst
        Write-Host "Copied $src -> $dst"
    } else {
        Write-Host "Source not found: $src"
    }
}

$mipmapFolders = @("mipmap-mdpi", "mipmap-hdpi", "mipmap-xhdpi", "mipmap-xxhdpi", "mipmap-xxxhdpi")

foreach ($folder in $mipmapFolders) {
    $srcFolder = Join-Path -Path $SourceIconsPath -ChildPath $folder
    $dstFolder = Join-Path -Path $DestResPath -ChildPath $folder
    if (-not (Test-Path $dstFolder)) { New-Item -ItemType Directory -Path $dstFolder | Out-Null }

    # Copy ic_launcher.png if present
    copy-icon (Join-Path $srcFolder 'ic_launcher.png') (Join-Path $dstFolder 'ic_launcher.png')
}

# AnyDpi folder: don't copy the PNGs to anydpi; write adaptive XMLs to reference the mipmap icons
$dstAnyDpiRoundFolder = Join-Path -Path $DestResPath -ChildPath 'mipmap-anydpi-v26'
if (-not (Test-Path $dstAnyDpiRoundFolder)) { New-Item -ItemType Directory -Path $dstAnyDpiRoundFolder | Out-Null }
Write-Host "Skipping anydpi png copies; using XML-based adaptive icons in $dstAnyDpiRoundFolder"

# Add/replace adaptive icons XML to point foreground/background to the mipmap ic_launcher
$adaptiveXml = @"
<?xml version="1.0" encoding="utf-8"?>
<adaptive-icon xmlns:android="http://schemas.android.com/apk/res/android">
    <background android:drawable="@mipmap/ic_launcher" />
    <foreground android:drawable="@mipmap/ic_launcher" />
</adaptive-icon>
"@

$adaptiveXmlPath = Join-Path -Path $dstAnyDpiRoundFolder -ChildPath 'ic_launcher.xml'
Set-Content -Path $adaptiveXmlPath -Value $adaptiveXml -Encoding UTF8
Write-Host "Written adaptive icon XML to $adaptiveXmlPath"

Write-Host "Icon sync complete. Please rebuild the app (gradlew installDebug) to see changes."

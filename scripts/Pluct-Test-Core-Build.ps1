# Pluct Test Core Build Management
# Smart build detection and execution

function Test-BuildRequired {
    # Check if key Kotlin files have changed since last build
    $lastBuildTime = Get-ChildItem "app\build\outputs\apk\debug\app-debug.apk" -ErrorAction SilentlyContinue | Select-Object -ExpandProperty LastWriteTime
    $keyFiles = @(
        "app\src\main\java\app\pluct\data\entity\VideoItem.kt",
        "app\src\main\java\app\pluct\viewmodel\HomeViewModel.kt",
        "app\src\main\java\app\pluct\ui\screens\HomeScreen.kt",
        "app\src\main\java\app\pluct\share\ShareIngestActivity.kt",
        "app\src\main\java\app\pluct\worker\TranscriptionWorker.kt"
    )
    
    if (-not $lastBuildTime) {
        Write-Log "No previous build found - build required" "Yellow"
        return $true
    }
    
    foreach ($file in $keyFiles) {
        if (Test-Path $file) {
            $fileTime = Get-ChildItem $file | Select-Object -ExpandProperty LastWriteTime
            if ($fileTime -gt $lastBuildTime) {
                Write-Log "File changed: $file - build required" "Yellow"
                return $true
            }
        }
    }
    
    return $false
}

function Build-App {
    Write-Log "Building app..." "Cyan"
    $buildResult = .\gradlew.bat assembleDebug --console=plain 2>&1
    
    if ($LASTEXITCODE -eq 0) {
        Write-Log "Build successful" "Green"
        return $true
    } else {
        Write-Log "Build failed" "Red"
        Write-Log "Build output: $buildResult" "Red"
        return $false
    }
}

function Invoke-Install {
    Write-Log "Installing app..." "Cyan"
    
    # Uninstall existing version
    adb uninstall $script:AppPackage | Out-Null
    
    # Install new version
    $apkPath = "app\build\outputs\apk\debug\app-debug.apk"
    if (-not (Test-Path $apkPath)) {
        Write-Log "APK not found at: $apkPath" "Red"
        return $false
    }
    
    $installResult = adb install -r $apkPath
    
    if ($LASTEXITCODE -eq 0) {
        Write-Log "Installation successful" "Green"
        return $true
    } else {
        Write-Log "Installation failed" "Red"
        return $false
    }
}

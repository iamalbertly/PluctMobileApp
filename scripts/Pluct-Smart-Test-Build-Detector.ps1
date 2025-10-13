# Pluct Smart Test Build Detector - Intelligent build detection and management
# Follows naming convention: [Project]-[ParentScope]-[ChildScope]-[CoreResponsibility]

function Test-SmartBuildRequired {
    param(
        [switch]$ForceBuild
    )
    
    if ($ForceBuild) {
        $script:SmartTestSession.SmartBuildDetection.BuildReason = "Force build requested"
        return $true
    }
    
    # Check if APK exists
    $apkPath = "app\build\outputs\apk\debug\app-debug.apk"
    if (-not (Test-Path $apkPath)) {
        $script:SmartTestSession.SmartBuildDetection.BuildReason = "APK not found"
        return $true
    }
    
    # Get APK modification time
    $apkTime = (Get-Item $apkPath).LastWriteTime
    $script:SmartTestSession.SmartBuildDetection.LastBuildTime = $apkTime
    
    # Check for changes in key Kotlin files
    $changedFiles = @()
    $keyFiles = @(
        "app\src\main\java\app\pluct\*.kt",
        "app\src\main\AndroidManifest.xml",
        "app\build.gradle.kts",
        "build.gradle.kts",
        "gradle.properties"
    )
    
    foreach ($pattern in $keyFiles) {
        $files = Get-ChildItem -Path $pattern -Recurse -ErrorAction SilentlyContinue
        foreach ($file in $files) {
            if ($file.LastWriteTime -gt $apkTime) {
                $changedFiles += $file.FullName
            }
        }
    }
    
    if ($changedFiles.Count -gt 0) {
        $script:SmartTestSession.SmartBuildDetection.ChangedFiles = $changedFiles
        $script:SmartTestSession.SmartBuildDetection.BuildReason = "Changed files: $($changedFiles.Count) files modified since last build"
        Write-SmartLog "Smart build detection: $($changedFiles.Count) files changed since last build" "Yellow"
        if ($Verbose) {
            foreach ($file in $changedFiles) {
                Write-SmartLog "  - $($file)" "Gray"
            }
        }
        return $true
    }
    
    # Check Git status for uncommitted changes
    try {
        $gitStatus = git status --porcelain 2>$null
        if ($gitStatus) {
            $script:SmartTestSession.SmartBuildDetection.BuildReason = "Git has uncommitted changes"
            Write-SmartLog "Smart build detection: Git has uncommitted changes" "Yellow"
            return $true
        }
    } catch {
        # Git not available or not a git repository
    }
    
    Write-SmartLog "Smart build detection: No changes detected" "Green"
    return $false
}

function Build-SmartApp {
    Write-SmartLog "Starting smart build process..." "Yellow"
    
    try {
        # Clean build for better performance
        Write-SmartLog "Cleaning previous build artifacts..." "Gray"
        $cleanResult = & .\gradlew clean 2>&1
        if ($LASTEXITCODE -ne 0) {
            Write-SmartLog "Clean failed: $cleanResult" "Red"
            return $false
        }
        
        # Build with optimizations
        Write-SmartLog "Building with smart optimizations..." "Gray"
        $buildResult = & .\gradlew assembleDebug --no-daemon --parallel --build-cache 2>&1
        if ($LASTEXITCODE -ne 0) {
            Write-SmartLog "Build failed: $buildResult" "Red"
            return $false
        }
        
        Write-SmartLog "Smart build completed successfully" "Green"
        return $true
    } catch {
        Write-SmartLog "Build exception: $($_.Exception.Message)" "Red"
        return $false
    }
}

function Test-SmartDeploymentNeeded {
    # Check if device has the latest APK
    try {
        $deviceApkInfo = adb shell dumpsys package app.pluct | Select-String "versionCode"
        if ($deviceApkInfo) {
            # Compare with local APK
            $localApkPath = "app\build\outputs\apk\debug\app-debug.apk"
            if (Test-Path $localApkPath) {
                $localApkTime = (Get-Item $localApkPath).LastWriteTime
                $deviceApkTime = [DateTime]::Parse($deviceApkInfo.ToString().Split("=")[1].Trim())
                
                if ($localApkTime -gt $deviceApkTime) {
                    Write-SmartLog "Smart deployment: Local APK is newer than device APK" "Yellow"
                    return $true
                }
            }
        }
        
        Write-SmartLog "Smart deployment: Device APK is up to date" "Green"
        return $false
    } catch {
        Write-SmartLog "Smart deployment: Could not check device APK version, assuming deployment needed" "Yellow"
        return $true
    }
}

function Deploy-SmartToDevice {
    Write-SmartLog "Starting smart deployment..." "Yellow"
    
    try {
        # Uninstall existing app
        Write-SmartLog "Uninstalling existing app..." "Gray"
        $uninstallResult = adb uninstall app.pluct 2>&1
        if ($LASTEXITCODE -ne 0 -and $uninstallResult -notmatch "not found") {
            Write-SmartLog "Uninstall warning: $uninstallResult" "Yellow"
        }
        
        # Install new APK
        Write-SmartLog "Installing new APK..." "Gray"
        $installResult = adb install -r "app\build\outputs\apk\debug\app-debug.apk" 2>&1
        if ($LASTEXITCODE -ne 0) {
            Write-SmartLog "Install failed: $installResult" "Red"
            return $false
        }
        
        # Clear app data for clean test
        Write-SmartLog "Clearing app data for clean test..." "Gray"
        $clearResult = adb shell pm clear app.pluct 2>&1
        if ($LASTEXITCODE -ne 0) {
            Write-SmartLog "Clear data warning: $clearResult" "Yellow"
        }
        
        Write-SmartLog "Smart deployment completed successfully" "Green"
        return $true
    } catch {
        Write-SmartLog "Deployment exception: $($_.Exception.Message)" "Red"
        return $false
    }
}

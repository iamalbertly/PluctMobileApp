# ClipForge Test Framework - Utilities Module
# Contains common utility functions used across the test framework

# Function to validate URL format
function Test-ValidUrl {
    param(
        [string]$Url
    )
    
    # Check for empty or null URL
    if ([string]::IsNullOrWhiteSpace($Url)) {
        Write-Log "URL is empty or null" "Red"
        return $false
    }
    
    # Check for basic HTTP/HTTPS format
    if (-not ($Url -match '^https?://')) {
        Write-Log "URL does not start with http:// or https://" "Red"
        return $false
    }
    
    # Check for TikTok domain
    if (-not ($Url -match 'tiktok\.com')) {
        Write-Log "URL does not contain tiktok.com domain" "Red"
        return $false
    }
    
    # Check for quotes and clean them if found
    if ($Url -match '["'']') {
        $cleanUrl = $Url -replace '["'']', ''
        Write-Log "URL contains quotes, cleaned: $cleanUrl" "Yellow"
        return $cleanUrl
    }
    
    return $Url
}

# Function to test if Android device is connected
function Test-AndroidDevice {
    try {
        $devices = adb devices
        if ($devices -match "\tdevice") {
            $deviceId = ($devices | Select-String -Pattern "(.*?)\tdevice").Matches.Groups[1].Value
            Write-Log "Android device connected: $deviceId" "Green"
            return $true
        } else {
            Write-Log "No Android device connected" "Red"
            return $false
        }
    } catch {
        Write-Log "Error checking Android device: $_" "Red"
        return $false
    }
}

# Function to build the app
function Build-App {
    param(
        [switch]$SkipBuild
    )
    
    if ($SkipBuild) {
        Write-Log "Skipping app build (using existing APK)" "Yellow"
        return $true
    }
    
    try {
        Write-Log "Building app..." "Cyan"
        # Add your build command here, e.g.:
        # ./gradlew assembleDebug
        
        # For now, we'll just check if the APK exists
        $apkPath = "./app/build/outputs/apk/debug/app-debug.apk"
        if (Test-Path $apkPath) {
            Write-Log "App built successfully" "Green"
            return $true
        } else {
            Write-Log "APK not found at expected path: $apkPath" "Red"
            return $false
        }
    } catch {
        Write-Log "Error building app: $_" "Red"
        return $false
    }
}

# Function to install the app
function Install-App {
    param(
        [switch]$SkipInstall
    )
    
    if ($SkipInstall) {
        Write-Log "Skipping app installation" "Yellow"
        return $true
    }
    
    try {
        $apkPath = "./app/build/outputs/apk/debug/app-debug.apk"
        if (-not (Test-Path $apkPath)) {
            Write-Log "APK not found at expected path: $apkPath" "Red"
            return $false
        }
        
        Write-Log "Installing app..." "Cyan"
        $result = adb install -r $apkPath
        
        if ($result -match "Success") {
            Write-Log "App installed successfully" "Green"
            return $true
        } else {
            Write-Log "Failed to install app: $result" "Red"
            return $false
        }
    } catch {
        Write-Log "Error installing app: $_" "Red"
        return $false
    }
}

# Function to capture WebView HTML content
function Capture-WebViewHtml {
    param(
        [switch]$CaptureHTML,
        [string]$TestName = "unknown"
    )
    
    if (-not $CaptureHTML) {
        return $null
    }
    
    try {
        Write-Log "Capturing WebView HTML content..." "Cyan"
        
        # Create directory if it doesn't exist
        $captureDir = "$script:HtmlCapturesDirectory"
        if (-not (Test-Path $captureDir)) {
            New-Item -Path $captureDir -ItemType Directory -Force | Out-Null
        }
        
        # Generate filename with timestamp
        $timestamp = Get-Date -Format "yyyyMMdd_HHmmss"
        $filename = "$captureDir\${TestName}_${timestamp}.html"
        
        # Use adb to capture WebView content
        # This is a simplified version - in a real implementation, you would need to use Chrome DevTools Protocol
        # or a similar approach to extract HTML from WebView
        $html = adb shell "run-as com.example.clipforge cat /data/data/com.example.clipforge/files/webview_content.html"
        
        if ($html) {
            $html | Out-File -FilePath $filename -Encoding utf8
            Write-Log "WebView HTML captured to: $filename" "Green"
            return $filename
        } else {
            Write-Log "Failed to capture WebView HTML" "Yellow"
            return $null
        }
    } catch {
        Write-Log "Error capturing WebView HTML: $_" "Red"
        return $null
    }
}

# Function to launch the app with a URL
function Launch-AppWithUrl {
    param(
        [string]$Url,
        [switch]$ValidateUrl = $true
    )
    
    if ($ValidateUrl) {
        $validatedUrl = Test-ValidUrl -Url $Url
        if (-not $validatedUrl) {
            Write-Log "Invalid URL format: $Url" "Red"
            return $false
        }
        $Url = $validatedUrl
    }
    
    try {
        # Prepare the URL (escape special characters)
        $escapedUrl = $Url.Replace("&", "^&")
        
        Write-Log "Launching app with URL: $Url" "Cyan"
        
        # Launch the app with the URL using an intent
        $launchCommand = "adb shell am start -a android.intent.action.VIEW -d '$escapedUrl' -n com.example.clipforge/.MainActivity"
        $launchResult = Invoke-Expression $launchCommand
        
        # Check for errors in the launch result
        if ($launchResult -match "Error") {
            Write-Log "Error launching app: $launchResult" "Red"
            return $false
        }
        
        # Give the app a moment to start
        Start-Sleep -Seconds 2
        
        Write-Log "App launched successfully" "Green"
        return $true
    } catch {
        Write-Log "Exception launching app: $_" "Red"
        return $false
    }
}

# Export functions
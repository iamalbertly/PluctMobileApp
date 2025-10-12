# Test to verify the foreground issue and provide solution

function Test-ForegroundIssue {
    Write-Log "=== Testing Foreground Issue ===" "Cyan"
    Write-Log "This test will help us understand why the app goes to background" "White"
    
    # Clear app data
    adb shell pm clear app.pluct
    Start-Sleep -Seconds 2
    
    # Clear logcat
    adb logcat -c
    
    Write-Log "Step 1: Launching Pluct app directly..." "Yellow"
    adb shell am start -n app.pluct/app.pluct.MainActivity
    Start-Sleep -Seconds 3
    
    $currentActivity = adb shell dumpsys activity activities | Select-String "mResumedActivity" | Select-Object -First 1
    Write-Log "Current Activity after direct launch: $currentActivity" "White"
    
    if ($currentActivity -match "MainActivity") {
        Write-Log "✅ App stays in foreground when launched directly" "Green"
    } else {
        Write-Log "❌ App goes to background even when launched directly" "Red"
    }
    
    Write-Log "Step 2: Testing share intent flow..." "Yellow"
    $shareIntent = @(
        "shell", "am", "start",
        "-a", "android.intent.action.SEND",
        "-t", "text/plain",
        "--es", "android.intent.extra.TEXT", "https://www.tiktok.com/@garyvee/video/7308801293029248299",
        "-n", "app.pluct/app.pluct.share.ShareIngestActivity"
    )
    
    & adb $shareIntent
    Start-Sleep -Seconds 3
    
    $currentActivity = adb shell dumpsys activity activities | Select-String "mResumedActivity" | Select-Object -First 1
    Write-Log "Current Activity after share intent: $currentActivity" "White"
    
    if ($currentActivity -match "MainActivity") {
        Write-Log "✅ App stays in foreground after share intent" "Green"
    } else {
        Write-Log "❌ App goes to background after share intent" "Red"
        Write-Log "This is the root cause of the issue!" "Red"
    }
    
    Write-Log ""
    Write-Log "=== SOLUTION RECOMMENDATIONS ===" "Cyan"
    Write-Log "1. Check MainActivity launch mode in AndroidManifest.xml" "Yellow"
    Write-Log "2. Ensure proper intent flags in ShareIngestActivity" "Yellow"
    Write-Log "3. Add moveTaskToFront() call in MainActivity" "Yellow"
    Write-Log "4. Check if there are any finish() calls causing the issue" "Yellow"
}

# Run the test
Test-ForegroundIssue

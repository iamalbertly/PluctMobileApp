# Test Quick Scan Flow
Write-Host "🎯 Testing Quick Scan Flow..."

# Launch the app
Write-Host "📱 Launching Pluct app..."
adb shell am start -n app.pluct/.MainActivity
Start-Sleep -Seconds 3

# Send a share intent to open the capture sheet
Write-Host "📤 Sending share intent..."
adb shell am start -a android.intent.action.SEND -t text/plain --es android.intent.extra.TEXT "https://www.tiktok.com/@garyvee/video/7308801293029248299" -n app.pluct/.share.PluctShareIngestActivity
Start-Sleep -Seconds 3

# Click the Quick Scan button
Write-Host "⚡️ Clicking Quick Scan button..."
adb shell input tap 540 800  # Approximate coordinates for Quick Scan button
Start-Sleep -Seconds 5

# Check logs for Quick Scan activity
Write-Host "📋 Checking logs for Quick Scan activity..."
$logOutput = adb logcat -d
$logOutput | Select-String "QUICK SCAN|CREATING VIDEO|HomeViewModel|PluctRepository|BUTTON CLICKED" | Select-Object -Last 20

# Check database for videos
Write-Host "🗄️ Checking database for videos..."
adb shell "run-as app.pluct sqlite3 /data/data/app.pluct/databases/pluct.db 'SELECT id, sourceUrl, title, author FROM video_items LIMIT 5;'"

Write-Host "✅ Test completed!"

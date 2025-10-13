# Pluct Smart Test Core Utilities - Single source of truth for testing utilities
# Follows naming convention: [Project]-[ParentScope]-[ChildScope]-[CoreResponsibility]

function Write-SmartLog {
    param(
        [string]$Message,
        [string]$Color = "White"
    )
    
    $timestamp = Get-Date -Format "HH:mm:ss"
    $colorMap = @{
        "Red" = "Red"
        "Green" = "Green"
        "Yellow" = "Yellow"
        "Cyan" = "Cyan"
        "Gray" = "Gray"
        "White" = "White"
    }
    
    $actualColor = $colorMap[$Color]
    if (-not $actualColor) { $actualColor = "White" }
    
    Write-Host "[$timestamp] $Message" -ForegroundColor $actualColor
}

function Test-SmartAndroidDevice {
    try {
        $deviceInfo = adb devices 2>&1
        $deviceCount = ($deviceInfo | Select-String "device").Count
        return $deviceCount -gt 0
    } catch {
        return $false
    }
}

function Report-SmartCriticalError {
    param(
        [string]$ErrorType,
        [string]$ErrorMessage
    )
    
    $script:SmartTestSession.CriticalErrors += @{
        Type = $ErrorType
        Message = $ErrorMessage
        Timestamp = Get-Date
    }
    
    Write-SmartLog "CRITICAL ERROR: $ErrorType" "Red"
    Write-SmartLog "Details: $ErrorMessage" "Red"
}

function Show-SmartTestReport {
    param([bool]$OverallSuccess)
    
    $duration = (Get-Date) - $script:SmartTestSession.StartTime
    $durationFormatted = "{0:F2}" -f $duration.TotalSeconds
    
    Write-SmartLog "=== SMART TEST REPORT ===" "Cyan"
    Write-SmartLog "Duration: $durationFormatted seconds" "White"
    Write-SmartLog "Test URL: $($script:SmartTestSession.TestUrl)" "White"
    Write-SmartLog "Build Required: $($script:SmartTestSession.BuildRequired)" "White"
    
    if ($script:SmartTestSession.SmartBuildDetection.BuildReason) {
        Write-SmartLog "Build Reason: $($script:SmartTestSession.SmartBuildDetection.BuildReason)" "Gray"
    }
    
    if ($script:SmartTestSession.CriticalErrors.Count -gt 0) {
        Write-SmartLog "Critical Errors: $($script:SmartTestSession.CriticalErrors.Count)" "Red"
        foreach ($error in $script:SmartTestSession.CriticalErrors) {
            Write-SmartLog "  - $($error.Type): $($error.Message)" "Red"
        }
    }
    
    if ($OverallSuccess) {
        Write-SmartLog "✅ All smart tests passed successfully" "Green"
    } else {
        Write-SmartLog "❌ Smart tests failed" "Red"
    }
    
    Write-SmartLog "Smart testing completed" "Cyan"
}

# --- UI Automation Helpers (ADB/UIAutomator) ---

function Get-UiDumpXmlPath {
    return Join-Path $env:TEMP "pluct_ui_dump.xml"
}

function Get-UiHierarchy {
    try {
        adb shell uiautomator dump /sdcard/pluct_ui_dump.xml 2>$null | Out-Null
        $localPath = Get-UiDumpXmlPath
        adb pull /sdcard/pluct_ui_dump.xml "$localPath" 1>$null 2>$null | Out-Null
        if (-not (Test-Path $localPath)) { return $null }
        [xml](Get-Content -Raw $localPath)
    } catch {
        return $null
    }
}

function Get-UiNodes {
    param([xml]$Dump)
    if (-not $Dump) { return @() }
    $nodes = @()
    $all = $Dump.SelectNodes('//node')
    foreach ($n in $all) {
        $nodes += [pscustomobject]@{
            Text        = $n.GetAttribute('text')
            Desc        = $n.GetAttribute('content-desc')
            ResId       = $n.GetAttribute('resource-id')
            Clickable   = ($n.GetAttribute('clickable') -eq 'true')
            Bounds      = $n.GetAttribute('bounds')
            Class       = $n.GetAttribute('class')
            Enabled     = ($n.GetAttribute('enabled') -eq 'true')
            Focusable   = ($n.GetAttribute('focusable') -eq 'true')
            Package     = $n.GetAttribute('package')
        }
    }
    $nodes
}

function Find-UiElementsByText {
    param(
        [xml]$UiXml,
        [string]$Text,
        [switch]$Contains
    )
    if (-not $UiXml) { return @() }
    $nodes = $UiXml.hierarchy.node
    if (-not $nodes) { return @() }
    $matches = @()
    foreach ($n in $UiXml.SelectNodes('//node')) {
        $t = ($n.GetAttribute('text'))
        $d = ($n.GetAttribute('content-desc'))
        if ($Contains) {
            if (($t -like "*${Text}*") -or ($d -like "*${Text}*")) { $matches += $n }
        } else {
            if (($t -eq $Text) -or ($d -eq $Text)) { $matches += $n }
        }
    }
    return $matches
}

function Get-UiNodeCenterFromBounds {
    param([string]$Bounds)
    # bounds format: [x1,y1][x2,y2]
    if (-not $Bounds) { return $null }
    $nums = [regex]::Matches($Bounds, '\d+') | ForEach-Object { [int]$_.Value }
    if ($nums.Count -ne 4) { return $null }
    $x = [int](($nums[0] + $nums[2]) / 2)
    $y = [int](($nums[1] + $nums[3]) / 2)
    return @{ X = $x; Y = $y }
}

function Click-UiNode {
    param($Node)
    try {
        $bounds = $Node.GetAttribute('bounds')
        $pt = Get-UiNodeCenterFromBounds $bounds
        if (-not $pt) { return $false }
        adb shell input tap $($pt.X) $($pt.Y) 2>$null | Out-Null
        return $true
    } catch { return $false }
}

function Wait-ForUiText {
    param([string]$Text, [int]$TimeoutSeconds = 8)
    $stopAt = (Get-Date).AddSeconds($TimeoutSeconds)
    while ((Get-Date) -lt $stopAt) {
        $xml = Get-UiHierarchy
        $hit = Find-UiElementsByText -UiXml $xml -Text $Text -Contains
        if ($hit.Count -gt 0) { return $true }
        Start-Sleep -Milliseconds 500
    }
    return $false
}

function Describe-ClickableSummary {
    $xml = Get-UiHierarchy
    if (-not $xml) { Write-SmartLog "UI dump unavailable" "Red"; return }
    $nodes = $xml.SelectNodes('//node')
    $clickables = @()
    foreach ($n in $nodes) {
        if ($n.GetAttribute('clickable') -eq 'true') {
            $clickables += @{ text = $n.GetAttribute('text'); desc = $n.GetAttribute('content-desc'); bounds = $n.GetAttribute('bounds') }
        }
    }
    Write-SmartLog ("Clickable elements found: {0}" -f $clickables.Count) "Gray"
    $clickables | Select-Object -First 10 | ForEach-Object {
        $label = if ($_.text) { $_.text } elseif ($_.desc) { $_.desc } else { '<no-label>' }
        Write-SmartLog (" - clickable: '{0}' bounds={1}" -f $label, $_.bounds) "Gray"
    }
}
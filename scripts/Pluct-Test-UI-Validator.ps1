# Pluct Test UI Validator - Comprehensive UI component validation
# Follows naming convention: [Project]-[ParentScope]-[ChildScope]-[CoreResponsibility]
# Single source of truth for UI element selectors and validation logic

param(
    [string]$TestStep = "",
    [string]$ExpectedComponents = "",
    [switch]$Verbose = $false
)

# UI Element Selectors - Single source of truth
$script:UIElements = @{
    # Main Activity Components
    MainActivity = @{
        Selectors = @(
            @{ Type = "resource-id"; Value = "app.pluct:id/main_container" },
            @{ Type = "content-desc"; Value = "Main Activity" },
            @{ Type = "class"; Value = "android.widget.FrameLayout" }
        )
        Validation = @("Main container visible", "Navigation ready")
    }
    
    # Share Intent Components
    ShareIngestActivity = @{
        Selectors = @(
            @{ Type = "resource-id"; Value = "app.pluct:id/share_container" },
            @{ Type = "content-desc"; Value = "Share Ingest" },
            @{ Type = "text"; Value = "Processing" }
        )
        Validation = @("Share container visible", "Processing indicator")
    }
    
    # Capture Sheet Components
    CaptureInsightSheet = @{
        Selectors = @(
            @{ Type = "resource-id"; Value = "app.pluct:id/capture_sheet" },
            @{ Type = "content-desc"; Value = "Capture Insight Sheet" },
            @{ Type = "text"; Value = "Choose Analysis Tier" }
        )
        Validation = @("Capture sheet visible", "Tier selection available")
    }
    
    # Processing Status Components
    ProcessingStatus = @{
        Selectors = @(
            @{ Type = "resource-id"; Value = "app.pluct:id/processing_status" },
            @{ Type = "content-desc"; Value = "Processing Status" },
            @{ Type = "text"; Value = "Processing" }
        )
        Validation = @("Status panel visible", "Processing indicator active")
    }
    
    # Action Buttons
    ActionButtons = @{
        Selectors = @(
            @{ Type = "resource-id"; Value = "app.pluct:id/btn_start" },
            @{ Type = "resource-id"; Value = "app.pluct:id/btn_process" },
            @{ Type = "resource-id"; Value = "app.pluct:id/btn_analyze" },
            @{ Type = "content-desc"; Value = "Start Analysis" },
            @{ Type = "content-desc"; Value = "Process Video" },
            @{ Type = "content-desc"; Value = "Analyze Content" }
        )
        Validation = @("Action buttons available", "User can interact")
    }
    
    # Navigation Components
    Navigation = @{
        Selectors = @(
            @{ Type = "resource-id"; Value = "app.pluct:id/nav_host" },
            @{ Type = "content-desc"; Value = "Navigation Host" },
            @{ Type = "class"; Value = "androidx.navigation.fragment.NavHostFragment" }
        )
        Validation = @("Navigation system active", "Screen transitions working")
    }
}

# Test Step Definitions - Single source of truth
$script:TestSteps = @{
    "AppLaunch" = @{
        Description = "Validate app launch and main activity"
        PreValidation = @("Device ready", "ADB connected")
        PostValidation = @("MainActivity visible", "Navigation ready")
        CriticalElements = @("MainActivity")
        Timeout = 10
    }
    
    "ShareIntent" = @{
        Description = "Validate share intent handling"
        PreValidation = @("MainActivity active", "Intent received")
        PostValidation = @("ShareIngestActivity visible", "URL processed")
        CriticalElements = @("ShareIngestActivity", "ProcessingStatus")
        Timeout = 15
    }
    
    "VideoProcessing" = @{
        Description = "Validate video processing flow"
        PreValidation = @("ShareIngestActivity active", "URL validated")
        PostValidation = @("ProcessingStatus visible", "Background worker started")
        CriticalElements = @("ProcessingStatus", "ActionButtons")
        Timeout = 30
    }
    
    "CaptureSheet" = @{
        Description = "Validate capture insight sheet"
        PreValidation = @("ProcessingStatus active", "Preliminary analysis ready")
        PostValidation = @("CaptureInsightSheet visible", "Tier selection available")
        CriticalElements = @("CaptureInsightSheet")
        Timeout = 20
    }
}

function Write-DetailedLog {
    param(
        [string]$Message,
        [string]$Level = "INFO",
        [string]$Color = "White",
        [string]$Step = "",
        [string]$Component = ""
    )
    
    $timestamp = Get-Date -Format "HH:mm:ss.fff"
    $prefix = if ($Step) { "[$Step]" } else { "" }
    $componentInfo = if ($Component) { " ($Component)" } else { "" }
    $logMessage = "[$timestamp] $prefix [$Level]$componentInfo $Message"
    
    switch ($Color) {
        "Red" { Write-Host $logMessage -ForegroundColor Red }
        "Green" { Write-Host $logMessage -ForegroundColor Green }
        "Yellow" { Write-Host $logMessage -ForegroundColor Yellow }
        "Cyan" { Write-Host $logMessage -ForegroundColor Cyan }
        "Magenta" { Write-Host $logMessage -ForegroundColor Magenta }
        default { Write-Host $logMessage }
    }
}

function Get-UIHierarchy {
    param([string]$Step = "")
    
    Write-DetailedLog "Capturing UI hierarchy..." "DEBUG" "Gray" $Step "UIValidator"
    
    try {
        # Use a temporary file for UI dump
        $tempFile = "/data/local/tmp/ui_dump_$(Get-Date -Format 'yyyyMMddHHmmss').xml"
        $uiDump = adb shell "uiautomator dump $tempFile && cat $tempFile && rm $tempFile" 2>$null
        
        if ($LASTEXITCODE -eq 0 -and $uiDump -and $uiDump -notmatch "UI hierchary dumped to") {
            Write-DetailedLog "UI hierarchy captured successfully" "SUCCESS" "Green" $Step "UIValidator"
            return $uiDump
        } else {
            Write-DetailedLog "Failed to capture UI hierarchy" "ERROR" "Red" $Step "UIValidator"
            return $null
        }
    } catch {
        Write-DetailedLog "UI hierarchy capture exception: $($_.Exception.Message)" "ERROR" "Red" $Step "UIValidator"
        return $null
    }
}

function Find-UIElement {
    param(
        [string]$UIHierarchy,
        [string]$ElementType,
        [string]$ElementValue,
        [string]$Step = ""
    )
    
    Write-DetailedLog "Searching for element: $ElementType='$ElementValue'" "DEBUG" "Gray" $Step "UIValidator"
    
    try {
        $xml = [xml]$UIHierarchy
        $xpath = switch ($ElementType) {
            "resource-id" { "//node[@resource-id='$ElementValue']" }
            "content-desc" { "//node[@content-desc='$ElementValue']" }
            "text" { "//node[@text='$ElementValue']" }
            "class" { "//node[@class='$ElementValue']" }
            default { "//node[@*='$ElementValue']" }
        }
        
        $nodes = $xml.SelectNodes($xpath)
        if ($nodes -and $nodes.Count -gt 0) {
            Write-DetailedLog "Found $($nodes.Count) matching elements" "SUCCESS" "Green" $Step "UIValidator"
            return $nodes
        } else {
            Write-DetailedLog "No elements found matching criteria" "WARN" "Yellow" $Step "UIValidator"
            return $null
        }
    } catch {
        Write-DetailedLog "Element search exception: $($_.Exception.Message)" "ERROR" "Red" $Step "UIValidator"
        return $null
    }
}

function Validate-UIComponents {
    param(
        [string]$Step,
        [array]$ExpectedComponents,
        [string]$UIHierarchy
    )
    
    Write-DetailedLog "Validating UI components for step: $Step" "INFO" "Cyan" $Step "UIValidator"
    
    $validationResults = @{
        Step = $Step
        Timestamp = Get-Date
        Components = @()
        OverallSuccess = $true
        CriticalErrors = @()
    }
    
    foreach ($componentName in $ExpectedComponents) {
        Write-DetailedLog "Validating component: $componentName" "DEBUG" "Gray" $Step "UIValidator"
        
        if (-not $script:UIElements.ContainsKey($componentName)) {
            $error = "Component '$componentName' not defined in UI elements registry"
            Write-DetailedLog $error "ERROR" "Red" $Step "UIValidator"
            $validationResults.CriticalErrors += $error
            $validationResults.OverallSuccess = $false
            continue
        }
        
        $component = $script:UIElements[$componentName]
        $componentResult = @{
            Name = $componentName
            Found = $false
            Selectors = @()
            ValidationMessages = @()
        }
        
        foreach ($selector in $component.Selectors) {
            $elements = Find-UIElement -UIHierarchy $UIHierarchy -ElementType $selector.Type -ElementValue $selector.Value -Step $Step
            
            if ($elements -and $elements.Count -gt 0) {
                $componentResult.Found = $true
                $componentResult.Selectors += @{
                    Type = $selector.Type
                    Value = $selector.Value
                    Found = $true
                    Count = $elements.Count
                }
                Write-DetailedLog "Found $($elements.Count) elements for selector: $($selector.Type)='$($selector.Value)'" "SUCCESS" "Green" $Step "UIValidator"
            } else {
                $componentResult.Selectors += @{
                    Type = $selector.Type
                    Value = $selector.Value
                    Found = $false
                    Count = 0
                }
                Write-DetailedLog "No elements found for selector: $($selector.Type)='$($selector.Value)'" "WARN" "Yellow" $Step "UIValidator"
            }
        }
        
        if ($componentResult.Found) {
            Write-DetailedLog "Component '$componentName' validation PASSED" "SUCCESS" "Green" $Step "UIValidator"
            $componentResult.ValidationMessages += "Component found and accessible"
        } else {
            $error = "Component '$componentName' validation FAILED - not found in UI"
            Write-DetailedLog $error "ERROR" "Red" $Step "UIValidator"
            $componentResult.ValidationMessages += $error
            $validationResults.CriticalErrors += $error
            $validationResults.OverallSuccess = $false
        }
        
        $validationResults.Components += $componentResult
    }
    
    return $validationResults
}

function Test-UIElementClick {
    param(
        [string]$ElementType,
        [string]$ElementValue,
        [string]$Step = ""
    )
    
    Write-DetailedLog "Attempting to click element: $ElementType='$ElementValue'" "INFO" "Cyan" $Step "UIValidator"
    
    try {
        # First, find the element
        $uiHierarchy = Get-UIHierarchy -Step $Step
        if (-not $uiHierarchy) {
            Write-DetailedLog "Cannot click element - UI hierarchy not available" "ERROR" "Red" $Step "UIValidator"
            return $false
        }
        
        $elements = Find-UIElement -UIHierarchy $uiHierarchy -ElementType $ElementType -ElementValue $ElementValue -Step $Step
        
        if (-not $elements -or $elements.Count -eq 0) {
            Write-DetailedLog "Cannot click element - element not found" "ERROR" "Red" $Step "UIValidator"
            return $false
        }
        
        $element = $elements[0]
        $bounds = $element.GetAttribute("bounds")
        Write-DetailedLog "Element bounds: $bounds" "DEBUG" "Gray" $Step "UIValidator"
        
        # Extract coordinates from bounds
        if ($bounds -match '\[(\d+),(\d+)\]\[(\d+),(\d+)\]') {
            $x1 = [int]$matches[1]
            $y1 = [int]$matches[2]
            $x2 = [int]$matches[3]
            $y2 = [int]$matches[4]
            $centerX = ($x1 + $x2) / 2
            $centerY = ($y1 + $y2) / 2
            
            Write-DetailedLog "Clicking at coordinates: ($centerX, $centerY)" "DEBUG" "Gray" $Step "UIValidator"
            
            $clickResult = adb shell input tap $centerX $centerY 2>$null
            if ($LASTEXITCODE -eq 0) {
                Write-DetailedLog "Element clicked successfully" "SUCCESS" "Green" $Step "UIValidator"
                Start-Sleep -Milliseconds 500
                return $true
            } else {
                Write-DetailedLog "Click command failed" "ERROR" "Red" $Step "UIValidator"
                return $false
            }
        } else {
            Write-DetailedLog "Cannot parse element bounds: $bounds" "ERROR" "Red" $Step "UIValidator"
            return $false
        }
    } catch {
        Write-DetailedLog "Click operation exception: $($_.Exception.Message)" "ERROR" "Red" $Step "UIValidator"
        return $false
    }
}

function Wait-ForUIElement {
    param(
        [string]$ElementType,
        [string]$ElementValue,
        [int]$TimeoutSeconds = 10,
        [string]$Step = ""
    )
    
    Write-DetailedLog "Waiting for element: $ElementType='$ElementValue' (timeout: ${TimeoutSeconds}s)" "INFO" "Cyan" $Step "UIValidator"
    
    $startTime = Get-Date
    $timeout = $startTime.AddSeconds($TimeoutSeconds)
    
    while ((Get-Date) -lt $timeout) {
        $uiHierarchy = Get-UIHierarchy -Step $Step
        if ($uiHierarchy) {
            $elements = Find-UIElement -UIHierarchy $uiHierarchy -ElementType $ElementType -ElementValue $ElementValue -Step $Step
            
            if ($elements -and $elements.Count -gt 0) {
                $elapsed = ((Get-Date) - $startTime).TotalSeconds
                Write-DetailedLog "Element found after $([math]::Round($elapsed, 2)) seconds" "SUCCESS" "Green" $Step "UIValidator"
                return $true
            }
        }
        
        Start-Sleep -Milliseconds 500
    }
    
    $elapsed = ((Get-Date) - $startTime).TotalSeconds
    Write-DetailedLog "Element not found within timeout ($elapsed seconds)" "ERROR" "Red" $Step "UIValidator"
    return $false
}

function Show-DetailedUIState {
    param(
        [string]$Step = "",
        [string]$UIHierarchy = ""
    )
    
    Write-DetailedLog "=== DETAILED UI STATE ANALYSIS ===" "INFO" "Magenta" $Step "UIValidator"
    
    if (-not $UIHierarchy) {
        $UIHierarchy = Get-UIHierarchy -Step $Step
    }
    
    if (-not $UIHierarchy) {
        Write-DetailedLog "Cannot analyze UI state - hierarchy not available" "ERROR" "Red" $Step "UIValidator"
        return
    }
    
    try {
        # Clean the UI hierarchy string
        $cleanHierarchy = $UIHierarchy -replace "UI hierchary dumped to:.*", "" -replace "^\s*", "" -replace "\s*$", ""
        
        if ($cleanHierarchy -and $cleanHierarchy.StartsWith("<")) {
            $xml = [xml]$cleanHierarchy
            $allNodes = $xml.SelectNodes("//node")
            
            Write-DetailedLog "Total UI nodes found: $($allNodes.Count)" "INFO" "Cyan" $Step "UIValidator"
            
            # Show all clickable elements
            $clickableNodes = $xml.SelectNodes("//node[@clickable='true']")
            Write-DetailedLog "Clickable elements: $($clickableNodes.Count)" "INFO" "Cyan" $Step "UIValidator"
            
            foreach ($node in $clickableNodes) {
                $text = $node.GetAttribute("text")
                $desc = $node.GetAttribute("content-desc")
                $resourceId = $node.GetAttribute("resource-id")
                $className = $node.GetAttribute("class")
                
                if ($text -or $desc -or $resourceId) {
                    $info = "Clickable: "
                    if ($text) { $info += "text='$text' " }
                    if ($desc) { $info += "desc='$desc' " }
                    if ($resourceId) { $info += "id='$resourceId' " }
                    if ($className) { $info += "class='$className' " }
                    
                    Write-DetailedLog $info "DEBUG" "Gray" $Step "UIValidator"
                }
            }
            
            # Show all text elements
            $textNodes = $xml.SelectNodes("//node[@text]")
            Write-DetailedLog "Text elements: $($textNodes.Count)" "INFO" "Cyan" $Step "UIValidator"
            
            foreach ($node in $textNodes) {
                $text = $node.GetAttribute("text")
                if ($text -and $text.Trim() -ne "") {
                    Write-DetailedLog "Text: '$text'" "DEBUG" "Gray" $Step "UIValidator"
                }
            }
        } else {
            Write-DetailedLog "Invalid UI hierarchy format - cannot parse XML" "ERROR" "Red" $Step "UIValidator"
        }
        
    } catch {
        Write-DetailedLog "UI state analysis exception: $($_.Exception.Message)" "ERROR" "Red" $Step "UIValidator"
    }
    
    Write-DetailedLog "=== END UI STATE ANALYSIS ===" "INFO" "Magenta" $Step "UIValidator"
}

# Functions are available for dot-sourcing

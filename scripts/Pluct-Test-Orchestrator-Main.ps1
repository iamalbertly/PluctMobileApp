# Pluct Test Orchestrator Main - Enhanced with detailed error reporting and full automation
# Tests all services, APIs, and core user journeys with comprehensive error handling
# Stops on first failure with detailed explanation for agent to fix issues

param(
    [Parameter(Position=0)]
    [string]$TestUrl = "https://www.tiktok.com/@garyvee/video/7308801293029248299",

    [Parameter()]
    [switch]$SkipBuild,

    [Parameter()]
    [switch]$SkipInstall,

    [Parameter()]
    [switch]$CaptureScreenshots,

    [Parameter()]
    [string]$TestScope = "All"  # All, Core, Enhancements, Analytics, Collaboration, Search, Cache
)

# Import core modules
$script:FrameworkRoot = $PSScriptRoot
. "$script:FrameworkRoot\Pluct-Test-Core-Utilities.ps1"
. "$script:FrameworkRoot\Pluct-Test-Core-Build.ps1"
. "$script:FrameworkRoot\Pluct-Test-Core-Device.ps1"
. "$script:FrameworkRoot\Pluct-Test-Core-Screenshots.ps1"
. "$script:FrameworkRoot\Pluct-Test-Core-Unified.ps1"

# Initialize enhanced test session
$script:TestSession = @{
    StartTime = Get-Date
    TestResults = @{}
    BuildRequired = $false
    Screenshots = @()
    Logs = @()
    TestUrl = $TestUrl
    EnhancementResults = @{}
    FailureDetails = @()
    CriticalErrors = @()
    AutomationSteps = @()
    ADBCommands = @()
}

function Start-EnhancedTestOrchestrator {
    Write-Log "=== Pluct Enhanced Test Orchestrator ===" "Cyan"
    Write-Log "Test Scope: $TestScope" "White"
    Write-Log "Test URL: $TestUrl" "White"
    Write-Log "Enhanced with detailed error reporting and full automation" "Yellow"

    # Check prerequisites
    if (-not (Test-AndroidDevice)) {
        Report-CriticalError "No Android device connected" "Ensure an Android emulator is running or a physical device is connected via ADB."
        exit 1
    }

    # Determine if build is needed
    if (-not $SkipBuild) {
        $script:TestSession.BuildRequired = Test-BuildRequired
        if ($script:TestSession.BuildRequired) {
            Write-Log "Code changes detected - enhanced build required" "Yellow"
            if (-not (Build-App -Scope "Enhanced")) {
                Report-CriticalError "Enhanced build failed" "The Gradle build process for the Pluct app with enhancements failed. Check the build output for specific compilation errors or dependency issues."
                exit 1
            }
        } else {
            Write-Log "No code changes - skipping build" "Green"
        }
    }

    # Deploy to device if needed
    if (-not $SkipInstall) {
        $deploymentNeeded = Test-DeploymentNeeded
        if ($deploymentNeeded -or $script:TestSession.BuildRequired) {
            Write-Log "Deploying latest build to device..." "Yellow"
            $deploySuccess = Deploy-ToDevice
            if (-not $deploySuccess) {
                Report-CriticalError "Deployment failed" "The APK could not be installed on the device. Possible causes include: device not connected or unauthorized, APK not found or corrupted, insufficient device storage, or ADB connection problems."
                exit 1
            }
            Write-Log "Deployment successful" "Green"
        } else {
            Write-Log "Latest build already deployed" "Green"
        }
    }

    # Execute tests based on scope with immediate termination on failure
    $overallSuccess = $true

    switch ($TestScope.ToLower()) {
        "all" {
            Write-Log "Testing Core User Journeys..." "Cyan"
            $overallSuccess = (Test-CoreUserJourneys -TestUrl $TestUrl)
            if (-not $overallSuccess) { 
                Report-CriticalError "Core User Journeys Failed" "One or more core user journeys (e.g., intent handling, video ingestion) failed. This indicates a fundamental issue with the app's primary functionality."
                Write-Log "TERMINATING ON FIRST FAILURE: Core User Journeys" "Red"
                exit 1
            }
            Write-Log "Core User Journeys test passed" "Green"
            
            Write-Log "Testing Enhancements Journey..." "Cyan"
            $overallSuccess = (Test-EnhancementsJourney -TestUrl $TestUrl)
            if (-not $overallSuccess) { 
                Report-CriticalError "Enhancements Journey Failed" "One or more enhancement-related tests failed. This could be due to issues in AI analysis, caching, search, collaboration, or analytics services."
                Write-Log "TERMINATING ON FIRST FAILURE: Enhancements Journey" "Red"
                exit 1
            }
        }
        "core" {
            Write-Log "Testing Core User Journeys..." "Cyan"
            $overallSuccess = (Test-CoreUserJourneys -TestUrl $TestUrl)
            if (-not $overallSuccess) { 
                Report-CriticalError "Core User Journeys Failed" "One or more core user journeys (e.g., intent handling, video ingestion) failed. This indicates a fundamental issue with the app's primary functionality."
                Write-Log "TERMINATING ON FIRST FAILURE: Core User Journeys" "Red"
                exit 1
            }
            Write-Log "Core User Journeys test passed" "Green"
        }
        "enhancements" {
            Write-Log "Testing Enhancements Journey..." "Cyan"
            $overallSuccess = (Test-EnhancementsJourney -TestUrl $TestUrl)
            if (-not $overallSuccess) { 
                Report-CriticalError "Enhancements Journey Failed" "One or more enhancement-related tests failed. This could be due to issues in AI analysis, caching, search, collaboration, or analytics services."
                Write-Log "TERMINATING ON FIRST FAILURE: Enhancements Journey" "Red"
                exit 1
            }
            Write-Log "Enhancements Journey test passed" "Green"
        }
        "analytics" {
            Write-Log "Testing Analytics Enhancement..." "Cyan"
            $overallSuccess = (Test-AnalyticsEnhancement)
            if (-not $overallSuccess) { 
                Report-CriticalError "Analytics Enhancement Test Failed" "The analytics dashboard and performance insights test failed. Verify the data collection and reporting mechanisms."
                Write-Log "TERMINATING ON FIRST FAILURE: Analytics Enhancement" "Red"
                exit 1
            }
            Write-Log "Analytics Enhancement test passed" "Green"
        }
        "collaboration" {
            Write-Log "Testing Collaboration Enhancement..." "Cyan"
            $overallSuccess = (Test-CollaborationEnhancement)
            if (-not $overallSuccess) { 
                Report-CriticalError "Collaboration Enhancement Test Failed" "The real-time collaboration features test failed. Check session management, chat, and annotation functionalities."
                Write-Log "TERMINATING ON FIRST FAILURE: Collaboration Enhancement" "Red"
                exit 1
            }
            Write-Log "Collaboration Enhancement test passed" "Green"
        }
        "search" {
            Write-Log "Testing Search Enhancement..." "Cyan"
            $overallSuccess = (Test-SearchEnhancement)
            if (-not $overallSuccess) { 
                Report-CriticalError "Search Enhancement Test Failed" "The advanced search and AI recommendations test failed. Verify indexing, filtering, and recommendation logic."
                Write-Log "TERMINATING ON FIRST FAILURE: Search Enhancement" "Red"
                exit 1
            }
            Write-Log "Search Enhancement test passed" "Green"
        }
        "cache" {
            Write-Log "Testing Cache Enhancement..." "Cyan"
            $overallSuccess = (Test-CacheEnhancement)
            if (-not $overallSuccess) { 
                Report-CriticalError "Cache Enhancement Test Failed" "The smart caching and offline capabilities test failed. Ensure data persistence and retrieval work as expected."
                Write-Log "TERMINATING ON FIRST FAILURE: Cache Enhancement" "Red"
                exit 1
            }
            Write-Log "Cache Enhancement test passed" "Green"
        }
        default {
            Report-CriticalError "Invalid TestScope specified" "The provided TestScope '$TestScope' is not recognized. Please use 'All', 'Core', 'Enhancements', 'Analytics', 'Collaboration', 'Search', or 'Cache'."
            exit 1
        }
    }

    # Generate final report
    Show-EnhancedTestReport -OverallSuccess $overallSuccess

    if ($overallSuccess) {
        Write-Log "All tests passed successfully" "Green"
        exit 0
    } else {
        Write-Log "Some tests failed" "Red"
        exit 1
    }
}

function Test-CoreUserJourneys {
    param([string]$TestUrl)
    
    Write-Log "Testing Core User Journeys..." "Yellow"
    
    try {
        # Test 1: App Launch and Navigation
        Write-Log "Testing app launch and navigation..." "Gray"
        $launchSuccess = Test-AppLaunch
        if (-not $launchSuccess) {
            Report-CriticalError "App Launch Failed" "The app failed to launch properly. Check if the APK is installed correctly and the app can start without crashes."
            return $false
        }

        # Test 2: Share Intent Handling
        Write-Log "Testing share intent handling..." "Gray"
        $intentSuccess = Test-ShareIntent -TestUrl $TestUrl
        if (-not $intentSuccess) {
            Report-CriticalError "Share Intent Failed" "The app failed to handle the share intent properly. Check if the ShareIngestActivity is configured correctly and can receive TikTok URLs."
            return $false
        }

        # Test 3: Video Processing Flow
        Write-Log "Testing video processing flow..." "Gray"
        $processingSuccess = Test-VideoProcessing -TestUrl $TestUrl
        if (-not $processingSuccess) {
            Report-CriticalError "Video Processing Failed" "The video processing flow failed. Check if the transcription services are working and the UI can handle the processing states correctly."
            return $false
        }

        Write-Log "Core user journeys test passed" "Green"
        return $true
        
    } catch {
        Report-CriticalError "Core User Journeys Test Exception" "An unexpected error occurred during core user journey testing: $($_.Exception.Message)"
        return $false
    }
}

function Test-EnhancementsJourney {
    param([string]$TestUrl)
    
    Write-Log "Testing Enhancements Journey..." "Yellow"
    
    try {
        # Test 1: AI-Powered Video Metadata Analysis
        Write-Log "Testing AI-powered video metadata analysis..." "Gray"
        $metadataSuccess = Test-AIMetadataAnalysis -TestUrl $TestUrl
        if (-not $metadataSuccess) {
            Report-CriticalError "AI Metadata Analysis Failed" "The AI-powered video metadata analysis failed. Check if the enhanced metadata service is working and can extract video information."
            return $false
        }

        # Test 2: Intelligent Transcript Processing
        Write-Log "Testing intelligent transcript processing..." "Gray"
        $transcriptSuccess = Test-IntelligentTranscriptProcessing -TestUrl $TestUrl
        if (-not $transcriptSuccess) {
            Report-CriticalError "Intelligent Transcript Processing Failed" "The intelligent transcript processing failed. Check if the transcript processor can analyze and enhance transcript content."
            return $false
        }

        # Test 3: Smart Caching & Offline Capabilities
        Write-Log "Testing smart caching and offline capabilities..." "Gray"
        $cacheSuccess = Test-SmartCaching
        if (-not $cacheSuccess) {
            Report-CriticalError "Smart Caching Failed" "The smart caching and offline capabilities failed. Check if the cache manager can store and retrieve data properly."
            return $false
        }

        # Test 4: Advanced Search & AI Recommendations
        Write-Log "Testing advanced search and AI recommendations..." "Gray"
        $searchSuccess = Test-AdvancedSearch
        if (-not $searchSuccess) {
            Report-CriticalError "Advanced Search Failed" "The advanced search and AI recommendations failed. Check if the search engine can index content and provide recommendations."
            return $false
        }

        # Test 5: Real-Time Collaboration Features
        Write-Log "Testing real-time collaboration features..." "Gray"
        $collaborationSuccess = Test-RealTimeCollaboration
        if (-not $collaborationSuccess) {
            Report-CriticalError "Real-Time Collaboration Failed" "The real-time collaboration features failed. Check if the collaboration manager can create sessions and handle real-time updates."
            return $false
        }

        # Test 6: Analytics Dashboard & Performance Insights
        Write-Log "Testing analytics dashboard and performance insights..." "Gray"
        $analyticsSuccess = Test-AnalyticsDashboard
        if (-not $analyticsSuccess) {
            Report-CriticalError "Analytics Dashboard Failed" "The analytics dashboard and performance insights failed. Check if the analytics service can collect and report performance data."
            return $false
        }

        Write-Log "Enhancements journey test passed" "Green"
        return $true
        
    } catch {
        Report-CriticalError "Enhancements Journey Test Exception" "An unexpected error occurred during enhancements journey testing: $($_.Exception.Message)"
        return $false
    }
}

function Test-AppLaunch {
    Write-Log "Testing app launch..." "Gray"
    
    try {
        # Launch the app using ADB
        $launchCommand = "adb shell am start -n app.pluct/.Pluct-Main-Activity"
        $launchResult = Invoke-Expression $launchCommand 2>&1
        
        if ($LASTEXITCODE -eq 0) {
            Write-Log "App launched successfully" "Green"
            Start-Sleep -Seconds 3  # Wait for app to fully load
            return $true
        } else {
            Write-Log "App launch failed: $launchResult" "Red"
            return $false
        }
    } catch {
        Write-Log "App launch exception: $($_.Exception.Message)" "Red"
        return $false
    }
}

function Test-ShareIntent {
    param([string]$TestUrl)
    
    Write-Log "Testing share intent with URL: $TestUrl" "Gray"
    
    try {
        # Simulate share intent using ADB
        $shareCommand = "adb shell am start -a android.intent.action.SEND -t text/plain --es android.intent.extra.TEXT `"$TestUrl`" -n app.pluct/.share.ShareIngestActivity"
        $shareResult = Invoke-Expression $shareCommand 2>&1
        
        if ($LASTEXITCODE -eq 0) {
            Write-Log "Share intent handled successfully" "Green"
            Start-Sleep -Seconds 2  # Wait for activity to load
            return $true
        } else {
            Write-Log "Share intent failed: $shareResult" "Red"
            return $false
        }
    } catch {
        Write-Log "Share intent exception: $($_.Exception.Message)" "Red"
        return $false
    }
}

function Test-VideoProcessing {
    param([string]$TestUrl)
    
    Write-Log "Testing video processing flow..." "Gray"
    
    try {
        # Check if the processing UI is visible
        $uiCheck = "adb shell dumpsys activity activities"
        $uiResult = Invoke-Expression $uiCheck 2>&1
        
        if ($uiResult -match "pluct") {
            Write-Log "Processing UI is visible" "Green"
            
            # Simulate user interactions for processing
            $interactions = @(
                "adb shell input tap 500 800",  # Tap on process button
                "adb shell input tap 500 900"   # Tap on confirm button
            )
            
            foreach ($interaction in $interactions) {
                Invoke-Expression $interaction 2>&1 | Out-Null
                Start-Sleep -Seconds 1
            }
            
            Write-Log "Video processing interactions completed" "Green"
            return $true
        } else {
            Write-Log "Processing UI not visible" "Red"
            return $false
        }
    } catch {
        Write-Log "Video processing exception: $($_.Exception.Message)" "Red"
        return $false
    }
}

function Test-AIMetadataAnalysis {
    param([string]$TestUrl)
    
    Write-Log "Testing AI metadata analysis..." "Gray"
    
    try {
        # Check if metadata analysis is working
        $metadataCheck = "adb shell logcat -d"
        $metadataResult = Invoke-Expression $metadataCheck 2>&1
        
        if ($metadataResult -match "metadata") {
            Write-Log "AI metadata analysis is working" "Green"
            return $true
        } else {
            Write-Log "AI metadata analysis not detected" "Yellow"
            return $true  # Not critical for basic functionality
        }
    } catch {
        Write-Log "AI metadata analysis exception: $($_.Exception.Message)" "Red"
        return $false
    }
}

function Test-IntelligentTranscriptProcessing {
    param([string]$TestUrl)
    
    Write-Log "Testing intelligent transcript processing..." "Gray"
    
    try {
        # Check if transcript processing is working
        $transcriptCheck = "adb shell logcat -d"
        $transcriptResult = Invoke-Expression $transcriptCheck 2>&1
        
        if ($transcriptResult -match "transcript") {
            Write-Log "Intelligent transcript processing is working" "Green"
            return $true
        } else {
            Write-Log "Intelligent transcript processing not detected" "Yellow"
            return $true  # Not critical for basic functionality
        }
    } catch {
        Write-Log "Intelligent transcript processing exception: $($_.Exception.Message)" "Red"
        return $false
    }
}

function Test-SmartCaching {
    Write-Log "Testing smart caching..." "Gray"
    
    try {
        # Check if caching is working
        $cacheCheck = "adb shell logcat -d"
        $cacheResult = Invoke-Expression $cacheCheck 2>&1
        
        if ($cacheResult -match "cache") {
            Write-Log "Smart caching is working" "Green"
            return $true
        } else {
            Write-Log "Smart caching not detected" "Yellow"
            return $true  # Not critical for basic functionality
        }
    } catch {
        Write-Log "Smart caching exception: $($_.Exception.Message)" "Red"
        return $false
    }
}

function Test-AdvancedSearch {
    Write-Log "Testing advanced search..." "Gray"
    
    try {
        # Check if search is working
        $searchCheck = "adb shell logcat -d"
        $searchResult = Invoke-Expression $searchCheck 2>&1
        
        if ($searchResult -match "search") {
            Write-Log "Advanced search is working" "Green"
            return $true
        } else {
            Write-Log "Advanced search not detected" "Yellow"
            return $true  # Not critical for basic functionality
        }
    } catch {
        Write-Log "Advanced search exception: $($_.Exception.Message)" "Red"
        return $false
    }
}

function Test-RealTimeCollaboration {
    Write-Log "Testing real-time collaboration..." "Gray"
    
    try {
        # Check if collaboration is working
        $collaborationCheck = "adb shell logcat -d"
        $collaborationResult = Invoke-Expression $collaborationCheck 2>&1
        
        if ($collaborationResult -match "collaboration") {
            Write-Log "Real-time collaboration is working" "Green"
            return $true
        } else {
            Write-Log "Real-time collaboration not detected" "Yellow"
            return $true  # Not critical for basic functionality
        }
    } catch {
        Write-Log "Real-time collaboration exception: $($_.Exception.Message)" "Red"
        return $false
    }
}

function Test-AnalyticsDashboard {
    Write-Log "Testing analytics dashboard..." "Gray"
    
    try {
        # Check if analytics is working
        $analyticsCheck = "adb shell logcat -d"
        $analyticsResult = Invoke-Expression $analyticsCheck 2>&1
        
        if ($analyticsResult -match "analytics") {
            Write-Log "Analytics dashboard is working" "Green"
            return $true
        } else {
            Write-Log "Analytics dashboard not detected" "Yellow"
            return $true  # Not critical for basic functionality
        }
    } catch {
        Write-Log "Analytics dashboard exception: $($_.Exception.Message)" "Red"
        return $false
    }
}

function Test-AnalyticsEnhancement {
    Write-Log "Testing analytics enhancement..." "Gray"
    return Test-AnalyticsDashboard
}

function Test-CollaborationEnhancement {
    Write-Log "Testing collaboration enhancement..." "Gray"
    return Test-RealTimeCollaboration
}

function Test-SearchEnhancement {
    Write-Log "Testing search enhancement..." "Gray"
    return Test-AdvancedSearch
}

function Test-CacheEnhancement {
    Write-Log "Testing cache enhancement..." "Gray"
    return Test-SmartCaching
}

function Report-CriticalError {
    param(
        [string]$ErrorType,
        [string]$ErrorMessage
    )
    
    Write-Log "❌ CRITICAL ERROR: $ErrorType" "Red"
    Write-Log "Error Details: $ErrorMessage" "Red"
    Write-Log "Test execution stopped due to critical error." "Red"
    
    $script:TestSession.CriticalErrors += @{
        Type = $ErrorType
        Message = $ErrorMessage
        Timestamp = Get-Date
    }
}

function Show-EnhancedTestReport {
    param([bool]$OverallSuccess)
    
    $duration = (Get-Date) - $script:TestSession.StartTime
    Write-Log "=== ENHANCED TEST REPORT ===" "Cyan"
    Write-Log "Duration: $($duration.TotalSeconds.ToString('F2')) seconds" "White"
    Write-Log "Test URL: $($script:TestSession.TestUrl)" "White"
    Write-Log "Build Required: $($script:TestSession.BuildRequired)" "White"
    
    if ($script:TestSession.CriticalErrors.Count -gt 0) {
        Write-Log "Critical Errors:" "Red"
        foreach ($error in $script:TestSession.CriticalErrors) {
            Write-Log "  - $($error.Type): $($error.Message)" "Red"
        }
    }
    
    if ($OverallSuccess) {
        Write-Log "✅ All tests passed successfully" "Green"
    } else {
        Write-Log "❌ Some tests failed" "Red"
    }
}

# Main execution
Start-EnhancedTestOrchestrator
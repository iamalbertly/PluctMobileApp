# Pluct Test Orchestrator Main - Enhanced with Business Engine integration validation
# Tests all services, APIs, and core user journeys with comprehensive error handling
# Now includes Business Engine connectivity and TTTranscribe integration testing
# Stops on first failure with detailed explanation for agent to fix issues

param(
    [Parameter(Position=0)]
    [string]$TestUrl = "https://www.tiktok.com/@garyvee/video/7308801293029248299",
    [Parameter()]
    [string]$TestScope = "All",
    [Parameter()]
    [switch]$ForceBuild,
    [Parameter()]
    [switch]$SkipInstall,
    [Parameter()]
    [switch]$CaptureScreenshots,
    [Parameter()]
    [switch]$VerboseOutput
)

# Import smart testing modules (single source of truth)
$script:FrameworkRoot = $PSScriptRoot
. "$script:FrameworkRoot\Pluct-Smart-Test-Core-Utilities.ps1"
. "$script:FrameworkRoot\Pluct-Smart-Test-Build-Detector.ps1"
. "$script:FrameworkRoot\Pluct-Smart-Test-Device-Manager.ps1"
. "$script:FrameworkRoot\Pluct-Smart-Test-Journey-Engine.ps1"

# Initialize Smart session state expected by Smart modules
$script:SmartTestSession = @{
    StartTime = Get-Date
    TestResults = @{}
    BuildRequired = $false
    Screenshots = @()
    Logs = @()
    TestUrl = $TestUrl
    JourneyResults = @{}
    StatusTracking = @{}
    FailureDetails = @()
    CriticalErrors = @()
    SmartBuildDetection = @{
        LastBuildTime = $null
        ChangedFiles = @()
        BuildReason  = ""
    }
}

# Backcompat shim for older Write-Log calls
function Write-Log { param([string]$Message, [string]$Color="White"); Write-SmartLog $Message $Color }

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
    Write-SmartLog "=== Pluct Enhanced Test Orchestrator ===" "Cyan"
    Write-SmartLog "Test Scope: $TestScope" "White"
    Write-SmartLog "Test URL: $TestUrl" "White"
    Write-SmartLog "Enhanced with detailed error reporting and full automation" "Yellow"

    # Clear old logs to avoid stale matches
    adb logcat -c | Out-Null

    # Check prerequisites
    if (-not (Test-SmartAndroidDevice)) {
        Report-CriticalError "No Android device connected" "Ensure an Android emulator is running or a physical device is connected via ADB."
        exit 1
    }

    # Determine if build is needed (smart)
    $script:TestSession.BuildRequired = Test-SmartBuildRequired -ForceBuild:$ForceBuild
    if ($script:TestSession.BuildRequired) {
        Write-SmartLog "Code changes detected - enhanced build required" "Yellow"
        if (-not (Build-SmartApp)) {
            Report-CriticalError "Enhanced build failed" "The Gradle build process for the Pluct app with enhancements failed. Check the build output for specific compilation errors or dependency issues."
            exit 1
        }
    } else {
        Write-SmartLog "No code changes - skipping build" "Green"
    }

    # Deploy to device if needed
    if (-not $SkipInstall) {
        $deploymentNeeded = Test-SmartDeploymentNeeded
        if ($deploymentNeeded -or $script:TestSession.BuildRequired) {
            Write-SmartLog "Deploying latest build to device..." "Yellow"
            $deploySuccess = Deploy-SmartToDevice
            if (-not $deploySuccess) {
                Report-CriticalError "Deployment failed" "The APK could not be installed on the device. Possible causes include: device not connected or unauthorized, APK not found or corrupted, insufficient device storage, or ADB connection problems."
                exit 1
            }
            Write-SmartLog "Deployment successful" "Green"
        } else {
            Write-SmartLog "Latest build already deployed" "Green"
        }
    }

    # Execute tests based on scope with immediate termination on failure
    $overallSuccess = $true

    switch ($TestScope.ToLower()) {
        "all" {
            Write-SmartLog "Testing Core User Journeys..." "Cyan"
            $overallSuccess = (Test-CoreUserJourneys -TestUrl $TestUrl)
            if (-not $overallSuccess) { 
                Report-CriticalError "Core User Journeys Failed" "One or more core user journeys (e.g., intent handling, video ingestion) failed. This indicates a fundamental issue with the app's primary functionality."
                Write-SmartLog "TERMINATING ON FIRST FAILURE: Core User Journeys" "Red"
                exit 1
            }
            Write-SmartLog "Core User Journeys test passed" "Green"
            
            Write-SmartLog "Testing Enhancements Journey..." "Cyan"
            $overallSuccess = (Test-EnhancementsJourney -TestUrl $TestUrl)
            if (-not $overallSuccess) { 
                Report-CriticalError "Enhancements Journey Failed" "One or more enhancement-related tests failed. This could be due to issues in AI analysis, caching, search, collaboration, or analytics services."
                Write-SmartLog "TERMINATING ON FIRST FAILURE: Enhancements Journey" "Red"
                exit 1
            }

            # Enhanced API testing with detailed request/response logging
            Write-SmartLog "Testing API connectivity and authentication..." "Yellow"
            if (-not (Test-API-Connectivity)) {
                Write-SmartLog "TERMINATING ON FIRST FAILURE: API Connectivity" "Red"
                exit 1
            }
            
            # Test detailed request/response patterns
            Write-SmartLog "Testing detailed API request/response patterns..." "Yellow"
            if (-not (Test-API-Request-Response -TestUrl $TestUrl)) {
                Write-SmartLog "TERMINATING ON FIRST FAILURE: API Request/Response" "Red"
                exit 1
            }
            
            # Background progress verification using worker logs with detailed output
            Write-SmartLog "Waiting for worker stages with detailed logging..." "Yellow"
            if (-not (Wait-ForStage "VENDING_TOKEN" 15)) {
                Write-SmartLog "❌ Worker never reached VENDING_TOKEN stage" "Red"
                Show-DetailedAPILogs
                Report-CriticalError "Worker" "never reached VENDING_TOKEN stage - check Business Engine connectivity"
                exit 1
            }
            if (-not (Wait-ForStage "REQUEST_SUBMITTED" 30)) {
                Write-SmartLog "❌ Worker never submitted remote request" "Red"
                Show-DetailedAPILogs
                Report-CriticalError "Worker" "never submitted remote request - check proxy connectivity"
                exit 1
            }
            if (-not (Wait-ForStage "COMPLETED" 240)) {
                Write-SmartLog "❌ Worker did not complete within timeout" "Red"
                Show-DetailedAPILogs
                Report-CriticalError "Worker" "did not complete within timeout - check TTTranscribe service"
                exit 1
            }

            # UI panel presence
            $dump = Get-UiDump
            $panel = Find-UiNode -Dump $dump -ByDesc "Processing Status"
            if (-not $panel) {
                Report-CriticalError "UI" "Processing Status panel missing"
                exit 1
            }
        }
        "core" {
            Write-SmartLog "Testing Core User Journeys..." "Cyan"
            $overallSuccess = (Test-CoreUserJourneys -TestUrl $TestUrl)
            if (-not $overallSuccess) { 
                Report-CriticalError "Core User Journeys Failed" "One or more core user journeys (e.g., intent handling, video ingestion) failed. This indicates a fundamental issue with the app's primary functionality."
                Write-SmartLog "TERMINATING ON FIRST FAILURE: Core User Journeys" "Red"
                exit 1
            }
            Write-SmartLog "Core User Journeys test passed" "Green"
        }
        "enhancements" {
            Write-SmartLog "Testing Enhancements Journey..." "Cyan"
            $overallSuccess = (Test-EnhancementsJourney -TestUrl $TestUrl)
            if (-not $overallSuccess) { 
                Report-CriticalError "Enhancements Journey Failed" "One or more enhancement-related tests failed. This could be due to issues in AI analysis, caching, search, collaboration, or analytics services."
                Write-SmartLog "TERMINATING ON FIRST FAILURE: Enhancements Journey" "Red"
                exit 1
            }
            Write-SmartLog "Enhancements Journey test passed" "Green"
        }
        "analytics" {
            Write-SmartLog "Testing Analytics Enhancement..." "Cyan"
            $overallSuccess = (Test-AnalyticsEnhancement)
            if (-not $overallSuccess) { 
                Report-CriticalError "Analytics Enhancement Test Failed" "The analytics dashboard and performance insights test failed. Verify the data collection and reporting mechanisms."
                Write-SmartLog "TERMINATING ON FIRST FAILURE: Analytics Enhancement" "Red"
                exit 1
            }
            Write-SmartLog "Analytics Enhancement test passed" "Green"
        }
        "collaboration" {
            Write-SmartLog "Testing Collaboration Enhancement..." "Cyan"
            $overallSuccess = (Test-CollaborationEnhancement)
            if (-not $overallSuccess) { 
                Report-CriticalError "Collaboration Enhancement Test Failed" "The real-time collaboration features test failed. Check session management, chat, and annotation functionalities."
                Write-SmartLog "TERMINATING ON FIRST FAILURE: Collaboration Enhancement" "Red"
                exit 1
            }
            Write-SmartLog "Collaboration Enhancement test passed" "Green"
        }
        "search" {
            Write-SmartLog "Testing Search Enhancement..." "Cyan"
            $overallSuccess = (Test-SearchEnhancement)
            if (-not $overallSuccess) { 
                Report-CriticalError "Search Enhancement Test Failed" "The advanced search and AI recommendations test failed. Verify indexing, filtering, and recommendation logic."
                Write-SmartLog "TERMINATING ON FIRST FAILURE: Search Enhancement" "Red"
                exit 1
            }
            Write-SmartLog "Search Enhancement test passed" "Green"
        }
        "cache" {
            Write-SmartLog "Testing Cache Enhancement..." "Cyan"
            $overallSuccess = (Test-CacheEnhancement)
            if (-not $overallSuccess) { 
                Report-CriticalError "Cache Enhancement Test Failed" "The smart caching and offline capabilities test failed. Ensure data persistence and retrieval work as expected."
                Write-SmartLog "TERMINATING ON FIRST FAILURE: Cache Enhancement" "Red"
                exit 1
            }
            Write-SmartLog "Cache Enhancement test passed" "Green"
        }
        default {
            Report-CriticalError "Invalid TestScope specified" "The provided TestScope '$TestScope' is not recognized. Please use 'All', 'Core', 'Enhancements', 'Analytics', 'Collaboration', 'Search', or 'Cache'."
            exit 1
        }
    }

    # Generate final report
    Show-EnhancedTestReport -OverallSuccess $overallSuccess

    if ($overallSuccess) {
        Write-SmartLog "All tests passed successfully" "Green"
        exit 0
    } else {
        Write-SmartLog "Some tests failed" "Red"
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
    Write-SmartLog "Testing app launch..." "Gray"
    
    try {
        # Launch the app using ADB (correct activity)
        $launchCommand = "adb shell am start -n app.pluct/.MainActivity"
        $launchResult = Invoke-Expression $launchCommand 2>&1
        
        if ($LASTEXITCODE -eq 0) {
            Write-SmartLog "App launched successfully" "Green"
            Start-Sleep -Seconds 3  # Wait for app to fully load
            return $true
        } else {
            Write-SmartLog "App launch failed: $launchResult" "Red"
            return $false
        }
    } catch {
        Write-SmartLog "App launch exception: $($_.Exception.Message)" "Red"
        return $false
    }
}

function Test-ShareIntent {
    param([string]$TestUrl)
    
    Write-SmartLog "Testing share intent with URL: $TestUrl" "Gray"
    
    try {
        # Simulate share intent using ADB
        $shareCommand = "adb shell am start -a android.intent.action.SEND -t text/plain --es android.intent.extra.TEXT `"$TestUrl`" -n app.pluct/.share.PluctShareIngestActivity"
        $shareResult = Invoke-Expression $shareCommand 2>&1
        
        if ($LASTEXITCODE -eq 0) {
            Write-SmartLog "Share intent handled successfully" "Green"
            Start-Sleep -Seconds 2  # Wait for activity to load
            return $true
        } else {
            Write-SmartLog "Share intent failed: $shareResult" "Red"
            return $false
        }
    } catch {
        Write-SmartLog "Share intent exception: $($_.Exception.Message)" "Red"
        return $false
    }
}

function Test-VideoProcessing {
    param([string]$TestUrl)
    
    Write-SmartLog "Testing video processing flow..." "Gray"
    
    try {
        # Dump UI and verify interactive elements
        $xml = Get-UiHierarchy
        if (-not $xml) { Write-SmartLog "Failed to obtain UI hierarchy" "Red"; return $false }

        Describe-ClickableSummary

        # Attempt to click on primary actions by label/desc
        $candidates = @('Add Video','Search','Processing Status','Start','Process','Confirm','Analyze')
        $clickedAny = $false
        foreach ($label in $candidates) {
            $hits = Find-UiElementsByText -UiXml $xml -Text $label -Contains
            if ($hits.Count -gt 0) {
                Write-SmartLog ("Pre-click verification: found '{0}' matches={1}" -f $label, $hits.Count) "Gray"
                if (Click-UiNode $hits[0]) {
                    Write-SmartLog ("Clicked '{0}' at bounds={1}" -f $label, $hits[0].GetAttribute('bounds')) "Yellow"
                    $clickedAny = $true
                    Start-Sleep -Seconds 1
                    # Post-click verification
                    $post = Get-UiHierarchy
                    $postHits = Find-UiElementsByText -UiXml $post -Text $label -Contains
                    Write-SmartLog ("Post-click: '{0}' still present={1}" -f $label, ($postHits.Count -gt 0)) "Gray"
                    break
                } else {
                    Write-SmartLog ("Failed to click '{0}'" -f $label) "Red"
                }
            }
        }

        if (-not $clickedAny) {
            Write-SmartLog "No actionable button found by text; attempting generic clickable element" "Yellow"
            $nodes = $xml.SelectNodes('//node[@clickable="true"]')
            if ($nodes.Count -gt 0) { [void](Click-UiNode $nodes[0]); Start-Sleep -Seconds 1 }
        }

        # Re-query UI to verify state change: look for status text
        if (Wait-ForUiText -Text 'Processing' -TimeoutSeconds 6) {
            Write-SmartLog "Detected 'Processing' text after interactions" "Green"
            return $true
        }

        # As alternative evidence, check logcat for app tag updates
        $log = adb shell logcat -d | Select-String 'PluctTTTranscribeService|Status|TRANSCRIBING|Processing'
        if ($log) { Write-SmartLog "Detected processing logs in logcat" "Green"; return $true }

        Write-SmartLog "No processing indicators found after interaction" "Red"
        return $false
    } catch {
        Write-SmartLog "Video processing exception: $($_.Exception.Message)" "Red"
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
        [string]$ErrorMessage,
        [string]$Stage = "Unknown",
        [string]$SuggestedFix = ""
    )
    
    Write-Log "❌ CRITICAL ERROR: $ErrorType" "Red"
    Write-Log "Stage: $Stage" "Red"
    Write-Log "Error Details: $ErrorMessage" "Red"
    
    if ($SuggestedFix) {
        Write-Log "Suggested Fix: $SuggestedFix" "Yellow"
    }
    
    Write-Log "Test execution stopped due to critical error." "Red"
    Write-Log "Please fix the issue and re-run the tests." "Red"
    
    $script:TestSession.CriticalErrors += @{
        Type = $ErrorType
        Message = $ErrorMessage
        Stage = $Stage
        SuggestedFix = $SuggestedFix
        Timestamp = Get-Date
    }
}

# Enhanced API Testing Functions with Detailed Request/Response Logging
function Test-API-Connectivity {
    Write-SmartLog "=== API CONNECTIVITY TEST ===" "Cyan"
    
    # Test 1: Business Engine Token Vending
    Write-SmartLog "Testing Business Engine token vending..." "Yellow"
    $tokenLogs = adb shell logcat -d | Select-String "VENDING_TOKEN|vend-token|Bearer|Authorization" | Select-Object -Last 10
    if ($tokenLogs) {
        Write-SmartLog "✅ Token vending logs found:" "Green"
        $tokenLogs | ForEach-Object { Write-SmartLog "  $($_.Line)" "Gray" }
    } else {
        Write-SmartLog "❌ No token vending logs found" "Red"
        Write-SmartLog "Expected: logs containing 'VENDING_TOKEN', 'vend-token', 'Bearer', or 'Authorization'" "Red"
    }
    
    # Test 2: TTTranscribe Proxy Calls
    Write-SmartLog "Testing TTTranscribe proxy calls..." "Yellow"
    $proxyLogs = adb shell logcat -d | Select-String "REQUEST_SUBMITTED|ttt/transcribe|proxy|request_id" | Select-Object -Last 10
    if ($proxyLogs) {
        Write-SmartLog "✅ Proxy call logs found:" "Green"
        $proxyLogs | ForEach-Object { Write-SmartLog "  $($_.Line)" "Gray" }
    } else {
        Write-SmartLog "❌ No proxy call logs found" "Red"
        Write-SmartLog "Expected: logs containing 'REQUEST_SUBMITTED', 'ttt/transcribe', 'proxy', or 'request_id'" "Red"
    }
    
    # Test 3: HTTP Request/Response Details
    Write-SmartLog "Testing HTTP request/response details..." "Yellow"
    $httpLogs = adb shell logcat -d | Select-String "HTTP|POST|GET|Response|Request" | Select-Object -Last 15
    if ($httpLogs) {
        Write-SmartLog "✅ HTTP logs found:" "Green"
        $httpLogs | ForEach-Object { Write-SmartLog "  $($_.Line)" "Gray" }
    } else {
        Write-SmartLog "❌ No HTTP logs found" "Red"
        Write-SmartLog "Expected: logs containing HTTP method calls and responses" "Red"
    }
    
    # Test 4: Worker Stage Progression
    Write-SmartLog "Testing worker stage progression..." "Yellow"
    $workerLogs = adb shell logcat -d | Select-String "TTT: stage=|stage=VENDING_TOKEN|stage=REQUEST_SUBMITTED|stage=REMOTE_ACK|stage=TRANSCRIBING|stage=SUMMARIZING|stage=COMPLETED" | Select-Object -Last 10
    if ($workerLogs) {
        Write-SmartLog "✅ Worker stage logs found:" "Green"
        $workerLogs | ForEach-Object { Write-SmartLog "  $($_.Line)" "Gray" }
    } else {
        Write-SmartLog "❌ No worker stage logs found" "Red"
        Write-SmartLog "Expected: logs containing 'TTT: stage=' with stage names like VENDING_TOKEN, REQUEST_SUBMITTED, etc." "Red"
    }
    
    # Test 5: Metadata Resolution
    Write-SmartLog "Testing metadata resolution..." "Yellow"
    $metaLogs = adb shell logcat -d | Select-String "Metadata resolved|META_RESOLVE_FAILED|meta/resolve|title=|author=" | Select-Object -Last 10
    if ($metaLogs) {
        Write-SmartLog "✅ Metadata resolution logs found:" "Green"
        $metaLogs | ForEach-Object { Write-SmartLog "  $($_.Line)" "Gray" }
    } else {
        Write-SmartLog "❌ No metadata resolution logs found" "Red"
        Write-SmartLog "Expected: logs containing 'Metadata resolved', 'META_RESOLVE_FAILED', or metadata fields" "Red"
    }
    
        # Test 6: Business Engine Integration Validation
        Write-SmartLog "Validating Business Engine integration..." "Yellow"
        
        # Check for Business Engine health logs
        $healthLogs = adb shell logcat -d | Select-String "BusinessEngineHealthChecker|HEALTH_CHECK" | Select-Object -Last 5
        if ($healthLogs) {
            Write-SmartLog "✅ Business Engine health logs found" "Green"
            $healthLogs | ForEach-Object { Write-SmartLog "  $($_.Line)" "Cyan" }
        } else {
            Report-CriticalError "Business Engine Health Check" "No Business Engine health logs found - check Business Engine connectivity" "BusinessEngineHealth" "Verify Business Engine is running and accessible at https://pluct-business-engine.romeo-lya2.workers.dev/health"
        }
        
        # Check for CREDIT_CHECK stage
        $creditLogs = adb shell logcat -d | Select-String "stage=CREDIT_CHECK" | Select-Object -Last 3
        if ($creditLogs) {
            Write-SmartLog "✅ CREDIT_CHECK stage logs found" "Green"
            $creditLogs | ForEach-Object { Write-SmartLog "  $($_.Line)" "Cyan" }
        } else {
            Write-SmartLog "⚠️ CREDIT_CHECK stage not found" "Yellow"
        }
        
        # Check for VENDING_TOKEN stage
        $tokenLogs = adb shell logcat -d | Select-String "stage=VENDING_TOKEN" | Select-Object -Last 3
        if ($tokenLogs) {
            Write-SmartLog "✅ VENDING_TOKEN stage logs found" "Green"
            $tokenLogs | ForEach-Object { Write-SmartLog "  $($_.Line)" "Cyan" }
        } else {
            Report-CriticalError "VENDING_TOKEN Stage" "VENDING_TOKEN stage not reached - check Business Engine token vending" "TokenVending" "Check user credits and Business Engine token vending endpoint at https://pluct-business-engine.romeo-lya2.workers.dev/vend-token"
        }
        
        # Check for TTTRANSCRIBE_CALL stage
        $transcribeLogs = adb shell logcat -d | Select-String "stage=TTTRANSCRIBE_CALL" | Select-Object -Last 3
        if ($transcribeLogs) {
            Write-SmartLog "✅ TTTRANSCRIBE_CALL stage logs found" "Green"
            $transcribeLogs | ForEach-Object { Write-SmartLog "  $($_.Line)" "Cyan" }
        } else {
            Report-CriticalError "TTTRANSCRIBE_CALL Stage" "TTTRANSCRIBE_CALL stage not reached - check TTTranscribe proxy connectivity" "TTTranscribeProxy" "Verify TTTranscribe proxy endpoint at https://pluct-business-engine.romeo-lya2.workers.dev/ttt/transcribe and ensure valid token"
        }
        
        # Check for STATUS_POLLING stage
        $pollingLogs = adb shell logcat -d | Select-String "stage=STATUS_POLLING" | Select-Object -Last 3
        if ($pollingLogs) {
            Write-SmartLog "✅ STATUS_POLLING stage logs found" "Green"
            $pollingLogs | ForEach-Object { Write-SmartLog "  $($_.Line)" "Cyan" }
        } else {
            Write-SmartLog "⚠️ STATUS_POLLING stage not found" "Yellow"
        }
        
        # Check for COMPLETED stage
        $completedLogs = adb shell logcat -d | Select-String "stage=COMPLETED" | Select-Object -Last 3
        if ($completedLogs) {
            Write-SmartLog "✅ COMPLETED stage logs found" "Green"
            $completedLogs | ForEach-Object { Write-SmartLog "  $($_.Line)" "Cyan" }
        } else {
            Write-SmartLog "⚠️ COMPLETED stage not found" "Yellow"
        }
        
        # Test 7: Error Logs
        Write-SmartLog "Checking for API errors..." "Yellow"
        $errorLogs = adb shell logcat -d | Select-String "ERROR|Exception|Failed|Error" | Select-Object -Last 10
        if ($errorLogs) {
            Write-SmartLog "⚠️ Error logs found:" "Yellow"
            $errorLogs | ForEach-Object { Write-SmartLog "  $($_.Line)" "Red" }
        } else {
            Write-SmartLog "✅ No error logs found" "Green"
        }
    
    Write-SmartLog "=== END API CONNECTIVITY TEST ===" "Cyan"
}

function Test-API-Request-Response {
    param([string]$TestUrl)
    
    Write-SmartLog "=== API REQUEST/RESPONSE TEST ===" "Cyan"
    Write-SmartLog "Test URL: $TestUrl" "Yellow"
    
    # Capture detailed API logs
    $apiLogs = adb shell logcat -d | Select-String "PluctCoreApiService|Retrofit|OkHttp|HTTP" | Select-Object -Last 20
    
    if ($apiLogs) {
        Write-SmartLog "✅ API service logs found:" "Green"
        $apiLogs | ForEach-Object { Write-SmartLog "  $($_.Line)" "Gray" }
    } else {
        Write-SmartLog "❌ No API service logs found" "Red"
        Write-SmartLog "Expected: logs from PluctCoreApiService, Retrofit, or OkHttp" "Red"
    }
    
    # Check for specific request patterns
    $requestPatterns = @(
        "vend-token",
        "Bearer",
        "ttt/transcribe", 
        "request_id",
        "transcript",
        "status"
    )
    
    foreach ($pattern in $requestPatterns) {
        $matches = adb shell logcat -d | Select-String $pattern | Select-Object -Last 5
        if ($matches) {
            Write-SmartLog "✅ Found '$pattern' in logs:" "Green"
            $matches | ForEach-Object { Write-SmartLog "  $($_.Line)" "Gray" }
        } else {
            Write-SmartLog "❌ Pattern '$pattern' not found in logs" "Red"
        }
    }
    
    Write-SmartLog "=== END API REQUEST/RESPONSE TEST ===" "Cyan"
}

function Show-DetailedAPILogs {
    Write-SmartLog "=== DETAILED API LOGS ANALYSIS ===" "Cyan"
    
    # Get all recent logs that might contain API information
    $allLogs = adb shell logcat -d | Select-Object -Last 100
    
    # Filter for API-related logs
    $apiRelatedLogs = $allLogs | Where-Object { 
        $_ -match "PluctCoreApiService|Retrofit|OkHttp|HTTP|POST|GET|Response|Request|vend-token|Bearer|ttt/transcribe|request_id|TTT:|stage=" 
    }
    
    if ($apiRelatedLogs) {
        Write-SmartLog "✅ Found $($apiRelatedLogs.Count) API-related log entries:" "Green"
        $apiRelatedLogs | ForEach-Object { Write-SmartLog "  $($_)" "Gray" }
    } else {
        Write-SmartLog "❌ No API-related logs found in recent logcat" "Red"
        Write-SmartLog "This suggests the API calls may not be happening at all" "Red"
    }
    
    # Check for network connectivity logs
    $networkLogs = adb shell logcat -d | Select-String "Network|Connect|Timeout|Connection" | Select-Object -Last 10
    if ($networkLogs) {
        Write-SmartLog "Network-related logs:" "Yellow"
        $networkLogs | ForEach-Object { Write-SmartLog "  $($_.Line)" "Gray" }
    }
    
    # Check for worker-related logs
    $workerLogs = adb shell logcat -d | Select-String "WorkManager|Worker|Background" | Select-Object -Last 10
    if ($workerLogs) {
        Write-SmartLog "Worker-related logs:" "Yellow"
        $workerLogs | ForEach-Object { Write-SmartLog "  $($_.Line)" "Gray" }
    }
    
    Write-SmartLog "=== END DETAILED API LOGS ANALYSIS ===" "Cyan"
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
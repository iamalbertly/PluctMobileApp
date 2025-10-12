# Pluct Simplified Codebase Test
# Tests the refactored codebase for simplicity, naming conventions, and functionality
# Ensures all files follow 300-line rule and consistent naming

param(
    [Parameter()]
    [string]$TestUrl = "https://vm.tiktok.com/ZMAPTWV7o/",
    
    [Parameter()]
    [switch]$SkipBuild,
    
    [Parameter()]
    [switch]$TestAPIOnly
)

# Import core modules
$script:FrameworkRoot = $PSScriptRoot
. "$script:FrameworkRoot\Pluct-Test-Core-Utilities.ps1"

# Initialize simplified test session
$script:SimplifiedTestSession = @{
    StartTime = Get-Date
    TestResults = @{}
    CriticalErrors = @()
    Warnings = @()
    SuccessCount = 0
    FailureCount = 0
    FileSizeCompliance = @{}
    NamingCompliance = @{}
    DuplicationCheck = @{}
}

function Start-SimplifiedCodebaseTest {
    Write-Log "=== Pluct Simplified Codebase Test ===" "Cyan"
    Write-Log "Testing refactored codebase for simplicity and compliance..." "Yellow"
    Write-Log "Ensuring 300-line rule and consistent naming conventions" "Yellow"

    # Test 1: File Size Compliance (300-line rule)
    if (Test-FileSizeCompliance) {
        Write-Log "✅ File size compliance test passed" "Green"
        $script:SimplifiedTestSession.SuccessCount++
    } else {
        Write-Log "❌ File size compliance test failed" "Red"
        $script:SimplifiedTestSession.FailureCount++
    }

    # Test 2: Naming Convention Compliance
    if (Test-NamingConventionCompliance) {
        Write-Log "✅ Naming convention compliance test passed" "Green"
        $script:SimplifiedTestSession.SuccessCount++
    } else {
        Write-Log "❌ Naming convention compliance test failed" "Red"
        $script:SimplifiedTestSession.FailureCount++
    }

    # Test 3: Duplication Elimination
    if (Test-DuplicationElimination) {
        Write-Log "✅ Duplication elimination test passed" "Green"
        $script:SimplifiedTestSession.SuccessCount++
    } else {
        Write-Log "❌ Duplication elimination test failed" "Red"
        $script:SimplifiedTestSession.FailureCount++
    }

    # Test 4: Code Compilation
    if (Test-CodeCompilation) {
        Write-Log "✅ Code compilation test passed" "Green"
        $script:SimplifiedTestSession.SuccessCount++
    } else {
        Write-Log "❌ Code compilation test failed" "Red"
        $script:SimplifiedTestSession.FailureCount++
    }

    # Test 5: Service Integration
    if (Test-ServiceIntegration) {
        Write-Log "✅ Service integration test passed" "Green"
        $script:SimplifiedTestSession.SuccessCount++
    } else {
        Write-Log "❌ Service integration test failed" "Red"
        $script:SimplifiedTestSession.FailureCount++
    }

    # Test 6: API Connectivity (if not API only)
    if (-not $TestAPIOnly) {
        if (Test-APIConnectivity) {
            Write-Log "✅ API connectivity test passed" "Green"
            $script:SimplifiedTestSession.SuccessCount++
        } else {
            Write-Log "❌ API connectivity test failed" "Red"
            $script:SimplifiedTestSession.FailureCount++
        }
    }

    # Generate final report
    Show-SimplifiedTestReport

    if ($script:SimplifiedTestSession.FailureCount -eq 0) {
        Write-Log "✅ All simplified codebase tests passed" "Green"
        return $true
    } else {
        Write-Log "❌ Some simplified codebase tests failed" "Red"
        return $false
    }
}

function Test-FileSizeCompliance {
    Write-Log "Testing file size compliance (300-line rule)..." "Yellow"
    
    try {
        $kotlinFiles = Get-ChildItem -Path "app\src\main\java\app\pluct" -Recurse -Name "*.kt"
        $compliantFiles = 0
        $nonCompliantFiles = 0
        $largeFiles = @()
        
        foreach ($file in $kotlinFiles) {
            $fullPath = "app\src\main\java\app\pluct\$file"
            $lineCount = (Get-Content $fullPath | Measure-Object -Line).Lines
            
            if ($lineCount -le 300) {
                $compliantFiles++
            } else {
                $nonCompliantFiles++
                $largeFiles += "$file ($lineCount lines)"
                Write-Log "File exceeds 300 lines: $file ($lineCount lines)" "Yellow"
            }
        }
        
        $script:SimplifiedTestSession.FileSizeCompliance = @{
            CompliantFiles = $compliantFiles
            NonCompliantFiles = $nonCompliantFiles
            LargeFiles = $largeFiles
        }
        
        Write-Log "File size compliance: $compliantFiles compliant, $nonCompliantFiles non-compliant" "White"
        
        if ($nonCompliantFiles -eq 0) {
            Write-Log "All files comply with 300-line rule" "Green"
            return $true
        } else {
            Write-Log "Some files exceed 300-line rule" "Yellow"
            return $false
        }
        
    } catch {
        Write-Log "File size compliance test failed: $($_.Exception.Message)" "Red"
        return $false
    }
}

function Test-NamingConventionCompliance {
    Write-Log "Testing naming convention compliance..." "Yellow"
    
    try {
        $kotlinFiles = Get-ChildItem -Path "app\src\main\java\app\pluct" -Recurse -Name "*.kt"
        $compliantFiles = 0
        $nonCompliantFiles = 0
        $nonCompliantFileList = @()
        
        foreach ($file in $kotlinFiles) {
            # Check if file follows Pluct-[ParentScope]-[ChildScope]-[CoreResponsibility] pattern
            if ($file -match "^Pluct-" -or $file -match "MainActivity" -or $file -match "PluctApplication") {
                $compliantFiles++
            } else {
                $nonCompliantFiles++
                $nonCompliantFileList += $file
                Write-Log "Non-compliant naming: $file" "Yellow"
            }
        }
        
        $script:SimplifiedTestSession.NamingCompliance = @{
            CompliantFiles = $compliantFiles
            NonCompliantFiles = $nonCompliantFiles
            NonCompliantFileList = $nonCompliantFileList
        }
        
        Write-Log "Naming convention compliance: $compliantFiles compliant, $nonCompliantFiles non-compliant" "White"
        
        if ($nonCompliantFiles -eq 0) {
            Write-Log "All files follow naming convention" "Green"
            return $true
        } else {
            Write-Log "Some files don't follow naming convention" "Yellow"
            return $false
        }
        
    } catch {
        Write-Log "Naming convention compliance test failed: $($_.Exception.Message)" "Red"
        return $false
    }
}

function Test-DuplicationElimination {
    Write-Log "Testing duplication elimination..." "Yellow"
    
    try {
        # Check for potential duplicate class names
        $kotlinFiles = Get-ChildItem -Path "app\src\main\java\app\pluct" -Recurse -Name "*.kt"
        $classNames = @()
        $duplicates = @()
        
        foreach ($file in $kotlinFiles) {
            $content = Get-Content "app\src\main\java\app\pluct\$file" -Raw
            if ($content -match "class\s+(\w+)") {
                $className = $matches[1]
                if ($classNames -contains $className) {
                    $duplicates += $className
                } else {
                    $classNames += $className
                }
            }
        }
        
        $script:SimplifiedTestSession.DuplicationCheck = @{
            TotalClasses = $classNames.Count
            Duplicates = $duplicates
        }
        
        if ($duplicates.Count -gt 0) {
            Write-Log "Potential duplicate class names found:" "Yellow"
            $duplicates | ForEach-Object { Write-Log "  $_" "Yellow" }
            return $false
        } else {
            Write-Log "No duplicate class names found" "Green"
            return $true
        }
        
    } catch {
        Write-Log "Duplication elimination test failed: $($_.Exception.Message)" "Red"
        return $false
    }
}

function Test-CodeCompilation {
    Write-Log "Testing code compilation..." "Yellow"
    
    try {
        Write-Log "Attempting to compile Kotlin code..." "Gray"
        $compileResult = & ".\gradlew.bat" compileDebugKotlin --no-daemon 2>&1
        
        if ($LASTEXITCODE -eq 0) {
            Write-Log "Compilation successful" "Green"
            return $true
        } else {
            Write-Log "Compilation failed" "Red"
            Write-Log "Compilation output: $compileResult" "Red"
            return $false
        }
        
    } catch {
        Write-Log "Code compilation test failed: $($_.Exception.Message)" "Red"
        return $false
    }
}

function Test-ServiceIntegration {
    Write-Log "Testing service integration..." "Yellow"
    
    try {
        # Check if all core services exist
        $coreServices = @(
            "app\src\main\java\app\pluct\api\Pluct-Api-Core-Service.kt",
            "app\src\main\java\app\pluct\transcription\Pluct-Transcription-Core-Manager.kt",
            "app\src\main\java\app\pluct\analytics\Pluct-Analytics-Core-Service.kt",
            "app\src\main\java\app\pluct\collaboration\Pluct-Collaboration-Core-Manager.kt",
            "app\src\main\java\app\pluct\search\Pluct-Search-Core-Engine.kt",
            "app\src\main\java\app\pluct\di\Pluct-DI-Core-Module.kt"
        )
        
        $missingServices = @()
        foreach ($service in $coreServices) {
            if (-not (Test-Path $service)) {
                $missingServices += $service
            }
        }
        
        if ($missingServices.Count -gt 0) {
            Write-Log "Missing core services:" "Red"
            $missingServices | ForEach-Object { Write-Log "  $_" "Red" }
            return $false
        }
        
        Write-Log "All core services present" "Green"
        return $true
        
    } catch {
        Write-Log "Service integration test failed: $($_.Exception.Message)" "Red"
        return $false
    }
}

function Test-APIConnectivity {
    Write-Log "Testing API connectivity..." "Yellow"
    
    try {
        # Test TTTranscribe API connectivity
        $baseUrl = "https://iamromeoly-tttranscibe.hf.space"
        $healthUrl = "$baseUrl/health"
        
        Write-Log "Checking TTTranscribe health endpoint: $healthUrl" "Gray"
        
        $response = Invoke-WebRequest -Uri $healthUrl -Method GET -TimeoutSec 30 -ErrorAction Stop
        
        if ($response.StatusCode -eq 200) {
            Write-Log "TTTranscribe API is accessible" "Green"
            return $true
        } else {
            Write-Log "TTTranscribe API returned status: $($response.StatusCode)" "Red"
            return $false
        }
    } catch {
        Write-Log "API connectivity test failed: $($_.Exception.Message)" "Red"
        return $false
    }
}

function Show-SimplifiedTestReport {
    $duration = (Get-Date) - $script:SimplifiedTestSession.StartTime
    Write-Log "=== SIMPLIFIED CODEBASE TEST REPORT ===" "Cyan"
    Write-Log "Duration: $($duration.TotalSeconds.ToString('F2')) seconds" "White"
    Write-Log "Success Count: $($script:SimplifiedTestSession.SuccessCount)" "Green"
    Write-Log "Failure Count: $($script:SimplifiedTestSession.FailureCount)" $(if ($script:SimplifiedTestSession.FailureCount -eq 0) { "Green" } else { "Red" })
    
    # File size compliance details
    if ($script:SimplifiedTestSession.FileSizeCompliance.NonCompliantFiles -gt 0) {
        Write-Log "File Size Issues:" "Yellow"
        Write-Log "  Non-compliant files: $($script:SimplifiedTestSession.FileSizeCompliance.NonCompliantFiles)" "Yellow"
        $script:SimplifiedTestSession.FileSizeCompliance.LargeFiles | ForEach-Object { Write-Log "    $_" "Yellow" }
    }
    
    # Naming convention issues
    if ($script:SimplifiedTestSession.NamingCompliance.NonCompliantFiles -gt 0) {
        Write-Log "Naming Convention Issues:" "Yellow"
        Write-Log "  Non-compliant files: $($script:SimplifiedTestSession.NamingCompliance.NonCompliantFiles)" "Yellow"
        $script:SimplifiedTestSession.NamingCompliance.NonCompliantFileList | ForEach-Object { Write-Log "    $_" "Yellow" }
    }
    
    # Duplication issues
    if ($script:SimplifiedTestSession.DuplicationCheck.Duplicates.Count -gt 0) {
        Write-Log "Duplication Issues:" "Yellow"
        Write-Log "  Duplicate classes found: $($script:SimplifiedTestSession.DuplicationCheck.Duplicates.Count)" "Yellow"
        $script:SimplifiedTestSession.DuplicationCheck.Duplicates | ForEach-Object { Write-Log "    $_" "Yellow" }
    }
    
    if ($script:SimplifiedTestSession.FailureCount -eq 0) {
        Write-Log "✅ All simplified codebase tests passed" "Green"
    } else {
        Write-Log "❌ Some simplified codebase tests failed" "Red"
    }
}

# Main execution
Start-SimplifiedCodebaseTest

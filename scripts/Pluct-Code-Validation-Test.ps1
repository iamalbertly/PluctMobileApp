# Pluct Code Validation Test
# Validates code compilation and basic functionality without device testing
# Focuses on ensuring all refactored code works correctly

param(
    [Parameter()]
    [string]$TestScope = "All"  # All, Compilation, Services, Dependencies
)

# Import core modules
$script:FrameworkRoot = $PSScriptRoot
. "$script:FrameworkRoot\Pluct-Test-Core-Utilities.ps1"

# Initialize validation session
$script:ValidationSession = @{
    StartTime = Get-Date
    TestResults = @{}
    CriticalErrors = @()
    Warnings = @()
    SuccessCount = 0
    FailureCount = 0
}

function Start-CodeValidationTest {
    Write-Log "=== Pluct Code Validation Test ===" "Cyan"
    Write-Log "Test Scope: $TestScope" "White"
    Write-Log "Validating refactored codebase..." "Yellow"

    # Test compilation
    if (Test-Compilation) {
        Write-Log "✅ Compilation successful" "Green"
        $script:ValidationSession.SuccessCount++
    } else {
        Write-Log "❌ CRITICAL ERROR: Compilation failed" "Red"
        $script:ValidationSession.FailureCount++
        Show-CompilationFailureDetails
        return $false
    }

    # Test service dependencies
    if (Test-ServiceDependencies) {
        Write-Log "✅ Service dependencies valid" "Green"
        $script:ValidationSession.SuccessCount++
    } else {
        Write-Log "❌ CRITICAL ERROR: Service dependencies invalid" "Red"
        $script:ValidationSession.FailureCount++
        Show-DependencyFailureDetails
        return $false
    }

    # Test naming conventions
    if (Test-NamingConventions) {
        Write-Log "✅ Naming conventions valid" "Green"
        $script:ValidationSession.SuccessCount++
    } else {
        Write-Log "⚠️ WARNING: Naming convention issues found" "Yellow"
        $script:ValidationSession.Warnings += "Naming convention issues"
    }

    # Test file size compliance
    if (Test-FileSizeCompliance) {
        Write-Log "✅ File size compliance valid" "Green"
        $script:ValidationSession.SuccessCount++
    } else {
        Write-Log "⚠️ WARNING: File size compliance issues found" "Yellow"
        $script:ValidationSession.Warnings += "File size compliance issues"
    }

    # Test duplicate elimination
    if (Test-DuplicateElimination) {
        Write-Log "✅ Duplicate elimination successful" "Green"
        $script:ValidationSession.SuccessCount++
    } else {
        Write-Log "⚠️ WARNING: Potential duplicates found" "Yellow"
        $script:ValidationSession.Warnings += "Potential duplicates found"
    }

    # Generate final report
    Show-ValidationReport

    if ($script:ValidationSession.FailureCount -eq 0) {
        Write-Log "✅ All code validation tests passed" "Green"
        return $true
    } else {
        Write-Log "❌ Code validation tests failed" "Red"
        return $false
    }
}

function Test-Compilation {
    Write-Log "Testing Kotlin compilation..." "Yellow"
    
    try {
        $result = & ".\gradlew.bat" compileDebugKotlin --no-daemon 2>&1
        $exitCode = $LASTEXITCODE
        
        if ($exitCode -eq 0) {
            Write-Log "Compilation successful" "Green"
            return $true
        } else {
            Write-Log "Compilation failed with exit code: $exitCode" "Red"
            Write-Log "Build output:" "Yellow"
            $result | ForEach-Object { Write-Log "  $_" "Gray" }
            return $false
        }
    } catch {
        Write-Log "Compilation test failed with exception: $($_.Exception.Message)" "Red"
        return $false
    }
}

function Test-ServiceDependencies {
    Write-Log "Testing service dependencies..." "Yellow"
    
    try {
        # Check if all new services exist
        $newServices = @(
            "app\src\main\java\app\pluct\api\Pluct-Api-Core-Service.kt",
            "app\src\main\java\app\pluct\transcription\Pluct-Transcription-Core-Manager.kt",
            "app\src\main\java\app\pluct\analytics\Pluct-Analytics-Core-Service.kt",
            "app\src\main\java\app\pluct\collaboration\Pluct-Collaboration-Core-Manager.kt",
            "app\src\main\java\app\pluct\search\Pluct-Search-Core-Engine.kt",
            "app\src\main\java\app\pluct\di\Pluct-DI-Core-Module.kt"
        )
        
        $missingServices = @()
        foreach ($service in $newServices) {
            if (-not (Test-Path $service)) {
                $missingServices += $service
            }
        }
        
        if ($missingServices.Count -gt 0) {
            Write-Log "Missing services:" "Red"
            $missingServices | ForEach-Object { Write-Log "  $_" "Red" }
            return $false
        }
        
        # Check if old services are deleted
        $oldServices = @(
            "app\src\main\java\app\pluct\data\service\ApiService.kt",
            "app\src\main\java\app\pluct\di\AppModule.kt",
            "app\src\main\java\app\pluct\data\search\AdvancedSearchEngine.kt",
            "app\src\main\java\app\pluct\collaboration\RealTimeCollaborationManager.kt",
            "app\src\main\java\app\pluct\analytics\AnalyticsDashboard.kt"
        )
        
        $remainingOldServices = @()
        foreach ($service in $oldServices) {
            if (Test-Path $service) {
                $remainingOldServices += $service
            }
        }
        
        if ($remainingOldServices.Count -gt 0) {
            Write-Log "Old services still exist (should be deleted):" "Yellow"
            $remainingOldServices | ForEach-Object { Write-Log "  $_" "Yellow" }
        }
        
        Write-Log "Service dependencies valid" "Green"
        return $true
        
    } catch {
        Write-Log "Service dependency test failed with exception: $($_.Exception.Message)" "Red"
        return $false
    }
}

function Test-NamingConventions {
    Write-Log "Testing naming conventions..." "Yellow"
    
    try {
        # Check for files following the naming convention
        $kotlinFiles = Get-ChildItem -Path "app\src\main\java\app\pluct" -Recurse -Name "*.kt"
        $conventionCompliant = 0
        $nonCompliant = 0
        
        foreach ($file in $kotlinFiles) {
            if ($file -match "^Pluct-.*-.*-.*\.kt$") {
                $conventionCompliant++
            } else {
                $nonCompliant++
                Write-Log "Non-compliant naming: $file" "Yellow"
            }
        }
        
        Write-Log "Naming convention compliance: $conventionCompliant compliant, $nonCompliant non-compliant" "White"
        
        if ($nonCompliant -eq 0) {
            Write-Log "All files follow naming convention" "Green"
            return $true
        } else {
            Write-Log "Some files don't follow naming convention" "Yellow"
            return $false
        }
        
    } catch {
        Write-Log "Naming convention test failed with exception: $($_.Exception.Message)" "Red"
        return $false
    }
}

function Test-FileSizeCompliance {
    Write-Log "Testing file size compliance (300-line rule)..." "Yellow"
    
    try {
        $kotlinFiles = Get-ChildItem -Path "app\src\main\java\app\pluct" -Recurse -Name "*.kt"
        $compliantFiles = 0
        $nonCompliantFiles = 0
        
        foreach ($file in $kotlinFiles) {
            $fullPath = "app\src\main\java\app\pluct\$file"
            $lineCount = (Get-Content $fullPath | Measure-Object -Line).Lines
            
            if ($lineCount -le 300) {
                $compliantFiles++
            } else {
                $nonCompliantFiles++
                Write-Log "File exceeds 300 lines: $file ($lineCount lines)" "Yellow"
            }
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
        Write-Log "File size compliance test failed with exception: $($_.Exception.Message)" "Red"
        return $false
    }
}

function Test-DuplicateElimination {
    Write-Log "Testing duplicate elimination..." "Yellow"
    
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
        
        if ($duplicates.Count -gt 0) {
            Write-Log "Potential duplicate class names found:" "Yellow"
            $duplicates | ForEach-Object { Write-Log "  $_" "Yellow" }
            return $false
        } else {
            Write-Log "No duplicate class names found" "Green"
            return $true
        }
        
    } catch {
        Write-Log "Duplicate elimination test failed with exception: $($_.Exception.Message)" "Red"
        return $false
    }
}

function Show-CompilationFailureDetails {
    Write-Log "=== COMPILATION FAILURE DETAILS ===" "Red"
    Write-Log "Compilation failed - possible causes:" "Yellow"
    Write-Log "1. Syntax errors in Kotlin/Java code" "White"
    Write-Log "2. Missing imports or dependencies" "White"
    Write-Log "3. Type mismatches or unresolved references" "White"
    Write-Log "4. Circular dependencies" "White"
    Write-Log "5. Missing service implementations" "White"
    Write-Log "Check compilation output above for specific errors" "Yellow"
}

function Show-DependencyFailureDetails {
    Write-Log "=== DEPENDENCY FAILURE DETAILS ===" "Red"
    Write-Log "Service dependencies invalid - possible causes:" "Yellow"
    Write-Log "1. Missing service files" "White"
    Write-Log "2. Old services not properly deleted" "White"
    Write-Log "3. Incorrect file paths" "White"
    Write-Log "4. Missing dependency injection setup" "White"
    Write-Log "5. Service interface mismatches" "White"
    Write-Log "Check service files and dependency injection" "Yellow"
}

function Show-ValidationReport {
    $duration = (Get-Date) - $script:ValidationSession.StartTime
    Write-Log "=== VALIDATION REPORT ===" "Cyan"
    Write-Log "Duration: $($duration.TotalSeconds.ToString('F2')) seconds" "White"
    Write-Log "Success Count: $($script:ValidationSession.SuccessCount)" "Green"
    Write-Log "Failure Count: $($script:ValidationSession.FailureCount)" $(if ($script:ValidationSession.FailureCount -eq 0) { "Green" } else { "Red" })
    Write-Log "Warning Count: $($script:ValidationSession.Warnings.Count)" $(if ($script:ValidationSession.Warnings.Count -eq 0) { "Green" } else { "Yellow" })
    
    if ($script:ValidationSession.Warnings.Count -gt 0) {
        Write-Log "Warnings:" "Yellow"
        $script:ValidationSession.Warnings | ForEach-Object { Write-Log "  - $_" "Yellow" }
    }
    
    if ($script:ValidationSession.FailureCount -eq 0) {
        Write-Log "✅ All validation tests passed" "Green"
    } else {
        Write-Log "❌ Some validation tests failed" "Red"
    }
}

# Main execution
Start-CodeValidationTest

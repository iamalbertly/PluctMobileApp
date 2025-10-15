# Pluct Test Error Handler - Unified error handling system
# Follows naming convention: [Project]-[ParentScope]-[ChildScope]-[CoreResponsibility]
# Single source of truth for all error handling and termination logic

param(
    [string]$ErrorType = "",
    [string]$ErrorMessage = "",
    [string]$Stage = "Unknown",
    [string]$Component = "",
    [string]$SuggestedFix = "",
    [string]$UIState = "",
    [switch]$Terminate = $true
)

# Global error tracking
$script:ErrorHandler = @{
    Errors = @()
    CriticalErrors = @()
    TerminationCount = 0
    LastError = $null
}

function Initialize-ErrorHandler {
    $script:ErrorHandler = @{
        Errors = @()
        CriticalErrors = @()
        TerminationCount = 0
        LastError = $null
    }
}

function Write-ErrorLog {
    param(
        [string]$Message,
        [string]$Level = "ERROR",
        [string]$Color = "Red",
        [string]$Component = "ErrorHandler"
    )
    
    $timestamp = Get-Date -Format "HH:mm:ss.fff"
    $logMessage = "[$timestamp] [$Level] ($Component) $Message"
    
    switch ($Color) {
        "Red" { Write-Host $logMessage -ForegroundColor Red }
        "Green" { Write-Host $logMessage -ForegroundColor Green }
        "Yellow" { Write-Host $logMessage -ForegroundColor Yellow }
        "Cyan" { Write-Host $logMessage -ForegroundColor Cyan }
        "Magenta" { Write-Host $logMessage -ForegroundColor Magenta }
        default { Write-Host $logMessage }
    }
}

function Report-CriticalError {
    param(
        [string]$ErrorType,
        [string]$ErrorMessage,
        [string]$Stage = "Unknown",
        [string]$Component = "",
        [string]$SuggestedFix = "",
        [string]$UIState = "",
        [switch]$Terminate = $true
    )
    
    $errorDetails = @{
        Type = $ErrorType
        Message = $ErrorMessage
        Stage = $Stage
        Component = $Component
        SuggestedFix = $SuggestedFix
        UIState = $UIState
        Timestamp = Get-Date
        Terminated = $Terminate
    }
    
    $script:ErrorHandler.Errors += $errorDetails
    $script:ErrorHandler.CriticalErrors += $errorDetails
    $script:ErrorHandler.LastError = $errorDetails
    
    Write-ErrorLog "❌ CRITICAL ERROR: $ErrorType" "ERROR" "Red" $Component
    Write-ErrorLog "Stage: $Stage" "ERROR" "Red" $Component
    Write-ErrorLog "Component: $Component" "ERROR" "Red" $Component
    Write-ErrorLog "Error Details: $ErrorMessage" "ERROR" "Red" $Component
    
    if ($SuggestedFix) {
        Write-ErrorLog "Suggested Fix: $SuggestedFix" "WARN" "Yellow" $Component
    }
    
    if ($UIState) {
        Write-ErrorLog "UI State at failure: $UIState" "DEBUG" "Gray" $Component
    }
    
    if ($Terminate) {
        $script:ErrorHandler.TerminationCount++
        Write-ErrorLog "Test execution terminated due to critical error." "ERROR" "Red" $Component
        Write-ErrorLog "Total terminations: $($script:ErrorHandler.TerminationCount)" "ERROR" "Red" $Component
        
        # Show error summary
        Show-ErrorSummary
        
        # Terminate immediately
        exit 1
    }
}

function Report-NonCriticalError {
    param(
        [string]$ErrorType,
        [string]$ErrorMessage,
        [string]$Stage = "Unknown",
        [string]$Component = "",
        [string]$SuggestedFix = ""
    )
    
    $errorDetails = @{
        Type = $ErrorType
        Message = $ErrorMessage
        Stage = $Stage
        Component = $Component
        SuggestedFix = $SuggestedFix
        Timestamp = Get-Date
        Terminated = $false
    }
    
    $script:ErrorHandler.Errors += $errorDetails
    
    Write-ErrorLog "⚠️ NON-CRITICAL ERROR: $ErrorType" "WARN" "Yellow" $Component
    Write-ErrorLog "Stage: $Stage" "WARN" "Yellow" $Component
    Write-ErrorLog "Component: $Component" "WARN" "Yellow" $Component
    Write-ErrorLog "Error Details: $ErrorMessage" "WARN" "Yellow" $Component
    
    if ($SuggestedFix) {
        Write-ErrorLog "Suggested Fix: $SuggestedFix" "INFO" "Cyan" $Component
    }
}

function Test-ErrorCondition {
    param(
        [bool]$Condition,
        [string]$ErrorType,
        [string]$ErrorMessage,
        [string]$Stage = "Unknown",
        [string]$Component = "",
        [string]$SuggestedFix = "",
        [switch]$Critical = $true
    )
    
    if (-not $Condition) {
        if ($Critical) {
            Report-CriticalError -ErrorType $ErrorType -ErrorMessage $ErrorMessage -Stage $Stage -Component $Component -SuggestedFix $SuggestedFix
        } else {
            Report-NonCriticalError -ErrorType $ErrorType -ErrorMessage $ErrorMessage -Stage $Stage -Component $Component -SuggestedFix $SuggestedFix
        }
        return $false
    }
    return $true
}

function Test-CommandSuccess {
    param(
        [string]$Command,
        [string]$ErrorType,
        [string]$ErrorMessage,
        [string]$Stage = "Unknown",
        [string]$Component = "",
        [string]$SuggestedFix = "",
        [switch]$Critical = $true
    )
    
    try {
        $result = Invoke-Expression $Command 2>&1
        $success = ($LASTEXITCODE -eq 0)
        
        if (-not $success) {
            $fullErrorMessage = "$ErrorMessage. Command: $Command. Result: $result"
            if ($Critical) {
                Report-CriticalError -ErrorType $ErrorType -ErrorMessage $fullErrorMessage -Stage $Stage -Component $Component -SuggestedFix $SuggestedFix
            } else {
                Report-NonCriticalError -ErrorType $ErrorType -ErrorMessage $fullErrorMessage -Stage $Stage -Component $Component -SuggestedFix $SuggestedFix
            }
            return $false
        }
        return $true
    } catch {
        $fullErrorMessage = "$ErrorMessage. Command: $Command. Exception: $($_.Exception.Message)"
        if ($Critical) {
            Report-CriticalError -ErrorType $ErrorType -ErrorMessage $fullErrorMessage -Stage $Stage -Component $Component -SuggestedFix $SuggestedFix
        } else {
            Report-NonCriticalError -ErrorType $ErrorType -ErrorMessage $fullErrorMessage -Stage $Stage -Component $Component -SuggestedFix $SuggestedFix
        }
        return $false
    }
}

function Test-FileExists {
    param(
        [string]$FilePath,
        [string]$ErrorType,
        [string]$ErrorMessage,
        [string]$Stage = "Unknown",
        [string]$Component = "",
        [string]$SuggestedFix = "",
        [switch]$Critical = $true
    )
    
    $exists = Test-Path $FilePath
    if (-not $exists) {
        $fullErrorMessage = "$ErrorMessage. File: $FilePath"
        if ($Critical) {
            Report-CriticalError -ErrorType $ErrorType -ErrorMessage $fullErrorMessage -Stage $Stage -Component $Component -SuggestedFix $SuggestedFix
        } else {
            Report-NonCriticalError -ErrorType $ErrorType -ErrorMessage $fullErrorMessage -Stage $Stage -Component $Component -SuggestedFix $SuggestedFix
        }
        return $false
    }
    return $true
}

function Test-PropertyExists {
    param(
        [object]$Object,
        [string]$PropertyName,
        [string]$ErrorType,
        [string]$ErrorMessage,
        [string]$Stage = "Unknown",
        [string]$Component = "",
        [string]$SuggestedFix = "",
        [switch]$Critical = $true
    )
    
    $exists = $Object.PSObject.Properties.Name -contains $PropertyName
    if (-not $exists) {
        $fullErrorMessage = "$ErrorMessage. Property: $PropertyName. Object: $($Object.GetType().Name)"
        if ($Critical) {
            Report-CriticalError -ErrorType $ErrorType -ErrorMessage $fullErrorMessage -Stage $Stage -Component $Component -SuggestedFix $SuggestedFix
        } else {
            Report-NonCriticalError -ErrorType $ErrorType -ErrorMessage $fullErrorMessage -Stage $Stage -Component $Component -SuggestedFix $SuggestedFix
        }
        return $false
    }
    return $true
}

function Initialize-ObjectProperty {
    param(
        [object]$Object,
        [string]$PropertyName,
        [object]$DefaultValue = $null
    )
    
    if (-not ($Object.PSObject.Properties.Name -contains $PropertyName)) {
        $Object | Add-Member -MemberType NoteProperty -Name $PropertyName -Value $DefaultValue -Force
    }
}

function Show-ErrorSummary {
    Write-ErrorLog "=== ERROR SUMMARY ===" "INFO" "Magenta" "ErrorHandler"
    Write-ErrorLog "Total Errors: $($script:ErrorHandler.Errors.Count)" "INFO" "White" "ErrorHandler"
    Write-ErrorLog "Critical Errors: $($script:ErrorHandler.CriticalErrors.Count)" "INFO" "White" "ErrorHandler"
    Write-ErrorLog "Terminations: $($script:ErrorHandler.TerminationCount)" "INFO" "White" "ErrorHandler"
    
    if ($script:ErrorHandler.CriticalErrors.Count -gt 0) {
        Write-ErrorLog "Critical Errors:" "ERROR" "Red" "ErrorHandler"
        foreach ($error in $script:ErrorHandler.CriticalErrors) {
            Write-ErrorLog "  - $($error.Type): $($error.Message)" "ERROR" "Red" "ErrorHandler"
            if ($error.SuggestedFix) {
                Write-ErrorLog "    Suggested Fix: $($error.SuggestedFix)" "WARN" "Yellow" "ErrorHandler"
            }
        }
    }
    
    Write-ErrorLog "=== END ERROR SUMMARY ===" "INFO" "Magenta" "ErrorHandler"
}

function Get-ErrorCount {
    return $script:ErrorHandler.Errors.Count
}

function Get-CriticalErrorCount {
    return $script:ErrorHandler.CriticalErrors.Count
}

function Get-LastError {
    return $script:ErrorHandler.LastError
}

# Initialize error handler
Initialize-ErrorHandler

# Trust Fixes Integration & Completion Plan

## Executive Summary

This plan completes the integration of all trust fixes edge case implementations into the existing codebase, ensures proper dependency injection, updates automated tests, and validates all negative user experiences are properly handled.

## Phase 1: Integration of Edge Case Implementations

### 1.1 Intent Queue Manager Integration

**File**: `app/src/main/java/app/pluct/ui/screens/Pluct-UI-Screen-01MainActivity-01IntentHandler.kt`

**Integration Points**:
- Add queue manager check before setting prefilled URL
- Queue intent if processing is active
- Show notification for queued videos

**Changes Required**:
1. Import queue manager and video repository
2. Add queue check in `handleTikTokIntent` before setting prefilled URL
3. Integrate with MainActivity to pass videoRepository and queueManager

**Dependencies**: 
- `PluctUIScreen01MainActivityIntentHandlerQueueManager`
- `PluctVideoRepository` (injected in MainActivity)
- `PluctQueueManager` (injected in MainActivity)

### 1.2 Atomic Credit Reservation Integration

**File**: `app/src/main/java/app/pluct/ui/components/Pluct-Mobile-UI-Component-CaptureCard-00Main.kt`

**Integration Points**:
- Reserve credit atomically before API call
- Commit reservation on success, release on failure
- Re-check credits immediately before submission

**Changes Required**:
1. Create singleton instance of `PluctCoreCredit01AtomicReservation01Service`
2. Add reservation before `handleCompleteAPIFlow` call
3. Commit reservation after successful token vend
4. Release reservation on error

**Dependencies**:
- `PluctCoreCredit01AtomicReservation01Service` (singleton)

### 1.3 Network Monitor Integration

**File**: `app/src/main/java/app/pluct/services/Pluct-Core-Background-01TranscriptionWorker.kt`

**Integration Points**:
- Start network monitoring when worker begins
- Handle network loss during processing
- Queue video for retry when network restored

**Changes Required**:
1. Create network monitor instance in `doWork()`
2. Start monitoring at beginning of processing
3. Handle network loss callbacks
4. Stop monitoring when work completes

**Dependencies**:
- `PluctCoreBackground01TranscriptionWorkerNetworkMonitor`
- `PluctVideoRepository` (via Hilt)
- `PluctQueueManager` (via Hilt)

### 1.4 Job Deduplication Integration

**File**: `app/src/main/java/app/pluct/ui/components/Pluct-UI-Component-03CaptureCard-02APIFlow.kt`

**Integration Points**:
- Check for existing job before creating WorkManager job
- Use existing job ID if found
- Merge notifications for duplicate jobs

**Changes Required**:
1. Check for existing job before enqueueing WorkManager job
2. Use existing job ID if found
3. Merge notifications if duplicate detected

**Dependencies**:
- `PluctCoreBackground01TranscriptionWorkerJobDeduplication`

### 1.5 Token Refresh Manager Integration

**File**: `app/src/main/java/app/pluct/services/Pluct-Core-API-01UnifiedService-01Main.kt`

**Integration Points**:
- Check token expiration before API calls
- Proactively refresh token if needed
- Handle 401 errors by refreshing and retrying

**Changes Required**:
1. Create token refresh manager instance
2. Check token expiration before vendToken and status checks
3. Refresh token proactively during polling
4. Handle 401 errors with token refresh

**Dependencies**:
- `PluctCoreAPI01UnifiedService02TokenRefresh01Manager`
- `PluctCoreAPIJWTGenerator` (existing)
- `PluctCoreUserIdentification` (existing)

### 1.6 Request Deduplication Integration

**File**: `app/src/main/java/app/pluct/services/Pluct-Core-API-01UnifiedService-01Main.kt`

**Integration Points**:
- Generate or get existing request ID for URL
- Check if request is in progress
- Cache responses for idempotency

**Changes Required**:
1. Create request deduplication handler instance
2. Generate request ID before vendToken
3. Check if request in progress before making API call
4. Cache token response for idempotency

**Dependencies**:
- `PluctCoreAPI01UnifiedService03RequestDeduplication01Handler`

## Phase 2: Dependency Injection Setup

### 2.1 Hilt Module for New Services

**New File**: `app/src/main/java/app/pluct/di/Pluct-Core-TrustFixes-01Module.kt`

**Purpose**: Provide Hilt bindings for new trust fixes services

**Bindings Required**:
- `PluctCoreCredit01AtomicReservation01Service` (Singleton)
- `PluctCoreAPI01UnifiedService02TokenRefresh01Manager` (Singleton)
- `PluctCoreAPI01UnifiedService03RequestDeduplication01Handler` (Singleton)

### 2.2 Update Existing Modules

**File**: `app/src/main/java/app/pluct/di/Pluct-Core-API-01Module.kt` (if exists)

**Changes**: Ensure token refresh manager and request deduplication handler are provided

## Phase 3: Fix Integration Issues

### 3.1 Fix Queue Manager Flow Access

**Issue**: Queue manager uses Flow.value which is not available in synchronous context

**File**: `app/src/main/java/app/pluct/ui/screens/Pluct-UI-Screen-01MainActivity-01IntentHandler-02QueueManager.kt`

**Fix**: Make `isProcessingActive` suspend function and use `first()` or `stateIn`

### 3.2 Fix Job Deduplication WorkManager API

**Issue**: WorkManager API usage may be incorrect

**File**: `app/src/main/java/app/pluct/services/Pluct-Core-Background-01TranscriptionWorker-03JobDeduplication.kt`

**Fix**: Use correct WorkManager API for checking existing jobs

### 3.3 Fix Network Monitor Flow Access

**Issue**: Network monitor uses Flow.value in synchronous context

**File**: `app/src/main/java/app/pluct/services/Pluct-Core-Background-01TranscriptionWorker-02NetworkMonitor.kt`

**Fix**: Make network state checking suspend or use StateFlow

## Phase 4: Update Existing Tests

### 4.1 Update Auto-Submit Intent Test

**File**: `scripts/nodejs/journeys/Journey-UX-11AutoSubmitIntent-Validation.js`

**Updates**:
- Add validation for queue manager when processing active
- Verify intent is queued, not auto-submitted when processing

### 4.2 Update Background Processing Test

**File**: `scripts/nodejs/journeys/Journey-UX-12BackgroundProcessing-Validation.js`

**Updates**:
- Add network monitor validation
- Verify network loss handling
- Verify retry on network restore

### 4.3 Update Credit Queue Flow Test

**File**: `scripts/nodejs/journeys/Journey-UX-14CreditQueueFlow-Validation.js`

**Updates**:
- Add atomic credit reservation validation
- Verify credit race condition prevention

## Phase 5: Create Integration Validation Tests

### 5.1 Intent Queue Integration Test

**New File**: `scripts/nodejs/journeys/Journey-Integration-01IntentQueue-Validation.js`

**Purpose**: Validate intent queue manager is properly integrated

**Test Steps**:
1. Start transcription manually
2. Send intent with TikTok URL
3. Verify intent is queued (not auto-submitted)
4. Verify notification shows queue status
5. Complete first transcription
6. Verify queued intent processes automatically

### 5.2 Atomic Credit Integration Test

**New File**: `scripts/nodejs/journeys/Journey-Integration-02AtomicCredit-Validation.js`

**Purpose**: Validate atomic credit reservation is properly integrated

**Test Steps**:
1. Set credits to 1
2. Rapidly trigger two transcriptions
3. Verify only one succeeds (other queued)
4. Verify no duplicate credit deduction

### 5.3 Network Monitor Integration Test

**New File**: `scripts/nodejs/journeys/Journey-Integration-03NetworkMonitor-Validation.js`

**Purpose**: Validate network monitor is properly integrated in background worker

**Test Steps**:
1. Start background transcription
2. Disable network mid-process
3. Verify error notification appears
4. Verify video queued for retry
5. Re-enable network
6. Verify auto-retry occurs

### 5.4 Job Deduplication Integration Test

**New File**: `scripts/nodejs/journeys/Journey-Integration-04JobDeduplication-Validation.js`

**Purpose**: Validate job deduplication is properly integrated

**Test Steps**:
1. Start transcription via intent
2. Manually submit same URL
3. Verify only one WorkManager job
4. Verify only one notification

### 5.5 Token Refresh Integration Test

**New File**: `scripts/nodejs/journeys/Journey-Integration-05TokenRefresh-Validation.js`

**Purpose**: Validate token refresh manager is properly integrated

**Test Steps**:
1. Start transcription
2. Simulate token expiration
3. Verify token refresh occurs
4. Verify transcription continues

### 5.6 Request Deduplication Integration Test

**New File**: `scripts/nodejs/journeys/Journey-Integration-06RequestDeduplication-Validation.js`

**Purpose**: Validate request deduplication is properly integrated

**Test Steps**:
1. Rapidly tap extract button 5 times
2. Verify only one token vend request
3. Verify same request ID used for retries

## Phase 6: Update Test Configuration

### 6.1 Add Integration Tests to Config

**File**: `scripts/nodejs/config/Pluct-Test-Config-05TrustFixes-Validation.json`

**Add**:
- Integration test features
- Test groups for integration tests

### 6.2 Update Journey Orchestrator

**File**: `scripts/nodejs/journeys/Pluct-Journey-01Orchestrator.js`

**Add**:
- Integration test journeys to execution order
- Name mappings for integration tests

## Phase 7: Focused Test Execution Enhancement

### 7.1 Update Focused Test Runner

**File**: `scripts/nodejs/Pluct-Test-Focused-02TrustFixes-01Runner.js`

**Enhancements**:
- Add integration tests to execution
- Improve auto-fix detection for integration issues
- Better error reporting for integration failures

### 7.2 Enhance Auto-Fix Service

**File**: `scripts/nodejs/core/Pluct-Test-AutoFix-02TrustFixes-01Service.js`

**Enhancements**:
- Detect missing Hilt bindings
- Detect incorrect Flow usage
- Auto-fix common integration issues

## Phase 8: File Naming Verification

### 8.1 Verify All New Files

**Check**:
- All files follow 5+ scope layer convention
- Sequence prefixes are correct (01, 02, 03)
- No duplicate scoped files

### 8.2 Fix Any Naming Issues

**Action**: Rename any files that don't comply

## Phase 9: Comprehensive Testing

### 9.1 Run Focused Test Suite

**Command**: `npm run test:trust-fixes:dev`

**Expected**:
- All 7 trust fixes tests pass
- All 8 edge case tests pass
- All 6 integration tests pass
- Total: 21 tests

### 9.2 Validate Auto-Fix

**Test**:
- Introduce intentional issues
- Verify auto-fix detects and fixes
- Verify rebuild and retry works

## Phase 10: Documentation Updates

### 10.1 Update README

**File**: `scripts/nodejs/README-Smart-Testing.md`

**Add**:
- Integration testing section
- Auto-fix capabilities for integration issues

### 10.2 Update Context

**File**: `context.md`

**Add**:
- Integration status
- Dependency injection setup
- Hilt module information

## Implementation Priority

1. **Phase 1** - Integration (Critical)
2. **Phase 2** - Dependency Injection (Critical)
3. **Phase 3** - Fix Integration Issues (High)
4. **Phase 4** - Update Existing Tests (High)
5. **Phase 5** - Create Integration Tests (High)
6. **Phase 6** - Update Test Configuration (Medium)
7. **Phase 7** - Enhance Test Infrastructure (Medium)
8. **Phase 8** - File Naming Verification (Low)
9. **Phase 9** - Comprehensive Testing (High)
10. **Phase 10** - Documentation (Low)

## Success Criteria

- ✅ All edge case implementations integrated
- ✅ Dependency injection properly configured
- ✅ All integration issues fixed
- ✅ 21 comprehensive tests (7 trust + 8 edge + 6 integration)
- ✅ Auto-fix handles integration issues
- ✅ Focused test execution terminates on first error
- ✅ All files follow naming convention
- ✅ Documentation updated


# Next Steps: Addressing Implementation Gaps

## Overview
This document identifies 6 critical gaps discovered during the UX improvements and technical debt cleanup phase. Each gap represents a real-world issue that could impact user experience, reliability, or maintainability.

---

## Gap 1: Error Recovery Guidance Not Fully Integrated in UI

**Current State:**
- `PluctCoreError07RecoveryGuidance` helper created but not fully integrated into error display components
- Recovery steps are available but not shown to users in error dialogs

**Impact:**
- Users see error messages but don't always know the specific steps to resolve issues
- Reduces self-service capability and increases support burden

**Solution:**
1. Update `Pluct-UI-Component-03CaptureCard-03ErrorDisplay.kt` to show recovery steps in expandable section
2. Add recovery guidance to `Pluct-UI-Error-01UnifiedHandler.kt` dialogs
3. Create visual indicators (icons, numbered steps) for recovery actions

**Files to Modify:**
- `app/src/main/java/app/pluct/ui/components/Pluct-UI-Component-03CaptureCard-03ErrorDisplay.kt`
- `app/src/main/java/app/pluct/ui/error/Pluct-UI-Error-01UnifiedHandler.kt`

**Maestro Test:**
- Update `Pluct-Maestro-Flow-26UX-12ErrorRecovery-01Validation.yaml` to validate recovery steps are visible

---

## Gap 2: Standardized Logging Not Adopted Across Codebase

**Current State:**
- `PluctCoreLogging02Standard` created but existing code still uses `Log.d()`, `Log.e()` directly
- Inconsistent logging levels make debugging difficult
- No centralized log filtering or structured logging

**Impact:**
- Harder to debug issues in production
- Inconsistent log levels make it difficult to filter important messages
- Missing structured data for log aggregation tools

**Solution:**
1. Create migration script to replace direct `Log.*` calls with `PluctCoreLogging02Standard.*`
2. Add structured logging with context (operation, user ID, request ID)
3. Implement log level configuration (dev vs production)
4. Add log sampling for high-volume operations

**Files to Modify:**
- All files using `Log.d()`, `Log.e()`, `Log.w()`, `Log.i()` directly
- Priority: Service layer, error handlers, API clients

**Migration Strategy:**
- Start with high-impact files (error handlers, API clients)
- Use find/replace with careful review
- Add structured context gradually

---

## Gap 3: Duplicate Progress Monitoring Logic Still Exists

**Current State:**
- `PluctUIScreen01MainActivity03ProgressMonitor` and `PluctUIScreen01MainActivity04EffectsHandler02ProgressPoller` both monitor progress
- Some overlap in polling logic and status updates
- Different polling intervals and retry strategies

**Impact:**
- Potential for race conditions
- Inconsistent user experience (different update frequencies)
- Code duplication increases maintenance burden

**Solution:**
1. Create unified `Pluct-Core-Progress-01Monitor.kt` that both components use
2. Consolidate polling strategies (adaptive intervals, retry logic)
3. Use single source of truth for progress state
4. Ensure only one active poller per video

**Files to Modify:**
- `app/src/main/java/app/pluct/ui/screens/Pluct-UI-Screen-01MainActivity-03ProgressMonitor.kt`
- `app/src/main/java/app/pluct/ui/screens/Pluct-UI-Screen-01MainActivity-04EffectsHandler-02ProgressPoller.kt`
- `app/src/main/java/app/pluct/ui/screens/Pluct-UI-Screen-01MainActivity-04EffectsHandler.kt`

**Edge Cases:**
- App backgrounded during polling
- Multiple videos processing simultaneously
- Network interruptions during polling

---

## Gap 4: Rate Limit Handling Lacks Proactive Queueing

**Current State:**
- Rate limit errors show dialog but don't automatically queue videos
- Users must manually queue after hitting rate limit
- No automatic retry when rate limit resets

**Impact:**
- Poor user experience - users must remember to retry
- Lost transcriptions if users don't manually queue
- Inconsistent with other error handling (network errors auto-queue)

**Solution:**
1. Auto-queue videos when rate limit (429) error occurs
2. Add rate limit reset detection in queue processor
3. Automatically process queued videos when rate limit window resets
4. Show clear messaging: "Video queued. Will process when rate limit resets (in X minutes)"

**Files to Modify:**
- `app/src/main/java/app/pluct/services/Pluct-Core-Queue-01Manager.kt`
- `app/src/main/java/app/pluct/ui/screens/Pluct-UI-Screen-01MainActivity-02TranscriptionOrchestrator.kt`
- `app/src/main/java/app/pluct/core/error/Pluct-Core-Error-03UserMessageFormatter.kt`

**Maestro Test:**
- Create `Pluct-Maestro-Flow-29UX-15RateLimitQueue-01Validation.yaml` to test auto-queueing

---

## Gap 5: Credit Balance Updates Not Always Immediate

**Current State:**
- Balance updates happen asynchronously after operations
- UI may show stale balance during critical operations
- No optimistic updates for balance changes

**Impact:**
- Users may see incorrect balance and make decisions based on stale data
- Confusion when balance doesn't update immediately after transcription
- Potential for attempting operations with insufficient credits

**Solution:**
1. Implement optimistic balance updates (decrement immediately on operation start)
2. Add balance update callbacks to all credit-consuming operations
3. Ensure balance refresh happens synchronously after vend-token
4. Add visual indicator when balance is updating

**Files to Modify:**
- `app/src/main/java/app/pluct/ui/screens/Pluct-UI-Screen-01MainActivity-05CreditManager.kt`
- `app/src/main/java/app/pluct/ui/screens/Pluct-UI-Screen-01MainActivity-02TranscriptionOrchestrator.kt`
- `app/src/main/java/app/pluct/ui/components/Pluct-UI-03Header.kt`

**Edge Cases:**
- Operation fails after optimistic update (rollback needed)
- Multiple simultaneous operations (race conditions)
- Network failure during balance check

---

## Gap 6: Maestro Test Coverage Incomplete for New Features

**Current State:**
- Some UX improvements have Maestro tests, but not all edge cases covered
- Tests use `optional: true` too liberally, reducing validation effectiveness
- Missing tests for error recovery flows, rate limit queueing, balance update timing

**Impact:**
- New features may regress without detection
- Incomplete test coverage reduces confidence in deployments
- Manual testing still required for critical paths

**Solution:**
1. Add comprehensive Maestro tests for all 5 UX improvements
2. Remove unnecessary `optional: true` flags to enforce strict validation
3. Add tests for error recovery guidance display
4. Add tests for rate limit auto-queueing
5. Add tests for balance update timing and optimistic updates
6. Create test data fixtures for consistent test scenarios

**Files to Create/Modify:**
- `maestro/flows/06-ux-improvements/Pluct-Maestro-Flow-29UX-15RateLimitQueue-01Validation.yaml`
- `maestro/flows/06-ux-improvements/Pluct-Maestro-Flow-30UX-16ErrorRecoverySteps-01Validation.yaml`
- `maestro/flows/06-ux-improvements/Pluct-Maestro-Flow-31UX-17BalanceOptimistic-01Validation.yaml`
- Update existing test flows to remove unnecessary `optional: true`

**Test Strategy:**
- Use real device scenarios (not just happy paths)
- Test error states explicitly
- Validate UI feedback timing
- Test edge cases (rapid taps, network loss, etc.)

---

## Implementation Priority

1. **High Priority (User-Facing):**
   - Gap 4: Rate Limit Auto-Queueing (prevents lost transcriptions)
   - Gap 5: Balance Update Visibility (prevents user confusion)

2. **Medium Priority (Developer Experience):**
   - Gap 1: Error Recovery Guidance Integration (improves self-service)
   - Gap 6: Maestro Test Coverage (prevents regressions)

3. **Lower Priority (Technical Debt):**
   - Gap 2: Logging Standardization (improves debugging)
   - Gap 3: Progress Monitoring Consolidation (reduces duplication)

---

## Success Metrics

- **Error Recovery:** 80%+ of errors show actionable recovery steps
- **Rate Limit:** 100% of rate-limited requests auto-queue
- **Balance Updates:** <500ms delay between operation and balance update
- **Test Coverage:** 90%+ of UX improvements covered by Maestro tests
- **Logging:** 100% of new code uses standardized logging
- **Progress Monitoring:** Single source of truth, no duplicate polling

---

## Notes

- All improvements should maintain backward compatibility
- Follow existing naming conventions strictly
- Update `context.md` when making architectural changes
- Run Maestro tests in dev mode (terminate on first error) during development
- All changes should align with Customer, Simplicity, and Trust values

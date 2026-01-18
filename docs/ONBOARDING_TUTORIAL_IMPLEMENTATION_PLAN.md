# Pluct Onboarding Tutorial Flow - Implementation Plan

## Core Values Alignment
| Value | Current Gap | Solution |
|-------|-------------|----------|
| **Customer** | User abandoned after permissions | Guided tutorial with visual instructions |
| **Simplicity** | No guidance on core flow | 3-step visual walkthrough |
| **Trust** | No value demonstration | Immediate practice + earned credits |

---

## PART 1: PROBLEM ANALYSIS & SOLUTION DESIGN

### 1.1 Current State Analysis

**Flow Today:**
1. App launches → Welcome dialog (3 free credits)
2. Permission requests (notifications, overlay)
3. User lands on empty home screen
4. Zero guidance on what to do next

**Problem:** 90%+ users never discover the TikTok share flow.

**Root Cause:** No value demonstration. User has credits but doesn't know how to use them.

### 1.2 Solution Architecture

**New Onboarding Flow:**
```
Welcome Dialog
    ↓
Permission Requests (existing)
    ↓
NEW: Tutorial Step 1 - "How to Get Transcripts"
    ↓
NEW: Tutorial Step 2 - Visual Share Instructions
    ↓
NEW: Tutorial Step 3 - Open TikTok CTA
    ↓
(User practices in TikTok)
    ↓
App receives intent → First transcript success
    ↓
NEW: Milestone reward unlocked (verified server-side)
```

### 1.3 Onboarding State Machine

**States (stored in SharedPreferences):**
- `ONBOARDING_NOT_STARTED` - Fresh install
- `WELCOME_COMPLETED` - Welcome dialog dismissed
- `PERMISSIONS_COMPLETED` - Permission flow finished
- `TUTORIAL_STEP_1_SEEN` - "How it works" shown
- `TUTORIAL_STEP_2_SEEN` - Visual instructions shown
- `TUTORIAL_STEP_3_SEEN` - TikTok CTA shown
- `FIRST_TRANSCRIPT_COMPLETED` - User completed first real flow
- `ONBOARDING_COMPLETED` - All steps done, milestone claimed

**New Preference Keys:**
```kotlin
KEY_ONBOARDING_STATE = "onboarding_state"
KEY_FIRST_TRANSCRIPT_COMPLETED = "first_transcript_completed"
KEY_ONBOARDING_MILESTONE_CLAIMED = "onboarding_milestone_claimed"
KEY_ONBOARDING_MILESTONE_REQUEST_ID = "onboarding_milestone_request_id"
```

### 1.4 Anti-Gaming Protection

**Server-Side Validation Requirements:**
1. User must have completed at least one transcription before claiming milestone
2. Milestone claim requires signed payload with:
   - `userId` (hardware-bound, cannot be spoofed)
   - `firstTranscriptJobId` (proves real transcription occurred)
   - `deviceFingerprint` (prevents multi-device gaming)
   - `timestamp` (prevents replay attacks)
3. Server tracks milestone claims per userId - one-time only
4. No client-side credit manipulation - all rewards via API

**Business Engine Integration:**
- New endpoint conceptually: `POST /v1/milestones/claim`
- However, since we cannot modify Business Engine, we'll use existing token vending with a milestone flag
- Token vending response already includes `bonus` field - server can detect first-complete-user and add bonus

**Practical Approach (Zero Backend Changes):**
- Track `first_transcript_completed` locally
- When first transcript completes, set flag
- Next balance refresh will pick up any server-side welcome bonus already configured
- No new API endpoints required - leverages existing bonus mechanism

---

## PART 2: IMPLEMENTATION SPECIFICATIONS

### 2.1 New UI Components

**File: `Pluct-UI-Component-07Onboarding-01Tutorial-01Flow.kt`**

```
Purpose: Multi-step onboarding tutorial component
Location: app/src/main/java/app/pluct/ui/components/

Steps:
1. HowItWorksStep - Explains the 3-tap flow
2. VisualInstructionsStep - Shows TikTok share button images
3. OpenTikTokStep - CTA to launch TikTok and practice
```

**Visual Design (Text-Based, No Images Required):**

Step 1 - "How It Works":
```
┌─────────────────────────────────────┐
│  📱 Get Transcripts in 3 Taps       │
│                                     │
│  1. Find a TikTok video             │
│  2. Tap Share → Pluct               │
│  3. Get your transcript instantly   │
│                                     │
│  [Next →]                           │
└─────────────────────────────────────┘
```

Step 2 - "Find the Share Button":
```
┌─────────────────────────────────────┐
│  👆 Look for the Share Arrow        │
│                                     │
│  In TikTok, tap the curved          │
│  arrow on the right side            │
│  of any video                       │
│                                     │
│  Then scroll and tap "Pluct"        │
│                                     │
│  [Got It →]                         │
└─────────────────────────────────────┘
```

Step 3 - "Try It Now":
```
┌─────────────────────────────────────┐
│  🎯 Ready to Try?                   │
│                                     │
│  Open TikTok and share any          │
│  video with Pluct                   │
│                                     │
│  [Open TikTok]  [Skip for Now]      │
└─────────────────────────────────────┘
```

### 2.2 Preference Updates

**File: `Pluct-Data-Preferences-UserPreferences.kt`**

New constants:
```kotlin
KEY_ONBOARDING_STEP = "onboarding_step"
KEY_FIRST_TRANSCRIPT_COMPLETED = "first_transcript_completed"
KEY_ONBOARDING_TUTORIAL_SEEN = "onboarding_tutorial_seen"
```

New methods:
```kotlin
fun getOnboardingStep(): Int
fun setOnboardingStep(step: Int)
fun isFirstTranscriptCompleted(): Boolean
fun markFirstTranscriptCompleted()
fun hasSeenOnboardingTutorial(): Boolean
fun markOnboardingTutorialSeen()
```

### 2.3 MainActivity Integration

**File: `PluctUIScreen01MainActivity.kt`**

After permission onboarding completes:
```kotlin
// After showPermissionOnboarding = false
if (!prefs.hasSeenOnboardingTutorial()) {
    showOnboardingTutorial = true
}
```

### 2.4 First Transcript Detection

**File: `Pluct-UI-Screen-01MainActivity-02TranscriptionOrchestrator.kt`**

After successful transcription:
```kotlin
if (!prefs.isFirstTranscriptCompleted()) {
    prefs.markFirstTranscriptCompleted()
    // Refresh balance to pick up any server-side bonus
    refreshCreditBalance()
}
```

### 2.5 TikTok Deep Link

**Intent to launch TikTok:**
```kotlin
val intent = Intent(Intent.ACTION_MAIN).apply {
    setPackage("com.zhiliaoapp.musically") // TikTok package
    addCategory(Intent.CATEGORY_LAUNCHER)
}
if (intent.resolveActivity(packageManager) != null) {
    startActivity(intent)
} else {
    // TikTok not installed - open Play Store
    startActivity(Intent(Intent.ACTION_VIEW,
        Uri.parse("market://details?id=com.zhiliaoapp.musically")))
}
```

---

## PART 3: TESTING SPECIFICATIONS

### 3.1 Automated Test: Journey-Onboarding-01Tutorial-01Validation.js

**Test Steps:**

| Step | Action | Validation |
|------|--------|------------|
| 1 | Clear app data | App state reset |
| 2 | Launch app | Welcome dialog appears |
| 3 | Tap "Get Started" | Welcome dialog dismisses |
| 4 | Grant permissions | Permission flow completes |
| 5 | Wait for tutorial | Tutorial Step 1 appears |
| 6 | Tap "Next" | Tutorial Step 2 appears |
| 7 | Tap "Got It" | Tutorial Step 3 appears |
| 8 | Tap "Open TikTok" | TikTok launches (or Play Store if not installed) |
| 9 | Return to app | App in expected state |
| 10 | Verify preferences | `onboarding_tutorial_seen = true` |

**Logcat Patterns to Validate:**
- `Onboarding.*tutorial.*started`
- `Onboarding.*step.*completed`
- `TikTok.*launch.*intent`
- `Onboarding.*tutorial.*completed`

### 3.2 Automated Test: Journey-Onboarding-02FirstTranscript-01Validation.js

**Test Steps:**

| Step | Action | Validation |
|------|--------|------------|
| 1 | Complete onboarding tutorial | Tutorial seen flag set |
| 2 | Send TikTok intent | URL received by app |
| 3 | Wait for transcription | Transcription completes |
| 4 | Verify first transcript flag | `first_transcript_completed = true` |
| 5 | Verify balance refresh | Balance endpoint called |

**Logcat Patterns to Validate:**
- `First.*transcript.*completed`
- `Balance.*refresh.*triggered`

### 3.3 Automated Test: Journey-Onboarding-03Skip-01Validation.js

**Test Steps:**

| Step | Action | Validation |
|------|--------|------------|
| 1 | Complete permissions | Tutorial starts |
| 2 | Tap "Skip for Now" at Step 3 | Tutorial dismisses |
| 3 | Verify home screen | Home screen visible |
| 4 | Verify preferences | `onboarding_tutorial_seen = true` |
| 5 | Relaunch app | Tutorial not shown again |

---

## PART 4: EDGE CASES

### 4.1 TikTok Not Installed

**Detection:** `packageManager.resolveActivity(tiktokIntent) == null`

**Behavior:**
- Show alternative text: "Install TikTok to get started"
- "Open TikTok" button → Opens Play Store TikTok listing
- User can still skip and use manual URL input

### 4.2 User Force-Closes During Onboarding

**Detection:** App killed between steps

**Behavior:**
- State persisted to SharedPreferences after each step
- On relaunch, resume from last completed step
- Never repeat already-seen steps

### 4.3 Permissions Permanently Denied

**Detection:** Permission onboarding shows "Skip" repeatedly

**Behavior:**
- Tutorial still shows - permissions optional for core flow
- Notification permission optional (transcripts work without)
- Settings shows "Required" badge for re-enabling

### 4.4 First Transcript Fails

**Detection:** Transcription returns error

**Behavior:**
- Do NOT mark first transcript completed
- User can retry
- Only mark complete on successful transcript

### 4.5 Network Loss During Onboarding

**Detection:** No connectivity

**Behavior:**
- Tutorial is fully offline - no network needed
- TikTok button still works (launches local TikTok app)
- First transcript will queue if network unavailable

---

## PART 5: FILE CHANGES SUMMARY

### New Files

| File | Purpose |
|------|---------|
| `Pluct-UI-Component-07Onboarding-01Tutorial-01Flow.kt` | Tutorial UI component |
| `Journey-Onboarding-01Tutorial-01Validation.js` | Tutorial flow test |
| `Journey-Onboarding-02FirstTranscript-01Validation.js` | First transcript test |
| `Journey-Onboarding-03Skip-01Validation.js` | Skip flow test |

### Modified Files

| File | Changes |
|------|---------|
| `Pluct-Data-Preferences-UserPreferences.kt` | Add tutorial state tracking |
| `PluctUIScreen01MainActivity.kt` | Integrate tutorial after permissions |
| `Pluct-UI-Screen-01MainActivity-02TranscriptionOrchestrator.kt` | Track first transcript |
| `Pluct-Journey-01Orchestrator.js` | Register new test journeys |

---

## PART 6: SUCCESS METRICS

### Validation Criteria

| Metric | Target |
|--------|--------|
| Tutorial shown to first-time users | 100% |
| Tutorial not shown on subsequent launches | 100% |
| TikTok launch works when installed | 100% |
| Play Store opens when TikTok not installed | 100% |
| Skip option returns to home screen | 100% |
| First transcript flag set on success | 100% |
| All automated tests pass | 100% |

---

## PART 7: RATIONALE SUMMARY

| Decision | Rationale |
|----------|-----------|
| Text-based visuals vs images | Zero-cost, no design dependency, accessible |
| 3-step tutorial vs single screen | Progressive disclosure, less overwhelming |
| Deep-link to TikTok vs in-app demo | Real practice builds muscle memory |
| Server-side milestone vs client reward | Prevents gaming, maintains trust |
| Optional skip button | Respects user autonomy, simplicity |
| Persist after each step | Crash resilience, edge case handling |

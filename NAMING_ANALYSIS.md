# Naming Consistency Analysis

**Date:** 2025-01-21  
**Purpose:** Analyze file and class naming patterns for consistency

---

## File Naming Pattern Analysis

### Pattern Categories Found:

#### 1. `Pluct-[Layer]-[Feature]-[Component]-[Variant].kt` (Most Common)
**Examples:**
- `Pluct-UI-Screen-01MainActivity-02VideoProcessor.kt`
- `Pluct-Core-API-01UnifiedService-01Main.kt`
- `Pluct-UI-Component-04VideoList-01Item.kt`
- `Pluct-Core-Debug-01LogManager.kt`

**Count:** ~60 files  
**Pattern:** `Pluct-[Layer]-[Category]-[Number][Name]-[SubNumber][SubName].kt`

#### 2. `Pluct-[Layer]-[Feature]-[Component].kt` (Simpler Variant)
**Examples:**
- `Pluct-UI-Screen-01HomeScreen.kt`
- `Pluct-Core-Network-01ConnectivityChecker.kt`
- `Pluct-Data-Entity-DebugLogEntry.kt`

**Count:** ~30 files  
**Pattern:** `Pluct-[Layer]-[Category]-[Number][Name].kt`

#### 3. `Pluct-Mobile-UI-Component-[Feature].kt` (Mobile-Specific)
**Examples:**
- `Pluct-Mobile-UI-Component-CaptureCard-00Main.kt`
- `Pluct-Mobile-UI-Component-CaptureCard-01URLInput.kt`
- `Pluct-Mobile-UI-Component-CaptureCard-02ChoiceEngine.kt`

**Count:** ~8 files  
**Pattern:** `Pluct-Mobile-UI-Component-[Feature]-[Number][Name].kt`

#### 4. Simple Class Names (Outliers)
**Examples:**
- `PluctUIScreen01MainActivity.kt` (no hyphens)
- `PluctApplication01Hilt.kt` (no hyphens)
- `PluctVideoRepository.kt` (no numbers)
- `PluctVideoDao.kt` (no numbers)

**Count:** ~10 files  
**Issue:** Inconsistent with main pattern

---

## Class/Function Naming Analysis

### "VideoProcessor" vs "TranscriptionOrchestrator"

**Current Usage:**
- File: `Pluct-UI-Screen-01MainActivity-02VideoProcessor.kt`
- Object: `PluctUIScreen01MainActivityVideoProcessor`
- Function: `processVideo()`

**Issue:** Name suggests local video processing, but code only orchestrates API calls

**Recommendation:**
- Rename to: `Pluct-UI-Screen-01MainActivity-02TranscriptionOrchestrator.kt`
- Object: `PluctUIScreen01MainActivityTranscriptionOrchestrator`
- Function: Keep `processVideo()` (widely used) OR rename to `orchestrateTranscription()`

### "process" vs "orchestrate" Terminology

**Functions with "process":**
- `processVideo()` - Actually orchestrates API calls
- `processTikTokVideo()` - Actually orchestrates API calls
- `processNewTranscription()` - Actually orchestrates API calls

**Recommendation:**
- Keep function names as-is (too many references)
- Update comments to clarify "orchestrates" not "processes"
- Consider future refactor to rename functions

---

## Naming Pattern Recommendations

### Standard Pattern (Recommended):
```
Pluct-[Layer]-[Category]-[Number][Name]-[SubNumber][SubName].kt
```

**Where:**
- `[Layer]` = UI, Core, Data, Services, Architecture
- `[Category]` = Screen, Component, API, Entity, etc.
- `[Number]` = 01, 02, 03... (sequence number)
- `[Name]` = PascalCase feature name
- `[SubNumber]` = Optional, for sub-components
- `[SubName]` = Optional, for sub-components

### Examples of Good Naming:
- ✅ `Pluct-UI-Screen-01MainActivity-02VideoProcessor.kt`
- ✅ `Pluct-Core-API-01UnifiedService-01Main.kt`
- ✅ `Pluct-Data-Entity-ProcessingTier.kt`

### Examples of Inconsistent Naming:
- ❌ `PluctUIScreen01MainActivity.kt` (should be `Pluct-UI-Screen-01MainActivity.kt`)
- ❌ `PluctApplication01Hilt.kt` (should be `Pluct-Application-01Hilt.kt`)
- ❌ `PluctVideoRepository.kt` (should be `Pluct-Data-Repository-01Video.kt`)

---

## Class Name Patterns

### Current Patterns:
1. **Hyphenated with Numbers:** `PluctUIScreen01MainActivityVideoProcessor`
2. **Simple PascalCase:** `PluctVideoRepository`
3. **Mixed:** `PluctApplication01Hilt`

### Recommendation:
- **Standardize on:** `Pluct[Layer][Category][Number][Name][SubNumber][SubName]`
- **Examples:**
  - `PluctUIScreen01MainActivity02TranscriptionOrchestrator`
  - `PluctCoreAPI01UnifiedService01Main`
  - `PluctDataRepository01Video`

---

## Action Items

### Priority 1: Critical Renames
1. ✅ Rename `VideoProcessor` → `TranscriptionOrchestrator`
2. ⚠️ Consider renaming files without hyphens (low priority)

### Priority 2: Standardization
1. ⚠️ Standardize class names to match file names
2. ⚠️ Add numbers to files without them (if needed for ordering)

### Priority 3: Documentation
1. ⚠️ Document naming convention in README
2. ⚠️ Create lint rule to enforce pattern

---

## Summary

**Current State:**
- ~70% of files follow consistent pattern
- ~20% follow simpler variant
- ~10% are outliers (inconsistent)

**Recommendation:**
- **Keep existing pattern** (it's mostly consistent)
- **Fix outliers** only if they cause confusion
- **Focus on semantic clarity** (VideoProcessor → TranscriptionOrchestrator) over strict pattern adherence

**Estimated Effort:**
- Critical renames: 1-2 hours
- Full standardization: 4-6 hours (low priority)


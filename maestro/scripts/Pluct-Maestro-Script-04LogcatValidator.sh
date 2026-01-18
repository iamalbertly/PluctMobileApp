#!/bin/bash
# Pluct-Maestro-Script-04LogcatValidator.sh
# Enhanced logcat validator with pattern matching for Maestro tests
# Usage: ./Pluct-Maestro-Script-04LogcatValidator.sh <pattern1> <pattern2> ...

set -e

# Capture logcat output
LOGCAT_OUTPUT=$(adb logcat -d | grep -i "PluctAPI\|PluctCoreAPIHTTPClient\|IntentHandler\|MainActivity\|CaptureCard\|PluctNotificationHelper" || true)

# Check for required patterns
REQUIRED_PATTERNS=("$@")
FOUND_PATTERNS=()
MISSING_PATTERNS=()

for pattern in "${REQUIRED_PATTERNS[@]}"; do
    if echo "$LOGCAT_OUTPUT" | grep -qiE "$pattern"; then
        FOUND_PATTERNS+=("$pattern")
    else
        MISSING_PATTERNS+=("$pattern")
    fi
done

# If required patterns were provided, check if all were found
if [ ${#REQUIRED_PATTERNS[@]} -gt 0 ]; then
    if [ ${#MISSING_PATTERNS[@]} -gt 0 ]; then
        echo "ERROR: Missing required patterns in logcat:"
        for pattern in "${MISSING_PATTERNS[@]}"; do
            echo "  - $pattern"
        done
        echo ""
        echo "Recent logcat output:"
        echo "$LOGCAT_OUTPUT" | tail -n 20
        exit 1
    fi
fi

# Check for errors
ERROR_COUNT=$(echo "$LOGCAT_OUTPUT" | grep -ciE "error|exception|failed" || true)

if [ "$ERROR_COUNT" -gt 0 ]; then
    echo "WARNING: Found $ERROR_COUNT error(s) in logcat"
    echo "$LOGCAT_OUTPUT" | grep -iE "error|exception|failed" | tail -n 5
fi

echo "SUCCESS: All required patterns found in logcat"
exit 0

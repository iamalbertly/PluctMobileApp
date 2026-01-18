#!/bin/bash
# Pluct-Maestro-Script-01LogcatValidator.sh
# Validates logcat output for API calls and errors
# Usage: ./Pluct-Maestro-Script-01LogcatValidator.sh <pattern1> <pattern2> ...

set -e

# Capture logcat output
LOGCAT_OUTPUT=$(adb logcat -d | grep -i "PluctAPI\|PluctCoreAPIHTTPClient" || true)

# Check for required patterns
REQUIRED_PATTERNS=("$@")
FOUND_PATTERNS=()

for pattern in "${REQUIRED_PATTERNS[@]}"; do
    if echo "$LOGCAT_OUTPUT" | grep -qi "$pattern"; then
        FOUND_PATTERNS+=("$pattern")
    fi
done

# Check for errors
ERROR_COUNT=$(echo "$LOGCAT_OUTPUT" | grep -ci "error\|exception\|failed" || true)

# If required patterns were provided, check if all were found
if [ ${#REQUIRED_PATTERNS[@]} -gt 0 ]; then
    if [ ${#FOUND_PATTERNS[@]} -lt ${#REQUIRED_PATTERNS[@]} ]; then
        echo "ERROR: Not all required patterns found in logcat"
        echo "Required: ${REQUIRED_PATTERNS[*]}"
        echo "Found: ${FOUND_PATTERNS[*]}"
        exit 1
    fi
fi

# Fail if critical errors found
if [ "$ERROR_COUNT" -gt 5 ]; then
    echo "ERROR: Too many errors in logcat: $ERROR_COUNT"
    echo "$LOGCAT_OUTPUT" | grep -i "error\|exception\|failed" | head -10
    exit 1
fi

echo "SUCCESS: Logcat validation passed"
exit 0

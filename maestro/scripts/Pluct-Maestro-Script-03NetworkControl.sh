#!/bin/bash
# Pluct-Maestro-Script-03NetworkControl.sh
# Controls network connectivity for testing
# Usage: ./Pluct-Maestro-Script-03NetworkControl.sh <enable|disable>

set -e

ACTION="$1"

if [ -z "$ACTION" ]; then
    echo "ERROR: Action parameter required (enable|disable)"
    exit 1
fi

if [ "$ACTION" = "disable" ]; then
    adb shell svc wifi disable
    adb shell svc data disable
    echo "SUCCESS: Network disabled"
elif [ "$ACTION" = "enable" ]; then
    adb shell svc wifi enable
    adb shell svc data enable
    echo "SUCCESS: Network enabled"
else
    echo "ERROR: Invalid action. Use 'enable' or 'disable'"
    exit 1
fi

exit 0

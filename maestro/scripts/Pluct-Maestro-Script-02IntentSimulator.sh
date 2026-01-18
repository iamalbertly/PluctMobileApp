#!/bin/bash
# Pluct-Maestro-Script-02IntentSimulator.sh
# Simulates TikTok share intent with URL
# Usage: ./Pluct-Maestro-Script-02IntentSimulator.sh <url>

set -e

URL="$1"

if [ -z "$URL" ]; then
    echo "ERROR: URL parameter required"
    exit 1
fi

# Simulate share intent
adb shell am start -a android.intent.action.SEND -t "text/plain" --es android.intent.extra.TEXT "$URL" app.pluct/.PluctUIScreen01MainActivity

# Wait for intent processing
sleep 3

echo "SUCCESS: Intent simulated with URL: $URL"
exit 0

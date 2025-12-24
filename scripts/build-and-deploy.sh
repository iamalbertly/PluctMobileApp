#!/bin/bash
set -e

echo "Building Android app..."
./gradlew assembleDebug

echo "Installing on connected device..."
adb install -r app/build/outputs/apk/debug/app-debug.apk

echo "Clearing app data for clean test..."
adb shell pm clear app.pluct

echo "Launching app..."
adb shell am start -n app.pluct/.PluctUIScreen01MainActivity

echo "Build and deploy complete!"

















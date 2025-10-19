#!/bin/bash
# Android Build Script

# Exit on any error
set -e

# Define output directory
OUTPUT_DIR="./output"

# Grant execute permission for gradlew
chmod +x gradlew

# Clean the project
echo "Cleaning the project..."
./gradlew clean

# Build the release APK
echo "Building the release APK..."
./gradlew assembleRelease

# Create output directory if it doesn't exist
mkdir -p $OUTPUT_DIR

# Find the APK and move it to the output directory
APK_PATH=$(find app/build/outputs/apk/release -name "*.apk" | head -n 1)
if [ -f "$APK_PATH" ]; then
    echo "Moving APK to $OUTPUT_DIR"
    mv "$APK_PATH" "$OUTPUT_DIR/"
    echo "Build successful! APK is at $OUTPUT_DIR/$(basename $APK_PATH)"
else
    echo "Error: Release APK not found!"
    exit 1
fi

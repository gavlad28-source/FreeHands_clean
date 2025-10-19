# Android Build Script for PowerShell

# Exit on any error
$ErrorActionPreference = "Stop"

# Define output directory
$OutputDirectory = "./output"

# Clean the project
Write-Host "Cleaning the project..."
./gradlew.bat clean

# Build the release APK
Write-Host "Building the release APK..."
./gradlew.bat assembleRelease

# Create output directory if it doesn't exist
if (-not (Test-Path -Path $OutputDirectory)) {
    New-Item -ItemType Directory -Path $OutputDirectory
}

# Find the APK and move it to the output directory
$apkFile = Get-ChildItem -Path app/build/outputs/apk/release -Filter *.apk | Select-Object -First 1
if ($apkFile) {
    Write-Host "Moving APK to $OutputDirectory"
    Move-Item -Path $apkFile.FullName -Destination $OutputDirectory
    Write-Host "Build successful! APK is at $($OutputDirectory)/$($apkFile.Name)"
} else {
    Write-Error "Error: Release APK not found!"
    exit 1
}

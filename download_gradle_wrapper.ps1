# Create wrapper directory if it doesn't exist
$wrapperDir = "$PSScriptRoot\gradle\wrapper"
if (-not (Test-Path $wrapperDir)) {
    New-Item -ItemType Directory -Path $wrapperDir -Force | Out-Null
}

# Download Gradle wrapper JAR
$gradleWrapperUrl = "https://github.com/gradle/gradle/raw/v8.13/gradle/wrapper/gradle-wrapper.jar"
$outputPath = "$wrapperDir\gradle-wrapper.jar"

Write-Host "Downloading Gradle Wrapper JAR..."
try {
    Invoke-WebRequest -Uri $gradleWrapperUrl -OutFile $outputPath -ErrorAction Stop
    Write-Host "Gradle Wrapper JAR downloaded successfully to $outputPath"
} catch {
    Write-Host "Failed to download Gradle Wrapper JAR: $_" -ForegroundColor Red
    exit 1
}

# Verify the download
if (Test-Path $outputPath) {
    $fileInfo = Get-Item $outputPath
    if ($fileInfo.Length -gt 0) {
        Write-Host "Gradle Wrapper JAR verified successfully (Size: $($fileInfo.Length) bytes)" -ForegroundColor Green
    } else {
        Write-Host "Downloaded file is empty" -ForegroundColor Red
        exit 1
    }
} else {
    Write-Host "Failed to verify Gradle Wrapper JAR" -ForegroundColor Red
    exit 1
}

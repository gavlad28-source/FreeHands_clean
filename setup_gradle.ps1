# Download Gradle wrapper files
$gradleVersion = "8.4"
$gradleUrl = "https://services.gradle.org/distributions/gradle-${gradleVersion}-bin.zip"
$gradleZip = "gradle-bin.zip"

Write-Host "Downloading Gradle ${gradleVersion}..." -ForegroundColor Cyan
Invoke-WebRequest -Uri $gradleUrl -OutFile $gradleZip

# Extract Gradle
Write-Host "Extracting Gradle..." -ForegroundColor Cyan
Expand-Archive -Path $gradleZip -DestinationPath . -Force

# Initialize Gradle wrapper
Write-Host "Initializing Gradle wrapper..." -ForegroundColor Cyan
$gradleDir = "gradle-${gradleVersion}"
& ".\$gradleDir\bin\gradle" wrapper --gradle-version=$gradleVersion --distribution-type=bin

# Clean up
Write-Host "Cleaning up..." -ForegroundColor Cyan
Remove-Item -Path $gradleZip -Force
Remove-Item -Path $gradleDir -Recurse -Force

Write-Host "Gradle wrapper setup complete!" -ForegroundColor Green

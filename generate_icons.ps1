# This script generates launcher icons for Android
# Create directories if they don't exist
$mipmapDensities = @(
    "mipmap-mdpi",
    "mipmap-hdpi",
    "mipmap-xhdpi",
    "mipmap-xxhdpi",
    "mipmap-xxxhdpi"
)

# Create directories
foreach ($density in $mipmapDensities) {
    $dir = Join-Path -Path "app\src\main\res" -ChildPath $density
    if (-not (Test-Path -Path $dir)) {
        New-Item -ItemType Directory -Path $dir -Force | Out-Null
        Write-Host "Created directory: $dir"
    }
}

# Create a simple launcher icon XML (you should replace this with proper icons in a real app)
$launcherIcon = @'
<?xml version="1.0" encoding="utf-8"?>
<adaptive-icon xmlns:android="http://schemas.android.com/apk/res/android">
    <background android:drawable="@color/primary_color"/>
    <foreground>
        <vector
            android:width="108dp"
            android:height="108dp"
            android:viewportWidth="108"
            android:viewportHeight="108">
            <path
                android:fillColor="#FFFFFF"
                android:pathData="M54,20C35.2,20 20,35.2 20,54C20,72.8 35.2,88 54,88C72.8,88 88,72.8 88,54C88,35.2 72.8,20 54,20ZM54,80.5C39.2,80.5 27.5,68.8 27.5,54C27.5,39.2 39.2,27.5 54,27.5C68.8,27.5 80.5,39.2 80.5,54C80.5,68.8 68.8,80.5 54,80.5Z"
                android:strokeWidth="1"
                android:strokeColor="#00000000" />
            <path
                android:fillColor="#FFFFFF"
                android:pathData="M54,30C40.2,30 29,41.2 29,55C29,68.8 40.2,80 54,80C67.8,80 79,68.8 79,55C79,41.2 67.8,30 54,30ZM54,73.5C44.9,73.5 37.5,65.1 37.5,55C37.5,44.9 44.9,37.5 54,37.5C63.1,37.5 70.5,44.9 70.5,55C70.5,65.1 63.1,73.5 54,73.5Z"
                android:strokeWidth="1"
                android:strokeColor="#00000000" />
            <path
                android:fillColor="#FFFFFF"
                android:pathData="M54,40C46.3,40 40,46.3 40,54C40,61.7 46.3,68 54,68C61.7,68 68,61.7 68,54C68,46.3 61.7,40 54,40Z"
                android:strokeWidth="1"
                android:strokeColor="#00000000" />
        </vector>
    </foreground>
</adaptive-icon>
'@

# Create a simple round launcher icon XML
$roundLauncherIcon = @'
<?xml version="1.0" encoding="utf-8"?>
<adaptive-icon xmlns:android="http://schemas.android.com/apk/res/android">
    <background android:drawable="@color/primary_color"/>
    <foreground>
        <vector
            android:width="108dp"
            android:height="108dp"
            android:viewportWidth="108"
            android:viewportHeight="108">
            <path
                android:fillColor="#FFFFFF"
                android:pathData="M54,20C35.2,20 20,35.2 20,54C20,72.8 35.2,88 54,88C72.8,88 88,72.8 88,54C88,35.2 72.8,20 54,20ZM54,80.5C39.2,80.5 27.5,68.8 27.5,54C27.5,39.2 39.2,27.5 54,27.5C68.8,27.5 80.5,39.2 80.5,54C80.5,68.8 68.8,80.5 54,80.5Z"
                android:strokeWidth="1"
                android:strokeColor="#00000000" />
            <path
                android:fillColor="#FFFFFF"
                android:pathData="M54,30C40.2,30 29,41.2 29,55C29,68.8 40.2,80 54,80C67.8,80 79,68.8 79,55C79,41.2 67.8,30 54,30ZM54,73.5C44.9,73.5 37.5,65.1 37.5,55C37.5,44.9 44.9,37.5 54,37.5C63.1,37.5 70.5,44.9 70.5,55C70.5,65.1 63.1,73.5 54,73.5Z"
                android:strokeWidth="1"
                android:strokeColor="#00000000" />
            <path
                android:fillColor="#FFFFFF"
                android:pathData="M54,40C46.3,40 40,46.3 40,54C40,61.7 46.3,68 54,68C61.7,68 68,61.7 68,54C68,46.3 61.7,40 54,40Z"
                android:strokeWidth="1"
                android:strokeColor="#00000000" />
        </vector>
    </foreground>
</adaptive-icon>
'@

# Save the launcher icon files
$launcherIcon | Out-File -FilePath "app\src\main\res\mipmap-anydpi-v26\ic_launcher.xml" -Force
$roundLauncherIcon | Out-File -FilePath "app\src\main\res\mipmap-anydpi-v26\ic_launcher_round.xml" -Force

# Create legacy launcher icons (for older Android versions)
$legacyIcon = @'
<?xml version="1.0" encoding="utf-8"?>
<vector xmlns:android="http://schemas.android.com/apk/res/android"
    android:width="108dp"
    android:height="108dp"
    android:viewportWidth="108"
    android:viewportHeight="108">
    <path
        android:fillColor="#2196F3"
        android:pathData="M54,20C35.2,20 20,35.2 20,54C20,72.8 35.2,88 54,88C72.8,88 88,72.8 88,54C88,35.2 72.8,20 54,20ZM54,80.5C39.2,80.5 27.5,68.8 27.5,54C27.5,39.2 39.2,27.5 54,27.5C68.8,27.5 80.5,39.2 80.5,54C80.5,68.8 68.8,80.5 54,80.5Z"
        android:strokeWidth="1"
        android:strokeColor="#00000000" />
    <path
        android:fillColor="#2196F3"
        android:pathData="M54,30C40.2,30 29,41.2 29,55C29,68.8 40.2,80 54,80C67.8,80 79,68.8 79,55C79,41.2 67.8,30 54,30ZM54,73.5C44.9,73.5 37.5,65.1 37.5,55C37.5,44.9 44.9,37.5 54,37.5C63.1,37.5 70.5,44.9 70.5,55C70.5,65.1 63.1,73.5 54,73.5Z"
        android:strokeWidth="1"
        android:strokeColor="#00000000" />
    <path
        android:fillColor="#2196F3"
        android:pathData="M54,40C46.3,40 40,46.3 40,54C40,61.7 46.3,68 54,68C61.7,68 68,61.7 68,54C68,46.3 61.7,40 54,40Z"
        android:strokeWidth="1"
        android:strokeColor="#00000000" />
</vector>
'@

# Save legacy launcher icons for different densities
$legacyIcon | Out-File -FilePath "app\src\main\res\mipmap-mdpi\ic_launcher.xml" -Force
$legacyIcon | Out-File -FilePath "app\src\main\res\mipmap-hdpi\ic_launcher.xml" -Force
$legacyIcon | Out-File -FilePath "app\src\main\res\mipmap-xhdpi\ic_launcher.xml" -Force
$legacyIcon | Out-File -FilePath "app\src\main\res\mipmap-xxhdpi\ic_launcher.xml" -Force
$legacyIcon | Out-File -FilePath "app\src\main\res\mipmap-xxxhdpi\ic_launcher.xml" -Force
$legacyIcon | Out-File -FilePath "app\src\main\res\mipmap-mdpi\ic_launcher_round.xml" -Force
$legacyIcon | Out-File -FilePath "app\src\main\res\mipmap-hdpi\ic_launcher_round.xml" -Force
$legacyIcon | Out-File -FilePath "app\src\main\res\mipmap-xhdpi\ic_launcher_round.xml" -Force
$legacyIcon | Out-File -FilePath "app\src\main\res\mipmap-xxhdpi\ic_launcher_round.xml" -Force
$legacyIcon | Out-File -FilePath "app\src\main\res\mipmap-xxxhdpi\ic_launcher_round.xml" -Force

Write-Host "Launcher icons have been generated successfully!"

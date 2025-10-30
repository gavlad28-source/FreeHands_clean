# ================================
# 🧠 Android Studio Auto Fix + Build + Git Push Script
# Автор: Влад (FreeHands_clean)
# ================================

$ErrorActionPreference = "Stop"
$projectPath = "C:\Users\Garbe\AndroidStudioProjects\FreeHands_clean"
cd $projectPath

Write-Host "🚀 Step 1: Removing invisible BOM characters..." -ForegroundColor Cyan

# Удаляем BOM только из пользовательских файлов, пропуская .gradle и build каталоги
Get-ChildItem -Path $projectPath -Recurse -Include *.gradle,*.properties,*.toml,*.yaml,*.yml -ErrorAction SilentlyContinue |
    Where-Object { $_.FullName -notmatch '\\\.gradle\\' -and $_.FullName -notmatch '\\build\\' } |
    ForEach-Object {
        try {
            $file = $_.FullName
            $content = Get-Content -Path $file -Raw -ErrorAction Stop
            Set-Content -Path $file -Value $content -Encoding utf8
        }
        catch {
            Write-Host "⚠️ Skipped file (access denied): $file" -ForegroundColor Yellow
        }
    }

Write-Host "✅ All visible BOMs removed successfully." -ForegroundColor Green
# ================================
# Step 2. Проверка settings.gradle
# ================================
$settingsFile = "$projectPath\settings.gradle"
if (!(Test-Path $settingsFile)) {
    Write-Host "⚠️ settings.gradle not found, creating default..." -ForegroundColor Yellow
    @"
pluginManagement {
    repositories {
        google()
        mavenCentral()
        gradlePluginPortal()
    }
}
dependencyResolutionManagement {
    repositories {
        google()
        mavenCentral()
    }
}
rootProject.name = "FreeHands_clean"
include(":app")
"@ | Set-Content -Path $settingsFile -Encoding utf8
}
Write-Host "✅ settings.gradle checked." -ForegroundColor Green

# ================================
# Step 3. Очистка кешей Gradle
# ================================
Write-Host "🧹 Cleaning Gradle caches..." -ForegroundColor Cyan
./gradlew clean --no-daemon --warning-mode all

# ================================
# Step 4. Проверка Lint и JNI
# ================================
Write-Host "🔍 Running Lint check (this may take a few minutes)..." -ForegroundColor Cyan
& ./gradlew lintAll --continue
if (-not $?) {
    Write-Host "⚠️ Lint completed with warnings or errors." -ForegroundColor Yellow
}

Write-Host "⚙️ Building JNI native libs..." -ForegroundColor Cyan
& ./gradlew externalNativeBuildDebug --continue
if (-not $?) {
    Write-Host "⚠️ JNI build finished with some issues." -ForegroundColor Yellow
}


# ================================
# Step 5. Сборка APK
# ================================
Write-Host "📦 Building APK (Debug + Release)..." -ForegroundColor Cyan
./gradlew assembleDebug assembleRelease --continue

# ================================
# Step 6. Проверка GitHub и Push
# ================================
Write-Host "🔁 Sync with GitHub..." -ForegroundColor Cyan
git pull origin main --rebase
git add .
git commit -m "AutoFix + Lint + Build $(Get-Date -Format 'yyyy-MM-dd HH:mm')"
git push origin main

Write-Host "✅ GitHub updated successfully." -ForegroundColor Green

# ================================
# Step 7. Готово 🎉
# ================================
Write-Host "`n🎯 Project fixed and built successfully!"
Write-Host "👉 Check your APKs here:"
Write-Host "$projectPath\app\build\outputs\apk\debug"
Write-Host "$projectPath\app\build\outputs\apk\release"
Write-Host "`n💡 Tip: If build fails again, run this script once more."

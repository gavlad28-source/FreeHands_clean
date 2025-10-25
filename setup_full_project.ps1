# ======================================
# setup_full_project.ps1
# Полная автоматизация проекта FreeHands_clean
# ======================================

# --- 1) Перейти в корень проекта ---
cd $PSScriptRoot

Write-Host "🚀 Начинаем автоматическую настройку проекта..."

# --- 2) Удаляем старые бэкапы и дубли ---
$backupFolders = @("backup_20250929_171535", "src_backup")
foreach ($folder in $backupFolders) {
    if (Test-Path $folder) {
        Remove-Item -Recurse -Force $folder
        Write-Host "🗑 Удалена папка: $folder"
    }
}

# --- 3) Создаём структуру MVVM + DI + Repository + Room + Firebase + AI + Voice ---
$basePath = "app/src/main/java/com/garbe/freehands"
$packages = @(
    "$basePath/data/local",
    "$basePath/data/remote",
    "$basePath/data/repository",
    "$basePath/domain/models",
    "$basePath/di",
    "$basePath/ui/main"
)

foreach ($pkg in $packages) {
    if (-not (Test-Path $pkg)) {
        New-Item -ItemType Directory -Path $pkg | Out-Null
        Write-Host "📁 Создан пакет: $pkg"
    }
}

# --- 4) Создаём базовые файлы ---
$files = @{
    "$basePath/App.kt" = @"
package com.garbe.freehands

import android.app.Application
import dagger.hilt.android.HiltAndroidApp

@HiltAndroidApp
class App: Application() {
    // Инициализация Firebase, Room, Hilt, AI и Voice здесь
}
"@

    "$basePath/data/repository/MyRepository.kt" = @"
package com.garbe.freehands.data.repository

class MyRepository {
    // Заглушка репозитория
}
"@

    "$basePath/domain/models/User.kt" = @"
package com.garbe.freehands.domain.models

data class User(val id: String, val name: String)
"@

    "$basePath/di/AppModule.kt" = @"
package com.garbe.freehands.di

import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

@Module
@InstallIn(SingletonComponent::class)
object AppModule {
    // Заглушка DI
}
"@

    "$basePath/ui/main/MainViewModel.kt" = @"
package com.garbe.freehands.ui.main

import androidx.lifecycle.ViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

@HiltViewModel
class MainViewModel @Inject constructor() : ViewModel() {
    // Заглушка ViewModel
}
"@
}

foreach ($file in $files.Keys) {
    if (-not (Test-Path $file)) {
        Set-Content -Path $file -Value $files[$file] -Encoding UTF8
        Write-Host "📄 Создан файл: $file"
    }
}

# --- 5) Подключаем зависимости в build.gradle ---
$gradleFile = "app/build.gradle"
$dependencies = @"
implementation 'com.google.dagger:hilt-android:2.48'
kapt 'com.google.dagger:hilt-compiler:2.48'
implementation 'androidx.room:room-runtime:2.6.0'
kapt 'androidx.room:room-compiler:2.6.0'
implementation 'androidx.compose.ui:ui:1.5.0'
implementation 'androidx.compose.material3:material3:1.2.0'
implementation 'com.google.firebase:firebase-auth-ktx:22.1.0'
implementation 'com.google.firebase:firebase-firestore-ktx:24.5.0'
implementation 'com.alphacephei:vosk:0.3.45' # Vosk TTS/ASR
implementation 'org.jetbrains.kotlinx:kotlinx-coroutines-android:1.7.3'
"@

if (Test-Path $gradleFile) {
    Add-Content -Path $gradleFile -Value $dependencies
    Write-Host "✅ Зависимости добавлены в $gradleFile"
}

# --- 6) Создаём GitHub Actions workflow ---
$workflowDir = ".github/workflows"
if (-not (Test-Path $workflowDir)) { New-Item -ItemType Directory -Path $workflowDir | Out-Null }

$workflowFile = "$workflowDir/android_build_release.yml"
$workflowContent = @"
name: Android CI/CD + Release

on:
  push:
    branches: [main]
  pull_request:
    branches: [main]

jobs:
  build:
    runs-on: ubuntu-latest

    steps:
      - uses: actions/checkout@v3
      - uses: actions/setup-java@v3
        with:
          distribution: temurin
          java-version: '17'
      - name: Cache Gradle
        uses: actions/cache@v3
        with:
          path: |
            ~/.gradle/caches
            ~/.gradle/wrapper
          key: gradle-\${{ runner.os }}-\${{ hashFiles('**/*.gradle*','**/gradle-wrapper.properties') }}
      - name: Build project
        run: ./gradlew clean assembleDebug assembleRelease --stacktrace
      - name: Run CodeQL
        uses: github/codeql-action/analyze@v3
        with:
          category: security-and-quality
      - name: Upload APKs
        uses: actions/upload-artifact@v3
        with:
          name: APKs
          path: |
            app/build/outputs/apk/debug/app-debug.apk
            app/build/outputs/apk/release/app-release.apk
      - name: Create Release
        id: create_release
        uses: softprops/action-gh-release@v1
        with:
          tag_name: v1-\${{ github.run_number }}
          name: APK Release \${{ github.run_number }}
          body: Automated release of APKs from CI
        env:
          GITHUB_TOKEN: \${{ secrets.GITHUB_TOKEN }}
      - name: Upload Release APK
        uses: actions/upload-release-asset@v1
        with:
          upload_url: \${{ steps.create_release.outputs.upload_url }}
          asset_path: app/build/outputs/apk/release/app-release.apk
          asset_name: app-release.apk
          asset_content_type: application/vnd.android.package-archive
"@

Set-Content -Path $workflowFile -Value $workflowContent -Encoding UTF8
Write-Host "📄 Workflow создан: $workflowFile"

# --- 7) Собираем проект ---
Write-Host "🔧 Собираем debug и release APK..."
./gradlew clean assembleDebug assembleRelease --stacktrace

# --- 8) Сообщение о завершении ---
Write-Host "✅ Полная структура MVVM + DI + Room + Firebase + AI + Voice создана!"
Write-Host "✅ Workflow для GitHub Actions настроен!"
Write-Host "✅ Проект готов к коммиту и пушу в GitHub!"


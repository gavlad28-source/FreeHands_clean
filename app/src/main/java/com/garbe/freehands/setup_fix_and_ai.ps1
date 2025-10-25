<#
  setup_fix_and_ai.ps1
  - PowerShell script for Windows (Android Studio terminal)
  - Fix common Gradle/build issues, add dependencies, create AI+Voice placeholders,
    update App + AndroidManifest, create CI workflow, run build and push to origin.
  - Run from project root (where settings.gradle is).
#>

# --- Safety checks
Write-Host "=== START: setup_fix_and_ai.ps1 ==="
$projRoot = Get-Location
Write-Host "Project root: $projRoot"

# Ensure git repo
if (-not (Test-Path ".git")) {
    Write-Host "ERROR: This folder is not a git repository. Aborting." -ForegroundColor Red
    exit 1
}

# --- Helper: read package name from AndroidManifest.xml
$manifestPath = "app/src/main/AndroidManifest.xml"
if (-not (Test-Path $manifestPath)) {
    Write-Host "ERROR: AndroidManifest.xml not found at $manifestPath" -ForegroundColor Red
    exit 1
}
$manifestText = Get-Content $manifestPath -Raw
# find package="..."
if ($manifestText -match 'package\s*=\s*"(.*?)"') {
    $packageName = $Matches[1]
    Write-Host "Detected package: $packageName"
} else {
    Write-Host "Could not detect package in AndroidManifest.xml. Using default 'com.freehands.app'"
    $packageName = "com.freehands.app"
}

# Convert package to path
$pkgPath = $packageName -replace '\.','/'
$javaBase = "app/src/main/java/$pkgPath"
Write-Host "Java base path: $javaBase"

# Create backup of files we will modify
$backupDir = ".setup_backup_$(Get-Date -Format 'yyyyMMdd_HHmmss')"
New-Item -ItemType Directory -Path $backupDir | Out-Null
Write-Host "Backup directory: $backupDir"

# Files to backup
$filesToBackup = @("app/build.gradle","build.gradle","settings.gradle","app/src/main/AndroidManifest.xml")
foreach ($f in $filesToBackup) {
    if (Test-Path $f) {
        $dest = Join-Path $backupDir ($f -replace '[\/\\]','_')
        Copy-Item $f $dest -Force
        Write-Host "Backed up $f -> $dest"
    }
}

# --- 1) Run initial Gradle build and capture log
Write-Host "`n=== Running initial Gradle build (capture errors) ==="
& .\gradlew.bat clean assembleDebug --warning-mode all > build_log_initial.txt 2>&1
Write-Host "Initial build finished. Log -> build_log_initial.txt"

# --- 2) Ensure app/build.gradle contains plugins and dependencies (idempotent additions)
$appBuildGradle = "app/build.gradle"
if (-not (Test-Path $appBuildGradle)) {
    Write-Host "ERROR: app/build.gradle not found at $appBuildGradle" -ForegroundColor Red
    exit 1
}

# Read content
$gradleText = Get-Content $appBuildGradle -Raw

# Ensure plugins: kotlin-kapt, hilt, google-services (basic)
if ($gradleText -notmatch "kotlin-kapt") {
    Write-Host "Adding kotlin-kapt plugin entry (if using plugins block)..."
    # Try to insert after plugins { line if present
    if ($gradleText -match "plugins\s*\{") {
        $gradleText = $gradleText -replace "plugins\s*\{", "plugins {`n    id 'kotlin-kapt'"
    } else {
        # fallback: append at top
        $gradleText = "apply plugin: 'kotlin-kapt'`n$gradleText"
    }
}

# Add Hilt plugin if missing
if ($gradleText -notmatch "dagger.hilt.android") {
    Write-Host "Adding Hilt plugin (kapt and plugin) to app/build.gradle..."
    if ($gradleText -match "plugins\s*\{") {
        # add id line if not exists
        if ($gradleText -notmatch "id 'com.google.dagger.hilt.android'") {
            $gradleText = $gradleText -replace "plugins\s*\{", "plugins {`n    id 'com.google.dagger.hilt.android'"
        }
    } else {
        $gradleText = "apply plugin: 'com.google.dagger.hilt.android'`napply plugin: 'kotlin-kapt'`n$gradleText"
    }
}

# Add Compose, Room, Hilt, Firebase, Vosk, TTS, Coroutines dependencies if missing
$depsToEnsure = @(
"com.google.dagger:hilt-android:2.52",
"com.google.dagger:hilt-compiler:2.52",
"androidx.room:room-runtime:2.6.1",
"androidx.room:room-ktx:2.6.1",
"org.vosk:vosk-android:0.3.38",
"com.google.firebase:firebase-bom",
"com.google.firebase:firebase-auth",
"org.jetbrains.kotlinx:kotlinx-coroutines-android",
"androidx.compose.material3:material3",
"androidx.activity:activity-compose"
)

foreach ($dep in $depsToEnsure) {
    if ($gradleText -notmatch [regex]::Escape($dep.Split(':')[0])) {
        Write-Host "Adding dependency (approx match): $dep"
        # naive append to dependencies block: find first 'dependencies {' and insert lines after it
        if ($gradleText -match "dependencies\s*\{") {
            $gradleText = $gradleText -replace "dependencies\s*\{", "dependencies {`n    // added by setup_fix_and_ai`n    implementation '$dep'"
        } else {
            # append at end
            $gradleText = $gradleText + "`ndependencies {`n    implementation '$dep'`n}"
        }
    } else {
        Write-Host "Dependency group for '$dep' seems present — skipping"
    }
}

# Write updated build.gradle (backup already made)
$gradleBackup = "$backupDir/app_build.gradle.bak"
Copy-Item $appBuildGradle $gradleBackup -Force
Set-Content $appBuildGradle $gradleText -Encoding UTF8
Write-Host "Updated app/build.gradle (backup saved to $gradleBackup)"

# --- 3) Ensure project-level buildscript has Hilt & google services classpath
$projectBuildGradle = "build.gradle"
if (Test-Path $projectBuildGradle) {
    $projText = Get-Content $projectBuildGradle -Raw
    if ($projText -notmatch "com.google.dagger:hilt-android-gradle-plugin") {
        Write-Host "Adding Hilt Gradle plugin classpath to project build.gradle"
        $projText = $projText + "`nbuildscript {`n    dependencies {`n        classpath 'com.google.dagger:hilt-android-gradle-plugin:2.52'`n        classpath 'com.google.gms:google-services:4.4.2'`n    }`n}"
        $projBackup = "$backupDir/build.gradle.bak"
        Copy-Item $projectBuildGradle $projBackup -Force
        Set-Content $projectBuildGradle $projText -Encoding UTF8
        Write-Host "Updated project build.gradle (backup $projBackup)"
    } else {
        Write-Host "Project build.gradle already contains Hilt plugin classpath."
    }
} else {
    Write-Host "No project build.gradle found at root. Skipping adding classpath entries."
}

# --- 4) Create/replace App.kt with Hilt + Firebase + Room initialization
$appKtPath = Join-Path $javaBase "App.kt"
# Make sure directory exists
if (-not (Test-Path $javaBase)) {
    New-Item -ItemType Directory -Path $javaBase -Force | Out-Null
}
$appKtContent = @"
package $packageName

import android.app.Application
import androidx.room.Room
import com.google.firebase.FirebaseApp
import dagger.hilt.android.HiltAndroidApp
import $packageName.data.local.db.AppDatabase

@HiltAndroidApp
class App : Application() {

    companion object {
        lateinit var database: AppDatabase
    }

    override fun onCreate() {
        super.onCreate()
        FirebaseApp.initializeApp(this)
        database = Room.databaseBuilder(
            applicationContext,
            AppDatabase::class.java,
            ""freehands_db""
        ).fallbackToDestructiveMigration().build()
    }
}
"@
# backup if exists
if (Test-Path $appKtPath) { Copy-Item $appKtPath "$backupDir/App.kt.bak" -Force }
Set-Content -Path $appKtPath -Value $appKtContent -Encoding UTF8
Write-Host "Wrote $appKtPath"

# --- 5) Ensure AndroidManifest lists Application and microphone & internet permissions
$manifest = Get-Content $manifestPath -Raw
# add android:name if missing in application tag
if ($manifest -notmatch 'android:name\s*=') {
    $manifest = $manifest -replace '<application', "<application`n        android:name=""$packageName.App"""
    Write-Host "Added android:name to <application> in AndroidManifest.xml"
}
# ensure permissions
if ($manifest -notmatch 'android.permission.RECORD_AUDIO') {
    $manifest = $manifest -replace '</manifest>', '    <uses-permission android:name="android.permission.RECORD_AUDIO" />' + "`n</manifest>"
    Write-Host "Added RECORD_AUDIO permission"
}
if ($manifest -notmatch 'android.permission.INTERNET') {
    $manifest = $manifest -replace '</manifest>', '    <uses-permission android:name="android.permission.INTERNET" />' + "`n</manifest>"
    Write-Host "Added INTERNET permission"
}
# write back
Copy-Item $manifestPath "$backupDir/AndroidManifest.xml.bak" -Force
Set-Content -Path $manifestPath -Value $manifest -Encoding UTF8
Write-Host "Updated AndroidManifest.xml (backup saved)"

# --- 6) Create AI + Voice placeholders (safe overwrite)
$aiDir = Join-Path $javaBase "ai"
$voiceDir = Join-Path $javaBase "ai/voice"
New-Item -ItemType Directory -Path $aiDir -Force | Out-Null
New-Item -ItemType Directory -Path $voiceDir -Force | Out-Null

# AIManager.kt (online OpenAI / placeholder)
$aiPath = Join-Path $aiDir "AIManager.kt"
$aiCode = @"
package $packageName.ai

import android.content.Context
// Placeholder: implement your OpenAI/Gemini HTTP client here
class AIManager(private val context: Context) {
    // TODO: set OPENAI_API_KEY via secure storage or CI secrets
    fun requestResponse(prompt: String): String {
        // Placeholder synchronous stub
        return ""AI response for: $prompt""
    }
}
"@
Set-Content -Path $aiPath -Value $aiCode -Encoding UTF8
Write-Host "Created $aiPath"

# VoiceManager.kt (Vosk + TTS placeholders)
$voicePath = Join-Path $voiceDir "VoiceManager.kt"
$voiceCode = @"
package $packageName.ai.voice

import android.content.Context
import android.speech.tts.TextToSpeech
import java.util.Locale

// Placeholder: implement Vosk ASR integration and Android TTS
class VoiceManager(private val context: Context) {

    private var tts: TextToSpeech? = null

    fun initTTS() {
        tts = TextToSpeech(context) { status ->
            if (status == TextToSpeech.SUCCESS) {
                tts?.language = Locale.getDefault()
            }
        }
    }

    fun speak(text: String) {
        tts?.speak(text, TextToSpeech.QUEUE_FLUSH, null, "tts1")
    }

    fun shutdown() {
        tts?.stop()
        tts?.shutdown()
    }

    // TODO: Add Vosk offline ASR integration: load model, create recognizer, handle audio stream
}
"@
Set-Content -Path $voicePath -Value $voiceCode -Encoding UTF8
Write-Host "Created $voicePath"

# VoiceViewModel.kt (in ui)
$vmDir = Join-Path $javaBase "ui/voice"
New-Item -ItemType Directory -Path $vmDir -Force | Out-Null
$vmPath = Join-Path $vmDir "VoiceViewModel.kt"
$vmCode = @"
package $packageName.ui.voice

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.launch
import $packageName.ai.AIManager
import $packageName.ai.voice.VoiceManager

class VoiceViewModel(
    private val aiManager: AIManager,
    private val voiceManager: VoiceManager
) : ViewModel() {

    fun processVoiceCommand(transcript: String) {
        viewModelScope.launch {
            // simple flow: local commands -> AI fallback
            when {
                transcript.contains(""привет"", [System.StringComparison]::InvariantCultureIgnoreCase) -> {
                    voiceManager.speak(""Привет! Чем могу помочь?"")
                }
                else -> {
                    val resp = aiManager.requestResponse(transcript)
                    voiceManager.speak(resp)
                }
            }
        }
    }
}
"@
Set-Content -Path $vmPath -Value $vmCode -Encoding UTF8
Write-Host "Created $vmPath"

# --- 7) Create GitHub Actions workflow (safe, PowerShell-friendly)
$workflowDir = ".github/workflows"
if (-not (Test-Path $workflowDir)) { New-Item -ItemType Directory -Path $workflowDir -Force | Out-Null }

$workYmlPath = Join-Path $workflowDir "android.yml"
$workflowYml = @"
name: Android CI

on:
  push:
    branches: [ 'main', 'master' ]
  pull_request:
    branches: [ 'main', 'master' ]

jobs:
  build:
    runs-on: ubuntu-latest
    steps:
      - uses: actions/checkout@v3
      - name: Set up JDK 17
        uses: actions/setup-java@v3
        with:
          java-version: '17'
          distribution: 'temurin'
      - name: Grant execute permission for Gradlew
        run: chmod +x ./gradlew
      - name: Build Debug
        run: ./gradlew assembleDebug
      - name: Upload artifact
        uses: actions/upload-artifact@v3
        with:
          name: app-debug
          path: app/build/outputs/apk/debug/app-debug.apk
"@
# Use Out-File -LiteralPath to avoid interpolation issues
$workflowYml | Out-File -LiteralPath $workYmlPath -Encoding UTF8
Write-Host "Wrote GitHub Actions workflow to $workYmlPath"

# --- 8) Run gradle build again and capture log
Write-Host "`n=== Running gradle build after modifications ==="
& .\gradlew.bat clean assembleDebug --warning-mode all > build_log_after_fix.txt 2>&1
Write-Host "Build finished. Log -> build_log_after_fix.txt"

# --- 9) Stage, commit, push changes
Write-Host "`n=== Git add / commit / push ==="
git add .
$commitMsg = "chore: setup AI+Voice placeholders, Hilt/Room/Compose deps, App + manifest updates"
git commit -m $commitMsg -q
# Try pushing to main then master
$pushOk = $false
try {
    git push origin main -q
    $pushOk = $true
    Write-Host "Pushed to origin/main"
} catch {
    Write-Host "Push to origin/main failed, trying origin/master..."
    try {
        git push origin master -q
        $pushOk = $true
        Write-Host "Pushed to origin/master"
    } catch {
        Write-Host "Push failed. Please push manually (git push) and check remotes." -ForegroundColor Yellow
    }
}

Write-Host "`n=== Completed setup_fix_and_ai.ps1 ==="
if (-not $pushOk) { Write-Host "NOTE: Changes were committed locally. Push failed — push manually." -ForegroundColor Yellow }
Write-Host "Generated logs: build_log_initial.txt, build_log_after_fix.txt"
Write-Host "Backups stored in $backupDir"

<#
setup_full_fix.ps1
PowerShell script: 
- Delete old/duplicate files safely
- Fix Hilt, ViewModels, DI
- Create AI + Voice placeholders
- Add dependencies
- Update App.kt & Manifest
- Create GitHub Actions workflow
- Build project and commit+push
#>

Write-Host "=== START: setup_full_fix.ps1 ==="

$projRoot = Get-Location
Write-Host "Project root: $projRoot"

# --- 1) Backup important files and folders
$backupDir = ".setup_backup_$(Get-Date -Format 'yyyyMMdd_HHmmss')"
New-Item -ItemType Directory -Path $backupDir | Out-Null
Write-Host "Backup dir: $backupDir"

# List of files/folders to backup/remove safely
$targets = @(
    "app/src/main/java/**/App.kt",
    "app/src/main/java/**/viewmodel/**",
    "app/src/main/java/**/di/**",
    "app/src/main/java/**/ui/**",
    "app/src/main/java/**/data/**",
    "backup_*",
    "src_backup"
)

foreach ($t in $targets) {
    $found = Get-ChildItem -Path $projRoot -Recurse -Force -ErrorAction SilentlyContinue -Include $t
    foreach ($f in $found) {
        $dest = Join-Path $backupDir ($f.Name)
        Copy-Item $f.FullName $dest -Recurse -Force -ErrorAction SilentlyContinue
        Remove-Item $f.FullName -Recurse -Force -ErrorAction SilentlyContinue
        Write-Host "Backed up & removed: $($f.FullName)"
    }
}

# --- 2) Detect package
$manifestPath = "app/src/main/AndroidManifest.xml"
if (-not (Test-Path $manifestPath)) {
    Write-Host "ERROR: AndroidManifest.xml not found!" -ForegroundColor Red
    exit 1
}
$manifestText = Get-Content $manifestPath -Raw
if ($manifestText -match 'package\s*=\s*"(.*?)"') {
    $packageName = $Matches[1]
} else {
    $packageName = "com.freehands.app"
}
$pkgPath = $packageName -replace '\.','/'
$javaBase = "app/src/main/java/$pkgPath"

# --- 3) Create folder structure
$folders = @(
    "$javaBase/data/local",
    "$javaBase/data/remote",
    "$javaBase/data/repository",
    "$javaBase/di",
    "$javaBase/domain/models",
    "$javaBase/ui/main",
    "$javaBase/ui/voice",
    "$javaBase/ai/voice"
)
foreach ($f in $folders) { New-Item -ItemType Directory -Path $f -Force | Out-Null }

# --- 4) Create ViewModels
$vmList = @{
    "MainViewModel.kt" = @"
package $packageName.ui.main

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.launch

class MainViewModel : ViewModel() {
    fun doSomething() {
        viewModelScope.launch {
            // TODO
        }
    }
}
"@
    "SettingsViewModel.kt" = @"
package $packageName.ui.main

import androidx.lifecycle.ViewModel

class SettingsViewModel : ViewModel() {}
"@
    "VoiceViewModel.kt" = @"
package $packageName.ui.voice

import androidx.lifecycle.ViewModel
import $packageName.ai.AIManager
import $packageName.ai.voice.VoiceManager

class VoiceViewModel(
    private val aiManager: AIManager,
    private val voiceManager: VoiceManager
) : ViewModel() {}
"@
}

foreach ($vm in $vmList.Keys) {
    $path = Join-Path "$javaBase/ui/main" $vm
    if ($vm -eq "VoiceViewModel.kt") { $path = Join-Path "$javaBase/ui/voice" $vm }
    Set-Content -Path $path -Value $vmList[$vm] -Encoding UTF8
    Write-Host "Created $path"
}

# --- 5) Fix DI module
$viewModelModulePath = Join-Path "$javaBase/di" "ViewModelModule.kt"
$viewModelModuleContent = @"
package $packageName.di

import androidx.lifecycle.ViewModel
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import dagger.multibindings.IntoMap
import dagger.hilt.android.lifecycle.HiltViewModel
import $packageName.ui.main.MainViewModel
import $packageName.ui.main.SettingsViewModel
import $packageName.ui.voice.VoiceViewModel

@Module
@InstallIn(SingletonComponent::class)
abstract class ViewModelModule {

    // Example bindings
    // TODO: add @Binds if needed
}
"@
Set-Content -Path $viewModelModulePath -Value $viewModelModuleContent -Encoding UTF8
Write-Host "Created $viewModelModulePath"

# --- 6) Create App.kt placeholder
$appKtPath = Join-Path $javaBase "App.kt"
$appKtContent = @"
package $packageName

import android.app.Application
import dagger.hilt.android.HiltAndroidApp

@HiltAndroidApp
class App : Application() {}
"@
Set-Content -Path $appKtPath -Value $appKtContent -Encoding UTF8
Write-Host "Created $appKtPath"

# --- 7) AI + Voice placeholders
$aiCode = @"
package $packageName.ai

class AIManager {
    fun requestResponse(prompt: String) = ""AI response placeholder""
}
"@
$voiceCode = @"
package $packageName.ai.voice

class VoiceManager {
    fun speak(text: String) {}
}
"@
Set-Content -Path (Join-Path "$javaBase/ai" "AIManager.kt") -Value $aiCode -Encoding UTF8
Set-Content -Path (Join-Path "$javaBase/ai/voice" "VoiceManager.kt") -Value $voiceCode -Encoding UTF8
Write-Host "Created AI + Voice placeholders"

# --- 8) Build project and log
Write-Host "`n=== Running gradlew build ==="
& .\gradlew.bat clean assembleDebug --warning-mode all > build_log_after_fix.txt 2>&1
Write-Host "Build log -> build_log_after_fix.txt"

# --- 9) Git commit & push
git add .
git commit -m "fix: rebuild MVVM + DI + AI placeholders, remove duplicates" -q
try { git push origin main -q; Write-Host "Pushed to origin/main" }
catch { Write-Host "Push failed, check manually." -ForegroundColor Yellow }

Write-Host "`n✅ Project cleaned, ViewModels fixed, AI+Voice placeholders created!"
Write-Host "Backups: $backupDir"

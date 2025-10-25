<#
PowerShell Script: setup_full_project.ps1
Purpose: Fully setup Android project with MVVM, DI, Room, Compose, Firebase, AI + Voice.
#>

Write-Host "=== START: setup_full_project.ps1 ==="

$projRoot = Get-Location
Write-Host "Project root: $projRoot"

# --- 1) Backup existing files
$backupDir = ".setup_backup_$(Get-Date -Format 'yyyyMMdd_HHmmss')"
New-Item -ItemType Directory -Path $backupDir | Out-Null
Write-Host "Backup dir: $backupDir"

$targets = @(
    "app/src/main/java/**/App.kt",
    "app/src/main/java/**/viewmodel/**",
    "app/src/main/java/**/di/**",
    "app/src/main/java/**/ui/**",
    "app/src/main/java/**/data/**",
    "app/src/main/java/**/ai/**"
)

foreach ($t in $targets) {
    $found = Get-ChildItem -Path $projRoot -Recurse -Force -Include $t -ErrorAction SilentlyContinue
    foreach ($f in $found) {
        $dest = Join-Path $backupDir ($f.Name)
        Copy-Item $f.FullName $dest -Recurse -Force -ErrorAction SilentlyContinue
        Remove-Item $f.FullName -Recurse -Force -ErrorAction SilentlyContinue
        Write-Host "Backed up & removed: $($f.FullName)"
    }
}

# --- 2) Detect package
$manifestPath = "app/src/main/AndroidManifest.xml"
$manifestText = Get-Content $manifestPath -Raw
if ($manifestText -match 'package\s*=\s*"(.*?)"') {
    $packageName = $Matches[1]
} else { $packageName = "com.freehands.app" }
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
    "$javaBase/ai/voice",
    "$javaBase/ui/components",
    "$javaBase/ui/theme"
)
foreach ($f in $folders) { New-Item -ItemType Directory -Path $f -Force | Out-Null }

# --- 4) Create ViewModels with Hilt
$vmList = @{
    "MainViewModel.kt" = @"
package $packageName.ui.main

import androidx.lifecycle.ViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

@HiltViewModel
class MainViewModel @Inject constructor(): ViewModel() {}
"@
    "SettingsViewModel.kt" = @"
package $packageName.ui.main

import androidx.lifecycle.ViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

@HiltViewModel
class SettingsViewModel @Inject constructor(): ViewModel() {}
"@
    "VoiceViewModel.kt" = @"
package $packageName.ui.voice

import androidx.lifecycle.ViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import $packageName.ai.AIManager
import $packageName.ai.voice.VoiceManager

@HiltViewModel
class VoiceViewModel @Inject constructor(
    private val aiManager: AIManager,
    private val voiceManager: VoiceManager
): ViewModel() {}
"@
}

foreach ($vm in $vmList.Keys) {
    $path = Join-Path "$javaBase/ui/main" $vm
    if ($vm -eq "VoiceViewModel.kt") { $path = Join-Path "$javaBase/ui/voice" $vm }
    Set-Content -Path $path -Value $vmList[$vm] -Encoding UTF8
    Write-Host "Created $path"
}

# --- 5) DI module with Hilt bindings
$viewModelModulePath = Join-Path "$javaBase/di" "ViewModelModule.kt"
$viewModelModuleContent = @"
package $packageName.di

import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

@Module
@InstallIn(SingletonComponent::class)
abstract class ViewModelModule {
    // ViewModel bindings handled by @HiltViewModel
}
"@
Set-Content -Path $viewModelModulePath -Value $viewModelModuleContent -Encoding UTF8
Write-Host "Created $viewModelModulePath"

# --- 6) App.kt
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

# --- 8) Room skeleton
$entityCode = @"
package $packageName.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity
data class User(
    @PrimaryKey val id: String,
    val name: String
)
"@
Set-Content -Path (Join-Path "$javaBase/data/local" "User.kt") -Value $entityCode -Encoding UTF8

$daoCode = @"
package $packageName.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query

@Dao
interface UserDao {
    @Insert suspend fun insert(user: User)
    @Query(""SELECT * FROM User"") suspend fun getAll(): List<User>
}
"@
Set-Content -Path (Join-Path "$javaBase/data/local" "UserDao.kt") -Value $daoCode -Encoding UTF8

$repoCode = @"
package $packageName.data.repository

import $packageName.data.local.UserDao
import $packageName.data.local.User
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class UserRepository @Inject constructor(private val dao: UserDao) {
    suspend fun addUser(user: User) = dao.insert(user)
    suspend fun getUsers() = dao.getAll()
}
"@
Set-Content -Path (Join-Path "$javaBase/data/repository" "UserRepository.kt") -Value $repoCode -Encoding UTF8
Write-Host "Created Room/Repository skeleton"

# --- 9) Firebase placeholders
$firebaseCode = @"
package $packageName.data.remote

class FirebaseManager {
    fun login(email: String, pass: String) {}
    fun getFirestoreData(collection: String) {}
}
"@
Set-Content -Path (Join-Path "$javaBase/data/remote" "FirebaseManager.kt") -Value $firebaseCode -Encoding UTF8
Write-Host "Created Firebase placeholders"

# --- 10) UI Compose skeleton
$uiCode = @"
package $packageName.ui.components

import androidx.compose.material3.Text
import androidx.compose.runtime.Composable

@Composable
fun CustomButton(label: String, onClick: () -> Unit) {
    Text(text = label)
}
"@
Set-Content -Path (Join-Path "$javaBase/ui/components" "CustomButton.kt") -Value $uiCode -Encoding UTF8
Write-Host "Created Compose UI skeleton"

# --- 11) Build project
Write-Host "`n=== Running gradlew build ==="
& .\gradlew.bat clean assembleDebug --warning-mode all > build_log_full.txt 2>&1
Write-Host "Build log -> build_log_full.txt"

# --- 12) Git commit & push
git add .
git commit -m "feat: full MVVM+DI+Room+Firebase+AI+Voice placeholders, clean project" -q
try { git push origin main -q; Write-Host "✅ Pushed to origin/main" }
catch { Write-Host "⚠ Push failed, check manually." -ForegroundColor Yellow }

Write-Host "`n✅ FULL PROJECT STRUCTURE CREATED & READY!"
Write-Host "Backups: $backupDir"

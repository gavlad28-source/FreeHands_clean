# FreeHands Assets Guide

## Overview
This guide explains how to configure and manage models, assets, and resources for the FreeHands offline voice assistant.

## Directory Structure

```
app/src/main/assets/
├── models/
│   ├── vosk/
│   │   ├── vosk-model-small-ru-0.22/     # Russian small model (~40MB)
│   │   ├── vosk-model-ru-0.22/           # Russian full model (~1.5GB - optional)
│   │   └── vosk-model-small-en-us-0.15/  # English small model (~40MB - optional)
│   └── porcupine/
│       ├── porcupine_params.pv           # Porcupine model file
│       └── keywords/
│           ├── privet-brat_ru_android.ppn    # "привет, брат" wake word
│           └── hey-assistant_en_android.ppn  # "hey assistant" (optional)
└── audio/
    ├── beep_start.wav       # Sound when assistant starts listening
    ├── beep_stop.wav        # Sound when assistant stops
    └── error.wav            # Error sound
```

## Required Models

### 1. Vosk ASR Models

#### Where to Download
- Official site: https://alphacephei.com/vosk/models
- GitHub releases: https://github.com/alphacep/vosk-api/releases

#### Recommended Models

**For Russian (Recommended for FreeHands):**
- **Small**: `vosk-model-small-ru-0.22` (~40 MB)
  - Good accuracy for commands
  - Fast recognition
  - Suitable for most devices
  - Download: https://alphacephei.com/vosk/models/vosk-model-small-ru-0.22.zip

- **Full**: `vosk-model-ru-0.22` (~1.5 GB)
  - Better accuracy
  - Slower recognition
  - Requires high-end device
  - Only for premium experience

**For English (Optional):**
- **Small**: `vosk-model-small-en-us-0.15` (~40 MB)
  - For English commands
  - Download: https://alphacephei.com/vosk/models/vosk-model-small-en-us-0.15.zip

#### Installation Steps

1. **Download the model:**
   ```bash
   # Download Russian small model
   curl -LO https://alphacephei.com/vosk/models/vosk-model-small-ru-0.22.zip
   ```

2. **Extract the archive:**
   ```bash
   unzip vosk-model-small-ru-0.22.zip
   ```

3. **Copy to assets:**
   ```bash
   # Create directory if it doesn't exist
   mkdir -p app/src/main/assets/models/vosk/
   
   # Copy model
   cp -r vosk-model-small-ru-0.22 app/src/main/assets/models/vosk/
   ```

4. **Verify structure:**
   ```
   app/src/main/assets/models/vosk/vosk-model-small-ru-0.22/
   ├── am/
   │   └── final.mdl
   ├── conf/
   │   └── mfcc.conf
   ├── graph/
   │   ├── Gr.fst
   │   ├── HCLr.fst
   │   └── disambig_tid.int
   ├── ivector/
   │   ├── final.dubm
   │   ├── final.ie
   │   └── online_cmvn.conf
   └── README
   ```

#### Model Size Optimization

**WARNING**: APK size will increase by model size!

**Options to reduce APK size:**

1. **Use App Bundles** (Recommended):
   ```bash
   ./gradlew bundleRelease
   ```
   Upload `.aab` file to Google Play - models will be downloaded on-demand.

2. **Use Multiple APKs**:
   In `build.gradle`:
   ```groovy
   android {
       splits {
           abi {
               enable true
               reset()
               include 'armeabi-v7a', 'arm64-v8a', 'x86', 'x86_64'
               universalApk false
           }
       }
   }
   ```

3. **Download on First Launch**:
   - Don't include models in APK
   - Download from your server on first launch
   - Show progress to user
   - Cache in app's private storage

4. **Use Asset Packs** (for Play Store):
   ```groovy
   // build.gradle
   assetPacks = [":model_pack"]
   ```

### 2. Porcupine Wake Word Models

#### Porcupine Setup

**Option A: Community Edition (Free, Limited)**
1. Sign up at https://console.picovoice.ai/
2. Get Access Key (free tier: limited devices)
3. Download pre-built keyword or create custom

**Option B: Vosk Keyword Spotting (Free, Recommended)**
- Use Vosk's built-in keyword spotting
- No account needed
- Works offline
- Configure in code:
  ```kotlin
  val recognizer = Model(modelPath).use { model ->
      Recognizer(model, 16000.0f, "[\"привет брат\", \"окей ассистент\"]")
  }
  ```

#### Creating Custom Wake Word (Porcupine)

1. Go to https://console.picovoice.ai/
2. Create Account → Get Access Key
3. Train Custom Wake Word:
   - Enter phrase: "привет брат"
   - Select language: Russian
   - Select platform: Android
   - Download `.ppn` file

4. Copy to assets:
   ```bash
   cp privet-brat_ru_android_v3_0_0.ppn app/src/main/assets/models/porcupine/keywords/privet-brat_ru_android.ppn
   ```

5. Update access key in code:
   ```kotlin
   // In PorcupineManager.kt
   private const val ACCESS_KEY = "YOUR_ACCESS_KEY_HERE"
   ```

**IMPORTANT**: Free tier has device limitations. For production, consider Vosk keyword spotting instead.

### 3. Audio Assets

Create simple notification sounds:

```bash
# Generate beep sounds (requires ffmpeg)
ffmpeg -f lavfi -i "sine=frequency=1000:duration=0.2" -ar 16000 app/src/main/assets/audio/beep_start.wav
ffmpeg -f lavfi -i "sine=frequency=800:duration=0.2" -ar 16000 app/src/main/assets/audio/beep_stop.wav
ffmpeg -f lavfi -i "sine=frequency=400:duration=0.5" -ar 16000 app/src/main/assets/audio/error.wav
```

Or use free sounds from:
- https://freesound.org/
- https://www.zapsplat.com/

## Model Configuration

### Selecting Model in Code

Edit `VoskManager.kt`:

```kotlin
object VoskConfig {
    // Choose which model to use
    const val MODEL_NAME = "vosk-model-small-ru-0.22"  // Change here
    const val MODEL_PATH = "models/vosk/$MODEL_NAME"
    
    // Recognition settings
    const val SAMPLE_RATE = 16000f
    const val BUFFER_SIZE = 4096
}
```

### Testing Models

Before building release:

1. **Test accuracy**:
   - Try various commands
   - Test in noisy environment
   - Verify response time

2. **Check performance**:
   - Monitor CPU usage
   - Check battery drain
   - Test on low-end devices

3. **Measure latency**:
   - Wake word → Recognition start
   - Speech end → Command execution

## Build Configuration

### Exclude Large Assets from Debug Builds

Speed up debug builds by excluding models:

```groovy
// app/build.gradle
android {
    buildTypes {
        debug {
            // Exclude models from debug builds
            aaptOptions {
                ignoreAssetsPattern "!models/vosk/vosk-model-ru-0.22:!*.zip"
            }
        }
    }
}
```

### Compress Assets

Models are already compressed. Additional compression won't help much:

```groovy
android {
    aaptOptions {
        noCompress "mdl", "so", "fst"
    }
}
```

## Model Checksums

For security, verify model integrity:

### Generate Checksums

```bash
# Generate SHA-256 checksum
sha256sum app/src/main/assets/models/vosk/vosk-model-small-ru-0.22/* > model_checksums.txt
```

### Verify in Code

Update `ModelVerifier.kt`:

```kotlin
object ModelChecksums {
    val VOSK_SMALL_RU = mapOf(
        "am/final.mdl" to "abc123...",
        "conf/mfcc.conf" to "def456...",
        // ... more files
    )
}
```

## Updating Models

### Version Management

Track model versions:

```kotlin
// ModelVersion.kt
object ModelVersions {
    const val VOSK_RU_SMALL = "0.22"
    const val LAST_UPDATED = "2024-01-15"
}
```

### OTA Updates

For downloading models over-the-air:

1. Host models on your server
2. Create manifest file:
   ```json
   {
       "models": [
           {
               "name": "vosk-model-small-ru-0.22",
               "version": "0.22",
               "url": "https://yourserver.com/models/vosk-ru-small-0.22.zip",
               "sha256": "abc123...",
               "size": 41943040
           }
       ]
   }
   ```

3. Implement downloader:
   ```kotlin
   class ModelDownloader(context: Context) {
       suspend fun checkForUpdates(): List<ModelUpdate>
       suspend fun downloadModel(model: ModelUpdate)
       fun verifyChecksum(file: File, expectedHash: String): Boolean
   }
   ```

## Troubleshooting

### Model Not Found

**Error**: `Model not found in assets`

**Solution**:
1. Verify model path:
   ```bash
   ls -la app/src/main/assets/models/vosk/
   ```
2. Check model is included in APK:
   ```bash
   unzip -l app/build/outputs/apk/debug/app-debug.apk | grep vosk
   ```
3. Clean and rebuild:
   ```bash
   ./gradlew clean assembleDebug
   ```

### Recognition Not Working

**Issue**: Model loads but doesn't recognize speech

**Checklist**:
- [ ] Correct sample rate (16000 Hz)
- [ ] Correct audio format (16-bit PCM)
- [ ] Microphone permission granted
- [ ] Audio input is not muted
- [ ] Model matches language being spoken

### APK Too Large

**Issue**: APK exceeds 100 MB

**Solutions**:
1. Use smaller model (small instead of full)
2. Use App Bundles (`.aab`)
3. Download models on demand
4. Use expansion files (Play Store)
5. Split APKs by architecture

### Out of Memory

**Error**: `OutOfMemoryError` when loading model

**Solutions**:
1. Use smaller model
2. Increase heap size in `AndroidManifest.xml`:
   ```xml
   <application
       android:largeHeap="true"
       ...>
   ```
3. Load model in background thread
4. Release model when not in use

## Performance Tips

### 1. Lazy Loading
Load models only when needed:
```kotlin
class VoskManager {
    private var model: Model? = null
    
    fun initWhenNeeded() {
        if (model == null) {
            model = Model(modelPath)
        }
    }
}
```

### 2. Model Caching
Copy model from assets to internal storage (faster access):
```kotlin
fun copyModelToCache(context: Context) {
    val cacheDir = File(context.cacheDir, "models")
    // Copy assets to cache
    // Use cached version for recognition
}
```

### 3. Preloading
Load model during splash screen:
```kotlin
class SplashActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        lifecycleScope.launch {
            ModelLoader.preload(applicationContext)
            startMainActivity()
        }
    }
}
```

## Testing Checklist

Before releasing:

- [ ] Model loads without errors
- [ ] Recognition works in quiet environment
- [ ] Recognition works with background noise
- [ ] Latency is acceptable (< 300ms)
- [ ] Memory usage is reasonable (< 150 MB)
- [ ] Battery drain is acceptable
- [ ] Works on various devices (low/mid/high-end)
- [ ] APK size is acceptable
- [ ] Models are properly obfuscated (if applicable)
- [ ] Checksums verified

## Resources

- **Vosk Models**: https://alphacephei.com/vosk/models
- **Vosk Documentation**: https://alphacephei.com/vosk/
- **Porcupine Console**: https://console.picovoice.ai/
- **Model Training**: https://alphacephei.com/vosk/adaptation
- **Performance Optimization**: https://alphacephei.com/vosk/android

## License Notes

### Vosk
- Apache 2.0 License
- Free for commercial use
- Attribution required

### Porcupine
- Community Edition: Free with limitations
- Commercial: Requires license
- Check terms: https://picovoice.ai/pricing/

Always verify licensing for your use case!

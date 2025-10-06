# FreeHands Security Guide

## Overview
This document describes security measures implemented in FreeHands assistant and provides instructions for secure build and deployment.

## Security Features

### 1. Release Signing
- **APK Signing**: All release builds are signed with a release keystore
- **Signature Verification**: App verifies its own signature at runtime to detect tampering
- **No Debug Builds in Production**: Debug builds are blocked from production servers

### 2. Data Protection
- **Android Keystore**: Cryptographic keys are stored in hardware-backed Android Keystore
- **AES-GCM Encryption**: All sensitive local data is encrypted using AES-256-GCM
- **No Backup**: `android:allowBackup="false"` prevents cloud backup of sensitive data
- **Secure Preferences**: SharedPreferences are encrypted using AndroidX Security library

### 3. Code Protection
- **ProGuard/R8**: Code obfuscation enabled for release builds
- **String Encryption**: Sensitive strings are encrypted and decrypted at runtime
- **Native Code**: Critical algorithms implemented in C++ (JNI)
- **Root Detection**: App detects rooted devices and warns users

### 4. Runtime Security
- **Debugger Detection**: App detects if debugger is attached
- **Emulator Detection**: Warns if running on emulator
- **Signature Verification**: Checks APK signature matches expected value
- **Certificate Pinning**: Network requests use certificate pinning (if applicable)

### 5. Permissions
- **Runtime Permissions**: All dangerous permissions requested at runtime
- **Minimal Permissions**: Only necessary permissions are requested
- **Permission Rationale**: Clear explanation provided for each permission

## Creating Release Keystore

### Step 1: Generate Keystore
```bash
keytool -genkey -v -keystore freehands-release.keystore \
  -alias freehands-release-key \
  -keyalg RSA \
  -keysize 2048 \
  -validity 10000
```

You will be prompted for:
- Keystore password (choose a strong password)
- Key password (can be same as keystore password)
- Your name, organization, city, state, country

**IMPORTANT**: 
- Keep your keystore file safe! If you lose it, you cannot update your app on Play Store
- Never commit keystore file to version control
- Store keystore password in a secure password manager

### Step 2: Configure keystore.properties

1. Copy `keystore.properties.example` to `keystore.properties`:
   ```bash
   cp keystore.properties.example keystore.properties
   ```

2. Edit `keystore.properties` with your actual values:
   ```properties
   storeFile=freehands-release.keystore
   storePassword=YOUR_ACTUAL_PASSWORD
   keyAlias=freehands-release-key
   keyPassword=YOUR_ACTUAL_PASSWORD
   ```

3. Verify `keystore.properties` is in `.gitignore`:
   ```bash
   echo "keystore.properties" >> .gitignore
   ```

### Step 3: Build Secure Release

```bash
./gradlew clean assembleSecureRelease
```

This command:
1. Verifies `keystore.properties` exists
2. Checks that minification is enabled
3. Builds signed, obfuscated release APK

Output will be in: `app/build/outputs/apk/release/app-release.apk`

## Verifying APK Signature

After building, verify the APK is properly signed:

```bash
# On Windows (PowerShell)
& "C:\Program Files\Android\Android Studio\jbr\bin\keytool" -printcert -jarfile app\build\outputs\apk\release\app-release.apk

# On Linux/Mac
keytool -printcert -jarfile app/build/outputs/apk/release/app-release.apk
```

Expected output should show:
- Certificate fingerprints (SHA-256, SHA-1, MD5)
- Owner details matching your keystore info
- Valid from/until dates

## Checking APK for Security Issues

### 1. ProGuard Mapping
After release build, check mapping file:
```
app/build/outputs/mapping/release/mapping.txt
```

Keep this file safe - you'll need it to decode crash reports!

### 2. APK Analysis
Use Android Studio APK Analyzer:
1. Build > Analyze APK
2. Select `app-release.apk`
3. Check for:
   - No unobfuscated class names
   - No debug symbols
   - Proper signing

### 3. Security Scan
```bash
# Install MobSF (Mobile Security Framework) or use online scan
# https://github.com/MobSF/Mobile-Security-Framework-MobSF
```

## Updating Vosk Models Securely

### Model Storage
- Models are stored in `app/src/main/assets/models/`
- Models are verified using SHA-256 checksum on first load
- Corrupted models are rejected

### Updating Models

1. Download new model from https://alphacephei.com/vosk/models
2. Verify SHA-256 checksum:
   ```bash
   sha256sum vosk-model-small-ru-0.22.zip
   ```
3. Extract to `app/src/main/assets/models/`
4. Update checksum in `ModelVerifier.kt`
5. Test thoroughly before release

## CI/CD Security

### GitHub Actions Setup

1. Store keystore in GitHub Secrets:
   ```bash
   base64 freehands-release.keystore > keystore.base64
   ```

2. Add secrets in GitHub repository settings:
   - `KEYSTORE_FILE`: Contents of `keystore.base64`
   - `KEYSTORE_PASSWORD`: Your keystore password
   - `KEY_ALIAS`: Your key alias
   - `KEY_PASSWORD`: Your key password

3. Workflow will decode and use keystore:
   ```yaml
   - name: Decode Keystore
     run: echo "${{ secrets.KEYSTORE_FILE }}" | base64 -d > freehands-release.keystore
   ```

## Runtime Security Checks

### Signature Verification
App checks its own signature on startup:

```kotlin
SecurityManager.verifyAppSignature(context)
```

If signature doesn't match expected value:
- Critical features are disabled
- User is warned about tampering
- App may exit

### Expected Signature
After building your release APK, get your certificate fingerprint:

```bash
keytool -list -v -keystore freehands-release.keystore -alias freehands-release-key
```

Copy the SHA-256 fingerprint and update in `SecurityManager.kt`:

```kotlin
private const val EXPECTED_SIGNATURE = "YOUR_SHA256_FINGERPRINT_HERE"
```

## Incident Response

### If Keystore is Compromised
1. Generate new keystore immediately
2. Build new signed APK
3. If app is on Play Store:
   - Contact Google Play support
   - Publish update with new signature (may require new package name)
4. Notify users to update

### If Source Code Leaks
1. Change all API keys/secrets immediately
2. Review leaked code for vulnerabilities
3. Build patched version with updated secrets
4. Consider making repository private

### If Vulnerability Discovered
1. Assess severity and impact
2. Develop and test fix
3. Build patched release
4. Notify users if critical
5. Update security documentation

## Best Practices

### Development
- Never commit secrets to version control
- Use different keystores for debug and release
- Test security features regularly
- Keep dependencies updated
- Review ProGuard rules for leaks

### Production
- Monitor crash reports (Crashlytics/Sentry)
- Enable Google Play App Signing
- Use staged rollouts for updates
- Collect and review security logs
- Regular security audits

### User Privacy
- Clearly document data collection
- Provide privacy policy
- Allow users to delete their data
- Minimize data collection
- Never share user data without consent

## Security Checklist

Before releasing a new version:

- [ ] Keystore file is secure and backed up
- [ ] `keystore.properties` not committed to git
- [ ] ProGuard/R8 enabled for release
- [ ] `android:allowBackup="false"` set
- [ ] `android:debuggable="false"` for release
- [ ] All API keys stored securely
- [ ] Signature verification implemented
- [ ] Sensitive data encrypted
- [ ] Permission requests justified
- [ ] Security scan passed
- [ ] APK analyzed for issues
- [ ] Mapping file saved
- [ ] Release notes reviewed
- [ ] Privacy policy updated
- [ ] Test on multiple devices

## Resources

- [Android Security Best Practices](https://developer.android.com/topic/security/best-practices)
- [OWASP Mobile Security](https://owasp.org/www-project-mobile-security/)
- [Android Keystore System](https://developer.android.com/training/articles/keystore)
- [ProGuard Manual](https://www.guardsquare.com/manual/home)
- [Google Play App Signing](https://support.google.com/googleplay/android-developer/answer/9842756)

## Support

For security issues, contact: security@freehands.app

**DO NOT** post security vulnerabilities in public issue trackers!

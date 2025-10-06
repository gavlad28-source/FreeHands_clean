# FreeHands - Offline Voice Assistant for Android

<p align="center">
  <img src="docs/logo.png" alt="FreeHands Logo" width="200"/>
</p>

<p align="center">
  Полностью офлайн голосовой ассистент для Android с wake word detection и системными командами
</p>

<p align="center">
  <a href="#features">Features</a> •
  <a href="#installation">Installation</a> •
  <a href="#building">Building</a> •
  <a href="#usage">Usage</a> •
  <a href="#security">Security</a> •
  <a href="#contributing">Contributing</a>
</p>

---

## 🎙️ Features

### Core Functionality
- ✅ **Offline Speech Recognition** using Vosk ASR
- ✅ **Wake Word Detection** - "Привет, брат" для активации
- ✅ **Foreground Service** - работает в фоне постоянно
- ✅ **Local Text-to-Speech** - голосовые ответы
- ✅ **System Commands** - управление телефоном голосом

### System Control
- 📶 **Wi-Fi Toggle** - включение/выключение Wi-Fi
- 📡 **Bluetooth Toggle** - управление Bluetooth
- 🔆 **Brightness Control** - изменение яркости экрана
- 🔕 **Do Not Disturb** - режим "Не беспокоить"
- 📱 **App Launch** - запуск приложений
- 📞 **Phone Calls** - звонки с подтверждением
- 💬 **SMS** - отправка сообщений

### Security Features
- 🔐 **Release Signing** - подпись релизных сборок
- 🔒 **Code Obfuscation** - ProGuard/R8
- 🔑 **Data Encryption** - AES-256-GCM через Android Keystore
- ✅ **Signature Verification** - проверка целостности APK
- 🚫 **Root Detection** - обнаружение root-доступа
- 🛡️ **No Backup** - запрет облачных бэкапов
- 🔐 **Encrypted Preferences** - шифрованные настройки

## 📋 Requirements

### Development
- Android Studio Hedgehog (2023.1.1) или новее
- JDK 21
- Android SDK 34
- Kotlin 2.2.0
- Gradle 8.x

### Runtime
- Android 8.0 (API 26) или выше
- Минимум 2 GB RAM
- 150 MB свободного места (включая модели)
- Микрофон

## 🚀 Installation

### Option 1: Download Release APK
1. Перейдите в [Releases](https://github.com/yourusername/freehands/releases)
2. Скачайте последний `app-release.apk`
3. Установите APK на устройство
4. Разрешите установку из неизвестных источников

### Option 2: Build from Source
См. [Building](#building) секцию ниже

## 🔨 Building

### 1. Clone Repository
```bash
git clone https://github.com/yourusername/freehands.git
cd freehands
```

### 2. Download Speech Models

**Vosk Model** (Required):
```bash
# Download Russian small model (~40 MB)
curl -LO https://alphacephei.com/vosk/models/vosk-model-small-ru-0.22.zip
unzip vosk-model-small-ru-0.22.zip
mkdir -p app/src/main/assets/models/vosk/
mv vosk-model-small-ru-0.22 app/src/main/assets/models/vosk/
```

Подробные инструкции: [README_ASSETS.md](README_ASSETS.md)

### 3. Create Keystore

For **debug** builds (no keystore needed):
```bash
./gradlew assembleDebug
```

For **release** builds:

#### Generate keystore:
```bash
keytool -genkey -v -keystore freehands-release.keystore \
  -alias freehands-release-key \
  -keyalg RSA -keysize 2048 -validity 10000
```

#### Create keystore.properties:
```bash
cp keystore.properties.example keystore.properties
# Edit keystore.properties with your actual values
```

**⚠️ IMPORTANT**: Never commit `keystore.properties` or `.keystore` files!

Подробные инструкции: [README_SECURITY.md](README_SECURITY.md)

### 4. Build APK

#### Debug Build:
```bash
./gradlew clean assembleDebug
```
Output: `app/build/outputs/apk/debug/app-debug.apk`

#### Release Build:
```bash
./gradlew clean assembleSecureRelease
```
Output: `app/build/outputs/apk/release/app-release.apk`

This task automatically:
- ✅ Checks keystore.properties exists
- ✅ Verifies minification is enabled
- ✅ Signs APK with release key
- ✅ Obfuscates code with R8

### 5. Install on Device

#### Via ADB:
```bash
adb install app/build/outputs/apk/release/app-release.apk
```

#### Via Android Studio:
1. Open project in Android Studio
2. Connect device via USB
3. Click Run ▶️

## 📱 Usage

### First Launch

1. **Grant Permissions**:
   - Microphone (required)
   - Notifications (optional)
   - System settings (for commands)

2. **Start Service**:
   - Tap the play button on main screen
   - Service will start in foreground

3. **Test Wake Word**:
   - Say "Привет, брат"
   - Assistant should respond "Слушаю"

### Voice Commands

#### Wi-Fi & Connectivity
```
"Включи Wi-Fi"
"Выключи Bluetooth"
```

#### Display
```
"Увеличь яркость"
"Максимальная яркость"
"Уменьши яркость"
```

#### Do Not Disturb
```
"Включи не беспокоить"
"Выключи DND"
```

#### Apps
```
"Открой Chrome"
"Запусти камеру"
"Открой настройки"
```

#### Communication (with confirmation)
```
"Позвони +79123456789"
```

### Settings

Open settings from main screen to configure:
- Auto-start on boot
- Wake word sensitivity
- TTS speed and pitch
- Command confirmation
- Energy saving mode

## 🔒 Security

### Release Build Security

FreeHands implements multiple security layers:

1. **Code Obfuscation**: ProGuard/R8 removes debug info and renames classes
2. **String Encryption**: Sensitive strings encrypted at compile time
3. **Signature Check**: App verifies its own signature on startup
4. **No Debug**: Debug builds disabled in production
5. **Encrypted Storage**: All sensitive data encrypted with AES-GCM
6. **No Backup**: Cloud backup disabled for security

### Command Confirmation

Critical commands require user confirmation:
- Phone calls
- SMS sending
- System settings changes (on some devices)

You can configure confirmation level in Settings.

### Privacy

- ✅ **100% Offline** - no data sent to servers
- ✅ **No Analytics** - no tracking
- ✅ **No Ads** - completely free
- ✅ **Open Source** - auditable code
- ✅ **Local Storage** - all data stays on device

See [README_SECURITY.md](README_SECURITY.md) for detailed security information.

## 🧪 Testing

### Run Unit Tests:
```bash
./gradlew test
```

### Run Instrumented Tests:
```bash
./gradlew connectedAndroidTest
```

### Test Coverage:
```bash
./gradlew testDebugUnitTest jacocoTestReport
```

## 📚 Documentation

- [README_SECURITY.md](README_SECURITY.md) - Security features and best practices
- [README_ASSETS.md](README_ASSETS.md) - Model setup and optimization
- [CHANGELOG.md](CHANGELOG.md) - Version history
- [CONTRIBUTING.md](CONTRIBUTING.md) - Contribution guidelines

## 🛠️ Technology Stack

### Speech Processing
- **[Vosk](https://alphacephei.com/vosk/)** - Offline speech recognition
- **Android TextToSpeech** - Voice responses
- **WebRTC VAD** - Voice activity detection

### Android
- **Kotlin** - Primary language
- **Jetpack Compose** - Modern UI
- **Coroutines** - Async processing
- **Hilt** - Dependency injection
- **Room** - Local database
- **WorkManager** - Background tasks

### Security
- **Android Keystore** - Key storage
- **AndroidX Security** - Encrypted preferences
- **R8** - Code optimization

## 🤝 Contributing

Contributions are welcome! Please read [CONTRIBUTING.md](CONTRIBUTING.md) first.

### Development Setup
```bash
git clone https://github.com/yourusername/freehands.git
cd freehands
./gradlew clean build
```

### Code Style
- Follow [Kotlin coding conventions](https://kotlinlang.org/docs/coding-conventions.html)
- Use Android Studio formatter
- Add KDoc comments for public APIs

### Submitting Changes
1. Fork the repository
2. Create feature branch (`git checkout -b feature/amazing-feature`)
3. Commit changes (`git commit -m 'Add amazing feature'`)
4. Push to branch (`git push origin feature/amazing-feature`)
5. Open Pull Request

## 📄 License

This project is licensed under the MIT License - see [LICENSE](LICENSE) file for details.

### Third-Party Licenses

- **Vosk** - Apache 2.0 License
- **Porcupine** - Apache 2.0 (Community Edition)
- **Android Libraries** - Apache 2.0 License

See [THIRD_PARTY_LICENSES.md](THIRD_PARTY_LICENSES.md) for complete list.

## 🙏 Acknowledgments

- [Vosk Speech Recognition](https://alphacephei.com/vosk/) - Amazing offline ASR
- [Picovoice Porcupine](https://picovoice.ai/) - Wake word detection
- Android Open Source Project

## 📞 Support

- 📧 Email: support@freehands.app
- 🐛 Issues: [GitHub Issues](https://github.com/yourusername/freehands/issues)
- 💬 Discussions: [GitHub Discussions](https://github.com/yourusername/freehands/discussions)

## 🗺️ Roadmap

### v1.1
- [ ] Custom wake word training
- [ ] Multi-language support
- [ ] Smart home integration

### v1.2
- [ ] Conversation context
- [ ] Custom command scripting
- [ ] Voice authentication

### v2.0
- [ ] Plugin system
- [ ] Cloud sync (optional)
- [ ] Wear OS support

## ⭐ Star History

If you find this project useful, please consider giving it a star!

---

<p align="center">
  Made with ❤️ by the FreeHands team
</p>

# Changelog

All notable changes to FreeHands Assistant will be documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.0.0/),
and this project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

## [1.0.0] - 2024-01-15

### Added
- 🎙️ Offline speech recognition using Vosk
- 🔊 Wake word detection ("привет, брат")
- 🔄 Foreground service for background operation
- 🗣️ Local text-to-speech responses
- 📱 System commands:
  - Wi-Fi toggle
  - Bluetooth toggle
  - Brightness control
  - Do Not Disturb mode
  - App launching
  - Phone calls (with confirmation)
- 🔐 Security features:
  - Release signing
  - Code obfuscation (R8)
  - AES-256-GCM encryption
  - Signature verification
  - Root detection
- ⚙️ Settings screen:
  - Auto-start configuration
  - Wake word sensitivity
  - TTS settings
  - Command confirmation
- 📊 Command history
- 🎨 Modern Material Design UI with Jetpack Compose
- 📖 Comprehensive documentation
- ✅ Unit and instrumented tests

### Security
- Android Keystore integration
- Encrypted SharedPreferences
- No cloud backup
- Debug builds disabled for release
- Secure build pipeline

### Documentation
- README with setup instructions
- Security guide (README_SECURITY.md)
- Assets guide (README_ASSETS.md)
- Contribution guidelines
- Third-party licenses

## [Unreleased]

### Planned for v1.1
- Custom wake word training
- Multi-language support (English, Ukrainian)
- Smart home integration (Home Assistant, Google Home)
- Voice profiles for multiple users
- Offline command history sync

### Planned for v1.2
- Conversation context memory
- Custom command scripting
- Voice authentication
- Widgets support
- Tasker integration

### Planned for v2.0
- Plugin system
- Optional cloud sync
- Wear OS companion app
- Desktop client
- REST API for automation

---

## Version History

- **1.0.0** - Initial release (2024-01-15)

## Migration Guide

### From Pre-release to 1.0.0

This is the first stable release. No migration needed.

## Known Issues

### v1.0.0
- Android 10+ requires manual Wi-Fi/Bluetooth toggle via settings panel
- Large model file increases APK size significantly
- TTS may not work on some devices without language data
- Wear OS not yet supported

### Workarounds
- Use smaller Vosk model to reduce APK size
- Download TTS language data from device settings
- Use App Bundle (AAB) for Play Store distribution

## Breaking Changes

None in v1.0.0 (initial release)

## Deprecations

None in v1.0.0 (initial release)

## Contributors

Thanks to all contributors who helped with this release!

## Support

For issues, questions, or feature requests:
- GitHub Issues: https://github.com/yourusername/freehands/issues
- Email: support@freehands.app

# Zero Keyboard
Master functionality in a masterpiece app.

Offline-first Arabic/English Android IME. Initial engineering baseline, not a release. See [requirements](REQUIREMENTS.md), [tracker](IMPLEMENTATION_TRACKER.md), [research](RESEARCH_REPORT.md).

Build with JDK 17 and Android SDK 35 using `./gradlew :app:testDebugUnitTest :app:lintDebug :app:assembleDebug`. No signed APK exists yet. The app has no INTERNET permission. RECORD_AUDIO is used only after explicit user action, runtime permission, a verified local model and an eligible editor. Do not distribute debug artifacts as releases.

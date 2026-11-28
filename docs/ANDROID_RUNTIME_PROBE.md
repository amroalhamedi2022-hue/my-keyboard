# Android runtime probe — 2026-11-28
Work's TEST_REPORT.md records a successful 8.9 MB debug build, 9 JUnit cases, clean lint, 7 Python tests and 23 JVM assertions. The screenshot from the owner shows an emulator boot in progress and Work allowance exhausted; it is not evidence of installation or runtime behavior. Current chat environment has java/javac but no adb/Gradle/emulator and GitHub remote DNS fails. Connector writes work. No runtime checks claimed here.
A debug-only test Activity supplies synthetic normal/password/numeric/private fields. It deliberately enables optional features in the test installation. No release component or private data is involved. The runner requires a disposable, fully booted emulator and an APK containing this new debug Activity. It does not invoke Gradle. Reuse one milestone build; a previous APK lacking the probe cannot run this script.
Commands:
    python3 -m unittest discover -s research/android -p 'test_*.py' -v
    ./gradlew :app:assembleDebug
    python3 research/android/ime_probe.py --serial emulator-5554 --apk app/build/outputs/apk/debug/app-debug.apk --report /tmp/zero-probe.json
Run only on a disposable emulator with synthetic data. Review results and fix failing checks; no microphone/model inference, actual glide accuracy, password-provider compatibility or latency claimed. UIAutomator availability and accessibility nodes can vary by device. No real screenshots created.

## Independent CI runtime milestone
Added .github/workflows/runtime-validation.yml, explicitly triggered by its own marker or manual dispatch, not every commit. Uses GitHub-hosted Ubuntu with KVM and a pinned ReactiveCircus emulator runner v2.34.0 (1dcd0090116d15e7c562f8db72807de5e036a4ed), API33 x86_64 Google APIs image. Builds once, checks merged APK permission dump for INTERNET, runs debug probe and uploads synthetic normal-field screenshot, JSON, build reports and debug APK separately. No actual run observed yet. Prior Work emulator is not accessible from normal chat. GitHub connector's workflow lookup only returns PR-triggered runs; lack of results does not prove push workflow did not run. See Actions directly.

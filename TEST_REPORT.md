# Test report — 2026-11-27
Verified read-back of GitHub files at e36ebcc0617158f9d57130eefbbb3fda3577572d. Local copies matched Git blob hashes:
- score.py 7396c60bd9d899a13f51c43003be4a7d855394ae
- test_score.py e1b6b77265d12626c1ea5d79c01b630bbdf449e5
- GeometricRanker.java 4fe568db32b42a00f89c38a586dee871a1af8596
- KeyboardLayout.java 07d04cc57933fe3485095b693b24f01bd8fc624c

Executed locally: python -m unittest discover -s research/asr -p 'test_*.py' -v -> 3 tests OK (exact mixed transcript, Latin loss, duplicate IDs). javac -encoding UTF-8 -d out GeometricRanker.java KeyboardLayout.java Smoke.java && java -ea -cp out Smoke -> PASS: 7 JVM smoke assertions. Reproduce from repo root with javac -encoding UTF-8 -d /tmp/zero-out app/src/main/java/com/zerokeyboard/ime/KeyboardLayout.java app/src/main/java/com/zerokeyboard/ime/glide/GeometricRanker.java research/glide/Smoke.java && java -ea -cp /tmp/zero-out Smoke.

These are synthetic unit/smoke tests only. No model inference, no real glide traces, no Android SDK/Gradle/adb in local environment, no device test, no Android APK or Android build. Android JUnit fixtures and CI are not executed. GitHub connector supports repo writes; no new workflow dispatch action identified. Local git remote DNS failed. PR remains draft.

## Batch 2
Glide touch integration and settings authored; Android compile, dispatch and composing behavior not verified. Prior smoke results cover old pure components only. No new test pass asserted until execution evidence exists.

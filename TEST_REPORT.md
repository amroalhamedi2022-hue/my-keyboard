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

## Batch 2 local ASR adapter tests
Executed python -m unittest -v on test_run_whisper.py with 4 passing tests: bad stereo format, SHA-256, mono PCM duration, mixed-script JSON parsing. Synthetic test fixtures only. Local git hashes run_whisper.py 4d5d627586f0bbf4af0b9cdcfd8273c0c1dae607; test_run_whisper.py f70a66de2f852bd4decf1439b48e1d06ec4c65e5. Actual model inference, Android compile and integration still pending. Local environment javac available, Gradle/SDK not found; git remote DNS failed again. Milestone workflow trigger added, outcome not yet observed.

Batch 3: checked prior commit combined status and PR workflow runs; no results returned. Native integration authored, not Android compiled or executed. Source verification and unit tests to be recorded separately.

Batch 3 local catalog smoke: javac and java -ea executed successfully, 7 assertions passed. Exact Git blob of tested ModelCatalog.java: 59d60984c9fe217547a5d65915fd6d08d02c833d, verified against repository. CatalogSmoke.java local blob 62f53240c1d2fd2a3f978a76f5808c9c6896b129. Reproduce: javac -encoding UTF-8 -d /tmp/zero-catalog app/src/main/java/com/zerokeyboard/ime/voice/ModelCatalog.java research/voice/CatalogSmoke.java && java -ea -cp /tmp/zero-catalog CatalogSmoke. Checks count, lookup for both allowlisted digests, unknown/null rejection, digest shape and non-.en names. Does not verify actual model bytes or recognition. Native/Android code remains uncompiled. Local environment command -v found javac/java only, no gradle/adb or SDK path.

Batch 04: pinned upstream header read and abort_callback fields verified. LayoutSmoke.java authored; execution evidence will be appended separately. No Android/JNI tests claimed.

Batch 04 local execution: javac 21.0.12.1 -encoding UTF-8 compiled LayoutProfile.java and LayoutSmoke.java. java -ea -cp out LayoutSmoke returned PASS: 9 layout assertions. Git hash-object exactly matched remote LayoutProfile.java c7330edc1fc18c78e9bff6f75c8c96858c269f58 and LayoutSmoke.java f41753d0bbc6125a9ab583ede4d30914a6ef308a. Covers split fallback, wide split geometry, left/right inset, clamped height, Arabic-Indic conversion and number row. No device layout or Android/JNI runtime claims.

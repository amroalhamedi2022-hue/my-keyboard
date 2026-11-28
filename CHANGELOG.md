# Changelog

## Unreleased — Batch 5

- Added Android 11+ provider-rendered Inline Autofill with stale-response isolation and an independent setting.
- Added bilingual setup resources, a provisional launcher icon and explicit backup/transfer exclusion.
- Added optional silence endpointing and trimming for local dictation, including deterministic unit tests.
- Hardened model installation with exact byte sizes, no-follow verification, synchronized mutation, stale-stage cleanup and atomic-move fallback.
- Cached glide templates by vocabulary and exact layout geometry, and added Arabic alef geometry normalization.
- Pinned the whisper.cpp source archive by URL and SHA-256 and packaged its MIT notice.
- Added a checksum-verified Gradle wrapper and reached a clean build, unit-test and lint milestone.
## 0.1.0-dev (unreleased)
Initial repository, requirement register, manual CI configuration, Android IME scaffold, Arabic/English tap layouts and setup flow. Not built or device verified.

- Batch 2: experimental touch-to-glide integration, candidate strip, composing replacement, trail, original seed words and persistent per-language/privacy preferences. Android validation pending.

- Batch 3: experimental local native voice integration, SHA-256 allowlisted Tiny/Base model import, selection/removal, explicit mic permission, bounded recording, stale-editor guard. Not compiled or device-tested.

- Batch 04: native cooperative cancellation, voice review, adaptive profiles, number row/Arabic-Indic digits, editor toolbar and spacebar cursor gestures. Android/JNI verification pending.

- Batch 06: explicit personal bilingual glide vocabulary, bounded candidate ranking, optional session clipboard and original emoji seed panel. Pure Java lexicon and clipboard checks passed; new Android integration unverified.

- Batch 07: optional bounded frequency dictionary import and deletion, background glide ranking, voice language/threads/endpoint controls and pause/resume. 13 pure-JVM checks passed, Android/JNI runtime unverified.

- Batch 07 follow-up: optional continuous segmented capture with live completed-chunk previews, bounded queue and within-session native weights reuse. 17 synthetic JVM checks pass; native/Android runtime pending.

# Changelog

## 0.2.3-engine-preview (105) — 2026-11-28

- Added a system permission flow for the microphone and a verified bundled
  multilingual Tiny model. Voice controls are inside the keyboard, with a
  recording indicator, pause, stop, review and discard.
- Added adaptive settings navigation, a tablet typing field, and Zero Glass
  light and dark themes. The phone keeps the single-pane settings view.
- Hardened private editor behavior, added deletion of all saved clipboard
  entries including pins, and cancelled obsolete glide ranking.
- Android 13 evaluation passed nine checks, including permission handling,
  Arabic and English touch input, dictation discard, English recording
  replay through the emulator microphone, editor insertion and hidden
  microphone controls in a password field.
- Release upgrade from 0.2.2 and fresh setup passed all fifteen launch checks
  with isolated CI signatures. The owner-signed APK has identical package
  payload entries and the same v3 signing certificate as 0.2.2.
- This preview still has a dictation word-boundary issue beside existing
  text. English replay-to-observed-insertion took 106.91 seconds on the
  emulator. Palestinian accuracy and physical Pad 5 performance remain open.

| Release identifier | Value |
|---|---|
| Application ID | `com.zerokeyboard.ime` |
| Production source | `a3c8160fd1184e790426dd73f04ca3dfa0010c31` |
| Owner certificate SHA-256 | `a0c2454c1ec3b80e0e20def4c94d9e31e8dc52368a589cf7299e97c6ed087afe` |
| Signed APK SHA-256 | `454e0c05e8e71e4df78d505eaa948f4c9f36b65d073e7f9cbe3a5ca91cd1f4e5` |
| Corresponding source SHA-256 | `e55df05707f7f17e6141d55b54ae61f35c602e7a2a80d68edb5d6b0183c22a54` |

- [Android microphone and editor results](https://github.com/amroalhamedi2022-hue/my-keyboard/actions/runs/46179051428)
- [Release upgrade and fresh-install results](https://github.com/amroalhamedi2022-hue/my-keyboard/actions/runs/46179952395)

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

- Batch 07 tools: bilingual offline emoji search and limited skin tones, preliminary semantic theme tokens and key/trail customization, optional feedback, clipboard expiry/privacy hardening, and microphone cleanup ordering. 52 pure-JVM checks pass; Android validation pending.

- Owner alpha milestone: SDK setup fix from actual CI logs, version code 100, stable release certificate and offline signing path, caps lock, repeat backspace, quick punctuation and stricter no-personalization toolbar policy. Build and device validation pending.

- Alpha02: compact three-row keyboard, consolidated toolbar/candidates, improved key placement and styling, alternate characters, expandable settings categories, cached gesture geometry. Build/runtime pending.

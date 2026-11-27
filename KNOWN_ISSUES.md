# Known issues
Baseline only: experimental glide only, no voice, suggestion strip, clipboard, inline autofill, settings persistence, tablet split or premium themes. Numeric layout provisional; Arabic geometry and key fit must be validated. Delete key handling, editor actions, lifecycle, Android 15+ insets and accessibility require real integration testing. No signed APK. No Android SDK locally verified.

Batch 2: seed vocabulary has narrow coverage; ranking threshold uncalibrated; composition replacement depends on editor selection callbacks; no Android integration tests executed. No repeated-letter or neural recognition, live candidate preview, split or contextual rank yet.

Batch 3 supersedes "no voice" with experimental bounded local voice source integration, not verified on device. No continuous dictation, streaming, true VAD, verified code-switch accuracy, download manager or model warm cache. Arm64/x86_64 only, no release. Native cancellation currently discards result but does not interrupt inference.

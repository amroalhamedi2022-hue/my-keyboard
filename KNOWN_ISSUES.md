# Known issues
Baseline only: experimental glide only, no voice, suggestion strip, clipboard, inline autofill, settings persistence, tablet split or premium themes. Numeric layout provisional; Arabic geometry and key fit must be validated. Delete key handling, editor actions, lifecycle, Android 15+ insets and accessibility require real integration testing. No signed APK. No Android SDK locally verified.

Batch 2: seed vocabulary has narrow coverage; ranking threshold uncalibrated; composition replacement depends on editor selection callbacks; no Android integration tests executed. No repeated-letter or neural recognition, live candidate preview, split or contextual rank yet.

Batch 3 supersedes "no voice" with experimental bounded local voice source integration, not verified on device. No continuous dictation, streaming, true VAD, verified code-switch accuracy, download manager or model warm cache. Arm64/x86_64 only, no release. Native cancellation currently discards result but does not interrupt inference.

Batch 04: native abort callback added, not compiled/tested; model loading still cannot be interrupted. Adaptive layouts and editor toolbar authored; no floating layout or device validation. Voice review optional, text held in memory until insert/discard/cancellation; simultaneous manual typing still cancels. Earlier limitations remain unless explicitly superseded.

Batch 06: personal dictionary is limited to 512 words/language; no production lexicon. Clipboard explicitly saved plain text only, session RAM only; sensitive heuristics incomplete, pinned entries expire at session end. Emoji seed limited, no search or skin tones. Android integration and optimized ranker unverified.

Batch 07: imported frequency resources optional, max 8k/language; no bundled production list, no frequency/context score; pending swipe superseded by next swipe before result; Android latency unverified. Pause/resume and language controls written but not run; 30 s audio/120 s wall cap, no streaming or validated code-switch accuracy. No APK generated in this chat.

Batch 07 follow-up supersedes absence of continuous mode at source level: segmented previews only, not verified on device; 12s segment cap/5min wall limit, overflow stops capture, no token-level streaming. No neural VAD or actual code-switch benchmark. JNI no-state API path not compiled yet.

Batch 07 tools: 96 original emoji metadata entries, limited skin tones, not complete CLDR coverage; preliminary palette/ripple only, no final visual approval or screenshot. Credential patterns are conservative and incomplete. Android expiry timer and all new UI/device behavior unverified.

Owner alpha: actual CI failed at legacy SDK tools setup; correction pending rerun. New release key differs from previous debug signer. No signed alpha APK yet; Android integration and voice/glide benchmarks pending.

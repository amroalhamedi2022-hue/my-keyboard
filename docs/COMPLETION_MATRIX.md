# Zero Keyboard completion matrix

Baseline on 2026-11-28. The mature line uses HeliBoard v3.6 at
`f8ec91a982835911cc224fa99434c3396d4cd562`. Version 0.2.3, code 105,
is an engineering candidate. Status describes the current evidence for each
feature. Previous emulator results apply to their exact source and payload.

| Workstream | Feature | State | Evidence or remaining gate |
|---|---|---|---|
| A | English and Arabic taps, delete, language switching | Tested with limitations | Actual Android 13 emulator touch test on earlier mature commit. Retest 105 and rapid taps. |
| A | Shift, caps, repeat delete, field-aware actions, numeric, email and URL layouts | Implemented but untested | Mature core paths. Exercise editor actions, selection, rotation, composition and rich text. |
| B | Local microphone permission, immediate editor return, bundled Tiny provisioning, missing Base fallback | Implemented but untested | Candidate 105 adds permission and capture smoke. Permission result requires a second explicit tap. |
| B | Capture, offline Whisper, review, pause, discard, continuous segments | Tested with limitations | Existing host inference and recovery tests. Actual Android audio-to-editor accuracy and lifecycle remain open. |
| B | Microphone activity, day/night voice panel, advanced controls | Implemented but untested | Candidate 105 adds a throttled level meter. Capture real screenshots and verify accessibility. |
| C | Arabic and English suggestions, next-word, optional correction, user dictionary | Implemented but untested | Mature dictionaries and correction defaults. Run word-ranking, correction-off and backup cases. |
| C | Palestinian and other informal dialect ranking, phrase and mixed-language evaluation | In development | Earlier host ASR and lexicon work. Collect consented typing samples and score actual candidate acceptances. |
| C | Personal learning and dictionary management | Implemented but untested | Defaults opt out. Candidate 105 hardens private fields. Verify manual deletion, imports and upgrades. |
| D | Open Arabic/English glide | Tested with limitations | Host admission checks and English trace research. Runtime traces, Arabic accuracy and alternatives remain open. Default remains off. |
| D | Spacebar cursor movement and swipe-to-delete | Implemented but untested | Mature settings and paths. Test conflicts with glide, tap and multi-touch. |
| E | Adaptive light/dark and two Zero Glass palettes | Implemented but untested | Candidate 105 adds rounded defaults to existing renderer. Check contrasts, legacy prefs, portrait and landscape screenshots. |
| E | Key motion, trail, visual consistency across symbols, panels and settings | In development | Capture matching screens on target tablet and phone. No compositor measurement. |
| F | Settings categories, search and themes | Implemented but untested | Mature settings shipped. Confirm each changed control affects the IME. |
| F | Adaptive two-pane settings and full bilingual UI | In development | Arabic theme and permission resources added. Layout and complete localization remain open. |
| G | Clipboard text, pinning, history and privacy | Tested with limitations | 0.2.2 includes hardening and passed startup gate. Candidate 105 adds explicit deletion of all saved clips, including pins, and stale-row guards. Runtime retention and editor-switch tests remain open. |
| G | Editing tools and reorderable toolbar | Implemented but untested | Existing upstream actions and reorder UI. Validate undo/redo, overflow and private behavior. |
| G | Image clipboard, opt-in screenshot insertion, history search and batch editing | Not started | Require editor MIME support, URI grants and explicit owner access. |
| H | Emoji categories, recents, symbols and Arabic punctuation | Implemented but untested | Mature engine. Evaluate Arabic emoji queries, favorites, skin tones and more symbols. |
| I | Voice preparation and retry cleanup | Implemented but untested | Model modification serialized off UI. Completion publishes after worker cleanup. |
| I | Input latency, frame time, memory, thermal and battery | Not started | No Perfetto or other device measurements for candidate 105. |
| J | Offline package and privacy defaults | Tested with limitations | Prior signed APK permission evidence. Generated 105 manifest preflight passes. Audit final APK. |
| J | Private fields and incognito learning guards | Implemented but untested | Candidate 105 extends policy to editor classification and restart. Plain text OTPs need an editor privacy flag to be identifiable. |
| J | Data deletion and dependency review | In development | Existing private storage, dependency notes and no proprietary glide loader. Runtime deletion and final license check remain. |
| K | Phone and tablet layouts, compact portrait defaults, split and height | Implemented but untested | Compact 0.82 default and existing split support. Measure Xiaomi Pad 5 with matched layout and editor. |
| K | Floating and one-handed experience | Not started | Evaluate feasibility and verify on real hardware. |
| L | Prior 0.2.2 installation, update and startup | Verified | Android 13 CI run 46145726737 passed 15 startup checks using test-signed copies. Owner APK signature verified independently. |
| L | 0.2.3 compile, emulator, real tablet visual and sound gates | In development | Debug gate covers bilingual touches, mic permission, capture, discard and one verified public English recording replayed through the emulator microphone. Broader recognition accuracy remains open. |
| L | 0.2.3 owner signature and distribution | Blocked | Depends on a validated release payload and the owner key. No candidate has been issued as an owner update. |
| L | Local backup compatibility and safe migration | In development | Prior upgrade preserved existing data paths. Test versioned settings, themes, synonyms and pinned clipboard opt-in. |

## Branch and release evidence

- Baseline branch heads were `main@fe5d4f7`, `feature/zero-ime-foundation@f3ce80a`,
  `hotfix/zero-0.2.1-launch@2abd3a2` and `feature/zero-mature-core@bdea974`.
  PRs #1 through #3 were open drafts. The mature release preparation already includes
  the startup fix and retains process diagnostics. No hotfix branch merge was needed.
- The downloaded 0.2.2 owner APK matched the recorded SHA-256
  `e52ef4de60ff3754f315e2a825b454c786f561653f3a5cd9e0b2eb2e12fcab26`.
  Package `com.zerokeyboard.ime`, code 104, owner certificate SHA-256
  `a0c2454c1ec3b80e0e20def4c94d9e31e8dc52368a589cf7299e97c6ed087afe`.
  Exact 0.2.2 release source is `35da25d90a535f4c4455147de606bdbecb7ebb23`.
- Reproduce candidate source with the pinned HeliBoard checkout and
  `research/rebase/prepare_candidate.py`, `attach_voice.py`, `attach_glide.py`,
  and `check_prepared.py`. The workflow pins and verifies Tiny. It gates the
  unsigned release on updating from the actual 0.2.2 CI payload and fresh setup.
- [Mature core architecture and evidence](MATURE_CORE_MIGRATION.md)
- [Owner comparison and visual acceptance criteria](OWNER_GBOARD_COMPARISON.md)
- [Prior passing startup run](https://github.com/amroalhamedi2022-hue/my-keyboard/actions/runs/46145726737)

## Checks for the 105 changes

- Source generation from the pinned HeliBoard commit, voice and glide adapters,
  debug geometry injection, generated manifest preflight and whitespace check passed.
- Twelve Python tests, 17 gesture admission checks and 100 interrupted-decoder
  sessions passed on the host. Java editor policy and voice sources compiled
  against SDK 35. The bundled Tiny checksum and the prior APK's owner v3 signature
  were independently checked.
- The replay harness opens a loopback connection to the CI emulator's microphone
  input. It exercises AudioRecord, local native decoding and editor insertion.
  The input is Whisper.cpp's pinned public English sample. This test remains
  pending and is separate from Palestinian Arabic and mixed-language accuracy.

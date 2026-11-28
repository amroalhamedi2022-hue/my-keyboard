# Zero Keyboard completion matrix

Baseline on 2026-11-28. The mature line uses HeliBoard v3.6 at
`f8ec91a982835911cc224fa99434c3396d4cd562`. Version 0.2.3, code 105,
is an engineering candidate. Status describes the current evidence for each
feature. Previous emulator results apply to their exact source and payload.

| Workstream | Feature | State | Evidence or remaining gate |
|---|---|---|---|
| A | English and Arabic taps, delete, language switching | Tested with limitations | Actual Android 13 emulator touch test on earlier mature commit. Retest 105 and rapid taps. |
| A | Shift, caps, repeat delete, field-aware actions, numeric, email and URL layouts | Implemented but untested | Mature core paths. Exercise editor actions, selection, rotation, composition and rich text. |
| B | Local microphone permission, return to editor, bundled Tiny provisioning, missing Base fallback | Tested with limitations | Android 13 emulator showed grant and denial dialogs, returned to the editor with its text and reached recording after an explicit second tap. Missing Base fallback and Xiaomi hardware remain open. |
| B | Capture, offline Whisper, review, pause, discard, continuous segments | Tested with limitations | Production JNI decoded public English on the host and rejected a cancelled session. Tiny WER was 92.0% auto and 84.8% forced Arabic on 16 Palestine clips. Android insertion and better Arabic accuracy remain open. |
| B | Microphone activity, day/night voice panel, advanced controls | Implemented but untested | Candidate 105 adds a throttled level meter. Capture real screenshots and verify accessibility. |
| C | Arabic and English suggestions, next-word, optional correction, user dictionary | Implemented but untested | Mature dictionaries and correction defaults. Run word-ranking, correction-off and backup cases. |
| C | Palestinian and other informal dialect ranking, phrase and mixed-language evaluation | In development | Earlier host ASR and lexicon work. Collect consented typing samples and score actual candidate acceptances. |
| C | Personal learning and dictionary management | Implemented but untested | Defaults opt out. Candidate 105 hardens private fields. Verify manual deletion, imports and upgrades. |
| D | Open Arabic/English glide | Tested with limitations | Host admission, cancellation and top-K equivalence checks plus English trace research. Runtime traces, Arabic accuracy and alternatives remain open. Default remains off. |
| D | Spacebar cursor movement and swipe-to-delete | Implemented but untested | Mature settings and paths. Test conflicts with glide, tap and multi-touch. |
| E | Adaptive light/dark and two Zero Glass palettes | Implemented but untested | Candidate 105 adds rounded defaults to existing renderer. Check contrasts, legacy prefs, portrait and landscape screenshots. |
| E | Key motion, trail, visual consistency across symbols, panels and settings | In development | Capture matching screens on target tablet and phone. No compositor measurement. |
| F | Settings categories, search and themes | Implemented but untested | Mature settings shipped. Confirm each changed control affects the IME. |
| F | Adaptive two-pane settings | Tested with limitations | Android 13 emulator checks and screenshots confirmed the single-pane phone view, tablet preview, and Appearance details next to the selected category. Xiaomi hardware and landscape remain open. |
| F | Full bilingual UI | In development | Unmodified upstream resources and added Arabic labels are used where available. Voice settings and the full locale audit remain open. |
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
- Fifteen Python tests, 17 gesture admission checks and 100 interrupted-decoder
  sessions passed on the host. Java editor policy and voice sources compiled
  against SDK 35. The bundled Tiny checksum and the prior APK's owner v3 signature
  were independently checked.
- The replay harness opens a loopback connection to the CI emulator's microphone
  input. It exercises AudioRecord, local native decoding and editor insertion.
  The input is Whisper.cpp's pinned public English sample. This test remains
  pending and is separate from Palestinian Arabic and mixed-language accuracy.
- [First Android 13 candidate run](https://github.com/amroalhamedi2022-hue/my-keyboard/actions/runs/46150003100)
  compiled and passed real English, backspace and mixed Arabic/English touches.
  The screenshot shows a microphone icon. UiAutomator returned the fixture
  window alone, so the probe timed out locating the mic. Debug-only geometry
  now reads actual microphone and panel control centers and exports recording
  state without capturing editor text or audio. [The runtime retry](https://github.com/amroalhamedi2022-hue/my-keyboard/actions/runs/46175360735)
  reached Android's system microphone permission prompt. The probe ignored XML
  leaf buttons because their truth value was false. A regression test covers
  that case. Capture and insertion still require a successful run.
- [Tiny voice and native regression results](../ASR_BENCHMARK.md)
- The [next two emulator runs](https://github.com/amroalhamedi2022-hue/my-keyboard/actions/runs/46150954018)
  built the candidate. The emulator action split an inline multi-line script before
  the device probe started. The revised workflow uses a checked-in shell script,
  pinned Python wheels and a runtime retry path that verifies the previously
  compiled APK's source and hash before reuse.
- [Adaptive settings candidate](https://github.com/amroalhamedi2022-hue/my-keyboard/actions/runs/46175857274)
  compiled. The first settings check saw the onboarding wizard because it had
  opened settings before selecting the IME. The retry opens settings after
  selection and preserves keyboard and microphone evidence even if the settings
  check fails.
- [Adaptive candidate runtime retry](https://github.com/amroalhamedi2022-hue/my-keyboard/actions/runs/46176680046)
  passed the phone and tablet settings check, bilingual typing, editing and part
  of the microphone check. It captured the app in the recording state. A
  screenshot showed that the floating voice controls covered part of the host
  editor. The Discard check timed out. Voice controls now mount inside the
  keyboard view and the probe asserts their position before testing Discard.
- [The inline voice build](https://github.com/amroalhamedi2022-hue/my-keyboard/actions/runs/46177344819)
  showed the keyboard-local panel and Android's microphone indicator together.
  Its geometry evidence exposed an overlap between the probe's numeric Discard
  marker and the engine's Symbols key code. The probe addresses controls by
  name in new builds and disambiguates the pinned binary for runtime retries.
- [The corrected runtime replay](https://github.com/amroalhamedi2022-hue/my-keyboard/actions/runs/46178252462)
  passed Discard, with the editor's text intact. The second recording reached
  local processing and exceeded the insertion timeout. The CI logs show that
  the artifact used the native CMake Debug variant. The next evaluation keeps
  the geometry probe and links the native Release variant for representative
  inference timing. Actual text insertion remains unverified.

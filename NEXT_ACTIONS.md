# Next batch
1. Run the new reproducible build on CI and install the debug engineering APK on API29/API33. Instrument editor lifecycle and sensitive-field behavior. Test manual input in representative apps and phone/tablet layouts.
2. Audit CleverKeys/Urik actual Arabic resources and decoder, and FlorisBoard gesture source; record commit IDs, licenses, data provenance. Implement independent baseline ranker and benchmark harness.
3. Compare quantized multilingual Whisper and sherpa/streaming bilingual candidates, check licenses and independent Palestinian/Levantine data; implement reproducible ASR evaluation, then native engine integration.
4. Add safe session cancellation, settings, adaptive layout, autofill and resource trust. Do not release until evidence and signing strategy exist.

Batch 2 priorities: compile and instrument GlideSurface tap vs drag cancellation, multi-touch, editor switch, selection callbacks, incognito/password handling. Replace seed lexicon after provenance and license approval; benchmark indexed candidate retrieval. Local ASR inference and microphone lifecycle remain highest-priority research.

Voice runner is ready for actual local model experiments, but no independent speech set has been executed. Local compilation, unit tests, lint and debug assembly now pass. CI, device installation, microphone capture, password-manager providers and physical Xiaomi Pad 5 behavior remain pending.

Batch 3: build native + Android, test actual recording, local inference and input insertion on Android 13; benchmark Tiny/Base with consented held-out mixed speech; implement abortable/warm native model sessions, VAD and live partials; audit full native dependency and licenses. Continue Glide dictionary/index and Autofill independently.

Batch 04: test adaptive profiles and split glide on physical tablet, validate editor actions and selection callbacks, verify native abort and voice review on device. Continue licensed production lexicon, real bilingual ASR benchmark, warm context lifecycle, streaming, Autofill.

Work handoff: resume existing Android 13 emulator, verify get-state=device and sys.boot_completed=1. If available, first test the existing 8.9 MB debug artifact without rebuilding. For the new automated probe, reuse next deliberate debug build and run docs/ANDROID_RUNTIME_PROBE.md. Preserve real logs/results. Do not claim runtime success until observed.

Added independent API33 KVM CI runtime milestone and explicit trigger marker. Check Actions for actual run, failure logs and artifacts. If no run started, dispatch the runtime workflow on its branch once it is registered on default branch, or execute existing emulator probe in Work. Do not merge unverified source simply to start CI. No repeated build until evidence warrants.

Batch 06: validate personal dictionary editing and Glide merge, heap/pruning equivalence against reference ranker; verify explicit clipboard save, expiration, sensitive fields and lifecycle on Android; expand licensed dictionaries and full emoji data. Check runtime milestone actual results, no redundant builds. Parallel workstream priority remains actual bilingual voice benchmark, Glide accuracy, daily tools and security.

# Initial evidence, not completed source audit
- HeliBoard FAQ https://github.com/HeliBorg/HeliBoard/wiki/FAQ explicitly says glide needs an external library; repository https://github.com/HeliBorg/HeliBoard GPL-3.0. Reject proprietary dependency as core.
- CleverKeys https://github.com/tribixbite/CleverKeys describes ONNX CTC plus geometric fallback, GPL-3.0-or-later. Arabic support and code/assets licenses must be checked from implementation before reuse.
- FlorisBoard https://github.com/florisboard/florisboard Apache-2.0; candidate for layout/theme and gesture source audit.
- whisper.cpp https://github.com/ggml-org/whisper.cpp MIT runtime; multilingual tiny/base quantized candidates, no verified mixed accuracy.
- sherpa-onnx https://k2-fsa.github.io/sherpa/onnx/pretrained_models/whisper/export-onnx.html provides multilingual tiny/base exports; .en variants excluded. Investigate streaming bilingual model alternatives.
- ArzEn https://aclanthology.org/2020.lrec-1.523/ is Egyptian code-switch corpus, not Palestinian evidence. Access/license must be checked at publisher before use.
- Sawt 2B https://huggingface.co/bnsaid/sawt-asr-ar-en-2b-v1 model card candidate for teacher research, not compact Android candidate; independent metrics and provenance unverified.
- Android IME https://developer.android.com/reference/android/inputmethodservice/InputMethodService and https://developer.android.com/identity/autofill/ime-autofill: inline requires XML and request/response; fallback system UI if unsupported.
- AGP https://developer.android.com/build/releases/agp-8-7-0-release-notes supports Gradle 8.9/JDK17/API35.
Remaining: audit AOSP LatinIME, AnySoftKeyboard, FUTO, Urik, CleverKeys internals, Vosk, Qwen ASR, WeNet, transducers, compact Conformer/Zipformer, datasets, licenses and version pinning. No benchmark run; no final engine selected.

## Batch 2 source follow-up
- Android InputConnection reference https://developer.android.com/reference/android/view/inputmethod/InputConnection describes composing and selection callback behavior; alternatives use confirmed composing spans, not arbitrary surrounding-text replacement.
- whisper.cpp CLI flags checked against https://github.com/ggml-org/whisper.cpp/blob/master/examples/cli/README.md and cli.cpp; CPU adapter uses JSON output, no translation, compares language selection modes.
- Sherpa Whisper documentation https://k2-fsa.github.io/sherpa/onnx/pretrained_models/whisper/index.html identifies it as non-streaming recognition; separate streaming transducer selection still required.
- FrequencyWords https://github.com/hermitdave/FrequencyWords explicitly distinguishes MIT code and CC BY-SA 4.0 content; not bundled. Need corpus provenance, attribution and derivative data distribution review.
- https://huggingface.co/Seif-Eldeen-Sameh/whisper-medium-arabic-codeswitched Egyptian code-switch fine-tune is not a validated Levantine or compact mobile solution; candidate teacher only.

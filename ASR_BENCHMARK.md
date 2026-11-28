# ASR benchmark status
No actual model run, no WER/CER results. Added research/asr/score.py for strict paired transcript metrics, mixed WER and English token error rate (proxy, not direct script preservation metric). Run python3 -m unittest discover -s research/asr -p 'test_*.py' and python3 research/asr/score.py predictions.jsonl. Synthetic fixtures only. Next: licensed independent speaker-disjoint audio, model adapters, latency/RAM/RTF/power/thermal measurement and explicit script alignment analysis. Never publish private audio.

Batch 2: CPU whisper-cli runner for verified local model file, private consented WAV manifest, JSON transcription output, per-clip elapsed time and cold-process RTF; 4 synthetic adapter tests pass. No actual whisper executable/model/audio provided or executed. Run same held-out samples for auto/ar/en and tiny/base quantized; do not conflate single-clip language selection with intra-sentence code switching. See research/asr/README.md.

Batch 3: native whisper.cpp CPU backend and two exact-hash multilingual candidates (Tiny Q5 32.2 MB, Base Q5 59.7 MB) integrated in source. No actual inference or accuracy/latency measurement. See docs/VOICE_INTEGRATION.md.

Batch 5 adds an optional low-cost energy endpoint detector. It rejects isolated transient frames, trims leading/trailing silence, and can stop after 1.2 seconds of detected silence. This is deterministic signal processing, not a trained VAD, and sustained noise may be treated as speech. Its host unit tests do not establish ASR accuracy.

Batch 07: per-session language/thread/endpoint controls and pause/resume source integration; same fixed Tiny/Base models. Actual mixed-language WER, model runtime, latency and mic resume behavior remain unmeasured. Auto remains default.

Continuous follow-up: 17 synthetic segmentation/queue checks, fake decoder only. Optional bounded chunk decoding and within-session weights reuse written. Measure actual WER, first completed segment latency, RTF, sustained queue backlog, pauses and forced boundaries before production. No real voice model run yet.

## Human Palestinian speech pilot, 2026-11-28

Both candidates ran on the same 16 public Casablanca Palestine test clips, with
four x86_64 CPU threads, automatic language detection, and one cold CLI process
per clip. The verified whisper.cpp source commit was
`a8d002cfd879315632a579e73f0148d06959de36`. Reference transcripts used the
published test text with strict scoring. The sample contained 250 word tokens,
1,184 characters and zero mixed-script references.

| Candidate | Model bytes | Strict WER | Strict CER | Cold p50 | Cold p95 | Aggregate RTF |
|---|---:|---:|---:|---:|---:|---:|
| Base multilingual Q5_1 | 59,707,625 | 76.8% (192/250) | 36.8% (436/1184) | 3.28 s | 6.01 s | 0.56 |
| Small multilingual Q5_1 | 190,085,487 | 57.6% (144/250) | 23.9% (283/1184) | 10.70 s | 12.25 s | 1.77 |

Small reduced WER 19.2 percentage points on this pilot. Its total processing
time was about 3.2 times Base on this host, and exceeded the audio duration.
Both models need substantially lower error rates for production dictation. English token error and mixed
WER are undefined on this sample. Pad 5 streaming and Android latency, RAM,
battery and thermal output remain unmeasured.

Sampling used the first 100 published test rows, excluded missing or truncated
references and clips outside 2–12 seconds, then selected 16 with Python seed
99. Row indices: 11, 19, 24, 29, 37, 39, 41, 60, 61, 65, 76, 83, 84, 91, 93,
97. Corpus: [Talafha et al., Casablanca](https://arxiv.org/abs/2410.04527),
[published Palestine test split](https://huggingface.co/datasets/UBC-NLP/Casablanca/tree/main/Palestine).
Dataset audio and transcripts are kept out of this repository and release.

These are the verified digests for the tested assets in the publisher's
`ggerganov/whisper.cpp` repository at
`f281eb45af861ab5e5297d23694b7d46e090c02c`.

- `ggml-base-q5_1.bin`: `422f1ae452ade6f30a004d7e5c6a43195e4433bc370bf23fac9cc591f01a8898`
- `ggml-small-q5_1.bin`: `ae85e4a935d7a567bd102fe55afc16bb595bdb618e11b2fc7591bc08120411bb`

A separate four-thread Base run with Arabic forced on the same clips scored
77.2% WER (193/250) and 36.1% CER (428/1184). This pilot gave no word-error
benefit from forcing Arabic. It gives no guidance on mixed-language decoding.

# Local voice research runner
Requires Python 3.11+, a trusted locally built whisper-cli pinned to an audited commit, and a multilingual GGML model with trusted SHA-256. Do not use .en-only models. No downloads, network calls, or Android integration performed by this script.
Create a private manifest JSONL next to consented WAV files: {"id":"u001","audio":"audio/u001.wav","reference":"بدي exam","dialect":"palestinian","condition":"quiet"}. Example text is illustrative, not an actual recording. WAV must be mono signed 16-bit PCM at 16 kHz, max 300 s. Keep manifests, transcripts and audio outside public repository; do not upload private recordings to CI.
Run:
    python research/asr/run_whisper.py --cli /path/whisper-cli --model /path/model.bin --model-sha256 TRUSTED_DIGEST --manifest /private/test.jsonl --output /private/results-auto.jsonl --language auto
    python research/asr/score.py /private/results-auto.jsonl
Repeat on same audio for ar and en language modes, tiny/base and quantized variants; compare WER/CER/mixed/English-token metrics. CPU execution via -ng; RTF includes startup and model loading, not streaming. The runner fails rather than silently skipping failed samples. Output must not exist. Partial output on failure is not a complete benchmark; rerun to a new file.
Candidate CLI reference: https://github.com/ggml-org/whisper.cpp/blob/master/examples/cli/README.md
Next: persistent native session, sherpa adapter, actual benchmark, code-switch language choice analysis, VAD, warm latency, memory and mobile device tests.

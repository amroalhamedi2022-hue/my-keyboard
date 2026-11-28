"""Offline research adapter: one whisper-cli subprocess per consented WAV.
Requires a locally installed, pinned executable and multilingual model.
Timing includes process/model startup; not a streaming or Android benchmark.
"""
import argparse
import hashlib
import json
import pathlib
import subprocess
import tempfile
import time
import wave


def sha256(path):
    with open(path, "rb") as f:
        return hashlib.file_digest(f, "sha256").hexdigest()


def duration(path):
    with wave.open(str(path), "rb") as w:
        if (w.getnchannels(), w.getsampwidth(), w.getframerate()) != (1, 2, 16000):
            raise ValueError("expected mono 16-bit PCM 16 kHz WAV")
        seconds = w.getnframes() / w.getframerate()
        if seconds <= 0 or seconds > 300:
            raise ValueError("WAV duration must be in (0, 300] seconds")
        return seconds


def parse_transcription(path):
    data = json.loads(path.read_text(encoding="utf-8"))
    return " ".join(segment["text"].strip() for segment in data["transcription"]).strip()


def run_one(cli, model, audio, language, threads, timeout):
    seconds = duration(audio)
    with tempfile.TemporaryDirectory(prefix="zero-asr-") as tmp:
        prefix = pathlib.Path(tmp) / "result"
        start = time.perf_counter()
        subprocess.run([str(cli), "-m", str(model), "-f", str(audio), "-l", language,
                        "-t", str(threads), "-oj", "-of", str(prefix), "-ng"],
                       check=True, timeout=timeout, stdout=subprocess.DEVNULL,
                       stderr=subprocess.DEVNULL)
        elapsed = time.perf_counter() - start
        return parse_transcription(prefix.with_suffix(".json")), elapsed, elapsed / seconds


def main():
    p = argparse.ArgumentParser(description=__doc__)
    for key in ("cli", "model", "manifest", "output"):
        p.add_argument("--" + key, required=True, type=pathlib.Path)
    p.add_argument("--model-sha256", required=True)
    p.add_argument("--language", choices=("auto", "ar", "en"), default="auto")
    p.add_argument("--threads", type=int, choices=range(1, 17), default=4)
    p.add_argument("--timeout", type=int, default=300)
    a = p.parse_args()
    cli, model = a.cli.resolve(strict=True), a.model.resolve(strict=True)
    if sha256(model) != a.model_sha256.lower():
        raise ValueError("model SHA-256 mismatch")
    cli_hash = sha256(cli)
    with a.manifest.open(encoding="utf-8") as f:
        rows = [json.loads(line) for line in f if line.strip()]
    ids = [row["id"] for row in rows]
    if not rows or len(ids) != len(set(ids)):
        raise ValueError("manifest empty or duplicate id")
    root = a.manifest.resolve().parent
    with a.output.open("x", encoding="utf-8") as out:
        for row in rows:
            audio = (root / row["audio"]).resolve(strict=True)
            if not audio.is_relative_to(root):
                raise ValueError("audio outside manifest directory")
            text, elapsed, rtf = run_one(cli, model, audio, a.language, a.threads, a.timeout)
            result = {k: row[k] for k in ("id", "reference", "dialect", "condition") if k in row}
            result.update(hypothesis=text, elapsed_seconds=elapsed, rtf=rtf,
                          model_sha256=a.model_sha256.lower(), cli_sha256=cli_hash,
                          language=a.language, threads=a.threads)
            out.write(json.dumps(result, ensure_ascii=False) + "\n")
            out.flush()


if __name__ == "__main__":
    main()

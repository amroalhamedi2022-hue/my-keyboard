import json
import pathlib
import tempfile
import unittest
import wave
from run_whisper import duration, parse_transcription, sha256


class AdapterTests(unittest.TestCase):
    def test_duration(self):
        with tempfile.TemporaryDirectory() as tmp:
            p = pathlib.Path(tmp) / "a.wav"
            with wave.open(str(p), "wb") as w:
                w.setparams((1, 2, 16000, 0, "NONE", "not compressed"))
                w.writeframes(b"\0\0" * 16000)
            self.assertEqual(duration(p), 1)

    def test_bad_audio(self):
        with tempfile.TemporaryDirectory() as tmp:
            p = pathlib.Path(tmp) / "a.wav"
            with wave.open(str(p), "wb") as w:
                w.setparams((2, 2, 16000, 0, "NONE", "not compressed"))
                w.writeframes(b"\0\0" * 32000)
            with self.assertRaises(ValueError):
                duration(p)

    def test_mixed_json(self):
        with tempfile.TemporaryDirectory() as tmp:
            p = pathlib.Path(tmp) / "a.json"
            p.write_text(json.dumps({"transcription": [{"text": " بدي"}, {"text": "exam "}]}), encoding="utf-8")
            self.assertEqual(parse_transcription(p), "بدي exam")

    def test_digest(self):
        with tempfile.TemporaryDirectory() as tmp:
            p = pathlib.Path(tmp) / "a"
            p.write_bytes(b"abc")
            self.assertEqual(sha256(p), "ba7816bf8f01cfea414140de5dae2223b00361a396177a9cb410ff61f20015ad")

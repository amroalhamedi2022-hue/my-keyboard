import unittest
from score import score
class ScoreTests(unittest.TestCase):
    def test_exact_mixed(self):
        r=score([{"id":"1","reference":"بدي exam","hypothesis":"بدي exam"}])
        self.assertEqual(r["wer"],0)
        self.assertEqual(r["mixed_wer"],0)
        self.assertEqual(r["english_token_error_rate"],0)
    def test_latin_loss(self):
        r=score([{"id":"1","reference":"بدي exam","hypothesis":"بدي إكزام"}])
        self.assertEqual(r["wer"],0.5)
        self.assertEqual(r["english_token_error_rate"],1)
    def test_mixed_english_rate_ignores_sentence_case_and_punctuation(self):
        rows = [
            {"id":"1","reference":"بدي Exam!","hypothesis":"بدي exam."},
            {"id":"2","reference":"English only","hypothesis":""},
        ]
        self.assertEqual(score(rows)["mixed_english_token_error_rate"], 0)
        self.assertEqual(score(rows[:1])["english_token_error_rate"], 1)

    def test_weighted_rtf_and_cold_latency(self):
        rows = [
            {"id":"1","reference":"a","hypothesis":"a","elapsed_seconds":1,"rtf":0.5},
            {"id":"2","reference":"a","hypothesis":"a","elapsed_seconds":3,"rtf":0.75},
        ]
        runtime = score(rows)["runtime"]
        self.assertEqual(runtime["timed_utterances"], 2)
        self.assertEqual(runtime["aggregate_rtf"], 4/6)
        self.assertEqual(runtime["cold_seconds_p50"], 2)
        self.assertEqual(runtime["cold_seconds_p95_nearest_rank"], 3)

    def test_duplicate(self):
        row={"id":"1","reference":"a","hypothesis":"a"}
        with self.assertRaises(ValueError): score([row,row])
if __name__=="__main__": unittest.main()

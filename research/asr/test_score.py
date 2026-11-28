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
    def test_duplicate(self):
        row={"id":"1","reference":"a","hypothesis":"a"}
        with self.assertRaises(ValueError): score([row,row])
if __name__=="__main__": unittest.main()

"""Score paired UTF-8 JSONL transcripts. No model inference or audio access.
Fields: id, reference, hypothesis. Optional: dialect, condition. Default scoring
is strict. WER uses whitespace tokens and CER includes spaces.
"""
import argparse
import json
import math
import re
import statistics
import unicodedata
from collections import defaultdict

def edit(a, b):
    prev = list(range(len(b) + 1))
    for i, x in enumerate(a, 1):
        cur = [i]
        for j, y in enumerate(b, 1):
            cur.append(min(cur[-1] + 1, prev[j] + 1, prev[j-1] + (x != y)))
        prev = cur
    return prev[-1]

def clean(s):
    return " ".join(unicodedata.normalize("NFC", s).split())

def normalize_arabic_text(text):
    """Ignore typography and common orthographic variants without changing words."""
    text = unicodedata.normalize("NFC", text).casefold()
    result = []
    for char in text:
        category = unicodedata.category(char)
        if char == "ـ" or (category.startswith("M") and "ARABIC" in unicodedata.name(char, "")):
            continue
        if char in "أإآٱ":
            char = "ا"
        elif category == "Nd":
            char = str(unicodedata.decimal(char))
        elif category.startswith("P"):
            char = " "
        result.append(char)
    return clean("".join(result))

def script(c):
    name = unicodedata.name(c, "")
    if c.isalpha() and "ARABIC" in name:
        return "ar"
    if c.isalpha() and "LATIN" in name:
        return "en"
    return None

def english_words(s):
    return [w for w in s.split() if any(script(c) == "en" for c in w)]

def latin_tokens(text):
    return re.findall(r"[a-z]+(?:'[a-z]+)*", text.replace("’", "'").casefold())


def runtime_summary(rows):
    timings = [(row["elapsed_seconds"], row["rtf"]) for row in rows
               if "elapsed_seconds" in row and "rtf" in row]
    if not timings:
        return None
    if any(not math.isfinite(t) or t <= 0 for timing in timings for t in timing):
        raise ValueError("inference timing must contain finite positive values")
    elapsed = sorted(seconds for seconds, _ in timings)
    rtf = sorted(factor for _, factor in timings)
    return {
        "timed_utterances": len(timings),
        "cold_seconds_p50": statistics.median(elapsed),
        "cold_seconds_p95_nearest_rank": elapsed[math.ceil(0.95 * len(elapsed)) - 1],
        "aggregate_rtf": sum(elapsed) / sum(seconds / factor for seconds, factor in timings),
        "rtf_p95_nearest_rank": rtf[math.ceil(0.95 * len(rtf)) - 1],
    }


def score(rows, normalize=clean):
    rows = list(rows)
    counts = defaultdict(int)
    seen = set()
    for row in rows:
        if row["id"] in seen:
            raise ValueError("duplicate id")
        seen.add(row["id"])
        a, b = normalize(row["reference"]), normalize(row["hypothesis"])
        counts["utterances"] += 1
        counts["word_errors"] += edit(a.split(), b.split())
        counts["reference_words"] += len(a.split())
        counts["char_errors"] += edit(a, b)
        counts["reference_chars"] += len(a)
        ae, be = english_words(a), english_words(b)
        counts["english_token_errors"] += edit(ae, be)
        counts["english_reference_tokens"] += len(ae)
        if {"ar", "en"} <= {script(c) for c in a}:
            counts["mixed_utterances"] += 1
            counts["mixed_word_errors"] += edit(a.split(), b.split())
            counts["mixed_reference_words"] += len(a.split())
            expected_english = latin_tokens(a)
            counts["mixed_english_reference_tokens"] += len(expected_english)
            counts["mixed_english_token_errors"] += edit(expected_english, latin_tokens(b))
    def ratio(num, den):
        return counts[num] / counts[den] if counts[den] else None
    return dict(counts, wer=ratio("word_errors","reference_words"),
                cer=ratio("char_errors","reference_chars"),
                mixed_wer=ratio("mixed_word_errors","mixed_reference_words"),
                english_token_error_rate=ratio("english_token_errors","english_reference_tokens"),
                mixed_english_token_error_rate=ratio("mixed_english_token_errors","mixed_english_reference_tokens"),
                runtime=runtime_summary(rows))

def main():
    ap = argparse.ArgumentParser(description=__doc__)
    ap.add_argument("jsonl")
    ap.add_argument("--arabic-normalized", action="store_true",
                    help="also available as an independent typography-insensitive scoring mode")
    args = ap.parse_args()
    with open(args.jsonl, encoding="utf-8") as f:
        rows = [json.loads(line) for line in f if line.strip()]
    if not rows:
        raise ValueError("empty evaluation set")
    groups = defaultdict(list)
    for row in rows:
        groups[(row.get("dialect","unknown"),row.get("condition","unknown"))].append(row)
    normalize = normalize_arabic_text if args.arabic_normalized else clean
    report = {"overall": score(rows, normalize),
              "groups": [{"dialect": k[0], "condition": k[1], "metrics": score(v, normalize)}
                         for k, v in sorted(groups.items())]}
    if args.arabic_normalized:
        report["normalization"] = "arabic-typography-v1"
    print(json.dumps(report, ensure_ascii=False, indent=2))

if __name__ == "__main__":
    main()

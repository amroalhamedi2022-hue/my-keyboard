"""Score paired UTF-8 JSONL transcripts. No model inference or audio access.
Fields: id, reference, hypothesis. Optional: dialect, condition. Strict text; no
Arabic folding or case folding. WER uses whitespace tokens; CER includes spaces.
"""
import argparse
import json
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

def script(c):
    name = unicodedata.name(c, "")
    if c.isalpha() and "ARABIC" in name:
        return "ar"
    if c.isalpha() and "LATIN" in name:
        return "en"
    return None

def english_words(s):
    return [w for w in s.split() if any(script(c) == "en" for c in w)]

def score(rows):
    counts = defaultdict(int)
    seen = set()
    for row in rows:
        if row["id"] in seen:
            raise ValueError("duplicate id")
        seen.add(row["id"])
        a, b = clean(row["reference"]), clean(row["hypothesis"])
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
    def ratio(num, den):
        return counts[num] / counts[den] if counts[den] else None
    return dict(counts, wer=ratio("word_errors","reference_words"),
                cer=ratio("char_errors","reference_chars"),
                mixed_wer=ratio("mixed_word_errors","mixed_reference_words"),
                english_token_error_rate=ratio("english_token_errors","english_reference_tokens"))

def main():
    ap = argparse.ArgumentParser(description=__doc__)
    ap.add_argument("jsonl")
    args = ap.parse_args()
    with open(args.jsonl, encoding="utf-8") as f:
        rows = [json.loads(line) for line in f if line.strip()]
    if not rows:
        raise ValueError("empty evaluation set")
    groups = defaultdict(list)
    for row in rows:
        groups[(row.get("dialect","unknown"),row.get("condition","unknown"))].append(row)
    print(json.dumps({"overall":score(rows),"groups":[{"dialect":k[0],"condition":k[1],"metrics":score(v)} for k,v in sorted(groups.items())]}, ensure_ascii=False,indent=2))

if __name__ == "__main__":
    main()

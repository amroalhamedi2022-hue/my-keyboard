# Glide benchmark status
Original geometric research baseline GeometricRanker: per-layout coordinate map, resampled paths, endpoint and length penalties. Unit fixtures Arabic/English; no real traces, no production dictionary, no IME integration, no speed/accuracy evidence. Limitations: O(vocabulary * samples), repeat-letter ambiguity, ligatures/diacritics not handled, no spatial probability/context ranking. Next: licensed lexicon, indexed candidate retrieval, real independent consenting traces, top-1/top-3 and latency across layouts, compare source-audited neural models.

Batch 2: original small seed list plus touch dispatch integration; independent toggles for AR/EN, trail and auto-spacing; alternatives through framework-confirmed composing span. Not production ready; no accuracy measurement. Dictionary candidates: https://github.com/hermitdave/FrequencyWords (verify data share-alike terms), https://github.com/rspeer/wordfreq (verify per-source notices). No copied resources in this commit.

Batch 5 caches resampled templates while both the actual key coordinates and ordered vocabulary remain unchanged. Exact geometry comparison invalidates the cache after rotation, resizing, language changes, or layout changes. English case and Arabic alef variants are mapped to available physical keys for geometry only, while committed spelling remains the dictionary spelling. Unit tests cover cache invalidation and alef mapping. Human top-1/top-3 accuracy and real-device latency remain unmeasured.

Batch 07: bounded frequency list parser/store and background ranking added; parser tests pass; no real 50k resource import or device latency/accuracy test. Resource membership capped at 8k per language for initial memory budget; measure actual template allocation and recognition before raising limit.

## Held-out human English traces, 2026-11-28

The current production-source geometric ranker was replayed on 250 eligible
human swipe traces from 250 held-out log files in
[How We Swipe](https://osf.io/sj67f/). The benchmark calibrated key-center
medians from a disjoint set of 100 participant logs. It selected successful
alphabetic traces recorded on the research keyboard and used Zero's 8,002
English seed and bundled words. No test trace tuned ranker weights. The
archive SHA-256 is
`c7e6398d630906d664654d8f5a7ba2b1531882636fd110d8d6c05a723ca00874`.

| Metric | Result |
|---|---:|
| Dictionary coverage | 213/250 (85.2%) |
| Correct top candidate | 155/250 (62.0%) |
| Correct candidate in top three | 186/250 (74.4%) |
| Emissions passing the current score cutoff | 241/250 (96.4%) |
| Correct among emitted | 155/241 (64.3%) |
| Warm host ranker p50 / p95 | 0.485 ms / 1.249 ms |

The research keyboard differs from Zero's installed geometry. This replay uses
the ranker and the production cutoff. Android touch admission, trail rendering,
ongoing text insertion, false activation on manual taps and Arabic accuracy
still require separate tests. Selection of successful traces may favor clean gestures.
Latencies are warm desktop JVM rank calls after three passes. They exclude
touch handling, Android dispatch and editor round trips.

Reproduce locally from the OSF swipe archive. Keep logs and derived trajectories
private to the evaluation directory. Run
`python3 research/glide/prepare_human_traces.py /path/swipelogs.zip /tmp/zero-glide-sample`,
then compile `HumanTraceBenchmark.java`, `GeometricRanker.java`,
`FrequencyLexicon.java` and `SeedLexicon.java` with Java 17 or later.
Pass `/tmp/zero-glide-sample` and
`app/src/main/assets/dictionaries/en-frequency.txt` to `HumanTraceBenchmark`.

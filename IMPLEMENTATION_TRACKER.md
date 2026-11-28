# Evidence tracker
| ID | Status | Evidence / missing |
|---|---|---|
| R01 | In Progress | IME manifest/service, setup, basic layouts/editor actions compile and package successfully; installation and device behavior pending |
| R02-R03 | In Progress | Experimental native whisper.cpp JNI builds for arm64-v8a/x86_64; exact-size/hash model import, bounded in-memory capture, cancellation, optional endpointing/review and editor insertion authored. Android inference and code-switch benchmarks pending |
| R04 | In Progress | Experimental IME touch capture, trail, seed dictionary, template cache, composing alternatives and Arabic alef geometry authored; unit/smoke tests pass. Explicit personal words and bounded top-K ranker added; 600 synthetic ranking-equivalence cases passed; production lexicon, device integration and real benchmarks missing |
| R05 | In Progress | Basic layout logic passed JVM smoke; language toggle and diacritics/symbols written; personal lexicon parser validated locally, UI untested |
| R06-R08 | In Progress | Four adaptive profile combinations, full/split/one-hand, height, digit row/format, editor toolbar, space cursor and voice review authored; 9 pure-JVM layout assertions passed; session-only clipboard and categorized emoji UI authored; clipboard core JVM smoke passed; device validation and remaining utilities pending |
| R09 | In Progress | Android 11+ Inline Autofill request/response host implemented with opaque provider views and generation guards; provider/device compatibility pending |
| R10 | In Progress | No INTERNET; explicit RECORD_AUDIO runtime checks, voice disabled by default, no-follow exact model verification, backup exclusion and sensitive editor guard. Runtime security tests pending |
| R11-R12 | Pending | Basic UI only; no premium theme or performance claims |
| R13 | Researching | Initial source triage; independent benchmarks pending |
| R14 | In Progress | Local Android build, 9 JUnit tests, clean lint, 7 Python tests and 23 JVM smoke assertions pass; instrumented/device tests pending |
| R15 | Pending | No release or signing key |
| R16 | Implemented | Checksum-verified Gradle wrapper and milestone CI workflow authored; local equivalent passes, GitHub Actions result pending |

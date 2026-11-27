# Evidence tracker
| ID | Status | Evidence / missing |
|---|---|---|
| R01 | In Progress | IME manifest/service, setup, basic layouts/editor actions written; Android compile/device tests pending |
| R02-R03 | Researching | Experimental native whisper.cpp JNI, verified model import/select/delete, mic permission, bounded in-memory capture, cancellation guard and editor insertion authored; 7 catalog smoke assertions passed; Native abort callback and optional review authored; Android build/inference and code-switch benchmarks pending |
| R04 | In Progress | Experimental IME touch capture, trail, seed dictionary, top candidate composing and alternatives authored; prior geometric smoke passed, Android integration untested; production lexicon and real benchmarks missing |
| R05 | In Progress | Basic layout logic passed JVM smoke; language toggle and diacritics/symbols written, no dictionaries |
| R06-R08 | In Progress | Four adaptive profile combinations, full/split/one-hand, height, digit row/format, editor toolbar, space cursor and voice review authored; 9 pure-JVM layout assertions passed; device validation and remaining utilities pending |
| R09 | Researching | Official inline API identified, not integrated |
| R10 | In Progress | No INTERNET; explicit RECORD_AUDIO runtime permission, voice disabled by default, catalog hash verification, sensitive editor guard; merged manifest and runtime security tests pending |
| R11-R12 | Pending | Basic UI only; no premium theme or performance claims |
| R13 | Researching | Initial source triage; independent benchmarks pending |
| R14 | In Progress | Prior recorded 3 Python/7 JVM smoke checks; 4 Python adapter tests and 7 catalog assertions previously recorded; 9 new layout assertions executed; Android tests pending |
| R15 | Pending | No release or signing key |
| R16 | Implemented | Manual CI plus explicit milestone-marker path trigger; no build result yet |

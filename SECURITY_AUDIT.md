# Initial static review (not release audit)
Source manifest contains no INTERNET permission. IME is protected by BIND_INPUT_METHOD, and cloud backup plus device transfer are excluded. Setup launcher is exported intentionally. No clipboard/history or learning is implemented. Numeric/phone and declared password fields conservatively block optional processing in EditorPolicy. OTP detection remains incomplete. Merged-manifest inspection, runtime traffic validation, password-manager testing and signing remain pending. No user input is collected by research scripts.

Batch 3: RECORD_AUDIO intentionally added; no INTERNET permission. Voice defaults off; explicit tap plus granted permission and eligible editor required. Model import capped and hash allowlisted, private atomic move; reverified before native load. No audio disk writes. Stale results discarded, PCM arrays cleared on normal cleanup (not a guarantee of forensic memory erasure). Native engine and dependency audit pending. No verified runtime claims.

Batch 5: model files require the catalogued byte length and SHA-256 and must be regular files when checked without following links. Mutations are serialized, abandoned staging files are removed, and promotion requests an atomic move with a same-directory fallback. Inline Autofill hosts Android/provider-rendered views and clears them across editor generations. Local lint is clean. Runtime adversarial tests and complete transitive dependency review remain pending.

## Mature core clipboard policy

The upstream history listener stored text even when Android marked the clip
sensitive. The mature preparation now applies the existing Zero editor policy
before automatic clipboard access and checks the Android sensitivity flag
before extracting text. Private, numeric and incognito editors cannot open
history, receive a preview or insert a historical entry. New automatic
history entries and previews use the existing Zero plain-text bounds and
credential-pattern filter. Explicit system paste remains available when
invoked by the user. Existing clipboard records are preserved, and Android
integration and legacy history behavior still require device tests.

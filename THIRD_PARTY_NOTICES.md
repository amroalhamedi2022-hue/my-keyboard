# Third-party notices

## whisper.cpp and ggml

The experimental offline voice backend builds pinned source archive `a8d002cfd879315632a579e73f0148d06959de36` from `ggml-org/whisper.cpp`. It is licensed under MIT. The verbatim notice is packaged in `app/src/main/assets/licenses/whisper-ggml-MIT.txt`. Voice model weights are user-imported and are not distributed in the application.

## AndroidX Autofill

The IME uses `androidx.autofill:autofill:1.3.0` to negotiate provider-rendered inline suggestion styles. AndroidX is licensed under Apache License 2.0. The dependency and its transitive graph remain subject to the release dependency audit.
No copied third-party engine/model/dictionary assets in this baseline. Android/Gradle/JUnit build dependencies need pinned transitive audit before distribution. Research references are not imported code. No project source license selected by owner; do not assume public repository grants reuse rights.

## FrequencyWords derived dictionaries
Source commit 525f9b560de45753a5ea01069454e72e9aa541c6, English blob bbf5fa991058ff642732784b31af023f66462e1d and Arabic blob 3dabab93d6bca73d1eec9a6ff91c4d85ee37aab3. Upstream README declares content CC BY-SA 4.0. Bundled filtered 8,000-entry derivatives retain that license and attribution; see app/src/main/assets/dictionaries/NOTICE.txt. Source corpus identified as OpenSubtitles2018; underlying corpus rights and dialect quality not independently verified. Resource license is separate from app source. Owner alpha only, further review before broad distribution.

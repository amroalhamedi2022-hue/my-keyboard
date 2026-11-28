# Zero Keyboard

Offline-first Android input method for Arabic and English. The mature core
retains the `com.zerokeyboard.ime` application ID and is generated from pinned
HeliBoard source with the Zero voice and glide adapters.

- [Mature core build and migration](docs/MATURE_CORE_MIGRATION.md)
- [Requirements](REQUIREMENTS.md) and [implementation tracker](IMPLEMENTATION_TRACKER.md)
- [Verified results and completion matrix](docs/COMPLETION_MATRIX.md)
- [Audio benchmark](ASR_BENCHMARK.md) and [glide benchmark](GLIDE_BENCHMARK.md)
- [Release and evaluation workflow](.github/workflows/mature-core-candidate.yml)

Build the generated core with JDK 17 and Android SDK 35. Use the pinned tools
and preparation steps in the workflow. The root Gradle project is the earlier
Zero implementation and remains the source of reusable Zero components.

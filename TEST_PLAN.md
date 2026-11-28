# Test plan
Initial unit: layouts, case, numeric override; add policy tests on Android, editor actions/selection/delete, composition, app switching and rotation. Then voice/glide independent data benchmarks, offline airplane mode, resource trust, sensitive context and network tests, autofill providers, real device and emulator matrix. No executed Android tests yet. CI workflow manually dispatched at milestones.

Debug-only runtime probe: see docs/ANDROID_RUNTIME_PROBE.md. Check real touch-driven normal/mixed text, sensitive field optional-tool visibility, numeric/private editor handling. Run after next deliberate build, using existing emulator. Independent physical-device, actual glide/voice and provider integration still required.

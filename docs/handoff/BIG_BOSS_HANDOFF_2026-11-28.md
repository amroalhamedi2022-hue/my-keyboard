# Zero Keyboard — Big Boss Handoff
Snapshot: 2026-11-28. Source: repository README and open-PR listing inspected on this date; active HEAD/CI not independently certified.

## Canonical source
README.md; MASTER_PLAN.md/REQUIREMENTS.md/IMPLEMENTATION_TRACKER.md if present; PRs #1/#2.

## Observed baseline
README initialization; PR #2 evaluates pinned HeliBoard core, PR #1 bilingual IME foundation. No verified APK established by README.

## Execution sequence / acceptance
Audit mature core migration, licenses and offline AR/EN only; bilingual glide, mixed-language local voice, manual correction control (off by default), privacy/no telemetry, layout/RTL, latency/memory/model size; no translation or medical dictionary; test real IME across apps and devices before signed release.

## Operational handoff rules
- Master functionality in a masterpiece app. Maximize meaningful tested work per batch; avoid repeated work and unnecessary builds.
- Before every continuation: fetch origin, inspect default branch and active PR heads, latest commits, Actions and existing plans; record exact SHA, branch, tests and remaining gates. Do not assume README reflects latest code. Never overwrite another active branch or merge competing PRs blindly.
- Recovery: clone the repository, inspect README and documented toolchain, check out the verified intended branch/commit, restore dependencies from lockfiles, run relevant focused tests, then build only at meaningful milestones. No final release without same-SHA evidence. Record exact commands, artifact hash, version, device acceptance, and rollback procedure.
- Secrets, credentials, keystores, personal data, private asset archives and private Drive URLs never belong in GitHub (including private archive). Recover from owner-controlled restricted Drive or configured secret manager; verify permissions, signer fingerprint and checksums without exposing secrets. Never change production identity or signing key silently.
- Update handoff after meaningful changes; mark implemented, CI-verified, device-verified and blocked separately. This document is an index/continuation supplement, not a replacement for detailed source plans. No builds or device tests were run as part of this handoff update.

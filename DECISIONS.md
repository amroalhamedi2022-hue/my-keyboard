# Decisions
ADR-001: clean IME baseline while researching external components, rather than fork or commit third-party source before license audit.
ADR-002: provisional package com.zerokeyboard.ime; API 29–35 build baseline, JDK17/AGP 8.7.3/Gradle8.9; Java for minimal bootstrap, Kotlin available for later modules. No release yet.
ADR-003: no network/mic permission until audited voice/resource implementation; no backup by default.
ADR-004: manual CI trigger, no repeated full builds per commit. Test when real engineering value.

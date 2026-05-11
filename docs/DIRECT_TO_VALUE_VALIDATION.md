# Direct-to-value UX validation

## What shipped

- Truth-first balance: no optimistic fake counts; failed balance fetch surfaces verify-failed state.
- Readiness strip (`readiness_strip`): checking, no network, no credits, service degraded, verify retry.
- Submit/retry gated until readiness is `READY` (reuse engine health + connectivity + balance).
- Settings: opaque `ModalBottomSheet` with merged **Account & help** (send report + logs) and diagnostic share intent.
- SSOT failure taxonomy on feed badges (`Pluct-Core-Error-08OutcomeFamily`).
- Diagnostic text bundle: `Pluct-Core-Debug-02DiagnosticShare-01Builder`.
- Identity label: `Pluct-Core-User-01Display-01Formatter`.

## Run on ADB device

1. Build: `.\gradlew.bat assembleDebug`
2. Install: `adb install -r app\build\outputs\apk\debug\app-debug.apk`
3. Focused automation (retry-heavy UI + logcat):

```text
node scripts/nodejs/Pluct-Test-Focused-07DirectToValue-01Runner.js
```

Or single journey:

```text
node scripts/nodejs/Pluct-Main-01Orchestrator.js --test Journey-UX-25DirectToValue-Readiness-01Validation.js
```

## Local Business Engine

Point `pluctEngineBaseUrl` / `ENGINE_BASE_URL` at `http://127.0.0.1:8787` for emulator and run `adb reverse tcp:8787 tcp:8787` when validating against wrangler.

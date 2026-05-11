# Credit fairness and TikTok URL loop (ADB)

## What this validates

- Invalid TikTok short URL still passes client-side shape checks, hits Business Engine `/ttt/transcribe`, and returns a refund-friendly error (`creditsRefunded`).
- Android shows **No charge** / **No credits used** on the capture card when the BE response includes `creditsRefunded > 0`.
- ADB journey: `scripts/nodejs/journeys/Journey-UX-26TikTok-Url-Refund-NoCharge-01Validation.js` (clipboard paste + UI + logcat).

## Android build and install

1. `.\gradlew.bat :app:assembleDebug`
2. `adb install -r app\build\outputs\apk\debug\app-debug.apk`

## Focused ADB automation (verify-and-retry inside the journey)

```text
node scripts/nodejs/Pluct-Test-Focused-08TikTok-Url-Refund-01Runner.js
```

Or single journey:

```text
node scripts/nodejs/Pluct-Main-01Orchestrator.js --test Journey-UX-26TikTok-Url-Refund-NoCharge-01Validation.js
```

NPM shortcut:

```text
npm run test:tiktok-refund
```

## Business Engine (separate repo)

Path: `C:\Shared\Projects\Pluct\pluct-business-engine`

Focused Vitest (live `.dev.vars` or env for JWT secret):

```text
cd C:\Shared\Projects\Pluct\pluct-business-engine
npx vitest run --bail=1 test/Pluct-Mobile-Error-Scenarios-02CreditsRefundOnClientErrors-01Validation.spec.ts
npx vitest run --bail=1 test/Pluct-TikTok-URL-01MetadataRecovery-01HelperValidation.spec.ts
```

## Git

Commit mobile and engine repos separately (two remotes). Message format: `[Type]: [Scope] - [Summary]`.

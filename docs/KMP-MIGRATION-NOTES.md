# KMP Migration Notes

## Moved to shared

- API DTOs for balance, estimate, token vending, metadata, transcription submit/status/result, build info, metrics, and health.
- TikTok URL extraction, validation, and normalization.
- API error category mapping.
- Retry decisions and exponential backoff.
- Transcription phase/state names.
- Client policy parsing.
- Device profile payload shape and fingerprinting.
- Request ID generation.

## Still Android-owned

- Compose UI.
- Hilt dependency injection.
- Room entities, DAO, migrations, and repository.
- DataStore and SharedPreferences wiring.
- WorkManager foreground/background processing.
- Android share intent, notification channels, progress notifications, and overlay behavior.
- Node/ADB and Maestro validation harnesses.
- Existing Android HTTP execution path for this migration pass.

## Compatibility approach

Android keeps existing package-level symbols where possible by using thin wrappers and typealiases. That keeps the current app behavior stable while making the same models and product rules available to iOS through the `Shared` KMP framework.

The iOS v1 app intentionally starts with manual paste/input. It does not read SMS, does not implement persistent Android-style notifications, and does not include a share extension. Share extension work should follow after the first TestFlight build succeeds.

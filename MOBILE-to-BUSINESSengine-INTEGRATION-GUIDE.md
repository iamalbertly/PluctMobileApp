# Mobile app to Pluct Business Engine

This document is the single integration reference for the Android client. **Do not store production JWT signing secrets in the mobile app or in public documentation.** Use your deployment’s secure configuration (CI secrets, remote config, or server-issued tokens) consistent with your security model.

## Base URL

Production Business Engine base URL (verify in your environment):

`https://pluct-business-engine.romeo-lya2.workers.dev`

## Contract overview

1. **Health**: `GET /health` — verify connectivity before user-visible flows.
2. **Balance**: `GET /v1/credits/balance` or legacy `POST /v1/user/balance` with JWT `Authorization: Bearer`.
3. **Vend token**: `POST /v1/vend-token` — exchanges credits for a short-lived transcription token.
4. **Metadata (optional)**: `GET /meta?url=...`
5. **Transcription**: `POST /ttt/transcribe` then `GET /ttt/status/{jobId}` with the short-lived token.

## JWT expectations (client)

- Claims must include `sub`, `scope` (e.g. `ttt:transcribe`), `iat`, `exp` with bounded lifetime.
- Handle `401`, `402`, `429`, and `500` with the retry and user-messaging policy defined in product requirements.

## Version coordination

Coordinate breaking changes using app `VERSION_CODE`, `GET /v1/public/client-policy`, and `GET /health/services` as described in the main [README.md](README.md).

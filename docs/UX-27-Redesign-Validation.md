# UX-27 — Redesign and shell validation

## Purpose

Validate the **Customer / Realism & Simplicity / Speed & Trust** rollout:

- Bottom navigation: Home, Library, Settings (single shell; fewer dead-ends than modal-only settings).
- Capture card: bordered shell, **wallet chip** (`capture_wallet_chip`) with number-only balance and tap-to-refresh.
- Header: **logo mark + Pluct** (`home_shell_top_bar`, `pluct_brand_logo_mark`) without duplicate header credit chip when using the shell.
- Home list: **Active** and **Recent** sections with **View all** routing to Library.
- Reliability: network/timeout persistent errors clear when connectivity returns; snackbar debounce bypass for forced paths; DB cache after canonical URL; local completed transcript fallback on orchestrated failure; completion notification cancels prior progress id.

## Automated run

```bash
npm run test:redesign
```

Or:

```bash
node scripts/nodejs/Pluct-Test-Focused-09Redesign-01Runner.js
```

Requires a device/emulator with `adb` and the debug build installed (`assembleDebug` + `adb install -r`). The journey installs `app/build/outputs/apk/debug/app-debug.apk` when present, wakes the screen, and falls back to **activity stack** checks if `uiautomator` still captures the lock shade (wireless ADB / Samsung overlays).

## Manual checklist

1. Open app → confirm bottom tabs and Home capture card visible.
2. Tap **Library** → list or empty state; open a row → detail; back clears detail to shell.
3. Tap **Settings** → same sections as sheet (shared body); debug logs opens overlay from Main when wired.
4. Toggle airplane mode during a network error on capture, then restore → error banner should clear when online.
5. Wallet chip tap triggers balance refresh (watch log / UI).

## Rationale summary

| Change | Why |
|--------|-----|
| Shell + tabs | Reduces modal stack depth; matches mental model (Home vs full library). |
| Wallet without “credits” word | Low-literacy and non-English users scan numbers + icon. |
| Post-canonical cache + DB fallback | Short links rot; canonical URL and local SSOT avoid double pain. |
| Snackbar `forceBypassDebounce` | Allows explicit recovery messaging without starving the queue. |
| `notificationManager.cancel` before complete | Avoids stuck ongoing progress when completion posts. |

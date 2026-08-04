---
description: Analyze APK metadata — package name, version, protections
agent: morphe-orchestrator
---
## Task: Recon APK `$ARGUMENTS`

Identify the APK metadata for the app `$ARGUMENTS`.

1. Find the APK file in `apks/` matching this app
2. Use `aapt dump badging` to extract package name, version, SDK targets
3. Check for protections (ProGuard, debug flags, exported components)
4. Check native library architectures
5. Write findings to `analysis/$ARGUMENTS/notes/recon.md`

Expected output: `analysis/$ARGUMENTS/notes/recon.md`

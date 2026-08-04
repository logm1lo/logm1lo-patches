---
description: Hunt for patch targets in decompiled APK code
agent: morphe-orchestrator
---
## Task: Hunt Targets for `$ARGUMENTS`

Search the decompiled code of `$ARGUMENTS` for patch targets.

For the first argument specify the app name, for the second specify what to find:
- `premium` — billing, license, subscription checks
- `ads` — ad SDK integrations (AdMob, UnityAds, etc.)
- `gates` — feature flags, remote config checks
- `auth` — login/authentication gates
- `all` — search everything

Guide the target-hunter agent to search Java sources with regex patterns,
verify findings in Smali bytecode, and document methods and their signatures
for fingerprinting.

Expected output: `analysis/$ARGUMENTS/notes/<target>-bypass.md`

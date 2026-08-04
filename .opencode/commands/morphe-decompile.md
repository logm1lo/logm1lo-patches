---
description: Decompile APK to Java source and Smali bytecode
agent: morphe-orchestrator
---
## Task: Decompile `$ARGUMENTS`

Decompile the APK for `$ARGUMENTS` for analysis.

1. Read the recon notes first: `analysis/$ARGUMENTS/notes/recon.md`
2. Run jadx to produce Java source at `analysis/$ARGUMENTS/decompiled/`
3. Run baksmali to produce Smali at `analysis/$ARGUMENTS/smali/`
4. Handle split APKs (.apks/.apkm) by extracting base APK first

Expected output:
- `analysis/$ARGUMENTS/decompiled/sources/`
- `analysis/$ARGUMENTS/smali/`

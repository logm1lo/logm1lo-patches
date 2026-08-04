# Logm1lo Patches — Morphe APK Patching Workspace

This is an AI-powered Android APK patching workspace built on the Morphe ecosystem.
It provides tools, agents, and reference material for decompiling APKs, finding patch
targets, writing Kotlin-based Morphe patches, and building/deploying patched APKs.

## Workflow Overview

The 6-stage pipeline:

```
RECON → DECOMPILE → HUNT TARGETS → WRITE PATCH → BUILD + DEPLOY
```

Each stage has a corresponding opencode subagent. The orchestrator routes tasks
to the correct specialist.

## Quick Commands

- `/morphe-recon <app-package>` — Identify APK metadata (package, version, protections)
- `/morphe-decompile <app-name>` — Decompile to Java (jadx) + Smali (baksmali)
- `/morphe-hunt <app-name> <target>` — Search code for patch targets (premium, ads, etc.)
- `/morphe-write <app-name>` — Write patch code (Constants.kt, Fingerprints.kt, Patch.kt)
- `/morphe-build <app-name>` — Build MPP, apply to APK, install via ADB

## APK Storage

Place APK files in `apks/`. Analysis output goes to `analysis/<app-name>/`.

## Patch Development

Patches live in `patches/src/main/kotlin/app/logm1lo/patches/<app-name>/`.

### Patch File Structure
```
patches/src/main/kotlin/app/logm1lo/patches/<app>/
├── shared/
│   └── Constants.kt        # Compatibility declarations
├── <category>/
│   ├── Fingerprints.kt      # Smali method fingerprints
│   └── <Name>Patch.kt       # Patch logic
```

### Patch Types
- `bytecodePatch` — modifies Dalvik bytecode (fast, preferred)
- `resourcePatch` — modifies decoded XML resources
- `rawResourcePatch` — modifies raw files without decoding

### Build
```bash
./gradlew buildAndroid          # compile patches to .mpp
./gradlew :patches:buildAndroid # same, scoped to patches module
```

## Branch Strategy
- `dev` — development (pre-releases via semantic-release)
- `main` — stable releases (Morphe Manager pulls from here)
- Always use conventional commits: `feat:`, `fix:`, `chore:`

## Key Tools
- `jadx` — decompiles APK to Java source
- `baksmali` — disassembles DEX to Smali bytecode
- `aapt` / `aapt2` — APK metadata extraction
- `apktool` — APK resource decoding
- `adb` — install patched APKs to device
- `morphe-cli.jar` — apply MPP to APK + sign

## Environment
- JDK 21+ required (auto-detected from `/usr/lib/jvm/java-21-openjdk`)
- GitHub PAT with `read:packages` scope needed in `~/.gradle/gradle.properties`:
  ```properties
  gpr.user = <github-username>
  gpr.key = <github-personal-access-token>
  ```

## Morphe Ecosystem References
- Morphe Patcher: https://github.com/MorpheApp/morphe-patcher
- Morphe Documentation: https://github.com/MorpheApp/morphe-documentation
- Morphe Patches (official): https://github.com/MorpheApp/morphe-patches
- Patches Template: https://github.com/MorpheApp/morphe-patches-template
- Community patterns: https://github.com/Paresh-Maheshwari/morphe-ai

## Local Knowledge Base
When hunting targets or writing patches, load these references from `.opencode/references/`:
- `smali-cheat-sheet.md` — complete Smali/Dalvik opcode and type descriptor reference
- `patch-anatomy.md` — Morphe patch DSL, compatibility, options, extensions, finalization
- `fingerprinting.md` — fingerprint API, filter types, location constraints, two-stage matching
- `community-patterns.md` — 60+ cataloged techniques from 70+ community patch repos
- `bypass-patterns.md` — search patterns for billing, ads, protections, SDKs, framework detection
- `advanced-techniques.md` — native binary patching, extension architecture, system spoofing, integrity bypass
- `patcher-api.md` — classDefBy, document, get, delete, BytecodeUtils APIs

## Framework-Aware Patching
Before writing fingerprints, identify the app's framework (see `bypass-patterns.md`):
- **Native Kotlin/Java** — standard smali bytecode patching
- **Flutter (Dart AOT)** — requires hex patching `libapp.so`, not smali
- **React Native (Hermes)** — requires binary patching of `index.android.bundle`
- **Unity (IL2CPP)** — requires ARM64 RET overwrites on `libil2cpp.so`
- **Cocos2d-x / GameMaker** — check native lib structures

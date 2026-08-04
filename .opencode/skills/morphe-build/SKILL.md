---
name: morphe-build
description: Build Morphe MPP files, apply patches to APKs using morphe-cli.jar, sign, install via ADB, and manage the keystore
---

# Morphe Build & Deploy

## What I do
- Guide building MPP files from patch source
- Show how to apply patches to APKs with morphe-cli.jar
- Explain signing, installation, and testing workflow
- Provide troubleshooting for common build issues

## When to use me
Use this when building patches, applying them to APKs, deploying to devices,
or debugging build/signing issues.

## Build Commands

### Compile Patches to MPP
```bash
./gradlew :patches:buildAndroid
```
Output at `patches/build/libs/patches-<version>.mpp`

### Apply Patches to APK (Quick)
```bash
java -Xms1024m -jar morphe-cli.jar patch \
  --patches patches/build/libs/patches-*.mpp \
  --out analysis/<app>/builds/<app>_patched.apk \
  apks/<app-file>
```

### Apply + Install in One Step
```bash
java -jar morphe-cli.jar patch \
  --patches patches/build/libs/patches-*.mpp \
  -i \
  apks/<app-file>
```

### List Available Patches
```bash
java -jar morphe-cli.jar list-patches \
  --patches patches/build/libs/patches-*.mpp
```

### List Compatible Versions
```bash
java -jar morphe-cli.jar list-versions \
  --patches patches/build/libs/patches-*.mpp \
  apks/<app-file>
```

## Key CLI Flags

| Flag | Description |
|------|-------------|
| `-p`, `--patches` | Path to MPP file (repeatable for multiple bundles) |
| `-e "Name"` | Enable specific patch |
| `-d "Name"` | Disable specific patch |
| `--exclusive` | Disable all except explicitly enabled |
| `-O key=value` | Set patch option |
| `-f`, `--force` | Skip version compatibility check |
| `-i`, `--install` | Install to ADB device after patching |
| `--mount` | Root mount install (preserves data) |
| `--keystore file` | Custom signing keystore (BKS format) |
| `--striplibs arch` | Keep only specific architectures |
| `--continue-on-error` | Skip failed patches |
| `--options-file path` | Load options from JSON file |

## Multiple Patch Bundles
```bash
java -jar morphe-cli.jar patch \
  -p my-patches.mpp -e "My Patch" \
  -p official-patches.mpp -d "Ads removal" \
  apks/<app-file>
```

## Keystore Management

### Generate new keystore
```bash
keytool -genkey -v -keystore Morphe.keystore \
  -alias Morphe -keyalg RSA -keysize 2048 \
  -validity 10000 -storepass "" -keypass "Morphe"
```

**Critical:** Use the same keystore for updates or existing installs will conflict.

### Default keystore location
Morphe CLI looks for `morphe-data/morphe.keystore` or `./Morphe.keystore`.

## Install Methods

| Method | Command | Notes |
|--------|---------|-------|
| ADB install | `adb install <apk>` | Standard, requires device |
| CLI install | `-i` flag | Same as ADB install |
| Root mount | `--mount` | Preserves app data, requires root |

## Gradle Auth

If builds fail with 401:
```properties
# ~/.gradle/gradle.properties
gpr.user = <your-github-username>
gpr.key = <github-pat-with-read:packages>
```

## Environment
- JDK 21+ for compilation
- JAVA_HOME must point to JDK 21+
- `git` for version control workflow
- `adb` for device deployment

## Integration Tests

### Verify build compiles
```bash
./gradlew :patches:buildAndroid && echo "BUILD SUCCESS"
```

### Quick test cycle
```bash
./gradlew :patches:buildAndroid && \
java -jar morphe-cli.jar patch \
  --patches patches/build/libs/patches-*.mpp \
  -f -i apks/<app-file>
```

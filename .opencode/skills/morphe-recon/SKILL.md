---
name: morphe-recon
description: APK reconnaissance — identify package name, version, protections, SDK targets, and APK structure using aapt and unzip
---

# Morphe APK Reconnaissance

## What I do
- Guide extraction of APK metadata
- Identify protection mechanisms (ProGuard, DexGuard, obfuscation)
- Detect split APK structures
- Enumerate native library architectures
- Map exported Android components

## When to use me
Use this when a new APK file is added to `apks/` and you need to analyze it
before decompilation and patching.

## Recon Commands

### Basic Metadata
```bash
aapt dump badging apks/<file> | head -50
```

Key fields:
- `package:` — package name, version code, version name
- `sdkVersion:` — min and target SDK versions
- `native-code:` — supported CPU architectures
- `uses-permission:` — requested permissions
- `launchable-activity:` — main activity

### APK Type Detection
```bash
unzip -l apks/<file> | head -30
```

- `.apk` — single APK, has `AndroidManifest.xml` at root
- `.apks` — APK set (split), contains multiple `.apk` files
- `.xapk` — XAPK bundle, may contain `.obb` expansion files
- `.apkm` — APKM bundle (APKPure format)

### Split APK Structure
```bash
unzip -l apks/<file> | grep "\.apk$"
```

Look for: `base.apk`, `config.arm64_v8a.apk`, `config.en.apk`, `config.xxhdpi.apk`, etc.

### Protection Detection
```bash
# Check for known obfuscators
unzip -l apks/<file> | grep -iE "(dexguard|proguard|dengrader|armadillo|bangcle)"

# Check for debug flags in manifest
aapt dump xmltree apks/<file> AndroidManifest.xml | grep -iE "debuggable"

# Check for exported components (potential attack surface)
aapt dump xmltree apks/<file> AndroidManifest.xml | grep -iE "exported.*true"

# Check for SSL pinning libraries
unzip -l apks/<file> | grep -iE "(okhttp|pinning|trustkit|certificate)"
```

### Native Libraries
```bash
unzip -l apks/<file> | grep "lib/" | head -30
```

Check architectures: `arm64-v8a`, `armeabi-v7a`, `x86_64`, `x86`

### Package List (all classes)
```bash
# From decompiled sources (after jadx)
find analysis/<app>/decompiled/sources/ -type f -name "*.java" | head -50
```

### Framework Detection

Determine what technology the app is built with — this determines the patching approach:

#### Flutter (Dart AOT)
```bash
unzip -l apks/<file> | grep -E "lib/arm64-v8a/(libapp|libflutter)\.so"
```
Flutter premium checks are in `libapp.so` (Dart AOT snapshot). Requires hex patching, not smali.

#### React Native
```bash
unzip -l apks/<file> | grep "assets/index.android.bundle"
unzip -l apks/<file> | grep -E "libhermes|libjsc"
```
Hermes stores JS as bytecode in bundle. May require binary patching.

#### Unity / IL2CPP
```bash
unzip -l apks/<file> | grep "libil2cpp.so"
unzip -l apks/<file> | grep "global-metadata.dat"
```
Unity games with IL2CPP require ARM64 RET overwrites on `libil2cpp.so`.

#### Cocos2d-x / GameMaker
```bash
unzip -l apks/<file> | grep -E "libcocos2dcpp|libcocos2djs|libyoyo"
```

#### Kotlin Multiplatform (KMP)
```bash
unzip -l apks/<file> | grep "libkn"
```

### Protection Detection (Extended)

#### Obfuscation
```bash
unzip -l apks/<file> | grep -iE "(dexguard|proguard|obfusc|guard|protect)"
```

#### PairIP License Protection
```bash
unzip -l apks/<file> | grep -iE "pairip|depairip|signaturecheck|vmrunner"
```

#### Knox / Samsung Integrity
```bash
unzip -l apks/<file> | grep -iE "knox|sak|samsung"
```

#### Device Attestation / Root Checks
```bash
unzip -l apks/<file> | grep -iE "safetynet|playintegrity|rootbeer|talsec|freerasp"
```

#### SSL Pinning / Certificate Checks
```bash
unzip -l apks/<file> | grep -iE "okhttp.*pinning|certificatepinner|trustkit"
```

#### Code Transparency (Microsoft Office)
```bash
unzip -l apks/<file> | grep -iE "codetransparency|codetrans"
```

### SDK Inventory
```bash
# Billing
unzip -l apks/<file> | grep -iE "billing|purchases|revenuecat"

# Ads
unzip -l apks/<file> | grep -iE "admob|adloader|unityads|applovin|ironsource"

# Analytics
unzip -l apks/<file> | grep -iE "firebase|appsflyer|bugsnag|adjust|amplitude"
```

## Output Template

Save to `analysis/<app>/notes/recon.md`:

```markdown
# Recon: <App Name>

## Package Info
- Package: `com.example.app`
- Version: `1.2.3` (code: `123`)
- APK Type: APK
- Target SDK: 34 | Min SDK: 24

## Framework
- Type: Native/Kotlin / Flutter / React Native / Unity / Cocos2d
- Primary patching approach: smali / hex (.so) / binary bundle

## Protection
- ProGuard/obfuscation: yes/no (classes named a, b, c...)
- PairIP: present/not found
- Knox/Samsung: present/not found
- Device Attestation: present/not found
- Code Transparency: present/not found
- Debuggable: true/false
- SSL Pinning: present/not found

## SDK Inventory
- Billing: Google Play Billing / RevenueCat / Custom
- Ads: AdMob / Unity Ads / AppLovin / Others
- Analytics: Firebase / AppsFlyer / Bugsnag / Others

## Architecture
- Native libs: arm64-v8a, armeabi-v7a
- Split APK: no

## Key Components
- Main Activity: `com.example.MainActivity`
- Exported Services: 2
- Exported Receivers: 3
- Permissions: INTERNET, ACCESS_NETWORK_STATE, etc.
```

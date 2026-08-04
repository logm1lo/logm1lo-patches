# Bypass Patterns Reference

Common patterns for bypassing app protections, billing, ads, and feature gates.

## Billing / License Bypass Patterns

### RevenueCat / Purchases SDK
Search for:
```bash
rg "CustomerInfo|EntitlementInfos|getEntitlements|getActive|purchases" smali/ -l
```

Common pattern — find `isActive` check and force true:
```kotlin
object EntitlementFingerprint : Fingerprint(
    returnType = "Z",
    parameters = listOf("Lcom/revenuecat/purchases/CustomerInfo;"),
    filters = listOf(
        methodCall(definingClass = "Lcom/revenuecat/purchases/CustomerInfo;", name = "getEntitlements"),
        methodCall(definingClass = "Lcom/revenuecat/purchases/EntitlementInfos;", name = "getActive"),
        opcode(Opcode.MOVE_RESULT_OBJECT, MatchAfterImmediately())
    )
)
```

### Google Play Billing v5+
Search for:
```bash
rg "BillingClient|BillingResult|queryPurchases|getPurchases" smali/ -l
```

Common pattern — find purchase verification:
```kotlin
object PurchaseVerifyFingerprint : Fingerprint(
    returnType = "Z",
    parameters = listOf("Ljava/lang/String;"),
    filters = listOf(
        string("inapp"),
        methodCall(name = "queryPurchasesAsync"),
        opcode(Opcode.IF_EQ, MatchAfterWithin(15))
    )
)
```

### Custom License Verification
Search for:
```bash
rg "LicenseCheck|LicenseValidator|verifyLicense|isLicensed" smali/ -l
```

## Ad Removal Patterns

### AdMob (Google Mobile Ads)
```bash
rg "AdLoader|AdView|InterstitialAd|MobileAds|loadAd|AdRequest" smali/ -l
rg "ca-app-pub-" smali/ -l  # Ad Unit IDs
```

### Unity Ads
```bash
rg "UnityAds|IUnityAdsLoadListener|IUnityAdsShowListener" smali/ -l
```

### AppLovin
```bash
rg "AppLovinSdk|MaxAd|MaxRewardedAd|showAd" smali/ -l
```

### IronSource
```bash
rg "IronSource|IronSourceBanner|loadInterstitial" smali/ -l
```

### Vungle / Liftoff
```bash
rg "Vungle|loadAd.*placement" smali/ -l
```

### Meta/Facebook Audience Network
```bash
rg "com.facebook.ads|AudienceNetwork" smali/ -l
```

### Generic Ad View (common base class)
```bash
rg "AdView|onAdLoaded|onAdFailed|destroyAd" smali/ -l
```

## Feature Gate Patterns

### Boolean Flag Gates
```bash
rg "isEnabled|isFeatureEnabled|featureFlag|FeatureGate" smali/ -l
rg "getBoolean.*feature|remoteConfig.*getBoolean" smali/ -l
```

Common pattern — simple boolean return method:
```kotlin
object FeatureGateFingerprint : Fingerprint(
    returnType = "Z",
    parameters = listOf("Ljava/lang/String;"),
    filters = listOf(
        methodCall(name = "getBoolean", returnType = "Z"),
        opcode(Opcode.MOVE_RESULT, MatchAfterImmediately()),
        opcode(Opcode.RETURN)
    )
)
```

### Firebase Remote Config
```bash
rg "FirebaseRemoteConfig|getBoolean|getString|getLong" smali/ -l
```

### Experiment / A/B Test Checks
```bash
rg "Experiment|variant|treatment|control|ABTest" smali/ -l
```

## Auth / Login Gate Patterns

### isLoggedIn / isAuthenticated
```bash
rg "isLoggedIn|isAuthenticated|checkAuth|requireAuth" smali/ -l
```

### Account Manager
```bash
rg "AccountManager|getAccounts|Account|authenticate" smali/ -l
```

### Token Validation
```bash
rg "accessToken|refreshToken|validateToken|tokenExpired" smali/ -l
```

## Protection / Anti-Tampering Patterns

### Root Detection
```bash
rg "isRooted|checkRoot|RootBeer|SafetyNet|su" smali/ -l
rg "test-keys|Superuser|Magisk" smali/ -l
```

### Debug / Emulator Detection
```bash
rg "isDebuggable|isEmulator|FLAG_DEBUGGABLE" smali/ -l
```

### SSL Pinning
```bash
rg "CertificatePinner|okhttp3.CertificatePinner|TrustManager" smali/ -l
rg "checkServerTrusted|checkClientTrusted" smali/ -l
```

## Smali Scanning Commands

### Find all boolean-returning methods
```bash
rg "^\.method.*\)Z$" analysis/<app>/smali/ -l
```

### Find all methods in a specific class
```bash
rg "^\.method " analysis/<app>/smali/classes/com/example/ClassName.smali
```

### Find methods by string content
```bash
rg -B5 "const-string.*premium" analysis/<app>/smali/ | rg "\.method "
```

### Find method invocations to a specific class
```bash
rg "invoke.*Lcom/revenuecat/" analysis/<app>/smali/
```

### Count methods in a DEX
```bash
rg -c "^\.method " analysis/<app>/smali/classes/
```

## App Framework Detection

### Flutter (Dart AOT)
```bash
unzip -l apks/<file> | grep "lib/arm64-v8a/libapp.so"
unzip -l apks/<file> | grep "lib/arm64-v8a/libflutter.so"
```
Flutter premium checks often compile to `libapp.so` — requires hex patching, not smali.

### React Native (Hermes / JavaScriptCore)
```bash
unzip -l apks/<file> | grep "assets/index.android.bundle"
unzip -l apks/<file> | grep "libhermes.so"
unzip -l apks/<file> | grep "libjsc.so"
```
Hermes apps store JS as bytecode in `index.android.bundle` — requires binary patching.

### Unity / IL2CPP
```bash
unzip -l apks/<file> | grep "lib/arm64-v8a/libil2cpp.so"
unzip -l apks/<file> | grep "global-metadata.dat"
unzip -l apks/<file> | grep "UnityPlayerActivity"
```
Unity games with IL2CPP require ARM64 RET overwrites, not smali patching.

### Cocos2d-x / GameMaker
```bash
unzip -l apks/<file> | grep "libcocos2dcpp.so"
unzip -l apks/<file> | grep "libcocos2djs.so"
unzip -l apks/<file> | grep "libyoyo.so"
```

### Kotlin Multiplatform (KMP)
```bash
unzip -l apks/<file> | grep "lib/arm64-v8a/libkn"
```

## PairIP / License Protection

### PairIP Detection
```bash
rg "SignatureCheck|VMRunner|StartupLauncher|verifyIntegrity" smali/ -l
rg "LicenseClient|checkLicense|startupLauncher" smali/ -l
```
PairIP uses code virtualization. Requires de-virtualization framework (see advanced-techniques.md).

### Microsoft Code Transparency
```bash
rg "CodeTransparencyCheck|transparencyVerification" smali/ -l
```
MS Office apps check code transparency on startup.

### Knox / Samsung Integrity (Samsung Apps)
```bash
rg "KnoxAdapter|IcccAdapter|KnoxControl|SakChecker" smali/ -l
rg "HomeAppCloseActivity|KnoxHandlerViewModel" smali/ -l
rg "com\.osp\.app\.signin" smali/ -l
```

### Device Attestation / Integrity
```bash
rg "SafetyNet|PlayIntegrity|IntegrityCheck|IsTampered" smali/ -l
rg "ProxyStateInterceptor|DeviceAttest" smali/ -l
```

## Extended SDK Bypass Patterns

### RevenueCat (additional)
```bash
rg "Purchases.sharedInstance|configure.*apiKey" smali/ -l
rg "CustomerInfo|PurchaserInfo|Entitlement" smali/ -l
```

### Analytics / Telemetry (12+ SDKs)
```bash
rg "AppsFlyer|Bugsnag|FirebaseAnalytics|Crashlytics" smali/ -l
rg "GoogleAnalytics|OneDS|Adjust|Amplitude|Mixpanel" smali/ -l
rg "Segment|CleverTap|MoEngage|Braze|FullStory" smali/ -l
```

### AppsFlyer Neutralization
```bash
rg "AppsFlyerLib.*init|AppsFlyerLib.*start" smali/ -l
rg "ConversionDataListener|AppsFlyerConversionListener" smali/ -l
```

### Bugsnag Neutralization
```bash
rg "Bugsnag.*start|Bugsnag.*init" smali/ -l
rg "com\.bugsnag\.android" smali/ -l
```

### Firebase Analytics / Crashlytics / Performance
```bash
rg "FirebaseAnalytics.*getInstance|FirebaseCrashlytics" smali/ -l
rg "com\.google\.firebase\.analytics" smali/ -l
rg "ComponentRegistrar|ComponentDiscoveryService" smali/ -l
```

### Google Ads / AdMob (additional)
```bash
rg "MobileAds\.initialize|AdLoader\.Builder" smali/ -l
rg "com\.google\.android\.gms\.ads" smali/ -l
rg "AD_SERVICES_CONFIG|google\.android\.gms\.ads" smali/ -l
```

### Google Engage
```bash
rg "GoogleEngage|engage\.createWork|EngageWorker" smali/ -l
```

### AppLovin MAX Mediation (full stack)
```bash
rg "AppLovinSdk.*initializeSdk|MaxAdFormat" smali/ -l
rg "Pangle|Amazon.*APS|Chartboost|InMobi" smali/ -l
rg "Mintegral|Fyber|Bigo|Vungle" smali/ -l
```

### freeRASP / Talsec
```bash
rg "Talsec|freeRASP|RASP|com\.aheaditec\.talsec" smali/ -l
rg "Unable to run Talsec" smali/ -l
```

### OneDS Logger (Microsoft Edge / Office)
```bash
rg "OneDS|EventLogger|LogManager" smali/ -l
rg "com\.microsoft\.clientservices" smali/ -l
```

### DNS Sinkhole Pattern (Telemetry)
```bash
rg "telemetry|analytics\.collect|csi\.google" smali/ -l
rg "events\.google|log\.ingest" smali/ -l
```

## System-Level Spoofing Targets

### Build.* Field Interception
```bash
rg "sget-object.*Landroid/os/Build;->(MODEL|BRAND|DEVICE|MANUFACTURER|FINGERPRINT|PRODUCT)" smali/ -l
rg "sget-object.*Landroid/os/Build\$VERSION;->(RELEASE|SDK_INT)" smali/ -l
```

### SystemProperties Interception
```bash
rg "SystemProperties|android\.os\.SystemProperties" smali/ -l
rg "SystemProperties\.get|getprop" smali/ -l
```

### SIM / Telephony Spoofing
```bash
rg "getSimCountryIso|getNetworkOperator|getSimOperator" smali/ -l
rg "TelephonyManager|SubscriptionManager" smali/ -l
```

### VPN / Network Detection
```bash
rg "TRANSPORT_VPN|hasTransport|hasCapability" smali/ -l
rg "NetworkCapabilities|ConnectivityManager|getNetworkInterface" smali/ -l
```

### Emulator Detection (25+ indicators)
```bash
rg "goldfish|ranchu|vbox|generic" smali/ -l
rg "libc_malloc_debug_qemu|qemu\.hw\.mainkeys" smali/ -l
rg "isEmulator|checkEmulator|emulator" smali/ -l
```

### Knox / SEAndroid / Root
```bash
rg "ro\.boot\.verifiedbootstate|ro\.boot\.flashed" smali/ -l
rg "ro\.build\.selinux|ro\.secure" smali/ -l
```

## Server-Side Gate Patterns

### Feature Flag / Rollout Check
```bash
rg "getBoolean.*flag|getBoolean.*feature|isFeatureEnabled" smali/ -l
rg "remoteConfig.*getBoolean|FirebaseRemoteConfig.*getBoolean" smali/ -l
rg "setExperimentIds|ExperimentId" smali/ -l
```

### Subscription Status (Server-Synced Cache)
```bash
rg "getSubscription|isSubscribed|syncSubscription" smali/ -l
rg "syncPlans|PremiumStatus|fetchEntitlements" smali/ -l
```

### VPN / Proxy Detection (Server-Side Verdict)
```bash
rg "ProxyStateInterceptor|checkProxy|isUsingProxy" smali/ -l
```

## Signature & Certificate Patterns

### Package Signature Verification
```bash
rg "getPackageInfo.*GET_SIGNATURES|getPackageInfo.*GET_SIGNING_CERTIFICATES" smali/ -l
rg "Signature.*hashCode|Signature.*toByteArray|checkSignatures" smali/ -l
```

### Certificate Pinning
```bash
rg "CertificatePinner|TrustManager|HostnameVerifier" smali/ -l
rg "checkServerTrusted|checkClientTrusted|verify" smali/ -l
```

### Package Manager
```bash
rg "PackageManager.*getInstallerPackageName|getInstallerPackages" smali/ -l
rg "PackageManager.*getApplicationInfo|getPackageInfo" smali/ -l
```

## Advanced Warning Patterns (Hard or Impossible)

These checks are server-enforced and cannot be bypassed with client-only patches:
- **Server-side subscription validation** (app checks with server, not local cache)
- **Server-enforced feature flags** (state lives on server, not client)
- **Remote config with server validation** (client gets values from server)
- **Real-time integrity attestation** (Play Integrity API with nonce)

For these, the approach should be to find what LOCAL effects the server verdict has
and intercept those, rather than trying to spoof the server response itself.

## Case Study: Zalo 26.08.01 — Obfuscated Class + Non-Obfuscated Package Coexistence

### Discovery
Zalo's main codebase is heavily R8-obfuscated (classes `a`, `aa`, `m00`, `gx`, etc.), but
identifiable packages survive under `com.zing.zalo.comm.*`. These are the module boundaries
where obfuscation doesn't cross: `com.zing.zalo.comm.sendmessage`, `com.zing.zalo.comm.chathead`,
`com.zing.zalo.comm.e2ee`, etc. Key classes like `FileTooLargeException`, `BuildConfig`, and
exception types remain in their original packages.

### File Size Bypass Pattern (Zalo-specific, applicable generally)

```bash
# Find FileTooLargeException references in smali
rg -l "FileTooLargeException\|exceeds maximum" smali/
# Trace the class that throws it back to the obfuscated caller
# grep the smali for `FileTooLargeException;-><init>(JJ)V` to find call sites
```

**Patch technique:** Replace `cmp-long` (file size vs max) with `const/4` to force comparison
to always return "equal", preventing the exception from ever being thrown.

### Feature Flag Manipulation Pattern

When an app has a `BuildConfig.java` class with `boolean` constants:
```bash
rg -l "BuildConfig\|ENABLE_\|BUILD_\|FLAVOR\|PRODUCTION" decompiled/sources/
```

These compile-time flags can be overridden by finding the `sget-boolean` reference in smali
and replacing with `const/4 vX, 0x1` or `0x0` depending on desired state. BuildConfig fields
are accessed as static fields `sget-boolean vX, Lpkg/BuildConfig;->FIELD_NAME:Z`.

## Case Study: Calistree 5.8.5 — Flutter + RevenueCat

### Detection
```bash
unzip -l apks/<file> | grep "flutter_assets"
unzip -l <arm64_split> | grep "libapp.so"
strings libapp.so | grep -i "purchases_flutter\|revenuecat"
```

### RevenueCat Detection in libapp.so
```bash
strings lib/arm64-v8a/libapp.so | grep -iE "Entitlement|CustomerInfo|purchases_flutter|revenuecat|subscription|activeSub"
```
Found strings include: `EntitlementInfo.fromJson`, `revenueCatEntitlements`, `activeSubscriptions`,
`startPurchase`, `purchase_membership`, `onTapPremium`, `FREE_TRIAL`.

### Hex Patching libapp.so
ARM64 MOV X0, #1 + RET hex for Dart AOT boolean functions:
```
MOV W0, #1: 20 00 80 52
RET:        C0 03 5F D6
```
Search for function prologues near RevenueCat strings, overwrite with `20 00 80 52 C0 03 5F D6`

## Non-Obfuscated Packages in Heavily Obfuscated Apps

When class names are all single/double letters but identifiable packages exist,
these packages typically form at module boundaries. Search for them:
```bash
find decompiled/sources/ -name "*.java" | xargs grep -l "package com\." 2>/dev/null | head -30
```
These are stable fingerprint targets even in obfuscated builds.

## BuildConfig Feature Flag Enumeration
```bash
rg "public static final boolean " decompiled/sources/ -r '$0'
```
Flags like `PRODUCTION`, `CI`, `ENABLE_FIREBASE_*`, `BUILD_PLAY_STORE` can be toggled
by patching the `sget-boolean` instructions that reference `BuildConfig` fields.

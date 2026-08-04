# Community Patch Techniques — Master Catalog

Techniques extracted from 70+ Morphe/ReVanced community patch repositories.
Organized by category for reference when writing patches.

## Table of Repositories Analyzed

| Repository | Stars | Notable for |
|-----------|-------|------------|
| hoo-dles/morphe-patches | — | Extension-based architecture, method injection |
| jkennethcarino/adobo | 54 | Per-SDK ad blocking, hosts-based blocking, BytecodeUtils library |
| crimera/piko | 370+ | Instagram patches, synthetic method injection, employee option unlock |
| wchill/patcheddit | — | Reddit OAuth factory, WebView interception, post undeletion |
| MeridianFresco/morphe-meta-patches | — | Facebook/Meta sponsored content, GPL attribution model |
| AmpleReVanced/revanced-patches | — | Integrity bypass suite, Moat/PackageManager/checksums, native .so injection, Loco packet interception |
| anddea/revanced-patches | 1,800+ | Extension module architecture, multi-version maintenance |
| binarymend/morphe-patches | 134 | Telemetry/analytics disabling, certificate seeding |
| rabilrbl/fluffy-patches | 25 | AI-assisted development workflow, spec-driven development |
| cesbar/zpatches | 11 | PairIP bypass, manifest permission stripping, Google Maps API key replacement, universal export-all-activities |
| rushiranpise/morphe-patches | 287 | 290+ patches, WebView CSS injection, Build property spoofing, multi-sub-patch orchestration, dynamic constructor rewriting, cert seeding |
| brosssh/morphe-patches | 307 | Instagram curation, Extension Java↔bytecode interop, reflection Enum manipulation, multi-class field extraction |
| xob0t/morphe-patches | 30 | Native RASP stub, VPN/emulator/USB spoofing, register-window scanning, freeRASP disabler |
| hxreborn/morphe-patches | 18 | Native .so binary patching (Rust core), AMOLED theme extension injection |
| kiraio-moe/Lain-Patches | 34 | 40 patches, instruction removal patterns, region lock bypass, universal utilities |
| BholeyKaBhakt/android-patches-xtra | 60 | IL2CPP native patching, PairIP de-virtualization framework, typed returnEarly() utilities |
| jasonwu1994/Gboard-patches | 175 | Custom web clipboard server, rollout gate unlocking, Gboard customization |
| icysymmetra/tiktok-patches | 186 | Feature Flag Lab, SIM spoofing, multi-category feed filter, hidden debug infrastructure |
| SouBryan/pinterest-morphed | 18 | Multi-SDK neutralization, Gson-anchored fingerprints, promoted pin model zeroing |
| chukfinley/tidal-patches | 1 | Structural framework hooks, post-build Python verification, hybrid Compose+RecyclerView |
| bigyank/morphe-patches-samsung | 7 | Multi-layer Knox bypass, dex-only strategy, obfuscated name resilience |
| ajstrick81/morphe-androidtv-patches | 103 | Native C hooks, SSAI manipulation, streaming TV apps |
| durgesh0505/chiggi_morphe_patches | 19 | VPN bypass, device attestation spoofing, UI injection, rewarded-ad auto-credit |
| franticg33k/morphe-patches | 10 | Dart AOT hex patching, GLSL shader patching, Hermes JS bundle patching |
| bufferk/morphe-patches | 86 | Freedium mirror integration, Truecaller comprehensive spoofing |
| MiguelNinja19/miguel-morphe-patches | 20 | IL2CPP hex patching, universal billing bypass scanner, WebView JS injection |
| browzomje/browzomje-patches | 31 | Pinterest feature injection, Morphe settings Activity integration |
| alan7383/sofatime-patches | 2 | Google OAuth WebView restore, Firebase ComponentRegistrar emptying |
| PrathxmOp/Prathxm-Patches | 51 | Embedded native Stockfish engine, overlay UI gesture system |
| Nai64/Nai64Patches | 215 | Universal cross-app patches (RevenueCat, PairIP, Play Store spoof) |
| ilikeadofai/vocacolle-patches | 4 | Morphe settings integration, compatibility probe, Compose bytecode localization |
| NekoGryphou/gryphous-morphe-patches | 5 | Resource patch + bytecode patch coordination pattern |
| RookieEnough/De-Vanced | 1,100+ | Largest migrated corpus, 32 apps |
| HellveticaStandard/HellveticaPatches | 9 | Multi-version targeting, minimal viable patch |
| IMXEren/mix-patches | 19 | Multi-app single repo, API endpoint fix patching |
| Tornillo2/movistar-block-ads-morphe | — | Patch+extension hybrid with Cordova bridge events |
| quantavil/edge-morphe-patches | — | Feature-flag disabling, DNS telemetry sinkhole |
| andronedev/morphe-patches | — | Google Maps API key user injection |
| bdgerszewski/morphe-patches-ihealth | — | Firebase-aware package renaming |
| ch3thanhs/stylus | 6 | Typography-only niche patching |
| RealCyberwash/max-patches | — | Geopolitical anti-censorship |
| HvQ/eksi-morphe | — | Composite Gradle builds |
| docbt/patched-up | 40 | Cross-ecosystem ReVanced→Morphe migration |
| totsiaw/proxma-patches | 9 | Anti-root/anti-emulator 16+ checks, carrier-specific ad skip |
| kondratjev/morphe-patches | 23 | Multi-SDK analytics disable (12+ providers), universal version code changer |
| Xhehab/Xhehab-Patches | 11 | PairIP as reusable pattern, 13 fitness apps |
| arandomhooman/hoomans-morphe-patches | 114 | 31 apps, live ad proxy routing, Google Maps key injection |
| tiaruebar1024/tiaruebar-patches | — | IAP completion on tap (UI interaction-based) |

---

## 1. Native Binary Patching

### ARM64 RET Head Overwrite (IL2CPP / Unity)
Source: BholeyKaBhakt/android-patches-xtra

```kotlin
// Overwrite function heads with ARM64 RET (0xD65F03C0) to disable Unity methods
val soData = File("lib/arm64-v8a/libil2cpp.so").readBytes()
val buffer = ByteBuffer.wrap(soData).order(ByteOrder.LITTLE_ENDIAN)
retSites.forEach { site ->
    if (buffer.getInt(site.offset) != site.prologue0 ||
        buffer.getInt(site.offset + 4) != site.prologue1) {
        error("Prologue mismatch at ${site.name} — version drift")
    }
    buffer.putInt(site.offset, 0xD65F03C0.toInt()) // RET
}
```

### Masked Byte Pattern Search + Replace in .so
Source: hxreborn/morphe-patches (Proton Mail Rust core)

```kotlin
fun ByteArray.replaceTrailingMasked(pattern: ByteArray, mask: ByteArray, replacement: ByteArray) {
    // Search with wildcard bytes (mask[i] == 0 means wildcard)
    // Replace at last match position only
    // Throw if multiple matches found (ambiguous)
}
```

### Dart AOT `libapp.so` Hex Patching (Flutter)
Source: franticg33k/morphe-patches

Flutter apps compile Dart to native via AOT. Premium flags in the Dart snapshot can be patched at the hex level in `libapp.so` for `arm64-v8a`. Pattern: find the `isPremium()` compiled function and overwrite its return with ARM64 `MOV X0, #1; RET`.

### GLSL Shader Patching (Watermark Removal)
Source: franticg33k/morphe-patches (Prismatica Pro)

Modify procedural shaders in `libharwin_native.so` to remove watermark rendering. Shader code is stored as ASCII/UTF-8 within the `.so`.

### Hermes JS Bundle Bytecode Patching (React Native)
Source: franticg33k/morphe-patches (Atlas Photo)

React Native Hermes engine compiles JS to bytecode. The `assets/index.android.bundle` is a Hermes bytecode file. Premium check reducers like `setIsProMember` can be patched by binary search in the bundle.

### Native C Hooks for SSAI Ad Stripping
Source: ajstrick81/morphe-androidtv-patches (Prime Video)

```kotlin
// Load custom native library that hooks into libpvhook.so
// Intercepts Prime Video's in-app ad loading pipeline
// No DNS/proxy required — works client-side
extendWith("libpvhook.mpe")
```

---

## 2. Extension Architecture (Bytecode → Java/Kotlin Interop)

### Basic Pattern
Source: anddea, rushiranpise, brosssh, xob0t

```kotlin
// Patches inject invoke-static hooks → extension does the heavy lifting at runtime
val myPatch = bytecodePatch(name = "Feature") {
    extendWith("extensions/extension.mpe") // Precompiled DEX merged into APK

    execute {
        TargetFingerprint.method.addInstructions(0, """
            invoke-static {p0}, Lcom/myapp/extension/Helper;->doWork(Ljava/lang/Object;)Z
            move-result v0
            return v0
        """)
    }
}
```

### WebView CSS/JS Injection
Source: rushiranpise/morphe-patches (Amazon Shopping)

```kotlin
// Hook onPageFinished → inject CSS selectors for ad blocking + dark mode
// Extension fetches live CSS rules from upstream repo at runtime
execute {
    WebViewPageFinishFingerprint.method.addInstructions(0, """
        invoke-static {p0, p1}, Lcom/amazon/extension/AmazonHelper;->onPageFinished(
            Landroid/webkit/WebView;Ljava/lang/String;)V
    """)
}
```

### Cordova/JS Bridge Event Firing
Source: Tornillo2/movistar-block-ads-morphe

```kotlin
// Extension fires app-layer events via reflection to skip ad playback
// Uses Class.forName + getDeclaredMethod.invoke to trigger JS bridge
Class.forName("com.movistar.player.MainActivity")
    .getMethod("fireEvent", String.class)
    .invoke(null, "ended")
```

### Google OAuth WebView Restore After Re-signing
Source: alan7383/sofatime-patches

```kotlin
// Re-signing breaks OAuth (SHA-1 mismatch for Google Sign-In)
// Extension opens in-app WebView OAuth flow, captures ID Token/Auth Code
// Boxes token into kotlin.Result, resumes coroutine
execute {
    GoogleSignInFingerprint.method.addInstructions(0, """
        invoke-static {p0}, Lcom/sofatime/extension/GoogleSignInHelper;->signIn(
            Landroid/app/Activity;)Ljava/lang/Object;
    """)
}
```

### Reflection-Based Obfuscated Enum Manipulation
Source: brosssh/morphe-patches (Instagram nav buttons)

```java
// Button is an obfuscated Enum — use reflection to match by name pattern
Field enumNameField = button.getClass().getDeclaredField(enumNameFieldName);
String enumName = (String) enumNameField.get(button);
if (enumName.contains("SEARCH")) { /* remove */ }
```

---

## 3. System-Level Spoofing

### Build Property Interception
Source: rushiranpise/morphe-patches (Google Photos)

```kotlin
// Scan methods for sget-object on Build fields (MODEL, BRAND, DEVICE, etc.)
// Replace sget-object with const-string to spoof device identity
val fieldAccessPattern = fieldAccess(opcode = Opcode.SGET_OBJECT, type = "Ljava/lang/String;")
BuildFieldFingerprint.matchAllOrNull()?.forEach { match ->
    match.method.apply {
        findInstructionIndicesReversedOrThrow(fieldAccessPattern).forEach { idx ->
            replaceInstruction(idx, "const-string v${register}, \"$spoofedValue\"")
        }
    }
}
```

### Register-Window Backward Scanning (R8-Resilient)
Source: xob0t/morphe-patches

```kotlin
// Walk up to 16 instructions backward to find string constant assigned to register
// Handles R8/RGuard register reuse where register values shift
fun constantStringForRegisterBefore(
    method: MutableMethod, instructionIndex: Int, register: Int, maxLookback: Int = 16
): String? {
    for (i in instructionIndex - 1 downTo maxOf(0, instructionIndex - maxLookback)) {
        val insn = method.getInstruction<Instruction21c>(i)
        if (insn?.opcode == Opcode.CONST_STRING && insn.registerA == register) {
            return insn.reference as? String
        }
    }
    return null
}
```

### SystemProperties.get() Interception
Source: rushiranpise, xob0t

```kotlin
// Hook SystemProperties.get(key, default) → return spoofed values
// Used to fake ROM build properties, security flags, feature flags
```

### SIM Country/Operator Spoofing
Source: icysymmetra/tiktok-patches-for-morphe

```kotlin
// Replace TelephonyManager.getSimCountryIso() / getNetworkOperator() returns
// With user-selectable country presets
```

### Emulator Detection Spoofing (25+ Indicators)
Source: xob0t/morphe-patches

```kotlin
// 25+ emulator indicator strings to detect, 12 substring markers
// 5 interception points:
//   1. Build field reads (FINGERPRINT, HARDWARE, etc.)
//   2. File.exists/canRead (checking emulator paths)
//   3. Runtime.exec / ProcessBuilder.start
//   4. System.getProperty / SystemProperties.get
//   5. PackageManager queries (checking emulator apps)
```

### VPN/Network Transport Spoofing
Source: xob0t/morphe-patches

```kotlin
// 8 interception points:
//   hasTransport(TRANSPORT_VPN), hasCapability(NOT_VPN),
//   registerNetworkCallback, NetworkCapabilities.toString(),
//   LinkProperties.getInterfaceName(), getHttpProxy(),
//   getRoutes(), NetworkInterface.getNetworkInterfaces()
// Smart context detection: only patches methods with VPN-related strings
fun hasVpnSignalContext(method: Method): Boolean {
    return method.implementation?.instructions.any { instruction ->
        when (instruction.opcode) {
            Opcode.CONST_STRING -> (instruction.reference as? String)?.contains("vpn", true) == true
            else -> false
        }
    }
}
```

### Knox/Samsung Multi-Layer Integrity Bypass
Source: bigyank/morphe-patches-samsung (Samsung Health)

```kotlin
// Layer 1: Stub 17 Knox/SAK methods to return safe values
// Layer 2: Scan DEX for Knox popup launchers by string/type patterns
// Layer 3: Stub static (File)Z methods for root file scanning
// Layer 4: Replace "com.osp.app.signin" → "com.notsamsung.dummy" account provider
// Dex-only strategy (no resource decoding) for 300MB+ APKs
```

### Device Attestation Spoofing
Source: durgesh0505/chiggi_morphe_patches (SonyLIV)

```kotlin
// 8 security flags including native IsTampered check
// Prevents 24-hour account locks on tampered devices
```

---

## 4. Advanced Fingerprinting Patterns

### Gson @SerializedName Anchoring (R8 Survivable)
Source: SouBryan/pinterest-morphed

```kotlin
// Instead of matching by obfuscated field name, match by Gson serialized name
// Gson field names survive R8/minification across releases
object PinModelFingerprint : Fingerprint(
    filters = listOf(
        // The @SerializedName("is_promoted") value never changes
        string("is_promoted"),
        fieldAccess(opcode = Opcode.IPUT_BOOLEAN, type = "Z"),
    )
)
```

### Structural Class Resolution (Minified Code)
Source: chukfinley/tidal-patches

```kotlin
// Resolve obfuscated class by searching for signature, not name
// Find: class declaring addAsLastInActives(Source) by structural search
execute {
    val playQueueClass = classes.first { classDef ->
        classDef.methods.any { method ->
            method.name == "addAsLastInActives" &&
            method.parameterTypes.size == 1 &&
            method.parameterTypes[0].endsWith("Source;")
        }
    }
    // Then fingerprint within the resolved class
}
```

### Multi-Class Field Name Extraction Chain
Source: brosssh/morphe-patches (Instagram)

```kotlin
// Fingerprint 1: Find request class
// Fingerprint 2: Find request constructor → extract obfuscated field name
// Fingerprint 3: Find header map accessor in different class → use extracted field name
// Pattern: getFieldNameFromFingerprint → findFieldAccessInConstructor → injectInterceptor
```

### Dynamic Constructor Field Resolution
Source: rushiranpise/morphe-patches (Duolingo)

```kotlin
execute {
    // Iterate constructor instructions, extract FieldReference instances dynamically
    val fields = mutableListOf<FieldReference>()
    SubscriptionInfoConstructorFingerprint.method.implementation?.instructions?.forEach { insn ->
        if (insn.opcode == Opcode.IPUT_OBJECT) {
            fields.add((insn as Instruction35c).reference as FieldReference)
        }
    }
    // Then rewrite constructor body with spoofed values for each field
    constructorMethod.removeInstructions(0, count)
    constructorMethod.addInstructions(0, buildSpoofedBody(fields))
}
```

### MatchAllOrNull for Global Operations
Source: rushiranpise, multiple

```kotlin
// Replace all occurrences of a string across entire APK
val filter = string("http://old-api.example.com")
Fingerprint(filters = listOf(filter)).matchAllOrNull()?.forEach { match ->
    match.method.apply {
        findInstructionIndicesReversedOrThrow(filter).forEach { idx ->
            val reg = getInstruction<OneRegisterInstruction>(idx).registerA
            replaceInstruction(idx, "const-string v$reg, \"https://new-api.example.com\"")
        }
    }
}
```

---

## 5. Multi-Sub-Patch Orchestration

Source: rushiranpise/morphe-patches (Microsoft Excel)

```kotlin
val mainPatch = bytecodePatch(name = "Unlock Excel") {
    dependsOn(
        removeSharedUserIdPatch,         // Manifest: remove sharedUserId
        bypassCodeTransparencyPatch,     // Stub MS code transparency verification
        disableLoginRequirementPatch,    // Hijack FTUX task chain
        unlock365FamilyPatch,            // Force all licensing states
        disableAdsPatch,                 // Block ad initialization
        spoofSignaturePatch              // Seed real MS certs + spoof package signature
    )
    execute { /* coordination logic */ }
}
```

---

## 6. SDK/Dependency Neutralization

### Multi-SDK Analytics Disable
Source: SouBryan/pinterest-morphed, binarymend/morphe-patches, xob0t/morphe-patches

```kotlin
// Apply to: AppsFlyer, Bugsnag, Firebase Analytics, Crashlytics, Performance,
//           Google Analytics, Google Engage, Adjust, Amplitude, Mixpanel,
//           OneDS, Segment, CleverTap, MoEngage, Braze

// Pattern per SDK:
// AppsFlyer: init() → no-op, isStopped() → always true
// Bugsnag: strip API key from manifest metadata
// Firebase: ComponentRegistrar.getComponents() → return empty list
// Google Engage: remove broadcast receiver, createWork() → return null
// OneDS Logger: stub event methods
```

### Firebase ComponentRegistrar Emptying
Source: alan7383/sofatime-patches

```kotlin
// Disable Crashlytics/Sessions/Analytics without breaking Auth/Firestore/FCM
// Patch ComponentRegistrar.getComponents() to return empty list
execute {
    FirebaseRegistrarFingerprint.method.addInstructions(0, """
        invoke-static {}, Ljava/util/Collections;->emptyList()Ljava/util/List;
        move-result-object v0
        return-object v0
    """)
}
```

### freeRASP SDK Disabler
Source: xob0t/morphe-patches

```kotlin
// Find: string "Unable to run Talsec" → trace to the SDK start() call
// Disable: methods with signature (Context, !String, !boolean, !int)
```

---

## 7. Integrity/DRM Bypass

### PairIP De-Virtualization Framework
Source: BholeyKaBhakt/android-patches-xtra

```kotlin
// Step 1: gutPairIpVm() — neutralize:
//   SignatureCheck.verifyIntegrity, VMRunner.<clinit> (blocks native lib load),
//   VMRunner.invoke, StartupLauncher.launch

// Step 2: restorePairIpHolders(resourceDir) — read harvest resources:
//   depairip_strings.tsv: holder-class grouped string/field/value tables
//   depairip_methods.tsv: base64-encoded method bodies for reflect-Method holders
//   Synthesizes <clinit> with const-string + sput-object from table data
```

### Code Transparency Check Bypass
Source: rushiranpise/morphe-patches (Microsoft Excel)

```kotlin
// MS Office checks APK code transparency verification
// Bypass: stub the callback to call transparencyVerificationSucceeded() directly
execute {
    CodeTransparencyFingerprint.method.addInstructions(0, """
        invoke-virtual {p0}, Lcom/microsoft/CodeTransparencyCheckCallback
            ->transparencyVerificationSucceeded()V
        return-void
    """)
}
```

### Real Certificate Seeding for Signature Spoofing
Source: rushiranpise/morphe-patches, cesbar/zpatches

```kotlin
// Embed real Microsoft Android Play App PCA certificate bytes
// Use rawResourcePatch to seed cert, then spoof package signature
val spoofSigPatch = rawResourcePatch(name = "Spoof signature") {
    execute {
        seedCert("microsoft_play_cert.bks")
    }
}
```

### Manifest Permission Stripping
Source: cesbar/zpatches

```kotlin
// Remove <uses-permission> from AndroidManifest.xml via resourcePatch
execute {
    document("AndroidManifest.xml").use { doc ->
        doc.documentElement.getElementsByTagName("uses-permission").let { list ->
            while (list.length > 0) {
                list.item(0)?.let { it.parentNode?.removeChild(it) }
            }
        }
    }
}
```

---

## 8. UI Injection & Feature Addition

### Hidden Debug Row Hijacking
Source: icysymmetra/tiktok-patches-for-morphe

```kotlin
// TikTok's "Open Debug" developer entry point → hijack as Morphe settings host
// Repurpose internal FeatureFlag infrastructure as user-toggleable settings
```

### Swipe Gesture Injection (Spotify-Style)
Source: chukfinley/tidal-patches

```kotlin
// Hook Compose's AnchoredDraggable with Spotify-equivalent physics:
// Row must travel >45% width, or be released at ≥125dp/s after ≥24dp drag
// Attach to ALL long-clickable Compose components, filter non-rows at runtime
// Suppress app's long-press detector during drag to prevent menu overlap
```

### Embedded Native Engine (Stockfish NNUE)
Source: PrathxmOp/Prathxm-Patches (Chess.com)

```kotlin
// Bundle Stockfish 18 NNUE binary as resource → extract at runtime
// Overlay UI: tap-and-hold logo for settings, double-tap for panic mode
// Fair-play enforcement: disable analysis during live online matches
```

### Custom Web Clipboard Server
Source: jasonwu1994/Gboard-patches

```kotlin
// Run an in-app web server that desktop browsers connect to over LAN
// Pairing code gate, Quick Settings Tile
// Sync clipboard between phone keyboard and desktop browser
```

### Morphe Settings Activity Integration
Source: browzomje/browzomje-patches, vocacolle-patches

```kotlin
// Add "Morphe" entry to app's native settings toolbar overflow
// Localized preference screen themed to match app
// Version display: "7.40.0 · Morphe 1.1.0-dev.1"
```

---

## 9. Resource/Manifest Manipulation

### Manifest XML DOM Helpers Library
Source: xob0t/morphe-patches

```kotlin
fun Element.childrenNamed(name: String): List<Element> { ... }
fun Element.getOrCreateApplicationMetaData(): Element { ... }
fun Element.setApplicationMetaData(name: String, value: String) { ... }
fun Element.disableComponentsWhere(predicate: (Element) -> Boolean) { ... }
fun Element.disableComponentsByPrefix(prefix: String) { ... }
fun Element.disableComponentsByName(name: String) { ... }
fun Element.removeComponentDiscoveryRegistrarsWhere(predicate: (Element) -> Boolean) { ... }
```

### Google Maps API Key Replacement
Source: cesbar/zpatches, andronedev/morphe-patches, hoomans-patches

```kotlin
// Re-signing invalidates Google Maps signing-key binding
// Patch: let user inject their own Maps API key to restore maps
execute {
    val currentKey = "YOUR_MAPS_API_KEY"
    // Replace in AndroidManifest metadata and in code references
}
```

### Package Rename for Side-by-Side Install
Source: kiraio-moe/Lain-Patches, jasonwu1994/Gboard-patches

```kotlin
// Allow patched app to coexist with original
// Must also handle Firebase crash from unregistered package name
```

### Ad View Zero-Sizing on Construction
Source: SouBryan/pinterest-morphed

```kotlin
// Instead of removing ad views, zero-size them on construction
// Ad chrome never draws even if feed adapter tries to render promoted item
```

---

## 10. Universal/Generic Patch Patterns

### Universal RevenueCat Entitlement Unlock
Source: Nai64/Nai64Patches, rushiranpise/morphe-patches

```kotlin
// Pattern: find EntitlementInfos.getActive() → force return non-null
// Applies to ANY app using RevenueCat SDK
```

### Universal Billing Bypass Scanner (5-Phase)
Source: MiguelNinja19/miguel-morphe-patches

```
Phase 1: Cocos2d-x → check libcocos2dcpp.so
Phase 2: GameMaker → check runner activity
Phase 3: Google Play Billing → standard fingerprint
Phase 4: Unity → check libil2cpp.so
Phase 5: Fallback → isPremium / isPro / isSubscribed string search
```

### Generic Method Call Transformer
Source: kiraio-moe/Lain-Patches

```kotlin
// DSL for matching and replacing method call patterns across ANY app
// Define: old method → new method, then apply transformer automatically
```

---

## 11. Ad Blocking Specifics

### SSAI Client-Side Manipulation (Server-Side Ad Insertion)
Source: ajstrick81/morphe-androidtv-patches (Pluto TV)

```kotlin
// Empty ad-break timeline entries for server-side-stitched ads
// Intercept before playback timeline is constructed
```

### Rewarded Ad Auto-Credit
Source: durgesh0505/chiggi_morphe_patches (CrazyGames)

```kotlin
// Fire reward lifecycle events without showing ad:
// onRewardedAdLoaded → onUserEarnedReward callback
// User gets reward credits instantly
```

### Dual-Hook Organic Flag + Feed Scheduler
Source: durgesh0505/chiggi_morphe_patches (Threads)

```kotlin
// Hook 1: Set organic flag on all feed items
// Hook 2: Intercept feed injection scheduler → skip promoted items
```

### Mandatory-Surface Fail-Loud Resource Patching
Source: xob0t/morphe-patches

```kotlin
// If any expected ad layout XML is missing, throw PatchException
// Collects all missing surfaces first, reports all at once
// Prevents silent ad pass-through when app updates
```

---

## 12. Build & Development Innovations

### Post-Build Verification Toolchain
Source: chukfinley/tidal-patches

```python
# check-classes.py: disassemble patched APK, verify every extension class reference
# check-members.py: verify every extension method reference resolves at runtime
# Catches: extension compiles against public Compose artifacts but links against
#          TIDAL's internal snapshots at runtime → compile-time verification cannot catch
```

### Compatibility Probe (No-Op Verification Patch)
Source: vocacolle-patches

```kotlin
// A patch that does nothing — proves decode→rebuild→sign pipeline works
// Validates toolchain without behavioral change before real patches
val probePatch = bytecodePatch(name = "Compatibility probe") {
    compatibleWith(COMPATIBILITY_APP)
    execute { /* no-op: just proves the pipeline works */ }
}
```

### Dex-Only Strategy for Large APKs
Source: bigyank/morphe-patches-samsung

```
// For 300MB+ APKs: skip resource decoding to avoid OOM
// Only use bytecode patches, no resource patches
// Set process runtime: 1280 MB
// Document force-stop/recovery for OOM loops
```

### .gitignore Note for Built APK Files
The `*.apk`, `*.apks`, `*.xapk`, `*.apkm` patterns in `.gitignore` prevent committing original
APK files. Build artifacts (`*.mpp`) are generated by CI and distributed via releases.

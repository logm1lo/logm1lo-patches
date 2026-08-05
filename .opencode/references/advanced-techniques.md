# Advanced Patching Techniques — Deep Dive

Production-grade techniques extracted from 70+ Morphe community repositories.
These go beyond basic boolean forcing and require understanding of Android internals.

---

## Native Binary Patching

### ARM64 RET Head Overwrite (IL2CPP Unity Games)

IL2CPP-compiled Unity games convert C# to C++ then to native ARM64. Premium gates
can be bypassed by overwriting function heads with `RET`.

```kotlin
// From BholeyKaBhakt/android-patches-xtra
data class RetSite(val name: String, val offset: Int, val prologue0: Int, val prologue1: Int)

val retSites = listOf(
    RetSite("isPremium", 0x1A2B3C, 0xA9BF7BFD, 0x910003FD),
)

execute {
    val soFile = get("lib/arm64-v8a/libil2cpp.so")
    val soData = soFile.readBytes()
    val buffer = ByteBuffer.wrap(soData).order(ByteOrder.LITTLE_ENDIAN)

    retSites.forEach { site ->
        require(buffer.getInt(site.offset) == site.prologue0) { "Version drift: ${site.name}" }
        require(buffer.getInt(site.offset + 4) == site.prologue1)
        buffer.putInt(site.offset, 0xD65F03C0.toInt()) // ARM64 RET
    }
    soFile.writeBytes(soData)
}
```

### Dart AOT `libapp.so` Hex Patching (Flutter Apps)

Flutter apps AOT-compile Dart to native. The Dart VM snapshot stores function bodies
in `lib/arm64-v8a/libapp.so`.

Workflow:
1. Find the Dart function using `nm libapp.so | grep function_name` or `objdump`
2. Identify stable surrounding instruction pattern
3. Overwrite target with `MOV X0, #1` + `RET` (ARM64)

Binary search: `rg -a "func_signature" lib/arm64-v8a/libapp.so`

### Masked Byte Search in .so (Rust Core)

```kotlin
// From hxreborn/morphe-patches (Proton Mail Rust core)
fun ByteArray.replaceTrailingMasked(
    pattern: ByteArray, mask: ByteArray, replacement: ByteArray
) {
    val matches = mutableListOf<Int>()
    for (i in 0..size - pattern.size) {
        var found = true
        for (j in pattern.indices) {
            if (mask[j] != 0.toByte() && this[i + j] != pattern[j]) {
                found = false; break
            }
        }
        if (found) matches.add(i)
    }
    require(matches.size == 1) { "Expected 1 match, got ${matches.size}" }
    replacement.copyInto(this, matches.last())
}
```

### Hermes JS Bundle Patching (React Native)

React Native with Hermes stores JS as bytecode in `assets/index.android.bundle`.
Search for the function's stable byte pattern in the bundle, replace with forced
return of the desired value.

---

## Extension Architecture Deep Dive

### Full Extension Module Setup

```
extensions/
├── build.gradle.kts           # Separate module for .mpe compilation
├── src/main/java/
│   └── com/yourapp/extension/
│       └── Helper.java        # Runtime logic
└── compile.sh                 # d8 to DEX
```

**Patch side (bytecode):**
```kotlin
val mainPatch = bytecodePatch(name = "Feature with Extension") {
    extendWith("extensions/extension.mpe")
    execute {
        TargetFingerprint.method.addInstructions(0, """
            invoke-static {p0, p1}, Lcom/yourapp/extension/Helper
                ->handle(Landroid/content/Context;Ljava/lang/String;)V
        """)
    }
}
```

**Extension side (Java — can use any Java/Kotlin runtime):**
```java
public class Helper {
    public static void handle(Context ctx, String param) {
        // Complex logic: SharedPreferences, network calls, reflection
        // Much easier to write in Java than Smali bytecode
    }
}
```

### WebView CSS/JS Injection at Runtime

Hook `onPageFinished` → inject CSS selectors to block ads or apply themes.
Extension fetches live CSS rules from upstream repo at runtime.

```kotlin
// Patch: route to extension
WebViewFinishedFingerprint.method.addInstructions(0, """
    invoke-static {p1, p2}, Lcom/helper/WebViewInjector
        ->onPageFinished(Landroid/webkit/WebView;Ljava/lang/String;)V
""")
```

```java
// Extension: inject CSS
public static void onPageFinished(WebView view, String url) {
    String css = fetchCssFromRepo();
    String js = "javascript:(function(){" +
        "var s=document.createElement('style');" +
        "s.textContent='" + css + "';" +
        "document.head.appendChild(s);" +
        "})()";
    view.loadUrl(js);
}
```

### Google OAuth WebView Restore (Fixes Re-signing SHA-1 Mismatch)

Re-signing an APK changes the signing certificate SHA-1, breaking `GoogleSignInClient`.
Solution: replace native sign-in with in-app WebView OAuth flow.

```java
// From alan7383/sofatime-patches
public class GoogleSignInHelper {
    public static Object signIn(Activity activity) {
        // 1. Open WebView with Google OAuth consent URL
        // 2. User signs in -> WebView intercepts redirect_uri
        // 3. Extract ID Token / Auth Code from redirect URL
        // 4. Box into kotlin.Result and resume coroutine
        // 5. Native flow continues as if GoogleSignInClient succeeded
    }
}
```

### Cordova/JS Bridge Event Firing

```java
// From Tornillo2/movistar-block-ads-morphe
// Detects ads at native layer, skips them by firing JS-layer events
public class AdSkipHelper {
    public static void skipAd(Activity activity) {
        try {
            Class<?> main = Class.forName("com.app.MainActivity");
            Method fire = main.getMethod("fireEvent", String.class);
            fire.invoke(null, "ended"); // triggers Cordova callback
        } catch (Exception e) { /* fail silent */ }
    }
}
```

---

## System-Level Spoofing Deep Dive

### Build Property Interception Engine

```kotlin
// From rushiranpise/morphe-patches (Google Photos Pixel spoofing)
// User-selectable device profiles that spoof ALL Build fields

val pixelProfiles = listOf(
    "Pixel 9 Pro" to mapOf(
        "MODEL" to "Pixel 9 Pro",
        "BRAND" to "google",
        "DEVICE" to "komodo",
        "MANUFACTURER" to "Google",
    )
)

execute {
    // Scan ALL methods for sget-object on android.os.Build fields
    // Replace each with const-string of spoofed value
    val sgetPattern = fieldAccess(
        opcode = Opcode.SGET_OBJECT,
        definingClass = "Landroid/os/Build;",
        type = "Ljava/lang/String;"
    )
    Fingerprint(filters = listOf(sgetPattern)).matchAllOrNull()?.forEach { match ->
        match.method.apply {
            findInstructionIndicesReversedOrThrow(sgetPattern).forEach { idx ->
                val reg = getInstruction<OneRegisterInstruction>(idx).registerA
                val fieldName = (getInstruction<Instruction35c>(idx)
                    .reference as FieldReference).name
                val spoofedValue = selectedProfile[fieldName] ?: return@forEach
                replaceInstruction(idx, "const-string v$reg, \"$spoofedValue\"")
            }
        }
    }
}
```

### Register-Window Backward Scanning (R8 Resilient)

```kotlin
// When R8/RGuard reuses registers, values shift between builds.
// Walk backward from the instruction to find what register holds.
// From xob0t/morphe-patches

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

### Emulator Detection Spoofing (25+ Indicators)

```kotlin
// 5 interception categories:
// 1. Build fields: FINGERPRINT, HARDWARE, MODEL, TAGS, etc.
// 2. File checks: paths like /system/lib/libc_malloc_debug_qemu.so
// 3. Process execution: Runtime.exec(), ProcessBuilder
// 4. System properties: ro.kernel.qemu, ro.hardware, etc.
// 5. PackageManager: queryIntentActivities for emulator-only apps

// Smart context detection — only patch methods with emulator-related constants
// Avoids false positives on unrelated Build field reads
```

### VPN/Network Transport Spoofing

```kotlin
// 8 interception points across ConnectivityManager / Network APIs:
// TRANSPORT_VPN flag, NOT_VPN capability, NetworkInterface names,
// LinkProperties routes, HTTP proxy detection, interface enumeration

// Pattern: only intercept when method body contains VPN-relevant strings
fun hasVpnSignal(method: Method): Boolean {
    return method.implementation?.instructions?.any { insn ->
        insn.opcode == Opcode.CONST_STRING &&
        (insn.reference as? String)?.contains("vpn", true) == true
    } ?: false
}
```

---

## Advanced Fingerprinting Patterns

### Gson @SerializedName Anchoring (Survives R8 Across Releases)

```kotlin
// Gson field names NEVER change between app updates (they mirror the API contract)
// Use them as stable anchors instead of obfuscated field names
// From SouBryan/pinterest-morphed — survived 8 consecutive Pinterest releases

object PromotedPinFingerprint : Fingerprint(
    filters = listOf(
        string("is_promoted"),          // @SerializedName("is_promoted")
        fieldAccess(                     // actual field write
            opcode = Opcode.IPUT_BOOLEAN,
            type = "Z"
        ),
    )
)

// Then zero out the promoted flag:
// After the IPUT_BOOLEAN for is_promoted, inject: const/4 vX, 0x0
```

### Structural Class Resolution (Minified/Obfuscated Code)

```kotlin
// When class names are minified, resolve by STRUCTURE not NAME
// From chukfinley/tidal-patches
execute {
    val targetClass = classes.first { classDef ->
        classDef.methods.any { method ->
            method.name == "addAsLastInActives" &&    // stable, non-obfuscated method
            method.parameterTypes.size == 1 &&
            method.parameterTypes[0].endsWith("Source;")
        }
    }
    // Now fingerprint within the resolved class
    val swipeFingerprint = Fingerprint(
        definingClass = targetClass.type,
        returnType = "V",
        parameters = listOf(),
        filters = listOf(...)
    )
}
```

### Multi-Class Field Name Extraction Chain

```kotlin
// Pattern from brosssh/morphe-patches (Instagram feed filtering):
// Stage 1: Find request class via stable fields
// Stage 2: Find constructor -> extract obfuscated field name
// Stage 3: Use extracted field name to find header map in ANOTHER class

val requestClassFp = Fingerprint(strings = listOf("feed_recs"))

val constructorFp = Fingerprint(
    definingClass = requestClassFp.originalClassDef.type,
    name = "<init>",
    returnType = "V",
    parameters = listOf("Ljava/lang/String;", "Ljava/util/Map;"),
    filters = listOf(
        fieldAccess(opcode = Opcode.IPUT_OBJECT, type = "Ljava/util/Map;"),
    )
)

// Extract field name from constructor match
val headerMapFieldName = constructorFp
    .instructionMatches[0]
    .getInstruction<Instruction35c>()
    .reference.name
```

### Dynamic Constructor Field Rewriting

```kotlin
// From rushiranpise/morphe-patches (Duolingo SubscriptionInfo)
// Instead of hardcoding field indices, extract them dynamically from constructor
execute {
    val ctor = SubscriptionInfoFingerprint.method
    val fields = mutableListOf<FieldReference>()
    ctor.implementation?.instructions?.forEach { insn ->
        if (insn.opcode == Opcode.IPUT_OBJECT || insn.opcode == Opcode.IPUT) {
            fields.add((insn as Instruction35c).reference as FieldReference)
        }
    }
    // Now rewrite entire constructor body with spoofed values
    val body = buildString {
        appendLine("invoke-direct {p0}, L${superClass};-><init>()V")
        fields.forEachIndexed { i, field ->
            appendLine("const/4 v0, ${spoofedValues[i]}")
            appendLine("iput v0, p0, L${field.definingClass};->${field.name}:${field.type}")
        }
    }
    ctor.removeInstructions(0, ctor.implementation!!.instructions.size)
    ctor.addInstructions(0, body)
}
```

---

## Integrity & DRM Bypass Deep Dive

### PairIP De-Virtualization Framework

```kotlin
// From BholeyKaBhakt/android-patches-xtra
// PairIP uses code virtualization to hide license checks
// Two-phase approach:

// Phase 1: Neutralize virtualization infrastructure
fun gutPairIpVm() {
    // 1. Neutralize SignatureCheck.verifyIntegrity -> always pass
    // 2. Stub VMRunner.<clinit> -> block native lib load
    // 3. NOP VMRunner.invoke -> prevent interpretation
    // 4. Neutralize StartupLauncher.launch -> skip init
}

// Phase 2: Restore obfuscated holders from harvest data
fun restorePairIpHolders(resourceDir: String) {
    // depairip_strings.tsv: table of class/field/string/value tuples
    // depairip_methods.tsv: base64-encoded method bodies
    // For each holder class: synthesize <clinit> with:
    //   const-string vX, "value" + sput-object vX, Lclass;->field:T
}
```

### Code Transparency Check Bypass (Microsoft Office)

```kotlin
// MS Office verifies code transparency via custom callback chain
// Bypass: directly invoke transparencyVerificationSucceeded() on the callback
execute {
    VerifyFingerprint.method.addInstructions(0, """
        invoke-virtual {p0}, Lcom/microsoft/office/appcore/codetrans
            /CodeTransparencyCheckCallback
            ->transparencyVerificationSucceeded()V
        return-void
    """)
}
```

### Real Certificate Seeding for Signature Spoofing

```kotlin
// Embed the original app's signing certificate into the patched APK
// Allows the app's own signature verification to pass
val spoofSigPatch = rawResourcePatch(name = "Spoof signature") {
    execute {
        // Seed real Microsoft Android certificate bytes
        val certBytes = Base64.decode(
            "MIIGqDCCBJCgAwI..." // real cert PEM
        )
        seedCert("microsoft_cert.bks")

        // Then hook PackageManager.checkSignatures -> always return match
    }
}
```

### Dex-Only Strategy for Large APKs (300MB+)

```kotlin
// From bigyank/morphe-patches-samsung (Samsung Health)
// Skip ALL resource decoding to avoid OOM on 300MB+ APKs
// Only use bytecodePatch, never resourcePatch or rawResourcePatch
// Use string/type patterns in DEX for targeting (NOT class names)

// Knox integrity bypass via DEX scanning:
// "HomeAppCloseActivity" + warranty-bit check patterns
// "KnoxHandlerViewModel" + flag-setting patterns
// Kotlin synthetic $this$isRooted checks
```

---

## Multi-Sub-Patch Orchestration

```kotlin
// From rushiranpise/morphe-patches (Excel) — 6 coordinated sub-patches
// Each sub-patch handles one aspect; main patch coordinates them

val unlockExcel = bytecodePatch(name = "Unlock Excel") {
    dependsOn(
        removeSharedUserId,         // Manifest: drop sharedUserId
        bypassCodeTransparency,     // Stub MS verification
        disableLoginRequirement,    // Hijack first-time user experience
        unlock365Family,            // Force license state
        disableAds,                 // Block AdMob init
        spoofSignature              // Seed certs + spoof package sig
    )
    execute { /* coordination logic if needed */ }
}
```

---

## Post-Build Verification

```kotlin
// From chukfinley/tidal-patches
// Python scripts verify the patched APK at build time:
// check-classes.py: disassemble APK, verify every extension class reference
// check-members.py: verify every extension method reference resolves
// Catches the class of bugs where extension compiles against public artifacts
// but links against internal snapshots at runtime — compile-time cannot catch this
```

---

## Compatibility Probe Pattern

```kotlin
// From vocacolle-patches
// A no-op patch that proves decode -> rebuild -> sign works
// Run this FIRST before writing real patches
val probePatch = bytecodePatch(name = "Compatibility probe") {
    compatibleWith(COMPATIBILITY_APP)
    execute { /* no-op: validates toolchain only */ }
}
```

## Case Study: Calistree 5.8.5 — Flutter PRO Bypass

### Architecture
- **Framework**: Flutter 3.x (Dart 3.12.2 AOT), RevenueCat for purchases, Firebase Auth/CloudDB
- **Patching approach**: 5 Java smali patches (RevenueCat SDK) + 10 Dart AOT hex patches (libapp.so)
- **Tools used**: B(l)utter for libapp.so decompilation, morphe-cli for Java patching

### Java Patches (RevenueCat SDK override)

| # | Target Method | Type | Effect |
|---|-------------|------|--------|
| 1 | `EntitlementInfos.getActive()` | `iget active` → `iget all` | All entitlements show as active |
| 2 | `CustomerInfo.getActiveSubscriptions()` | Body replacement | Returns `{"pro"}` |
| 3 | `EntitlementInfosMapperKt.map()` | Body replacement | Fake PRO entitlement data |
| 4 | `CustomerInfo.getAllPurchasedProductIds()` | Body replacement | Returns `{"pro"}` |
| 5 | `CustomerInfoMapperKt.map()` | Inject before return | Adds `latestExpirationDate: 2099` |

### Dart Hex Patches (libapp.so ARM64)

| Offset | Original | Patched | Function | Effect |
|--------|----------|---------|----------|--------|
| `0x20aa36c` | `mov x0,x2` | `add x0,x27,#0x20` | `updateState` param | SetHasProAccess always true |
| `0x20aa398` | `ldur x2,[fp,#-0x10]` | `add x2,x27,#0x20` | `updateState` state= | State always true |
| `0x20aa414` | `add x0,x22,#0x30` (false) | `add x0,x22,#0x20` (true) | `hasProAccess` default | Null cache → true |
| `0x20aa41c` | `mov x0,x1` | `add x0,x22,#0x20` (true) | `hasProAccess` cached | Stored value → true |
| `0x20a9b28` | `mov x2,x3` | `add x2,x22,#0x20` (true) | `init()` setHasProAccess | Sync can't overwrite |
| `0x29f5360` | `add x0,x22,#0x30` (false) | `add x0,x22,#0x20` (true) | Promotional check | Always returns true |
| `0x22d6914` | `b.eq #0x22d691c` | `b #0x22d6954` | `hasReachedPlanLimit` null gate | Skip Go PRO popup |
| `0x20a98bc` | `tbnz w1,#4,...` | `nop` | `init()` backup skip | Always set initial state |
| `0x20a9860` | `mov x2,x22` (null) | `add x2,x22,#0x20` (true) | `StateNotifier` init | Root fix — initial state=true |
| `0x22d6960` | `b.lt #0x22d69f0` | `b #0x22d69f0` | `hasReachedPlanLimit` | Remove ALL plan limits |

### Key Insights

1. **B(l)utter is essential** — Flutter Dart AOT can't be patched via smali. Need B(l)utter for asm/pp.txt analysis
2. **Systematic gating** — Found 5 separate PRO gates: RevenueCat, LocalPreferences cache, Promotional access, Plan limits, Riverpod state
3. **Persistence is subtle** — `hasReachedPlanLimit` uses `proAccessProvider` via Riverpod. State goes through `StateNotifier` init → `updateState` → `hasProAccess` cache chain
4. **Register encoding matters** — Different Dart functions use different NULL registers (x22 vs x27). Must verify bytes at each offset
5. **Label injection is fragile** — Smali `addInstructions` label placement doesn't work reliably. Use instruction replacement or register manipulation instead

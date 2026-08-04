---
name: morphe-patching
description: Write Kotlin-based Morphe patches for Android APKs using bytecodePatch, Fingerprints, and the Morphe patching DSL
---

# Morphe Patching

## What I do
- Guide you through writing Morphe patch code in Kotlin
- Show the correct file structure for patches
- Explain the patch DSL: `bytecodePatch`, `resourcePatch`, `rawResourcePatch`
- Show how to declare Compatibility, Fingerprints, and Patch logic
- Help verify patches build correctly

## When to use me
Use this when writing or editing Kotlin patch files (`.kt`), creating fingerprints,
declaring app compatibility, or debugging build failures in the patches module.

## Patch File Structure

```
patches/src/main/kotlin/app/logm1lo/patches/<app>/
├── shared/
│   └── Constants.kt        # Compatibility declarations
├── <category>/
│   ├── Fingerprints.kt      # Smali method fingerprints
│   └── <Name>Patch.kt       # Patch logic
```

## Patch Types

### bytecodePatch (most common)
Modifies Dalvik bytecode directly. Fast, preferred for most patches.

```kotlin
@Suppress("unused")
val myPatch = bytecodePatch(
    name = "Patch Name",
    description = "What it does in present tense.",
    default = true
) {
    compatibleWith(COMPATIBILITY_APP)
    dependsOn(otherPatch)  // optional
    extendWith("extension.mpe")  // optional precompiled DEX

    execute {
        // Modify bytecode here
        SomeFingerprint.method.addInstructions(0, """
            const/4 v0, 0x1
            return v0
        """)
    }

    finalize {
        // Cleanup after all patches run
    }
}
```

### resourcePatch
Modifies decoded XML resources. Slower — triggers decode/encode.

```kotlin
val myResourcePatch = resourcePatch(
    name = "Resource modification",
    description = "Modifies app resources."
) {
    execute {
        document("res/values/strings.xml").use { doc ->
            // XML DOM manipulation
        }
    }
}
```

### rawResourcePatch
Modifies raw files without resource decoding. Fast.

```kotlin
val myRawPatch = rawResourcePatch(
    name = "Raw file modification",
    description = "Modifies raw files."
) {
    execute {
        get("assets/some_file.json").writeText(newContent)
    }
}
```

## Compatibility Declaration

```kotlin
val COMPATIBILITY_APP = Compatibility(
    name = "App Name",
    packageName = "com.example.app",
    apkFileType = ApkFileType.APK,  // APK, XAPK, APKM, APK_REQUIRED
    appIconColor = 0xFF6200EE.toInt(),
    targets = listOf(
        AppTarget(version = "1.0.0"),
        AppTarget(version = "2.0.0", isExperimental = true)
    )
)
```

## Internal Patches (hidden)
Patches without `name` or `description` are hidden from the UI but usable as dependencies:

```kotlin
val internalHelper = bytecodePatch {
    execute {
        // Prep work for other patches
    }
}
```

## Options
Patches can expose configurable options:

```kotlin
val myPatch = bytecodePatch(name = "Configurable patch") {
    val enableFeature by option<Boolean>(name = "Enable feature")
    val text by stringOption(name = "Custom text")

    execute {
        if (enableFeature) {
            // do something
        }
    }
}
```

## Key Imports
```kotlin
import app.morphe.patcher.patch.bytecodePatch
import app.morphe.patcher.patch.resourcePatch
import app.morphe.patcher.patch.rawResourcePatch
import app.morphe.patcher.data.Compatibility
import app.morphe.patcher.data.AppTarget
import app.morphe.patcher.data.ApkFileType
import app.morphe.patcher.patch.Fingerprint
import app.morphe.patcher.patch.filter.*
import app.morphe.patcher.data.AccessFlags
import app.morphe.patcher.data.Opcode
```

## Conventions
1. Name patches after what they do ("Remove ads", "Premium Unlock")
2. Write descriptions in third person, present tense, ending with period
3. Name fingerprints after best guess of method behavior
4. Keep patches minimal — put complex logic in extensions (.mpe)
5. Never use obfuscated names in fingerprints
6. A fingerprint should contain information unlikely to change between app updates

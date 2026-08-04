# Patch Anatomy Reference

## Patch DSL Overview

Morphe patches are defined using Kotlin DSL builders. Three patch types exist:

| Type | DSL Function | Context | Speed |
|------|-------------|---------|-------|
| Bytecode | `bytecodePatch { }` | `BytecodePatchContext` | Fast |
| Resource | `resourcePatch { }` | `ResourcePatchContext` | Slow (decode/encode) |
| Raw Resource | `rawResourcePatch { }` | `RawResourcePatchContext` | Fast |

## BytecodePatch — Full Reference

```kotlin
@Suppress("unused")
val patchName = bytecodePatch(
    name = "Patch Name",               // Required: displayed in UI
    description = "Description.",       // Optional: third person, present tense
    default = true                      // Default enabled state
) {
    compatibleWith(COMPATIBILITY_APP)   // Required: app compatibility
    dependsOn(otherPatch)               // Optional: dependencies run first
    extendWith("extension.mpe")         // Optional: merge precompiled DEX

    val option1 by stringOption(name = "Option Name")
    val option2 by option<Boolean>(name = "Enable feature")

    execute {
        // Main patch logic — runs when patch executes
        SomeFingerprint.method.addInstructions(0, """
            const/4 v0, 0x1
            return v0
        """)
    }

    finalize {
        // Cleanup — runs after ALL patches complete (reverse order)
    }
}
```

## Compatibility

```kotlin
val COMPATIBILITY_APP = Compatibility(
    name = "App Display Name",
    packageName = "com.example.app",   // Android package name
    apkFileType = ApkFileType.APK,     // APK, XAPK, APKM, APK_REQUIRED
    appIconColor = 0xFF6200EE.toInt(),
    targets = listOf(
        AppTarget(version = "1.0.0"),
        AppTarget(version = "2.0.0", isExperimental = true),
        AppTarget(version = null, isExperimental = true),  // any version
    )
)
```

If `compatibleWith` is not called, the patch is compatible with any app.
If a target has null version, it matches any version.

## Fingerprints — What They Match

Fingerprints locate methods in obfuscated bytecode using stable characteristics:

```kotlin
object MyFingerprint : Fingerprint(
    definingClass = "Lcom/example/Class;",     // class descriptor
    name = "methodName",                        // only for non-obfuscated methods
    accessFlags = listOf(AccessFlags.PUBLIC),   // method access flags
    returnType = "Z",                           // return type descriptor
    parameters = listOf("Ljava/lang/String;"),  // parameter type descriptors
    filters = listOf(...),                      // ordered instruction filters
    strings = listOf("constant1", "constant2"), // unordered string constants
    classFingerprint = ClassFinderFingerprint,  // find class via another fingerprint
)
```

All declared fields must match. Use only information that is stable across app versions.

## Instruction Filters Reference

| Filter | Syntax | Matches |
|--------|--------|---------|
| `string("text")` | `string(text: String)` | `const-string` with exact text |
| `methodCall(...)` | `methodCall(definingClass, name, returnType, parameters)` | Any invoke-* with matching method |
| `fieldAccess(...)` | `fieldAccess(opcode, definingClass, name, type)` | Get/put field operations |
| `opcode(op)` | `opcode(opcode, location?)` | Any instruction with matching opcode |
| `literal(n)` | `literal(value: Long)` | Numeric literal constant |
| `anyInstruction(...)` | `anyInstruction(vararg filters)` | Any of the sub-filters |

### Simplified methodCall / fieldAccess
```kotlin
methodCall(smali = "Landroid/net/Uri;->parse(Ljava/lang/String;)Landroid/net/Uri;")
fieldAccess(smali = "Landroid/os/Build;->MODEL:Ljava/lang/String;")
```

### Location Constraints
| Constraint | Behavior |
|-----------|----------|
| (default) | Any number of instructions between this and previous filter |
| `MatchAfterImmediately()` | Must match instruction right after previous |
| `MatchAfterWithin(n: Int)` | Must match within n instructions |
| `MatchFirst()` | Must match at first instruction of method |

## Execute Block APIs

### classDefBy
```kotlin
val superClass = classDefBy(fingerprint.originalMethod.returnType).superclass
```

### mutableClassDefBy
```kotlin
val mutableClass = mutableClassDefBy(superClass)
mutableClass.methods.add(Method())
```

### get / delete (resource files)
```kotlin
val file = get("res/values/strings.xml")          // read resource
val content = file.readText()
file.writeText(content)                             // write resource
delete("path/to/file.xml")                          // mark for deletion
get("assets/config.json", recursive = true)         // recursive directory reading
```

### document (XML DOM)
```kotlin
document("res/values/strings.xml").use { doc ->
    val element = doc.createElement("string").apply {
        textContent = "Hello, World!"
    }
    doc.documentElement.appendChild(element)
}
```

### Fingerprint match access
```kotlin
fingerprint.instructionMatches                 // List<InstructionMatch>
fingerprint.instructionMatches[index].index    // instruction index in method
fingerprint.instructionMatches[index].getInstruction<T>() // typed instruction
fingerprint.method                             // mutable method
fingerprint.originalMethod                     // immutable method
fingerprint.classDef                           // mutable class
fingerprint.originalClassDef                   // immutable class
fingerprint.matchAllOrNull()                   // find all matching methods
fingerprint.clearMatch()                       // reset match for re-matching
```

## Internal / Hidden Patches

Patches without `name` or `description` are invisible to Morphe Manager users but usable as dependencies:

```kotlin
val baseInfra = bytecodePatch {
    execute {
        // Setup work shared by other patches
    }
}

val userPatch = bytecodePatch(name = "User Feature") {
    dependsOn(baseInfra)
    execute {
        // Uses setup from baseInfra
    }
}
```

## Options

```kotlin
val patch = bytecodePatch(name = "Configurable") {
    val toggle by option<Boolean>(name = "Enable feature")
    val text by stringOption(name = "Custom text")
    val count by option<Int>(name = "Count", Default(42), Allowed(1, 2, 3, 42))

    execute {
        if (toggle) { /* do thing */ }
    }
}
```

Options are set externally after loading:
```kotlin
patchInstance.options["Enable feature"] = true
```

Shared options (referenced by multiple patches):
```kotlin
val sharedOption = stringOption(name = "Shared setting")
val patchA = bytecodePatch(name = "A") { val opt by sharedOption() }
val patchB = bytecodePatch(name = "B") { val opt by sharedOption() }
```

## Finalization Order

Finalization runs after all patches execute, in reverse execution order:
```kotlin
val base = bytecodePatch(name = "Base") {
    execute { /* runs 1st */ }
    finalize { /* runs 2nd */ }
}
val dependent = bytecodePatch(name = "Dependent") {
    dependsOn(base)
    execute { /* runs 2nd */ }
    finalize { /* runs 1st */ }
}
```

## Extensions (.mpe files)

Precompiled DEX files merged into the APK before patch execution:
```kotlin
extendWith("my-extension.mpe")
```

In the patch execute block, reference extension classes:
```smali
invoke-static {}, LMyExtensionClass;->helperMethod()V
```

Write extensions in Java/Kotlin, compile to DEX, place in `extensions/` directory.

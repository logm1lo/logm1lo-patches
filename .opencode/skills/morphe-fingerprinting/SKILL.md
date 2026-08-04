---
name: morphe-fingerprinting
description: Create Smali bytecode fingerprints to locate obfuscated methods across app versions using filter patterns
---

# Morphe Fingerprinting

## What I do
- Explain how to create fingerprints for locating methods in Smali bytecode
- Show all filter types and how to combine them
- Demonstrate how to use fingerprint matches in patches
- Provide real-world fingerprinting examples

## When to use me
Use this when creating or debugging fingerprints, searching for methods in decompiled code,
or writing patch execute blocks that need to locate specific methods.

## Fingerprint Basics

A fingerprint is a partial method description used to uniquely match a method by its stable characteristics — not by names that change with updates.

```kotlin
object AdLoaderFingerprint : Fingerprint(
    definingClass = "Lcom/some/app/ads/AdsLoader;",
    accessFlags = listOf(AccessFlags.PUBLIC, AccessFlags.FINAL),
    returnType = "Z",
    parameters = listOf("Ljava/lang/String;", "I", "L"),
    filters = listOf(
        fieldAccess(opcode = Opcode.IGET, definingClass = "this", type = "Ljava/util/Map;"),
        string("showBannerAds"),
        methodCall(definingClass = "Ljava/lang/String;", name = "equals"),
        opcode(Opcode.MOVE_RESULT, InstructionLocation.MatchAfterImmediately()),
        literal(1337),
        opcode(Opcode.IF_EQ)
    )
)
```

## Filter Types

| Filter | Purpose | Example |
|--------|---------|---------|
| `string("text")` | Match a const-string instruction | `string("premium_feature")` |
| `methodCall(...)` | Match a method invocation | `methodCall(definingClass="Ljava/lang/String;", name="equals")` |
| `fieldAccess(...)` | Match a field access | `fieldAccess(opcode=IGET, type="Ljava/lang/String;")` |
| `opcode(op, location)` | Match a specific opcode | `opcode(Opcode.IF_EQZ)` |
| `literal(int)` | Match a literal constant | `literal(0)` |
| `anyInstruction(...)` | Match one of several possibilities | `anyInstruction(string("old"), string("new"))` |

## Location Constraints

| Constraint | Purpose |
|-----------|---------|
| (default) | Zero or more instructions can exist between filters |
| `MatchAfterImmediately()` | Filter must match right after previous one |
| `MatchAfterWithin(n)` | Filter must match within n instructions of previous |
| `MatchFirst()` | Filter must match at the first instruction |

## Simplified methodCall / fieldAccess

Use copy-pasted Smali for un-obfuscated API calls:
```kotlin
methodCall(smali = "Landroid/net/Uri;->parse(Ljava/lang/String;)Landroid/net/Uri;")
fieldAccess(smali = "Landroid/os/Build;->MODEL:Ljava/lang/String;")
```

## Using Fingerprints in Patches

```kotlin
execute {
    AdLoaderFingerprint.let {
        val filter3 = it.instructionMatches[2]  // 0-indexed
        val index = filter3.index
        val register = filter3.getInstruction<OneRegisterInstruction>().registerA
        it.method.addInstructions(index + 1, "const/4 v$register, 0x0")
    }
}
```

## Fingerprint Properties

- `originalClassDef` / `originalClassDefOrNull` — immutable class (read-only)
- `originalMethod` / `originalMethodOrNull` — immutable method (read-only)
- `classDef` / `classDefOrNull` — mutable class (replace original when accessed)
- `method` / `methodOrNull` — mutable method (replace original when accessed)

## matchAllOrNull() — Global Replace

Replace strings across ALL matching methods:
```kotlin
Fingerprint(filters = listOf(string("old_url"))).matchAllOrNull()?.forEach { match ->
    match.method.apply {
        findInstructionIndicesReversedOrThrow(stringFilter).forEach { index ->
            val reg = getInstruction<OneRegisterInstruction>(index).registerA
            replaceInstruction(index, "const-string v$reg, \"new_url\"")
        }
    }
}
```

## Manual Matching

```kotlin
showAdFingerprint.match(classes)                        // match in class list
showAdFingerprint.match(adsLoaderClass)                 // match in specific class
showAdFingerprint.match(adsLoaderFingerprint.originalClassDef)  // use another fingerprint's class
```

## classFingerprint — Two-stage Matching

Use one fingerprint to find a class, then match within that class:
```kotlin
val showAdFingerprint = Fingerprint(
    classFingerprint = Fingerprint(strings = listOf("classField=")),
    returnType = "Z",
    parameters = listOf("Ljava/lang/String;"),
    filters = listOf(
        methodCall(name = "getValue", returnType = "Z"),
        opcode(Opcode.MOVE_RESULT, MatchAfterImmediately())
    )
)
```

## Multi-modification Order

When making multiple changes to the same method, modify from last index to first to preserve correct indices. After modifications, call `clearMatch()` then `match()` to refresh match data.

## Advanced Fingerprinting Patterns

### Gson @SerializedName Anchoring (R8 Survivable)

When targeting data model classes, Gson serialized field names NEVER change between app updates
(they mirror the API contract). Use them as stable fingerprint anchors:

```kotlin
// The @SerializedName("is_promoted") value is permanent
// Use it to locate the field even when the obfuscated Java field name changes
object PromotedPinFingerprint : Fingerprint(
    filters = listOf(
        string("is_promoted"),         // @SerializedName stable value
        fieldAccess(                     // actual field IPUT/SGET
            opcode = Opcode.IPUT_BOOLEAN,
            type = "Z"
        ),
    )
)
```

### Structural Class Resolution (Minified Code)

When class names are R8-minified (a, b, c...), resolve by STRUCTURE not NAME:

```kotlin
execute {
    // Find class that declares a KNOWN non-obfuscated method signature
    val targetClass = classes.first { classDef ->
        classDef.methods.any { method ->
            method.name == "addAsLastInActives" &&  // stable method name
            method.parameterTypes.size == 1 &&
            method.parameterTypes[0].endsWith("Source;")
        }
    }
    // Now use the resolved class type for a fingerprint
    val methodFp = Fingerprint(
        definingClass = targetClass.type,
        filters = listOf(...)
    )
}
```

### Multi-Class Field Name Extraction Chain

Chaining fingerprints across classes to extract and use obfuscated field names:

```kotlin
// Stage 1: Find the data class via stable strings/fields
val requestClassFp = Fingerprint(strings = listOf("feed_recs"))

// Stage 2: Find the constructor → extract the obfuscated field name
val constructorFp = Fingerprint(
    definingClass = requestClassFp.originalClassDef!!.type,
    name = "<init>",
    returnType = "V",
    parameters = listOf("Ljava/lang/String;", "Ljava/util/Map;"),
    filters = listOf(
        fieldAccess(opcode = Opcode.IPUT_OBJECT, type = "Ljava/util/Map;"),
    )
)

// Extract the field name for use in another class
val obfuscatedFieldName = constructorFp
    .instructionMatches[0]
    .getInstruction<Instruction35c>()
    .reference.name
```

### Register-Window Backward Scanning (R8 Resilient)

When register assignments shift between R8 builds, walk backward to find the constant:

```kotlin
fun findStringBefore(
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

### Dynamic Constructor Field Rewriting

Extract field references from a constructor dynamically, then rewrite the body:

```kotlin
execute {
    val ctor = SomeConstructorFingerprint.method
    val fields = mutableListOf<FieldReference>()
    ctor.implementation?.instructions?.forEach { insn ->
        if (insn.opcode in setOf(Opcode.IPUT_OBJECT, Opcode.IPUT, Opcode.IPUT_WIDE)) {
            fields.add((insn as Instruction35c).reference as FieldReference)
        }
    }
    // Now rewrite the constructor body with spoofed field values
    ctor.removeInstructions(0, ctor.implementation!!.instructions.size)
    ctor.addInstructions(0, generateSpoofedBody(fields, spoofedValues))
}
```

### Pure Opcode Pattern Matching (Last Resort)

```kotlin
// Using OpcodesFilter when no other filter combination works
filters = OpcodesFilter.opcodesToFilters(
    Opcode.CONST_4,
    Opcode.IF_EQZ,
    Opcode.INVOKE_DIRECT,
    Opcode.RETURN_VOID
)
```
WARNING: Fragile — use only when method has an extremely unusual opcode sequence.

# Fingerprinting Reference

## Core Concept

Fingerprints locate methods in obfuscated Android bytecode without relying on method names or class names that change between app versions. Instead, they match on **stable characteristics**: return types, parameter types, access flags, and ordered instruction patterns.

## Fingerprint Declaration

### As named object (preferred — includes name in error traces)
```kotlin
object PremiumCheckFingerprint : Fingerprint(
    returnType = "Z",
    accessFlags = listOf(AccessFlags.PUBLIC, AccessFlags.STATIC),
    parameters = listOf("Landroid/content/Context;"),
    filters = listOf(
        methodCall(definingClass = "Lcom/revenuecat/purchases/CustomerInfo;", name = "getEntitlements"),
        methodCall(definingClass = "Lcom/revenuecat/purchases/EntitlementInfos;", name = "getActive"),
        opcode(Opcode.MOVE_RESULT_OBJECT, InstructionLocation.MatchAfterImmediately())
    )
)
```

### As local variable (when dependent on previous fingerprint)
```kotlin
val localFingerprint = Fingerprint(
    returnType = "Z",
    definingClass = otherFingerprint.originalClassDef.type,
    parameters = listOf(),
    filters = listOf(
        string("some_constant"),
        methodCall(name = "getValue", returnType = "Z")
    )
)
```

## Filter Types Deep Reference

### stringFilter
```kotlin
string("exact string literal")
```
Matches `const-string` instructions with exactly matching text.

### methodCall
```kotlin
methodCall(
    definingClass = "Ljava/lang/String;",  // class descriptor
    name = "equals",                        // method name
    returnType = "Z",                       // return type descriptor
    parameters = listOf("Ljava/lang/Object;") // parameter type descriptors
)
```
Matches any invoke-* instruction. Use `name` only for non-obfuscated API calls. For obfuscated methods, omit `name` and rely on returnType + parameters.

Simplified copy-paste:
```kotlin
methodCall(smali = "Landroid/net/Uri;->parse(Ljava/lang/String;)Landroid/net/Uri;")
```

### fieldAccess
```kotlin
fieldAccess(
    opcode = Opcode.IGET,                  // IGET, IPUT, SGET, SPUT
    definingClass = "this",                // "this" = method's own class
    name = "fieldName",                    // field name (non-obfuscated only)
    type = "Ljava/lang/String;"            // field type descriptor
)
```

Simplified:
```kotlin
fieldAccess(smali = "Landroid/os/Build;->MODEL:Ljava/lang/String;")
```

### opcode
```kotlin
opcode(Opcode.MOVE_RESULT)
opcode(Opcode.IF_EQZ)
opcode(Opcode.IF_EQZ, InstructionLocation.MatchAfterImmediately())
opcode(Opcode.IF_EQZ, InstructionLocation.MatchAfterWithin(10))
```

### literal
```kotlin
literal(0)         // const/4 with value 0
literal(1337)      // const/16 with value 1337
literal(-1)        // const/4 with value -1
```

### anyInstruction
```kotlin
anyInstruction(
    string("old_string_v1"),
    string("new_string_v2")
)
```
Matches if ANY of the sub-filters match at the current position. Useful when strings change between versions.

## Location Constraints

| Constraint | Effect |
|-----------|--------|
| None (default) | 0-N instructions may exist between this and previous match |
| `InstructionLocation.MatchAfterImmediately()` | Must match immediately after previous match |
| `InstructionLocation.MatchAfterWithin(n)` | Must match within `n` instructions after previous match |
| `InstructionLocation.MatchFirst()` | Must match the first instruction of the method |

## String Matching Strategies

### Ordered strings (preferred)
```kotlin
filters = listOf(
    string("first_string"),
    string("second_string"),
    // ...other filters...
)
```
Strings must appear in the method in the declared order.

### Unordered strings (legacy)
```kotlin
strings = listOf("enum_value_1", "enum_value_2", "enum_value_3")
```
Matches in any order. Best for enum initialization methods. Match indices not in `instructionMatches`.

## Pure Opcode Matching (avoid if possible)

```kotlin
filters = OpcodesFilter.opcodesToFilters(Opcode.MOVE_RESULT, Opcode.IF_EQZ, Opcode.GOTO)
```
Fragile — all opcodes must appear exactly consecutively. Only use when no other filters work.

## classFingerprint — Two-stage Class Discovery

```kotlin
val findClassFingerprint = Fingerprint(
    strings = listOf("unique_class_identifier_string"),
)
val targetMethodFingerprint = Fingerprint(
    classFingerprint = findClassFingerprint,
    returnType = "Z",
    parameters = listOf(),
    filters = listOf(
        methodCall(name = "getPremiumStatus", returnType = "Z"),
    )
)
```

## Using Fingerprint Matches in Patches

### Basic usage
```kotlin
execute {
    SomeFingerprint.let {
        val match3 = it.instructionMatches[2]  // 0-indexed match of 3rd filter
        val index = match3.index                 // instruction position in method
        val reg = match3.getInstruction<OneRegisterInstruction>().registerA
        it.method.addInstructions(index + 1, "const/4 v$reg, 0x0")
    }
}
```

### Match properties
```kotlin
fingerprint.instructionMatches          // List<InstructionMatch>
match.index                              // Int: instruction position
match.getInstruction<Type>()             // Typed access to matched instruction
fingerprint.method                       // Mutable method (MorpheMethod)
fingerprint.originalMethod               // Immutable method (MethodImplementation)
fingerprint.classDef                     // Mutable class (MutableClassDef)
fingerprint.originalClassDef            // Immutable class (ClassDef)
```

### matchAllOrNull — global operations
```kotlin
val stringFilter = string("http://old-api.example.com")
Fingerprint(filters = listOf(stringFilter)).matchAllOrNull()?.forEach { match ->
    match.method.apply {
        findInstructionIndicesReversedOrThrow(stringFilter).forEach { index ->
            val reg = getInstruction<OneRegisterInstruction>(index).registerA
            replaceInstruction(index, "const-string v$reg, \"https://new-api.example.com\"")
        }
    }
}
```

### Manual matching
```kotlin
// Match in list of known classes
val match = fingerprint.match(classes)

// Match in specific class
val match = fingerprint.match(classDefBy("Lcom/example/Class;"))

// Match in class found by another fingerprint
val match = fingerprint.match(otherFingerprint.originalClassDef)
```

### Re-matching after modifications
When making multiple modifications to a method, work from last index to first to preserve correctness. Then:
```kotlin
fingerprint.clearMatch()
fingerprint.match()
```

## Practical Examples

### Force boolean return true
```kotlin
object IsPremiumFingerprint : Fingerprint(
    returnType = "Z",
    parameters = listOf(),
    filters = listOf(
        string("premium_access"),
        methodCall(returnType = "Z")
    )
)

execute {
    IsPremiumFingerprint.method.addInstructions(0, """
        const/4 v0, 0x1
        return v0
    """)
}
```

### Remove ad loading
```kotlin
object LoadAdFingerprint : Fingerprint(
    returnType = "V",
    parameters = listOf(),
    filters = listOf(
        string("ca-app-pub-"),
        methodCall(name = "loadAd")
    )
)

execute {
    LoadAdFingerprint.method.addInstructions(0, "return-void")
}
```

### Override feature gate
```kotlin
object FeatureGateFingerprint : Fingerprint(
    returnType = "Z",
    accessFlags = listOf(AccessFlags.STATIC),
    parameters = listOf("Ljava/lang/String;"),
    filters = listOf(
        string("pro_features"),
        methodCall(name = "getBoolean", returnType = "Z"),
        opcode(Opcode.MOVE_RESULT, MatchAfterImmediately()),
        opcode(Opcode.IF_EQZ)
    )
)

execute {
    FeatureGateFingerprint.let {
        val moveIdx = it.instructionMatches[2].index
        val reg = it.instructionMatches[2].getInstruction<OneRegisterInstruction>().registerA
        it.method.addInstructions(moveIdx + 1, "const/4 v$reg, 0x1")
    }
}
```

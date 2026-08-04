---
name: morphe-smali
description: Complete Smali/Dalvik bytecode reference — type descriptors, opcodes, register conventions, and common patch patterns
---

# Morphe Smali Reference

## What I do
- Provide a complete Smali/Dalvik bytecode cheat sheet
- Show common patch injection patterns
- Explain register conventions and type descriptors
- Reference all common opcodes

## When to use me
Use this when writing `addInstructions()` or `replaceInstruction()` calls in patches,
or when analyzing Smali bytecode to create fingerprints.

## Type Descriptors

| Java Type | Smali Descriptor |
|-----------|-----------------|
| `void` | `V` |
| `boolean` | `Z` |
| `byte` | `B` |
| `short` | `S` |
| `char` | `C` |
| `int` | `I` |
| `long` | `J` |
| `float` | `F` |
| `double` | `D` |
| `String` | `Ljava/lang/String;` |
| `int[]` | `[I` |
| `Object[]` | `[Ljava/lang/Object;` |
| `SomeClass` | `Lcom/example/SomeClass;` |

## Method Signatures

```
<access-flags> <name>(<parameters>)<return-type>
```

Examples:
- `public static boolean isPremium()` → `public static isPremium()Z`
- `private void onStart(String, int)` → `private onStart(Ljava/lang/String;I)V`

## Registers

- `v0`-`vN` — local registers (can hold any type except long/double which use 2)
- `p0`-`pN` — parameter registers (p0 = `this` in non-static methods)
- Parameter count: first N registers are also available as v(N-1)..v0 in reverse

## Common Opcodes

### Constants / Returns
| Opcode | Description | Example |
|--------|-------------|---------|
| `const/4 vA, #B` | Load 4-bit constant | `const/4 v0, 0x1` |
| `const/16 vA, #B` | Load 16-bit constant | `const/16 v0, 0x539` |
| `const-string vA, "text"` | Load string constant | `const-string v0, "hello"` |
| `const/4 v0, 0x0` | Load zero (false) | Return false |
| `const/4 v0, 0x1` | Load one (true) | Return true |
| `return-void` | Return nothing | |
| `return vA` | Return value in register | `return v0` |
| `return-object vA` | Return object reference | |

### Move
| Opcode | Description |
|--------|-------------|
| `move vA, vB` | Move vB to vA |
| `move-result vA` | Move result of previous invoke to vA |
| `move-result-object vA` | Move object result of previous invoke |
| `move-exception vA` | Move exception to vA |

### Method Invocation
| Opcode | Description |
|--------|-------------|
| `invoke-virtual {params}, method` | Virtual method call |
| `invoke-static {params}, method` | Static method call |
| `invoke-direct {params}, method` | Direct method call (constructors, private) |
| `invoke-interface {params}, method` | Interface method call |
| `invoke-super {params}, method` | Superclass method call |

### Field Access
| Opcode | Description |
|--------|-------------|
| `iget vA, vB, field` | Get instance field |
| `iput vA, vB, field` | Set instance field |
| `sget vA, field` | Get static field |
| `sput vA, field` | Set static field |
| `iget-object` / `iput-object` | Object field versions |
| `sget-object` / `sput-object` | Static object versions |

### Branching
| Opcode | Description |
|--------|-------------|
| `if-eq vA, vB, :label` | Branch if equal |
| `if-ne vA, vB, :label` | Branch if not equal |
| `if-eqz vA, :label` | Branch if zero (false) |
| `if-nez vA, :label` | Branch if non-zero (true) |
| `if-lt vA, vB, :label` | Branch if less than |
| `goto :label` | Unconditional jump |

### Object / Array
| Opcode | Description |
|--------|-------------|
| `new-instance vA, type` | Create new instance |
| `check-cast vA, type` | Type cast |
| `array-length vA, vB` | Get array length |
| `aget` / `aput` | Array element get/set |

## Common Patch Patterns

### Force return true
```smali
const/4 v0, 0x1
return v0
```

### Force return false
```smali
const/4 v0, 0x0
return v0
```

### Force return null
```smali
const/4 v0, 0x0
return-object v0
```

### Early void return (skip method body)
```smali
return-void
```

### Override boolean after method call
```smali
invoke-static {}, LSomeClass;->getValue()Z
move-result v0
const/4 v0, 0x1        # override result
return v0
```

### Conditional always-true
```smali
# Replace: if-eqz v0, :skip_ads
# With unconditional goto skip:
goto :skip_ads
```

### Replace method call with constant
```smali
# Old: invoke-static {}, Lcom/app/Premium;->isPro()Z
# New:
const/4 v0, 0x1
```

### Access Flags
| Flag | Smali | Hex |
|------|-------|-----|
| PUBLIC | 0x1 | |
| PRIVATE | 0x2 | |
| PROTECTED | 0x4 | |
| STATIC | 0x8 | |
| FINAL | 0x10 | |
| SYNCHRONIZED | 0x20 | |
| BRIDGE | 0x40 | |
| VARARGS | 0x80 | |
| NATIVE | 0x100 | |
| ABSTRACT | 0x400 | |
| STRICTFP | 0x800 | |
| SYNTHETIC | 0x1000 | |
| CONSTRUCTOR | 0x10000 | |
| DECLARED_SYNCHRONIZED | 0x20000 | |

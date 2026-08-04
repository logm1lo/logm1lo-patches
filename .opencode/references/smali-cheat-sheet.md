# Smali/Dalvik Bytecode Reference

## Type Descriptors

| Java | Smali |
|------|-------|
| `void` | `V` |
| `boolean` | `Z` |
| `byte` | `B` |
| `short` | `S` |
| `char` | `C` |
| `int` | `I` |
| `long` | `J` |
| `float` | `F` |
| `double` | `D` |
| `Object` | `Ljava/lang/Object;` |
| `String` | `Ljava/lang/String;` |
| `int[]` | `[I` |
| `String[]` | `[Ljava/lang/String;` |
| `MyClass` | `Lcom/example/MyClass;` |

## Method Header Format
```
.method <access-flags> <name>(<params>)<return>
    .registers <N>
    ...
.end method
```

Examples:
- `public static isPro()Z` — public static boolean method
- `protected final onCreate(Landroid/os/Bundle;)V` — protected final void with Bundle param
- `private getValue(Ljava/lang/String;)Ljava/lang/Object;` — private returns Object

## Registers

- `v0..vN` — local registers (long/double use 2 registers: `v0` + `v1`)
- `p0..pN` — parameter registers
- In non-static methods: `p0` = `this`
- Parameter mapping: last param is `pN`, first param maps to high v-registers

## Complete Opcode Reference

### Constants
| Opcode | Args | Description |
|--------|------|-------------|
| `const/4 vA, #+B` | vA, int (4-bit) | Load 4-bit signed constant (-8 to 7) |
| `const/16 vA, #+B` | vA, int (16-bit) | Load 16-bit constant |
| `const vA, #+B` | vA, int (32-bit) | Load 32-bit constant |
| `const/high16 vA, #+B` | vA, int | Load high 16 bits |
| `const-wide/16 vA, #+B` | vA, long | Load 16-bit wide constant |
| `const-wide vA, #+B` | vA, long | Load 64-bit wide constant |
| `const-string vA, string` | vA, string | Load string constant |
| `const-class vA, type` | vA, type | Load Class object |
| `const/4 v0, 0x0` | | false / zero |
| `const/4 v0, 0x1` | | true / one |

### Return
| Opcode | Description |
|--------|-------------|
| `return-void` | Return from void method |
| `return vA` | Return int/boolean |
| `return-wide vA` | Return long/double |
| `return-object vA` | Return object reference |

### Move
| Opcode | Description |
|--------|-------------|
| `move vA, vB` | Move vB to vA |
| `move/from16 vA, vB` | Move with 16-bit source |
| `move/16 vA, vB` | Move with 16-bit both |
| `move-wide vA, vB` | Move wide (long/double) |
| `move-object vA, vB` | Move object reference |
| `move-result vA` | Move previous invoke's result |
| `move-result-wide vA` | Move previous invoke's wide result |
| `move-result-object vA` | Move previous invoke's object result |
| `move-exception vA` | Move exception |

### Arithmetic
| Opcode | Description |
|--------|-------------|
| `add-int vA, vB, vC` | vA = vB + vC |
| `sub-int vA, vB, vC` | vA = vB - vC |
| `mul-int vA, vB, vC` | vA = vB * vC |
| `div-int vA, vB, vC` | vA = vB / vC |
| `rem-int vA, vB, vC` | vA = vB % vC |
| `neg-int vA, vB` | vA = -vB |
| `add-int/lit8 vA, vB, #+C` | vA = vB + literal (8-bit) |
| `sub-int/lit8 vA, vB, #+C` | vA = vB - literal |
| `mul-int/lit8 vA, vB, #+C` | vA = vB * literal |
| `div-int/lit8 vA, vB, #+C` | vA = vB / literal |
| `add-long`, `sub-long`, etc. | Wide variants |
| `add-float`, `sub-float`, etc. | Float variants |
| `add-double`, `sub-double`, etc. | Double variants |

### Invocation (Method Calls)
| Opcode | Syntax | Description |
|--------|--------|-------------|
| `invoke-virtual` | `{params}, Lclass;->method(params)R` | Virtual method |
| `invoke-static` | `{params}, Lclass;->method(params)R` | Static method |
| `invoke-direct` | `{params}, Lclass;->method(params)R` | Direct (constructors, private) |
| `invoke-interface` | `{params}, Lclass;->method(params)R` | Interface method |
| `invoke-super` | `{params}, Lclass;->method(params)R` | Superclass method |
| `invoke-virtual/range` | `{vC..vD}, method` | Virtual with range |

### Field Access
| Opcode | Description |
|--------|-------------|
| `iget vA, vB, Lclass;->field:T` | Read instance field (int/short/boolean) |
| `iget-wide vA, vB, field` | Read wide instance field |
| `iget-object vA, vB, field` | Read object instance field |
| `iput vA, vB, Lclass;->field:T` | Write instance field |
| `iput-wide vA, vB, field` | Write wide instance field |
| `iput-object vA, vB, field` | Write object instance field |
| `sget vA, Lclass;->field:T` | Read static field |
| `sget-wide vA, field` | Read wide static field |
| `sget-object vA, field` | Read object static field |
| `sput vA, Lclass;->field:T` | Write static field |
| `sput-wide vA, field` | Write wide static field |
| `sput-object vA, field` | Write object static field |

### Branching / Conditionals
| Opcode | Description |
|--------|-------------|
| `if-eq vA, vB, :label` | Branch if vA == vB |
| `if-ne vA, vB, :label` | Branch if vA != vB |
| `if-lt vA, vB, :label` | Branch if vA < vB |
| `if-le vA, vB, :label` | Branch if vA <= vB |
| `if-gt vA, vB, :label` | Branch if vA > vB |
| `if-ge vA, vB, :label` | Branch if vA >= vB |
| `if-eqz vA, :label` | Branch if vA == 0 |
| `if-nez vA, :label` | Branch if vA != 0 |
| `if-ltz vA, :label` | Branch if vA < 0 |
| `if-lez vA, :label` | Branch if vA <= 0 |
| `if-gtz vA, :label` | Branch if vA > 0 |
| `if-gez vA, :label` | Branch if vA >= 0 |
| `goto :label` | Unconditional branch |
| `goto/16 :label` | Branch with 16-bit offset |
| `goto/32 :label` | Branch with 32-bit offset |

### Object / Array
| Opcode | Description |
|--------|-------------|
| `new-instance vA, Ltype;` | Create new object instance |
| `new-array vA, vB, [type` | Create new array |
| `filled-new-array {params}, [type` | Create and fill array |
| `filled-new-array/range {vC..vD}, [type` | Range variant |
| `array-length vA, vB` | Get array length |
| `aget vA, vB, vC` | Get array element (int) |
| `aget-wide`, `aget-object` | Wide/object variants |
| `aput vA, vB, vC` | Set array element (int) |
| `aput-wide`, `aput-object` | Wide/object variants |
| `check-cast vA, Ltype;` | Type cast check |
| `instance-of vA, vB, Ltype;` | Instanceof check |
| `monitor-enter vA` | Acquire monitor |
| `monitor-exit vA` | Release monitor |

### Unary / Bitwise
| Opcode | Description |
|--------|-------------|
| `not-int vA, vB` | Bitwise NOT |
| `neg-int vA, vB` | Negate |
| `and-int vA, vB, vC` | Bitwise AND |
| `or-int vA, vB, vC` | Bitwise OR |
| `xor-int vA, vB, vC` | Bitwise XOR |
| `shl-int vA, vB, vC` | Shift left |
| `shr-int vA, vB, vC` | Shift right (signed) |
| `ushr-int vA, vB, vC` | Shift right (unsigned) |
| `rsub-int vA, vB, #+C` | Reverse subtract |
| `rsub-int/lit8 vA, vB, #+C` | Reverse subtract lit8 |
| `int-to-float vA, vB` | Int to float conversion |
| `int-to-double vA, vB` | Int to double |
| `float-to-int vA, vB` | Float to int |
| `double-to-int vA, vB` | Double to int |

## Common Patch Injection Patterns

### Pattern 1: Force return true
```smali
# Replace any boolean method with:
const/4 v0, 0x1
return v0
```
Use: `fingerprint.method.addInstructions(0, "const/4 v0, 0x1\nreturn v0")`

### Pattern 2: Force return false
```smali
const/4 v0, 0x0
return v0
```

### Pattern 3: Override boolean after method call
```smali
invoke-virtual {p0}, Lcom/app/Auth;->checkSubscription()Z
move-result v0
# Insert: const/4 v0, 0x1
return v0
```

### Pattern 4: Skip conditional check
```smali
# Original:
#   if-eqz v0, :skip_feature
#   ...load feature...
#   :skip_feature
# Patched to always skip:
#   goto :skip_feature
```

### Pattern 5: Replace static method call with constant
```smali
# Original: invoke-static {}, Lcom/app/Premium;->isPro()Z
# Replace entire instruction sequence with:
const/4 v0, 0x1
```

### Pattern 6: Override const-string
```smali
# Original: const-string v0, "free_tier"
# Replace with:
const-string v0, "premium"
```

### Pattern 7: Early void return (skip method body)
```smali
# Insert at method start:
return-void
```

## Access Flags Reference

| Flag | Kotlin |
|------|--------|
| 0x1 | `AccessFlags.PUBLIC` |
| 0x2 | `AccessFlags.PRIVATE` |
| 0x4 | `AccessFlags.PROTECTED` |
| 0x8 | `AccessFlags.STATIC` |
| 0x10 | `AccessFlags.FINAL` |
| 0x20 | `AccessFlags.SYNCHRONIZED` |
| 0x40 | `AccessFlags.BRIDGE` |
| 0x80 | `AccessFlags.VARARGS` |
| 0x100 | `AccessFlags.NATIVE` |
| 0x200 | `AccessFlags.INTERFACE` |
| 0x400 | `AccessFlags.ABSTRACT` |
| 0x800 | `AccessFlags.STRICTFP` |
| 0x1000 | `AccessFlags.SYNTHETIC` |
| 0x2000 | `AccessFlags.ANNOTATION` |
| 0x4000 | `AccessFlags.ENUM` |
| 0x10000 | `AccessFlags.CONSTRUCTOR` |
| 0x20000 | `AccessFlags.DECLARED_SYNCHRONIZED` |

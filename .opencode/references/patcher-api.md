# Patcher Advanced APIs

## classDefBy — Find immutable classes

```kotlin
execute {
    val classType = classDefBy("Lcom/example/SomeClass;")
    val superClass = classDefBy(fingerprint.originalMethod.returnType).superclass
}
```

Returns: `ClassDef` (immutable class definition)

## mutableClassDefBy — Convert to mutable

```kotlin
execute {
    val immutableClass = classDefBy("Lcom/example/SomeClass;")
    val mutableClass = mutableClassDefBy(immutableClass)
    mutableClass.methods.add(Method())
}
```

Accessing this replaces the original immutable class with the mutable copy in the patcher.

## get — Read/write resource files

```kotlin
execute {
    val file = get("res/values/strings.xml")
    val content = file.readText()
    file.writeText(content)
}
```

For recursive access:
```kotlin
get("res/values/", recursive = true)
```

Returns: `java.io.File`

## delete — Mark files for deletion

```kotlin
execute {
    delete("res/values/old_strings.xml")
    delete("assets/deprecated_config.json")
}
```

Files are deleted when the APK is rebuilt after all patches run.

## document — DOM (XML) manipulation

### Read and modify XML
```kotlin
execute {
    document("res/values/strings.xml").use { doc ->
        val elements = doc.getElementsByTagName("string")
        for (i in 0 until elements.length) {
            val element = elements.item(i)
            if (element.getAttribute("name") == "app_name") {
                element.textContent = "New App Name"
            }
        }
    }
}
```

### Read from InputStream
```kotlin
execute {
    val inputStream = classLoader.getResourceAsStream("template.xml")
    document(inputStream).use { doc ->
        // DOM manipulation
    }
}
```

## Fingerprint Match Navigation

### OneRegisterInstruction
```kotlin
val instruction = fingerprint.instructionMatches[idx].getInstruction<OneRegisterInstruction>()
val register = instruction.registerA
```

### Instruction35c (method/field calls)
```kotlin
val instruction = fingerprint.instructionMatches[idx].getInstruction<Instruction35c>()
val reference = instruction.reference  // method or field reference
```

### Instruction21c (const-string)
```kotlin
val instruction = fingerprint.instructionMatches[idx].getInstruction<Instruction21c>()
val string = instruction.reference  // string constant reference
```

## Method Manipulation

```kotlin
fingerprint.method.apply {
    addInstructions(position, smaliText)        // insert at position
    replaceInstruction(position, smaliText)     // replace single instruction
    removeInstruction(position)                 // remove single instruction
    removeInstructions(start, count)            // remove multiple
    getInstruction<T>(position)                 // get typed instruction
    findInstructionIndicesReversedOrThrow(filter) // find filter match indices (last to first)
}
```

## Loading Patches

```kotlin
import app.morphe.patcher.patch.loadPatchesFromJar

val patches = loadPatchesFromJar(
    setOf(File("patches/build/libs/patches-1.0.0.mpp"))
)
```

## Patcher Usage

```kotlin
import app.morphe.patcher.Patcher
import app.morphe.patcher.PatcherConfig
import kotlinx.coroutines.runBlocking

val patcherResult = Patcher(
    PatcherConfig(apkFile = File("app.apk"))
).use { patcher ->
    patcher += patches
    runBlocking {
        patcher().collect { patchResult ->
            println("Patch: ${patchResult.patch} → ${patchResult.exception ?: "OK"}")
        }
    }
    patcher.get()  // PatcherResult(dexFiles, resources)
}
```

## BytecodeUtils

```kotlin
// Find all instruction indices matching a filter (reversed order)
findInstructionIndicesReversedOrThrow(stringFilter)

// Find all instruction indices (forward order)
findInstructionIndicesOrThrow(anyFilter)
```

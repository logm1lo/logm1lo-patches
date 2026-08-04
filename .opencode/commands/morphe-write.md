---
description: Write Morphe patch code (Constants.kt, Fingerprints.kt, Patch.kt)
agent: morphe-orchestrator
---
## Task: Write Patches for `$ARGUMENTS`

Generate Morphe patch files for `$ARGUMENTS`.

1. Read hunting notes: `analysis/$ARGUMENTS/notes/`
2. Create `patches/src/main/kotlin/app/logm1lo/patches/$ARGUMENTS/shared/Constants.kt`
3. Create `patches/src/main/kotlin/app/logm1lo/patches/$ARGUMENTS/<category>/Fingerprints.kt`
4. Create `patches/src/main/kotlin/app/logm1lo/patches/$ARGUMENTS/<category>/<Name>Patch.kt`
5. Verify build: `./gradlew :patches:buildAndroid`

Route to the patch-writer agent with the app name and hunting target notes.

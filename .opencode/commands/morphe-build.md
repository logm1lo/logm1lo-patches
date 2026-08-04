---
description: Build MPP, apply patches to APK, install via ADB
agent: morphe-orchestrator
---
## Task: Build & Deploy `$ARGUMENTS`

Build patches into an MPP file, apply to the APK, and deploy.

1. Build: `./gradlew :patches:buildAndroid`
2. Apply patches to APK with morphe-cli.jar
3. Install patched APK via ADB: `adb install analysis/$ARGUMENTS/builds/$ARGUMENTS_patched.apk`

Route to the patch-deployer agent with the app name.

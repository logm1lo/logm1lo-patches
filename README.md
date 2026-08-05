# Logm1lo Patches

Custom Morphe patches for Android apps.

**Add to Morphe Manager:** [morphe-patches.software/?github=logm1lo/logm1lo-patches](https://morphe-patches.software/?github=logm1lo/logm1lo-patches)

<!-- PATCHES_START -->
> **[v1.0.8-dev.2](https://github.com/logm1lo/logm1lo-patches/releases/tag/v1.0.8-dev.2)**&nbsp;&nbsp;•&nbsp;&nbsp;`dev`&nbsp;&nbsp;•&nbsp;&nbsp;1 patches total
<details open>
<summary>📦 Calistree&nbsp;&nbsp;•&nbsp;&nbsp;1 patch</summary>
<br>

**🎯 Supported versions:**

| 5.8.5 |
| :---: |

| 💊&nbsp;Patch | 📜&nbsp;Description | ⚙️&nbsp;Options |
|----------|----------------|-----------|
| [Premium Unlock](#premium-unlock) | Unlocks all Calistree PRO features. |  |

</details>

<!-- PATCHES_END -->

## Getting Started

### Prerequisites
- JDK 21+
- Android SDK Platform Tools (adb)
- GitHub PAT with `read:packages` scope

### Setup
```bash
# Clone with dev branch
git clone -b dev git@github.com:logm1lo/logm1lo-patches.git
cd logm1lo-patches

# Configure GitHub auth for package registry
mkdir -p ~/.gradle
cat >> ~/.gradle/gradle.properties << EOF
gpr.user = YOUR_GITHUB_USERNAME
gpr.key = YOUR_GITHUB_PAT
EOF

# Download morphe-cli.jar
# From: https://github.com/MorpheApp/morphe-desktop/releases
# Place in project root as morphe-cli.jar
```

### Build Patches
```bash
./gradlew :patches:buildAndroid
```
Output: `patches/build/libs/patches-<version>.mpp`

### Decompile an APK
```bash
./scripts/decompile.sh com.example.app
```

### Build and Install Patched APK
```bash
./scripts/build-and-test.sh com.example.app both
```

## FAQ

**How do I use this?**
Install Morphe Manager, add this repo as a source, select Calistree, and apply the Premium Unlock patch.

**What version of Calistree does this work with?**
5.8.5. Other versions may work but are not tested.

**Why can't I sign in after patching?**
Re-signing the APK breaks Google Play Services auth. Sign in before patching your APK, or use email/password instead of Google sign-in.

**Can you make a patch for another app?**
Possibly. Submit a request in the [App Requests](https://github.com/logm1lo/logm1lo-patches/discussions/categories/app-requests) discussion area.

**The patch does not work.**
Make sure you are using the correct APK version. If it still fails, open an issue with details.

## License
GPL-3.0

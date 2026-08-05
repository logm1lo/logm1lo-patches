# Logm1lo Patches

Custom Morphe patches for Android apps.

<!-- PATCHES_START -->
<details open><summary><b>Available Patches</b></summary>

### Calistree (`com.calistree.calistree`)
| Patch | Description | Versions |
|-------|-------------|----------|
| Premium Unlock | Unlocks all premium features in Calistree. | 5.8.5 |

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

## License
GPL-3.0

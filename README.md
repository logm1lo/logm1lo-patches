# Logm1lo Patches

[![Release](https://github.com/logm1lo/logm1lo-patches/actions/workflows/release.yml/badge.svg)](https://github.com/logm1lo/logm1lo-patches/actions/workflows/release.yml)
[![License: GPL v3](https://img.shields.io/badge/License-GPL%20v3-yellow.svg)](https://opensource.org/licenses/GPL-3.0)

Custom Morphe patches for Android apps. **Add to Morphe Manager:** [deeplink](https://morphe-patches.software/?github=logm1lo/logm1lo-patches)

> App requests go to [Discussions](https://github.com/logm1lo/logm1lo-patches/discussions/categories/app-requests). Issues are for bugs and feature requests on existing supported apps.

<!-- PATCHES_START -->
> **[v1.2.0-dev.1](https://github.com/logm1lo/logm1lo-patches/releases/tag/v1.2.0-dev.1)**&nbsp;&nbsp;•&nbsp;&nbsp;`dev`&nbsp;&nbsp;•&nbsp;&nbsp;27 patches total

| App | Package | Versions | Patches |
|-----|---------|----------|---------|
| Calistree | `com.calistree.calistree` | 5.8.5, 5.9.1 | <ul><li>Dart Pro gates (libapp.so)</li><li>Premium Unlock</li></ul> |
| MT Manager | `bin.mt.plus` | 2.26.8 | <ul><li>Feed file browser data (extension)</li><li>Feed slide-panel file list (extension)</li><li>Fix VIP list variant</li><li>Fix account map getter</li><li>Fix file listing (direct FileSource)</li><li>Fix file-browser click (fed list)</li><li>Fix file-browser pane geometry (full height + no shift)</li><li>Fix file-browser pane visibility</li><li>Fix file-browser status counter (self-feeding)</li><li>Fix two-pane row text width (expanded rows)</li><li>Force User Agreement accepted</li><li>Force file-browser ListView to draw rows (real target)</li><li>Force file-browser ListView to layout rows</li><li>Force file-browser pane to draw rows</li><li>Login removal / bypass</li><li>Neutralize anti-tamper</li><li>Patch native keys</li><li>Reimplement native tools (extension)</li><li>Stub native methods</li><li>Unlock VIP</li><li>Unlock VIP (methods)</li><li>Unlock VIP (wrappers)</li></ul> |
| Cube Solver | `com.jeffprod.cubesolver` | 5.0.3 | <ul><li>Puzzles Unlock</li><li>Remove Ads</li></ul> |
| Locket | `com.locket.Locket` | 1.235.0 | Unlock Locket Gold |

<!-- PATCHES_END -->

## FAQ

**How do I use this?**
Install [Morphe Manager](https://github.com/MorpheApp/morphe-desktop/releases), add this repo as a source, select your app, and apply the Premium Unlock patch.

**Why can't I sign in after patching?**
Re-signing the APK breaks Google Play Services auth. Sign in before patching, or use email/password instead of Google sign-in.

**What versions are supported?**
- Calistree: 5.8.5
- Cube Solver: 5.0.3

Other versions may work but aren't tested.

**Can you make a patch for another app?**
Submit a request in the [App Requests](https://github.com/logm1lo/logm1lo-patches/discussions/categories/app-requests) discussion area.

**The patch doesn't work.**
Make sure you're using the correct APK version. If it still fails, [open an issue](https://github.com/logm1lo/logm1lo-patches/issues/new?template=bug_report.yml) with details.

## Development

**Prerequisites:** JDK 21+

```bash
git clone -b dev git@github.com:logm1lo/logm1lo-patches.git
cd logm1lo-patches

# GitHub package registry auth
echo "gpr.user = YOUR_USERNAME" >> ~/.gradle/gradle.properties
echo "gpr.key = YOUR_GITHUB_PAT" >> ~/.gradle/gradle.properties

# Build
./gradlew :patches:buildAndroid
```

Output: `patches/build/libs/patches-<version>.mpp`

## License

GPL-3.0

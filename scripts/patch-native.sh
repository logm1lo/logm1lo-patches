#!/usr/bin/env bash
set -euo pipefail
WORKDIR="$(cd "$(dirname "$0")/.." && pwd)"
BUILDDIR="$WORKDIR/analysis/zalo/builds"
SDK="$HOME/Android/Sdk/build-tools/35.0.0"
JAVA_HOME=/usr/lib/jvm/java-21-openjdk

echo "==> Step 1: Extract split APKs from bundle"
unzip -oq "$WORKDIR/apks/com.zing.zalo_26.08.01.apks" -d "$BUILDDIR/splits/"

echo "==> Step 2: Extract libnative_utils.so from arm64 split"
cd "$BUILDDIR"
unzip -oq splits/split_config.arm64_v8a.apk lib/arm64-v8a/libnative_utils.so -d "$BUILDDIR/"

echo "==> Step 3: Hex-patch libnative_utils.so"
SOFILE="$BUILDDIR/lib/arm64-v8a/libnative_utils.so"
SIZE=$(stat -c%s "$SOFILE")
echo "  File size: $SIZE bytes"

# Backup original
cp "$SOFILE" "$SOFILE.bak"

# Python script for hex patching
python3 << 'PYEOF'
import struct

so_path = "/home/logmilo/Projects/logm1lo-patches/analysis/zalo/builds/lib/arm64-v8a/libnative_utils.so"
with open(so_path, "rb") as f:
    data = bytearray(f.read())

def patch_bytes(offset, hex_bytes):
    for i, b in enumerate(hex_bytes):
        data[offset + i] = b
    print(f"  PATCHED offset 0x{offset:x}: {hex_bytes.hex()}")

# ARM64 RET = 0xD65F03C0 (little-endian in file: c0 03 5f d6)
# ARM64 MOV W0, #0 = 0x52800000 (little-endian: 00 00 80 52)
#   Actually mov x0, #0 is: 00 00 80 d2 (MOV X0, XZR in ARM64 is 4 bytes but encoded differently)
# For x0 = 0: "mov w0, wzr" = 2a 1f 03 2a (but this is 4 bytes, same as sub instruction...)
# Let's use: "mov x0, xzr" = e0 03 1f aa (4 bytes), followed by ret

RET = bytes([0xc0, 0x03, 0x5f, 0xd6])
MOV_X0_XZR = bytes([0xe0, 0x03, 0x1f, 0xaa])

# Target 1: InitializeConfig_initialize (0x288e4) — void, just RET is fine
patch_bytes(0x288e4, RET)

# Target 2: NativeCrashReporter_initSignalHandler (0x2c6c0) — void, RET
patch_bytes(0x2c6c0, RET)

# Target 3: RSAUtils_testNativeAccess (0x268fc) — returns jint, force 0
patch_bytes(0x268fc, MOV_X0_XZR + RET)

with open(so_path, "wb") as f:
    f.write(data)
print("  libnative_utils.so patched successfully")
PYEOF

echo "==> Step 4: Repack patched .so into arm64 split"
cd "$BUILDDIR"
zip -d splits/split_config.arm64_v8a.apk lib/arm64-v8a/libnative_utils.so 2>/dev/null || true
cd "$BUILDDIR"
zip -0 splits/split_config.arm64_v8a.apk lib/arm64-v8a/libnative_utils.so
"$SDK/zipalign" -f 4 splits/split_config.arm64_v8a.apk splits/split_config.arm64_v8a_aligned.apk 2>/dev/null
mv splits/split_config.arm64_v8a_aligned.apk splits/split_config.arm64_v8a.apk

echo "==> Step 5: Patch base.apk with remove file size limit"
$JAVA_HOME/bin/java -Xms1024m -jar "$WORKDIR/morphe-cli.jar" patch \
  --patches "$WORKDIR/patches/build/libs/patches-1.0.0.mpp" \
  -e "Remove File Size Limit" -e "Manifest Fix" -e "Integrity Bypass" -f \
  --out "$BUILDDIR/splits/base_patched.apk" \
  "$BUILDDIR/splits/base.apk" 2>&1 | grep -E "Applied|Error|Warn"
"$SDK/zipalign" -f 4 "$BUILDDIR/splits/base_patched.apk" "$BUILDDIR/splits/base_aligned.apk" 2>/dev/null
mv "$BUILDDIR/splits/base_aligned.apk" "$BUILDDIR/splits/base.apk"

echo "==> Step 6: Sign all split APKs with same key"
KEYSTORE="$BUILDDIR/zalo_sign.jks"
if [ ! -f "$KEYSTORE" ]; then
    keytool -genkey -v -keystore "$KEYSTORE" -alias signer \
      -keyalg RSA -keysize 2048 -validity 10000 \
      -storepass android -keypass android \
      -dname "CN=Zalo, OU=Patched, O=Logm1lo, L=Hanoi, ST=VN, C=VN" 2>&1 | tail -1
fi

for apk in "$BUILDDIR/splits"/*.apk; do
    "$SDK/apksigner" sign --ks "$KEYSTORE" \
      --ks-key-alias signer --ks-pass pass:android --key-pass pass:android "$apk" 2>/dev/null
done
echo "  All APKs signed"

echo "==> Step 7: Install split bundle"
adb install-multiple "$BUILDDIR/splits"/*.apk 2>&1

echo "==> DONE =="

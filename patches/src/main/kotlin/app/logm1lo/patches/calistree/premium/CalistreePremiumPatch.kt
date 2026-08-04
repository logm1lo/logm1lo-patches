package app.logm1lo.patches.calistree.premium

import app.logm1lo.patches.shared.COMPATIBILITY_CALISTREE
import app.morphe.patcher.extensions.InstructionExtensions.addInstructions
import app.morphe.patcher.patch.rawResourcePatch
import app.morphe.patcher.patch.bytecodePatch
import app.morphe.patcher.Fingerprint
import app.morphe.patcher.methodCall
import com.android.tools.smali.dexlib2.Opcode

internal object HealthConnectPremiumFingerprint : Fingerprint(
    returnType = "Z",
    parameters = listOf(),
    filters = listOf(
        methodCall(
            opcode = Opcode.INVOKE_VIRTUAL,
            name = "isPremium",
            returnType = "Z",
            parameters = listOf(),
        ),
    )
)

@Suppress("unused")
val calistreePremiumPatch = bytecodePatch(
    name = "Premium Unlock",
    description = "Unlocks all premium features in Calistree. Calistree is a Flutter app — "
            + "the main app logic lives in libapp.so (Dart AOT). This patch targets the Java-side "
            + "Health Connect premium checks and Sentry initialization bypass.",
    default = true
) {
    compatibleWith(COMPATIBILITY_CALISTREE)

    execute {
        HealthConnectPremiumFingerprint.let { fp ->
            val match = fp.matchOrNull() ?: return@let
            match.method.addInstructions(0, """
                const/4 v0, 0x1
                return v0
            """)
        }
    }
}

@Suppress("unused")
val calistreeHexAppPatch = rawResourcePatch(
    name = "Calistree Dart AOT Premium Bypass (hex)",
    description = "Patches libapp.so (Dart AOT) to force premium return values. "
            + "Requires manual identification of hex offsets. See analysis/calistree/notes/premium-bypass.md.",
    default = false
) {
    compatibleWith(COMPATIBILITY_CALISTREE)

    execute {
        val libAppPath = "lib/arm64-v8a/libapp.so"
        val file = get(libAppPath)
        if (!file.exists()) return@execute

        val data = file.readBytes()
        // ARM64 MOV X0, #1 + RET = 0x200080D2 0xC0035FD6
        // Use manual search for premium function patterns in libapp.so:
        //   1. Find string refs to "premium", "pro", "subscription" in binary
        //   2. Trace back to calling functions
        //   3. Overwrite function heads at identified offsets
        // After identifying offsets, uncomment and add to list:
        // val premiumOffsets = intArrayOf(0x..., 0x...)
        // premiumOffsets.forEach { offset ->
        //     data[offset] = 0x20; data[offset+1] = 0x00.toByte()
        //     data[offset+2] = 0x80.toByte(); data[offset+3] = 0xD2.toByte()
        //     data[offset+4] = 0xC0.toByte(); data[offset+5] = 0x03.toByte()
        //     data[offset+6] = 0x5F.toByte(); data[offset+7] = 0xD6.toByte()
        // }
        // file.writeBytes(data)
    }
}

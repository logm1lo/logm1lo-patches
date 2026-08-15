package app.logm1lo.patches.locket.premium

import app.logm1lo.patches.shared.COMPATIBILITY_LOCKET
import app.morphe.patcher.patch.rawResourcePatch

/**
 * Unlock Locket Gold by patching the Hermes bytecode bundle.
 *
 * Locket is a React Native (Hermes) app. The Gold gate lives in
 * `assets/index.android.bundle` (Hermes bytecode, HBC v96):
 *
 *   PaymentsProvider computes:
 *     hasGoldSubscription = !!currentUser?.subscription_entitlement
 *                          || androidGoldSubscriptionOverride()
 *
 * `androidGoldSubscriptionOverride()` reads the remote-config flag
 * `android_gold_subscription_override` — an internal dev/test override. When
 * it returns true, Gold is treated as active regardless of the backend
 * entitlement (which is set server-side only after a real Play purchase).
 *
 * Patch: rewrite function `androidGoldSubscriptionOverride` (function ID 21520,
 * bytecode offset 0x53354A, 35 bytes) so its first instruction is
 * `LoadConstTrue r0; Ret r0`, forcing it to always return true. The remaining
 * bytes are padded with valid unreachable instructions (LoadConstUndefined /
 * LoadConstUInt8) so the function body stays the same length and every
 * byte-offset in the file remains valid.
 *
 * The Hermes bytecode carries a SHA-1 footer (last 20 bytes = SHA-1 of all
 * preceding bytes). It MUST be recomputed after the edit or Hermes rejects
 * the bundle.
 *
 * This is a versioned offset patch. On a different Locket build the function
 * offsets may drift — the patch first verifies the original 35 bytes match
 * before writing, so a mismatch is reported instead of corrupting the bundle.
 */
@Suppress("unused")
val locketGoldUnlockPatch = rawResourcePatch(
    name = "Unlock Locket Gold",
    description = "Unlocks Locket Gold by forcing the internal android_gold_subscription_override flag to true.",
    default = true
) {
    compatibleWith(COMPATIBILITY_LOCKET)

    execute {
        val bundle = get("assets/index.android.bundle") ?: return@execute
        val bytes = bundle.readBytes()

        // Hermes HBC v96 function `androidGoldSubscriptionOverride` (21520).
        // Instruction stream offset + original 35-byte body (verified below).
        val funcOffset = 0x5336EA // 5453546
        val expected = hexToBytes(
            "2900002e0000023601000172760051020100370102025d9c73000fdb53000102005c00"
        )

        if (funcOffset + expected.size > bytes.size) {
            println("Locket Gold: bundle too small — not patched")
            return@execute
        }

        val actual = bytes.copyOfRange(funcOffset, funcOffset + expected.size)
        if (!actual.contentEquals(expected)) {
            println(
                "Locket Gold: function bytes at 0x%X do not match expected — bundle may differ from 1.235.0".format(
                    funcOffset
                )
            )
            return@execute
        }

        // LoadConstTrue r0 (78 00) ; Ret r0 (5c 00)
        // then 14 x LoadConstUndefined r0 (76 00) + LoadConstUInt8 r0,1 (6e 00 01)
        // = 4 + 28 + 3 = 35 bytes, matching the original function size.
        val replacement = hexToBytes(
            "78005c00760076007600760076007600760076007600760076007600760076006e0001"
        )
        replacement.copyInto(bytes, funcOffset)

        // Recompute the Hermes SHA-1 footer (last 20 bytes).
        val sha1 = java.security.MessageDigest.getInstance("SHA-1")
        val digest = sha1.digest(bytes.copyOfRange(0, bytes.size - 20))
        digest.copyInto(bytes, bytes.size - 20)

        bundle.writeBytes(bytes)
        println("Locket Gold: patched androidGoldSubscriptionOverride -> true (SHA-1 footer updated)")
    }
}

private fun hexToBytes(hex: String): ByteArray {
    require(hex.length % 2 == 0) { "hex string must have even length" }
    return ByteArray(hex.length / 2) { i ->
        ((Character.digit(hex[i * 2], 16) shl 4) + Character.digit(hex[i * 2 + 1], 16)).toByte()
    }
}

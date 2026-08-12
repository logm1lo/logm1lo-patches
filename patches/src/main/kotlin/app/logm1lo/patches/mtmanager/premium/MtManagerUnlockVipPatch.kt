package app.logm1lo.patches.mtmanager.premium

import app.logm1lo.patches.shared.COMPATIBILITY_MTMANAGER
import app.morphe.patcher.extensions.InstructionExtensions.replaceInstruction
import app.morphe.patcher.patch.bytecodePatch
import com.android.tools.smali.dexlib2.Opcode
import com.android.tools.smali.dexlib2.iface.instruction.OneRegisterInstruction

/**
 * Unlock VIP: forces all six VIP feature getters on `l.ۨ᩸ܰ` to return true
 * at every call site, so the caller sees "VIP granted" regardless of native
 * login/VIP state.
 *
 * Getters (and what they unlock):
 * - ۘ()Z — MT_Protector, FTP menu, theme/settings
 * - ܺ()Z — FTP/SFTP/SMB/WebDav (shows "(VIP)" when false)
 * - ۠()Z — UI spans / theme features
 * - ᩶()Z — misc VIP gates
 * - ܰ()Z — misc VIP gates
 * - ᩷()Z — misc VIP gates
 *
 * Pattern at each call site:
 *     invoke-static {}, Ll/ۨ᩸ܰ;->X()Z
 *     move-result vN
 *
 * We replace `move-result vN` with `const/16 vN, 0x1` — the native invoke
 * still executes (harmless getter, returns false), but the result register
 * is forced to 1. The branch/conditional after it then sees "true" and
 * proceeds as if VIP were active.
 *
 * DEPRECATED / SUPERSEDED: the method-level `Unlock VIP (methods)` patch
 * (mtmanagerUnlockVipMethodsPatch) now replaces ALL SEVEN getter method
 * bodies directly, which is cleaner and covers every call site uniformly.
 * This call-site patch is kept default=false to avoid double-patching the
 * same move-result instructions.
 *
 * Does NOT affect the login check (see Login Removal / Bypass patch for that).
 */
@Suppress("unused")
val mtmanagerUnlockVipPatch = bytecodePatch(
    name = "Unlock VIP",
    description = "Unlocks VIP features (FTP/SFTP/SMB/WebDav, MT_Protector, themes).",
    default = false
) {
    compatibleWith(COMPATIBILITY_MTMANAGER)

    execute {
        var total = 0
        val fingerprints = listOf(
            VipGetter1Fingerprint, // ۘ()Z – MT_Protector, FTP menu (18 callers)
            VipGetter2Fingerprint, // ܺ()Z – FTP/SFTP/SMB/WebDav (23 callers)
            VipGetter3Fingerprint, // ۠()Z – UI spans / themes (6 callers)
            VipGetter4Fingerprint, // ᩶()Z – misc (3 callers)
            VipGetter5Fingerprint, // ܰ()Z – misc (3 callers)
            VipGetter6Fingerprint, // ᩷()Z – misc (2 callers)
        )
        for (fp in fingerprints) {
            val matches = fp.matchAllOrNull() ?: continue
            for (match in matches) {
                val method = match.method
                val invokeIdx = match.instructionMatches[0].index
                val impl = method.implementation ?: continue
                val nextIdx = invokeIdx + 1
                if (nextIdx >= impl.instructions.size) continue
                val next = impl.instructions[nextIdx]
                if (next.opcode != Opcode.MOVE_RESULT) continue
                val reg = (next as OneRegisterInstruction).registerA
                method.replaceInstruction(nextIdx, "const/16 v$reg, 0x1")
                total++
            }
        }
        println("MT Manager Unlock VIP: forced $total native VIP getter call sites to true")
    }
}

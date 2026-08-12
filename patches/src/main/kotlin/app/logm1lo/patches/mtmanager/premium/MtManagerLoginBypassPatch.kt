package app.logm1lo.patches.mtmanager.premium

import app.logm1lo.patches.shared.COMPATIBILITY_MTMANAGER
import app.morphe.patcher.extensions.InstructionExtensions.replaceInstruction
import app.morphe.patcher.patch.bytecodePatch
import com.android.tools.smali.dexlib2.Opcode
import com.android.tools.smali.dexlib2.iface.instruction.OneRegisterInstruction

/**
 * Login removal / bypass: forces the native login-confirmation getter
 * `l.ۨ᩸ܰ->֡()Z` to return true at its single call site. The app stores
 * login credentials via the native setter `ۨ᩸ܰ->۟(Ljava/lang/String;)V`,
 * then immediately checks `֡()Z` to confirm the login state was set.
 * Forcing the result register to 1 makes the app believe it is logged in
 * even without an MT account.
 *
 * Does NOT affect any VIP feature gates. Use alongside "Unlock VIP" if
 * you want both login bypass AND VIP features.
 */
@Suppress("unused")
val mtmanagerLoginBypassPatch = bytecodePatch(
    name = "Login removal / bypass",
    description = "Makes the app think you are logged into an MT account.",
    default = true
) {
    compatibleWith(COMPATIBILITY_MTMANAGER)

    execute {
        var total = 0
        val match = LoginConfirmedFingerprint.matchOrNull()
        if (match != null) {
            val method = match.method
            val invokeIdx = match.instructionMatches[0].index
            val impl = method.implementation ?: return@execute
            val nextIdx = invokeIdx + 1
            if (nextIdx < impl.instructions.size) {
                val next = impl.instructions[nextIdx]
                if (next.opcode == Opcode.MOVE_RESULT) {
                    val reg = (next as OneRegisterInstruction).registerA
                    method.replaceInstruction(nextIdx, "const/16 v$reg, 0x1")
                    total++
                }
            }
        }
        println("MT Manager Login bypass: forced $total login-confirm getter call site(s) to true")
    }
}

package app.logm1lo.patches.mtmanager.premium

import app.logm1lo.patches.shared.COMPATIBILITY_MTMANAGER
import app.morphe.patcher.extensions.InstructionExtensions.addInstructionsWithLabels
import app.morphe.patcher.patch.bytecodePatch

/**
 * Force the EULA/User-Agreement flag to always read as "agreed".
 *
 * MT Manager stores the agreement state as a boolean config key
 * "user_agreement" in the encrypted settings store `l.۬۟᩶`
 * (getBoolean(key, def)). On a native-stubbed build the write is
 * unreliable and the flag sometimes reads back false -> the User
 * Agreement dialog re-shows on activity restarts (QA finding 2026-08-12).
 *
 * Fix: in `l.۬۟᩶.getBoolean(String, boolean)`, if the key equals
 * "user_agreement", return true regardless of the stored value.
 * Detected at runtime via Frida: after "READ AND AGREE" the app writes
 * `getBoolean "user_agreement" def=false -> true`.
 */
@Suppress("unused")
val mtmanagerForceAgreementPatch = bytecodePatch(
    name = "Force User Agreement accepted",
    description = "Makes l.۬۟᩶.getBoolean return true for the user_agreement key so the EULA never re-shows.",
    default = true
) {
    compatibleWith(COMPATIBILITY_MTMANAGER)

    execute {
        val cfgClass = mutableClassDefByOrNull("Ll/\u06ec\u06df\u1a76;") ?: return@execute // l.۬۟᩶
        val method = cfgClass.methods.firstOrNull {
            it.name == "getBoolean" &&
                it.parameterTypes.size == 2 &&
                it.parameterTypes[0].toString() == "Ljava/lang/String;" &&
                it.parameterTypes[1].toString() == "Z"
        } ?: return@execute

        // Non-static: p0 = this, p1 = key, p2 = default. v0 is scratch.
        method.addInstructionsWithLabels(
            0,
            """
                const-string v0, "user_agreement"
                invoke-virtual {p1, v0}, Ljava/lang/String;->equals(Ljava/lang/Object;)Z
                move-result v0
                if-eqz v0, :not_agreement
                const/4 v0, 0x1
                return v0
                :not_agreement
                nop
            """
        )
        println("MT Manager: l.۬۟᩶.getBoolean now forces user_agreement=true")
    }
}

package app.logm1lo.patches.mtmanager.premium

import app.logm1lo.patches.shared.COMPATIBILITY_MTMANAGER
import app.morphe.patcher.extensions.InstructionExtensions.addInstruction
import app.morphe.patcher.patch.bytecodePatch

/**
 * DIAGNOSTIC ONLY: log when the flat-browser row binder l/᩻ۚܳ.۟(row) runs.
 * Confirms whether the visible ListView uses the l/᩵ۚܳ -> l/᩻ۚܳ row path.
 */
@SuppressWarnings("unused")
val mtmanagerRowBinderDiagPatch = bytecodePatch(
    name = "Row binder diagnostic (log)",
    default = true
) {
    compatibleWith(COMPATIBILITY_MTMANAGER)

    execute {
        val cls = mutableClassDefByOrNull("Ll/\u1a7b\u06da\u0733;") ?: return@execute // l/᩻ۚܳ
        val binder = cls.methods.firstOrNull {
            it.name == "\u06df" && // ۟
                it.parameterTypes.size == 4 && // (adapter, int, View, ViewGroup)
                it.returnType.startsWith("Ll/")
        } ?: return@execute
        // Inject a log at index 0 using free locals. .registers 15 -> p0..p3 high,
        // v0..v? free.
        binder.addInstruction(0, "const-string v0, \"MtTools\"")
        binder.addInstruction(1, "const-string v1, \"MT-DEBUG: row binder l/᩻ۚܳ.۟() called pos=\"")
        binder.addInstruction(2, "invoke-static {p1}, Ljava/lang/String;->valueOf(I)Ljava/lang/String;")
        binder.addInstruction(3, "move-result-object v2")
        binder.addInstruction(4, "invoke-virtual {v1, v2}, Ljava/lang/String;->concat(Ljava/lang/String;)Ljava/lang/String;")
        binder.addInstruction(5, "move-result-object v2")
        binder.addInstruction(6, "invoke-static {v0, v2}, Landroid/util/Log;->i(Ljava/lang/String;Ljava/lang/String;)I")
        println("MT Manager: DIAG row binder log injected")
    }
}

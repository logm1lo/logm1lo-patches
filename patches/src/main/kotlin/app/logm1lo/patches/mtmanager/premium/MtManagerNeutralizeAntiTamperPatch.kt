package app.logm1lo.patches.mtmanager.premium

import app.logm1lo.patches.shared.COMPATIBILITY_MTMANAGER
import app.morphe.patcher.patch.bytecodePatch
import com.android.tools.smali.dexlib2.Opcode

/**
 * Neutralize the app's self-verification (自校验失败) anti-tamper dialog.
 *
 * `bin.mt.plus.Main` throws RuntimeException("自校验失败" / "self-verification
 * failed") in <init> and onCreate when it detects a re-signed APK. Each block
 * is `new-instance vX, Ljava/lang/RuntimeException;` ... `throw vX`. We replace
 * the whole block with `return-void` so the app continues without the dialog.
 *
 * Proven fix (mtmanager_2.26.8_vip_unlocked.apk, PatchAntitamper.java).
 */
@Suppress("unused")
val mtmanagerNeutralizeAntiTamperPatch = bytecodePatch(
    name = "Neutralize anti-tamper",
    description = "Removes the RuntimeException self-verification throws in Main.<init>/onCreate.",
    default = true
) {
    compatibleWith(COMPATIBILITY_MTMANAGER)

    execute {
        var patched = 0
        val main = mutableClassDefByOrNull("Lbin/mt/plus/Main;") ?: return@execute
        val methods = main.methods.toList()
        for (method in methods) {
            if (method.name != "<init>" && method.name != "onCreate") continue
            val impl = method.implementation ?: continue
            var done = false
            while (!done) {
                done = true
                val insns = impl.instructions
                for (i in 0 until insns.size) {
                    val insn = insns[i]
                    if (insn.opcode != Opcode.NEW_INSTANCE) continue
                    val ni = insn as? com.android.tools.smali.dexlib2.builder.instruction.BuilderInstruction21c ?: continue
                    if (ni.reference.toString() != "Ljava/lang/RuntimeException;") continue
                    var throwIdx = -1
                    for (j in i until insns.size.coerceAtMost(i + 6)) {
                        if (insns[j].opcode == Opcode.THROW) { throwIdx = j; break }
                    }
                    if (throwIdx == -1) continue
                    for (j in throwIdx downTo i) {
                        impl.removeInstruction(j)
                    }
                    impl.addInstruction(i, com.android.tools.smali.dexlib2.builder.instruction.BuilderInstruction10x(Opcode.RETURN_VOID))
                    patched++
                    done = false
                    break
                }
            }
        }
        println("MT Manager: neutralized $patched anti-tamper throw block(s)")
    }
}

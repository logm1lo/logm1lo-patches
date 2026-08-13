package app.logm1lo.patches.mtmanager.premium

import app.logm1lo.patches.shared.COMPATIBILITY_MTMANAGER
import app.morphe.patcher.extensions.InstructionExtensions.addInstruction
import app.morphe.patcher.extensions.InstructionExtensions.replaceInstruction
import app.morphe.patcher.patch.bytecodePatch
import com.android.tools.smali.dexlib2.Opcode

/**
 * Fix the two-pane file browser's invisible row text (white page).
 *
 * ROOT CAUSE: the file browser rows are `l.۠᩺ܳ` (a custom ViewGroup). Its
 * `onMeasure`/`onLayout` branch on `ۧ۟`:
 *
 *   - `ۧ۟ == false` -> COMPLEX branch: measures file_name + sub-text with
 *     real width -> text renders.
 *   - `ۧ۟ == true`  -> SIMPLE branch: measures only the container `ܶ۟` and
 *     never measures file_name -> it stays 0-width (invisible).
 *
 * The constructors set `ۧ۟ = false` (complex, correct). But the adapter calls
 * `l.۠᩺ܳ.۟(Z)V` with `true` during binding, flipping `ۧ۟` to `true` -> the
 * simple branch runs at measure time -> file_name is never measured -> 0
 * width -> white page.
 *
 * FIX: in `l.۠᩺ܳ.۟(Z)V`, force `ۧ۟ = false` (write a const 0 via v0) so the
 * complex measure branch always runs and file_name/sub-text get real width.
 * The sub-view show/hide logic (which depends on the p1 parameter) is kept.
 */
@Suppress("unused")
val mtmanagerFixTwoPaneTextWidthPatch = bytecodePatch(
    name = "Fix two-pane row text width (expanded rows)",
    description = "Forces l.۠᩺ܳ rows to stay in the complex measure branch so file names and sub-text render instead of being 0-width.",
    default = true
) {
    compatibleWith(COMPATIBILITY_MTMANAGER)

    execute {
        val cls = mutableClassDefByOrNull("Ll/\u06e0\u1a7a\u0733;") ?: return@execute // l.۠᩺ܳ

        val toggle = cls.methods.firstOrNull {
            it.name == "\u06df" && // ۟
                it.parameterTypes.size == 1 &&
                it.parameterTypes[0] == "Z" &&
                it.returnType == "V"
        }
        if (toggle != null) {
            val impl = toggle.implementation
            if (impl != null) {
                val iputIdx = impl.instructions.indexOfFirst {
                    it.opcode == Opcode.IPUT_BOOLEAN
                }
                if (iputIdx >= 0) {
                    // Replace `iput-boolean p1, p0, ->ۧ۟:Z` with
                    // `const/4 v0, 0x0` + `iput-boolean v0, p0, ->ۧ۟:Z`
                    // so ۧ۟ is always false (complex measure branch).
                    toggle.addInstruction(iputIdx, "const/4 v0, 0x0")
                    toggle.replaceInstruction(iputIdx + 1, "iput-boolean v0, p0, Ll/\u06e0\u1a7a\u0733;->\u06e7\u06df:Z")
                    println("MT Manager: l.۠᩺ܳ.۟(Z)V forces ۧ۟=false (complex branch) at index $iputIdx")
                }
            }
        }
    }
}

package app.logm1lo.patches.mtmanager.premium

import app.logm1lo.patches.shared.COMPATIBILITY_MTMANAGER
import app.morphe.patcher.extensions.InstructionExtensions.replaceInstruction
import app.morphe.patcher.patch.bytecodePatch
import com.android.tools.smali.dexlib2.Opcode
import com.android.tools.smali.dexlib2.builder.instruction.BuilderInstruction11n

/**
 * Fix the file-browser render wall: force pane children VISIBLE.
 *
 * ROOT CAUSE (discovered 2026-08-13, N-41): `l.ۗ᩺ܰ.addView(View, int, LayoutParams)`
 * hides children with ids 0x7f0a008d / 0x7f0a008e by setting them INVISIBLE (4)
 * whenever the slide-offset field `l.ۗ᩺ܰ.֡۟:F` == 0.0:
 *
 *   iget v0, p0, Ll/ۗ᩺ܰ;->֡۟:F     // slide offset
 *   cmpg-float v0, v0, v3          // compare to 0
 *   if-nez v0, :cond_23            // offset != 0 -> keep visible
 *   const/4 v2, 0x4                // offset == 0 -> INVISIBLE
 *   setVisibility(v2)
 *
 * In the native-keyed build the slide offset stays 0.0 (the panel-open code
 * path that would set a non-zero ratio never runs), so every added child
 * (the `l/֨ۛ` pane containers) is INVISIBLE -> the file browser renders
 * "Folders: 0" even though rows are bound (N-38: 3 rows, adapter count 3,
 * text set, but isShown()=false).
 *
 * FIX: in addView, always pass VISIBLE (0) as the visibility value instead
 * of INVISIBLE (4), for both branches (ids 0x7f0a008d and 0x7f0a008e).
 * Verified via Frida: forcing the two ancestor containers to VISIBLE +
 * invalidate makes the rows render with text.
 */
@Suppress("unused")
val mtmanagerFixPaneVisibilityPatch = bytecodePatch(
    name = "Fix file-browser pane visibility",
    description = "Forces l.ۗ᩺ܰ.addView to keep pane children VISIBLE so the file list renders.",
    default = true
) {
    compatibleWith(COMPATIBILITY_MTMANAGER)

    execute {
        val cls = mutableClassDefByOrNull("Ll/\u06d7\u1a7a\u0730;") ?: return@execute // l.ۗ᩺ܰ
        val addView = cls.methods.firstOrNull {
            it.name == "addView" &&
                it.parameterTypes.size == 3 &&
                it.parameterTypes[0].toString() == "Landroid/view/View;" &&
                it.parameterTypes[1].toString() == "I" &&
                it.parameterTypes[2].toString() == "Landroid/view/ViewGroup\$LayoutParams;"
        } ?: return@execute

        val impl = addView.implementation ?: return@execute
        // Replace BOTH "const/4 v2, 0x4" with "const/4 v2, 0x0" in addView.
        // Work from the end backwards so indices stay valid.
        val indices = mutableListOf<Int>()
        impl.instructions.forEachIndexed { i, insn ->
            if (insn.opcode == Opcode.CONST_4) {
                val inst = insn as? BuilderInstruction11n
                if (inst != null && inst.registerA == 2 && inst.narrowLiteral == 4) {
                    indices.add(i)
                }
            }
        }
        if (indices.isEmpty()) return@execute
        indices.reversed().forEach { i ->
            addView.replaceInstruction(i, "const/4 v2, 0x0")
        }
        println("MT Manager: l.ۗ᩺ܰ.addView patched " + indices.size + " INVISIBLE->VISIBLE")
    }
}

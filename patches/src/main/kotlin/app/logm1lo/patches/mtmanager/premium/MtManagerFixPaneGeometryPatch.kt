package app.logm1lo.patches.mtmanager.premium

import app.logm1lo.patches.shared.COMPATIBILITY_MTMANAGER
import app.morphe.patcher.extensions.InstructionExtensions.replaceInstruction
import app.morphe.patcher.patch.bytecodePatch
import com.android.tools.smali.dexlib2.AccessFlags
import com.android.tools.smali.dexlib2.Opcode
import com.android.tools.smali.dexlib2.builder.instruction.BuilderInstruction11n
import com.android.tools.smali.dexlib2.builder.instruction.BuilderInstruction22c
import com.android.tools.smali.dexlib2.builder.instruction.BuilderInstruction35c
import com.android.tools.smali.dexlib2.iface.reference.FieldReference
import com.android.tools.smali.dexlib2.iface.reference.MethodReference

/**
 * Fix the file-browser pane geometry so ALL rows render + scroll.
 *
 * ROOT CAUSE (N-41 follow-up): the slide-panel container `l.ۗ᩺ܰ`
 * (FrameLayout) caps the pane height and shifts it down when the slide
 * offset is 0 (the native-keyed build never opens the panel):
 *
 *   onMeasure(II)V  -> pane child (id 0x7f0a008d) measured to
 *                      `availableHeight * ܶ۟:F` where `ܶ۟:F` defaults to
 *                      0.5f (bottom_content_view_ratio) => only ~2 rows fit.
 *   onLayout(ZIIII)V -> `translationY = (1.0 - offset) * (childH + pads)`;
 *                      with offset==0.0 the pane is pushed DOWN by its full
 *                      height, leaving only the top rows peeking into view.
 *
 * FIX (two 1-for-1 instruction replacements, no register changes):
 *   1. onMeasure:  iget v2, p0, ->ܶ۟:F   ->  const/high16 v2, 0x3f800000
 *      (use ratio 1.0 so the pane fills the full available height)
 *   2. onLayout:   sub-float/2addr p3, p4 -> const/4 p3, 0x0
 *      (translationY becomes 0 * p2 = 0; the pane stays at natural position)
 */
@Suppress("unused")
val mtmanagerFixPaneGeometryPatch = bytecodePatch(
    name = "Fix file-browser pane geometry (full height + no shift)",
    description = "Makes l.ۗ᩺ܰ measure the pane at full available height and never translate it down, so all file rows render and scroll.",
    default = true
) {
    compatibleWith(COMPATIBILITY_MTMANAGER)

    execute {
        val cls = mutableClassDefByOrNull("Ll/\u06d7\u1a7a\u0730;") ?: return@execute // l.ۗ᩺ܰ
        var patched = 0

        // ---- 1. onMeasure(II)V : force full height ratio ----
        val onMeasure = cls.methods.firstOrNull {
            it.name == "onMeasure" &&
                it.parameterTypes.size == 2 &&
                it.returnType == "V"
        }
        if (onMeasure != null) {
            val impl = onMeasure.implementation
            if (impl != null) {
                // find `iget v2, p0, Ll/ۗ᩺ܰ;->ܶ۟:F` (field read of the ratio)
                val idx = impl.instructions.indexOfFirst { insn ->
                    if (insn.opcode != Opcode.IGET) return@indexOfFirst false
                    val inst = insn as? BuilderInstruction22c ?: return@indexOfFirst false
                    val ref = inst.reference as? FieldReference ?: return@indexOfFirst false
                    ref.name == "\u0736\u06df" && ref.type == "F" // ܶ۟
                }
                if (idx >= 0) {
                    onMeasure.replaceInstruction(idx, "const/high16 v2, 0x3f800000")
                    println("MT Manager: l.ۗ᩺ܰ.onMeasure full-height ratio applied at index $idx")
                    patched++
                }
            }
        }

        // ---- 2. onLayout(ZIIII)V : force translationY = 0 ----
        val onLayout = cls.methods.firstOrNull {
            it.name == "onLayout" &&
                it.parameterTypes.size == 5 &&
                it.returnType == "V"
        }
        if (onLayout != null) {
            val impl = onLayout.implementation
            if (impl != null) {
                // find the slide-offset read `iget p4, p0, ->֡۟:F`
                val igetIdx = impl.instructions.indexOfFirst { insn ->
                    if (insn.opcode != Opcode.IGET) return@indexOfFirst false
                    val inst = insn as? BuilderInstruction22c ?: return@indexOfFirst false
                    val ref = inst.reference as? FieldReference ?: return@indexOfFirst false
                    ref.name == "\u05a1\u06df" && ref.type == "F" // ֡۟
                }
                // the next instruction is `sub-float/2addr p3, p4`
                if (igetIdx >= 0 && igetIdx + 1 < impl.instructions.size) {
                    val sub = impl.instructions[igetIdx + 1]
                    if (sub.opcode == Opcode.SUB_FLOAT_2ADDR) {
                        onLayout.replaceInstruction(igetIdx + 1, "const/4 p3, 0x0")
                        println("MT Manager: l.ۗ᩺ܰ.onLayout translation zeroed at index ${igetIdx + 1}")
                        patched++
                    }
                }
            }
        }

        // ---- 3. l.ۙ᩸ܳ.<init> : don't hide l.۟ۨܰ (file list) when offset==0 ----
        // The 3-arg <init> reads the slide-panel offset (l.ۗ᩺ܰ.۬()F); when it
        // returns 0 the code does `const/4 v9, 0x4` + setVisibility(v1) which
        // marks the file-list container (id 0x7f0a0636) INVISIBLE. Replace the
        // hide constant 4 (INVISIBLE) with 0 (VISIBLE).
        val browserCls = mutableClassDefByOrNull("Ll/\u06d9\u1a78\u0733;") ?: return@execute // l.ۙ᩸ܳ
        val init = browserCls.methods.firstOrNull {
            it.name == "<init>" &&
                it.parameterTypes.size == 3 &&
                it.returnType == "V"
        }
        if (init != null) {
            val impl = init.implementation
            if (impl != null) {
                val idx = impl.instructions.indexOfFirst { insn ->
                    if (insn.opcode != Opcode.CONST_4) return@indexOfFirst false
                    val inst = insn as? BuilderInstruction11n ?: return@indexOfFirst false
                    inst.narrowLiteral == 4
                }
                if (idx >= 0 && idx + 1 < impl.instructions.size) {
                    val next = impl.instructions[idx + 1]
                    val isHide = next.opcode == Opcode.INVOKE_VIRTUAL &&
                        (next as? BuilderInstruction35c)?.reference?.let {
                            (it as? MethodReference)?.name == "setVisibility"
                        } == true
                    if (isHide) {
                        val reg = (impl.instructions[idx] as BuilderInstruction11n).registerA
                        init.replaceInstruction(idx, "const/4 v$reg, 0x0")
                        println("MT Manager: l.ۙ᩸ܳ.<init> file-list hide (offset==0) neutralized at index $idx")
                        patched++
                    }
                }
            }
        }

        // ---- 4. l.ۙ᩸ܳ.۟(Ll/ۙ᩸ܳ;F)V : keep file list visible in offset callback ----
        // The panel-offset callback (registered via l.ۘۛܳ) also hides the file
        // list when the offset parameter is 0: `const/4 p1, 0x4` + setVisibility.
        // Force it to VISIBLE (0) instead.
        val offsetCb = browserCls.methods.firstOrNull {
            it.name == "\u06df" && // ۟
                it.accessFlags and AccessFlags.STATIC.value != 0 &&
                it.parameterTypes.size == 2 &&
                it.parameterTypes[0] == "Ll/\u06d9\u1a78\u0733;" &&
                it.parameterTypes[1] == "F" &&
                it.returnType == "V"
        }
        if (offsetCb != null) {
            val impl = offsetCb.implementation
            if (impl != null) {
                val idx = impl.instructions.indexOfFirst { insn ->
                    if (insn.opcode != Opcode.CONST_4) return@indexOfFirst false
                    val inst = insn as? BuilderInstruction11n ?: return@indexOfFirst false
                    inst.narrowLiteral == 4
                }
                if (idx >= 0 && idx + 1 < impl.instructions.size) {
                    val next = impl.instructions[idx + 1]
                    val isHide = next.opcode == Opcode.GOTO
                    if (isHide) {
                        val reg = (impl.instructions[idx] as BuilderInstruction11n).registerA
                        offsetCb.replaceInstruction(idx, "const/4 v$reg, 0x0")
                        println("MT Manager: l.ۙ᩸ܳ.offset-callback hide neutralized at index $idx")
                        patched++
                    }
                }
            }
        }

        println("MT Manager: pane geometry patched $patched/4 (onMeasure + onLayout + init-hide + callback-hide)")
    }
}

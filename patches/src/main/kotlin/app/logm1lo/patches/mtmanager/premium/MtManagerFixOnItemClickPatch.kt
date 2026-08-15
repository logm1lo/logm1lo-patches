package app.logm1lo.patches.mtmanager.premium

import app.logm1lo.patches.shared.COMPATIBILITY_MTMANAGER
import app.morphe.patcher.extensions.InstructionExtensions.addInstruction
import app.morphe.patcher.extensions.InstructionExtensions.replaceInstruction
import app.morphe.patcher.patch.bytecodePatch

/**
 * Fix the file-browser onItemClick IndexOutOfBounds crash.
 *
 * The adapter shows the FED list (l/ۤۚܳ.᩷(), ~370 items via feedStatusItems
 * cache) but the click handler reads the STALE warm-state list this.֡۟
 * (42 native "Android" placeholders). Clicking a row beyond 42 ->
 * IndexOutOfBoundsException at l/ۤۚܳ.onItemClick -> l/ۗۡ᩷.get.
 *
 * FIX: in l/ۤۚܳ.onItemClick, read the item list via this.᩷() (the fed list)
 * instead of this.֡۟ (stale). Replace:
 *   iget-object p1, p0, Ll/ۤۚܳ;->֡۟:Ljava/util/List;
 * with:
 *   invoke-virtual {p0}, Ll/ۤۚܳ;->᩷()Ljava/util/List;
 *   move-result-object p1
 */
@SuppressWarnings("unused")
val mtmanagerFixOnItemClickPatch = bytecodePatch(
    name = "Fix file-browser click (fed list)",
    description = "Makes the file-browser onItemClick read the fed file list (l/ۤۚܳ.᩷()) instead of the stale warm-state list, fixing IndexOutOfBounds on row clicks.",
    default = true
) {
    compatibleWith(COMPATIBILITY_MTMANAGER)

    execute {
        val cls = mutableClassDefByOrNull("Ll/\u06e4\u06da\u0733;") ?: return@execute // l.ۤۚܳ
        val onClick = cls.methods.firstOrNull {
            it.name == "onItemClick" && it.parameterTypes.size == 4
        } ?: return@execute
        val impl = onClick.implementation ?: return@execute

        // Find the iget-object p1, p0, ->֡۟:Ljava/util/List; instruction
        val targetOpcode = com.android.tools.smali.dexlib2.Opcode.IGET_OBJECT
        var found = -1
        for (i in 0 until impl.instructions.size) {
            val insn = impl.instructions[i]
            if (insn.opcode != targetOpcode) continue
            val inst = insn as? com.android.tools.smali.dexlib2.builder.instruction.BuilderInstruction22c ?: continue
            val ref = inst.reference as? com.android.tools.smali.dexlib2.iface.reference.FieldReference ?: continue
            if (ref.name == "\u05a1\u06df" && ref.type == "Ljava/util/List;") { // ֡۟
                found = i
                break
            }
        }
        if (found < 0) {
            println("MT Manager: onItemClick ֡۟ read not found (may already be patched)")
            return@execute
        }

        // Replace the iget-object with a call to this.᩷()Ljava/util/List; + move-result
        onClick.replaceInstruction(found, "invoke-virtual {p0}, Ll/\u06e4\u06da\u0733;->\u1a77()Ljava/util/List;")
        // Insert move-result-object p1 right after
        onClick.addInstruction(found + 1, "move-result-object p1")
        println("MT Manager: onItemClick now reads fed list (l/ۤۚܳ.᩷())")
    }
}

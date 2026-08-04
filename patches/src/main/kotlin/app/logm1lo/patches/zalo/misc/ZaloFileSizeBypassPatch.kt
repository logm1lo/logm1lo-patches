package app.logm1lo.patches.zalo.misc

import app.logm1lo.patches.shared.COMPATIBILITY_ZALO
import app.morphe.patcher.extensions.InstructionExtensions.replaceInstruction
import app.morphe.patcher.patch.bytecodePatch
import com.android.tools.smali.dexlib2.Opcode
import com.android.tools.smali.dexlib2.iface.instruction.TwoRegisterInstruction

@Suppress("unused")
val zaloFileSizeBypassPatch = bytecodePatch(
    name = "Remove File Size Limit",
    description = "Removes the maximum file size restriction when sending files in Zalo. "
            + "Allows sending files of any size by bypassing the client-side File.length() check.",
    default = true
) {
    compatibleWith(COMPATIBILITY_ZALO)

    execute {
        setOf(FileSizeCheckFingerprint, FileSizeCheckFingerprint2).forEach { fp ->
            val match = fp.matchOrNull() ?: return@forEach
            // The cmp-long is at match index 2 (0-indexed)
            // Replace cmp-long v0, v0, v2 with const/4 v0, 0x0
            // This forces the comparison result to always be "equal"
            // so if-gtz never jumps to the FileTooLargeException path
            val cmpIdx = fp.instructionMatches[2].index
            match.method.replaceInstruction(cmpIdx, "const/4 v0, 0x0")
        }
    }
}

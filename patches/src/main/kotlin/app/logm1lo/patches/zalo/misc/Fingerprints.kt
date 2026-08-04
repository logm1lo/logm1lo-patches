package app.logm1lo.patches.zalo.misc

import app.morphe.patcher.Fingerprint
import app.morphe.patcher.InstructionLocation.MatchAfterWithin
import app.morphe.patcher.methodCall
import app.morphe.patcher.opcode
import app.morphe.patcher.string
import com.android.tools.smali.dexlib2.AccessFlags
import com.android.tools.smali.dexlib2.Opcode

internal object FileSizeCheckFingerprint : Fingerprint(
    definingClass = "Lm00/h;",
    accessFlags = listOf(AccessFlags.PUBLIC, AccessFlags.FINAL),
    returnType = "Ljava/lang/Object;",
    parameters = listOf("Ljava/lang/Object;"),
    filters = listOf(
        methodCall(
            opcode = Opcode.INVOKE_VIRTUAL,
            definingClass = "Ljava/io/File;",
            name = "length",
            returnType = "J",
            parameters = listOf(),
        ),
        opcode(Opcode.IGET_WIDE, MatchAfterWithin(15)),
        opcode(Opcode.CMP_LONG, MatchAfterWithin(5)),
        opcode(Opcode.IF_GTZ, MatchAfterWithin(3)),
        string("bytes) exceeds maximum allowed size of "),
    )
)

internal object FileSizeCheckFingerprint2 : Fingerprint(
    definingClass = "Lm00/e;",
    accessFlags = listOf(AccessFlags.PUBLIC, AccessFlags.FINAL),
    returnType = "Ljava/lang/Object;",
    parameters = listOf("Ljava/lang/Object;"),
    filters = listOf(
        string("bytes) exceeds maximum allowed size of "),
        methodCall(
            opcode = Opcode.INVOKE_VIRTUAL,
            definingClass = "Ljava/io/File;",
            name = "length",
            returnType = "J",
            parameters = listOf(),
        ),
        opcode(Opcode.IGET_WIDE, MatchAfterWithin(30)),
        opcode(Opcode.CMP_LONG, MatchAfterWithin(10)),
        opcode(Opcode.IF_GTZ, MatchAfterWithin(5)),
    )
)

package app.logm1lo.patches.mtmanager.premium

import app.logm1lo.patches.shared.COMPATIBILITY_MTMANAGER
import app.morphe.patcher.patch.bytecodePatch
import app.morphe.patcher.util.proxy.mutableTypes.MutableMethod
import com.android.tools.smali.dexlib2.Opcode
import com.android.tools.smali.dexlib2.builder.MutableMethodImplementation
import com.android.tools.smali.dexlib2.builder.instruction.BuilderInstruction11n
import com.android.tools.smali.dexlib2.builder.instruction.BuilderInstruction11x
import com.android.tools.smali.dexlib2.immutable.ImmutableMethod

/**
 * Splice-safe VIP unlock: force the FOUR pure-Java wrapper methods that
 * delegate to the native VIP getters on `l.ۨ᩸ܰ` to return true.
 *
 * WHY NOT the call-site patch (mtmanagerUnlockVipPatch) or the method patch
 * (mtmanagerUnlockVipMethodsPatch):
 *  - Call-site: forces ~67 native-getter call sites, INCLUDING inside the
 *    wrapper `l/ܽۜ.᩷ۢ᩶()` which gates resource-ID selection → on splice
 *    builds it crashed with `Resources$NotFoundException: String resource ID
 *    #0xffffffff` (Main.java:301). Too broad.
 *  - Methods: replaces the NATIVE getter bodies on `l/ۨ᩸ܰ` → with the splice
 *    the native lib registers those methods, so a Java body there breaks
 *    RegisterNatives (NoSuchMethodError). Must stay excluded from splice.
 *
 * THIS patch targets the wrapper methods, which are:
 *  - PURE JAVA (no native flag → RegisterNatives untouched, splice-safe)
 *  - THE single choke point — Main.smali and all UI code call these, not the
 *    native getters directly:
 *      l/ܽۜ.᩷ۢ᩶()Z  -> delegates to ۘ()Z (MT_Protector / FTP menu)
 *      l/ᩴ᩶.۫ۨۢ()Z  -> delegates to ܺ()Z (FTP/SFTP/SMB/WebDav)
 *      l/᩵᩶.۠֫ۧ()Z  -> delegates to ۘ()Z (MT_Protector)
 *      l/᩸ۗ.᩹֡۫()Z  -> delegates to ܺ()Z (FTP/SFTP/SMB/WebDav)
 *  - Only 4 methods to patch vs 67 call sites → minimal blast radius.
 *
 * RISK: `᩷ۢ᩶()` is called by 2 methods that also use getResources() (resource
 * ID selection). If forcing it true breaks them (String resource ID
 * #0xffffffff), this patch must be excluded too. Empirical test required.
 */
@Suppress("unused")
val mtmanagerUnlockVipWrappersPatch = bytecodePatch(
    name = "Unlock VIP (wrappers)",
    description = "Forces the four pure-Java VIP wrapper methods (l/ܽۜ.᩷ۢ᩶, l/ᩴ᩶.۫ۨۢ, l/᩵᩶.۠֫ۧ, l/᩸ۗ.᩹֡۫) to return true — splice-safe (no native methods touched).",
    default = false
) {
    compatibleWith(COMPATIBILITY_MTMANAGER)

    execute {
        val targets = listOf(
            "Ll/\u073d\u06dc;" to "\u1a77\u06e2\u1a76", // l/ܽۜ.᩷ۢ᩶()Z
            "Ll/\u1a74\u1a76;" to "\u06eb\u06e8\u06e2",       // l/ᩴ᩶.۫ۨۢ()Z
            "Ll/\u1a75\u1a76;" to "\u06e0\u05ab\u06e7",       // l/᩵᩶.۠֫ۧ()Z
            "Ll/\u1a78\u06d7;" to "\u1a79\u05a1\u06eb",       // l/᩸ۗ.᩹֡۫()Z
        )
        var patched = 0
        for ((clsName, methodName) in targets) {
            val cls = mutableClassDefByOrNull(clsName) ?: continue
            val method = cls.methods.firstOrNull {
                it.name == methodName && it.returnType == "Z" && it.parameterTypes.isEmpty()
            } ?: continue
            // Verify it's a pure-Java wrapper (not native) — refuse if native.
            if (method.accessFlags and com.android.tools.smali.dexlib2.AccessFlags.NATIVE.value != 0) {
                println("MT Manager: SKIP $clsName.$methodName — native, refusing to touch")
                continue
            }
            val impl = MutableMethodImplementation(1)
            impl.addInstruction(BuilderInstruction11n(Opcode.CONST_4, 0, 1))
            impl.addInstruction(BuilderInstruction11x(Opcode.RETURN, 0))
            val newMethod = ImmutableMethod(
                method.definingClass, method.name, method.parameters, method.returnType,
                method.accessFlags, method.annotations, method.hiddenApiRestrictions, impl
            )
            cls.methods.remove(method)
            cls.methods.add(MutableMethod(newMethod))
            patched++
            println("MT Manager: forced wrapper $clsName.$methodName()Z -> true")
        }
        println("MT Manager: unlocked $patched VIP wrapper method(s)")
    }
}

package app.logm1lo.patches.mtmanager.premium

import app.logm1lo.patches.shared.COMPATIBILITY_MTMANAGER
import app.morphe.patcher.patch.bytecodePatch
import com.android.tools.smali.dexlib2.AccessFlags
import com.android.tools.smali.dexlib2.Opcode
import com.android.tools.smali.dexlib2.builder.MutableMethodImplementation
import com.android.tools.smali.dexlib2.builder.instruction.BuilderInstruction10x
import com.android.tools.smali.dexlib2.builder.instruction.BuilderInstruction11n
import com.android.tools.smali.dexlib2.builder.instruction.BuilderInstruction11x
import com.android.tools.smali.dexlib2.builder.instruction.BuilderInstruction21s
import com.android.tools.smali.dexlib2.builder.instruction.BuilderInstruction31i
import com.android.tools.smali.dexlib2.immutable.ImmutableMethod

/**
 * Patch native keys: stub the native <clinit> canaries and the m43976
 * registration driver, and replace every read of the 39 native XOR-key static
 * int fields and 12 native-set boolean fields with a const literal.
 *
 * Why: the native <clinit> canaries throw RuntimeException("K2M7") on a
 * re-signed APK (anti-tamper). The 39 XOR keys are needed for string
 * decryption. Both are replaced with plain-Java constants so no native call is
 * ever required. Values captured from a genuine install via Frida
 * (see 39-keys-captured.md).
 *
 * Key table: class descriptor -> field name -> int value.
 * Bool table: class descriptor -> field name -> boolean value.
 */
@Suppress("unused")
val mtmanagerPatchNativeKeysPatch = bytecodePatch(
    name = "Patch native keys",
    description = "Stubs K2M7 <clinit> canaries and patches the 39 native XOR keys / 12 bool flags to constants.",
    default = true
) {
    compatibleWith(COMPATIBILITY_MTMANAGER)

    execute {
        var stubCount = 0
        var keyPatchCount = 0

        classDefForEach { cls ->
            val mutable = mutableClassDefBy(cls)
            val methods = mutable.methods.toList()
            for (method in methods) {
                val flags = method.accessFlags
                if ((flags and AccessFlags.NATIVE.value) == 0) continue
                val name = method.name
                val rt = method.returnType
                val isNative = true

                // Stub native <clinit>()V canaries (K2M7 anti-tamper)
                if (name == "<clinit>" && rt == "V") {
                    stubVoid(mutable, method)
                    stubCount++
                    continue
                }
                // Stub the m43976 registration driver l.᩹ܶ֡.ۛ᩶ۗ(I)V
                if (name == "\u06db\u1a76\u06d7" && rt == "V" &&
                    method.parameterTypes.size == 1 &&
                    method.parameterTypes[0].toString() == "I"
                ) {
                    stubVoid(mutable, method)
                    stubCount++
                    continue
                }
                // Non-void natives are handled by the "Stub native methods" patch.
                if (isNative) continue
            }
        }

        // Patch key/bool reads. Do this over ALL classes (not just l.*).
        classDefForEach { cls ->
            val mutable = mutableClassDefBy(cls)
            val methods = mutable.methods.toList()
            for (method in methods) {
                val impl = method.implementation ?: continue
                val insns = impl.instructions
                // Collect replacements first, then apply from the end to avoid
                // index invalidation (MutableMethodImplementation is a live view).
                var i = insns.size - 1
                while (i >= 0) {
                    val insn = insns[i]
                    val op = insn.opcode
                    if (op == Opcode.SGET) {
                        val c = insn as? com.android.tools.smali.dexlib2.builder.instruction.BuilderInstruction21c ?: run { i--; continue }
                        val ref = c.reference as? com.android.tools.smali.dexlib2.iface.reference.FieldReference ?: run { i--; continue }
                        val v = keyValueFor(ref.definingClass, ref.name) ?: run { i--; continue }
                        impl.replaceInstruction(i, BuilderInstruction31i(Opcode.CONST, c.registerA, v))
                        keyPatchCount++
                    } else if (op == Opcode.SGET_BOOLEAN) {
                        val c = insn as? com.android.tools.smali.dexlib2.builder.instruction.BuilderInstruction21c ?: run { i--; continue }
                        val ref = c.reference as? com.android.tools.smali.dexlib2.iface.reference.FieldReference ?: run { i--; continue }
                        val v = boolValueFor(ref.definingClass, ref.name) ?: run { i--; continue }
                        impl.replaceInstruction(i, BuilderInstruction21s(Opcode.CONST_16, c.registerA, v))
                        keyPatchCount++
                    }
                    i--
                }
            }
        }

        println("MT Manager: stubbed $stubCount native clinits/driver, patched $keyPatchCount key/bool reads")
    }
}

/** Swap a native void method for a return-void stub. */
private fun stubVoid(mutable: app.morphe.patcher.util.proxy.mutableTypes.MutableClass, method: app.morphe.patcher.util.proxy.mutableTypes.MutableMethod) {
    val newFlags = method.accessFlags and AccessFlags.NATIVE.value.inv()
    val impl = MutableMethodImplementation(1)
    impl.addInstruction(BuilderInstruction10x(Opcode.RETURN_VOID))
    val newMethod = ImmutableMethod(
        method.definingClass, method.name, method.parameters, method.returnType,
        newFlags, method.annotations, method.hiddenApiRestrictions,
        impl
    )
    mutable.methods.remove(method)
    mutable.methods.add(app.morphe.patcher.util.proxy.mutableTypes.MutableMethod(newMethod))
}

// ---- Key / bool lookup tables (captured from genuine install, 2026-08-11) ----

private val KEYS: Map<Pair<String, String>, Int> = buildMap {
    put("Ll/\u06d6;" to "\u05a1\u1a75\u1a79", 3472)
    put("Ll/\u06d6\u06e8;" to "\u06e7\u06d9\u06dc", 3939)
    put("Ll/\u06da\u06d6;" to "\u06ec\u073d\u06d7", -2251)
    put("Ll/\u06da\u06d8;" to "\u073f\u0733\u06e7", -967)
    put("Ll/\u06e1\u06eb;" to "\u06ec\u073d\u06dc", -8328)
    put("Ll/\u06e2\u06ec;" to "\u1a7a\u1a77\u06e8", 2647)
    put("Ll/\u06e4\u06d6;" to "\u06e0\u1a76\u05ab", -2097)
    put("Ll/\u06e4\u06d8;" to "\u1a77\u06e4\u1a79", -7838)
    put("Ll/\u06e4\u06da;" to "\u06df\u073f\u0733", 5505)
    put("Ll/\u06da\u0736;" to "\u06db\u06e7\u06dc", -5911)
    put("Ll/\u06df\u1a79;" to "\u06e8\u05a8\u073a", -9464)
    put("Ll/\u1a77;" to "\u073f\u06d6\u1a74", -247)
    put("Ll/\u0730\u06d9;" to "\u1a7b\u073a\u05ab", 5745)
    put("Ll/\u0733\u06d7;" to "\u06da\u1a73\u1a78", -4190)
    put("Ll/\u0733\u06d8;" to "\u0733\u06e2\u1a75", 0)
    put("Ll/\u0733\u073d;" to "\u0730\u1a7b\u1a7a", 0)
    put("Ll/\u0733\u073f;" to "\u06d7\u1a77\u1a75", -5891)
    put("Ll/\u073a\u06ec;" to "\u06e7\u06e2\u06da", -2384)
    put("Ll/\u073d\u05ab;" to "\u06e8\u06e4\u06d9", 0)
    put("Ll/\u073d\u05ab;" to "\u06ec", 0)
    put("Ll/\u073d\u06db;" to "\u05a1\u06dc\u073f", 9004)
    put("Ll/\u073d\u06dc;" to "\u05a8\u1a79\u06da", -4772)
    put("Ll/\u073d\u06e0;" to "\u06da\u06d7\u05a8", -5688)
    put("Ll/\u073d\u06e1;" to "\u0730\u1a73\u06d7", 6132)
    put("Ll/\u073d\u1a74;" to "\u1a7b\u073d\u06eb", 7251)
    put("Ll/\u1a74\u1a76;" to "\u1a7b\u0736\u05a1", -4873)
    put("Ll/\u1a75\u06ec;" to "\u05a8\u05a8\u06e4", -7291)
    put("Ll/\u1a75\u1a76;" to "\u073f\u06e7\u06eb", -5944)
    put("Ll/\u1a75\u1a77;" to "\u1a7a\u0736\u1a75", 3194)
    put("Ll/\u1a75\u1a79;" to "\u073d\u06da\u06da", 2642)
    put("Ll/\u1a75\u1a7a;" to "\u06d9\u0733\u0733", -4387)
    put("Ll/\u1a75\u1a7b;" to "\u1a73\u0733\u0736", 1647)
    put("Ll/\u1a78\u06d7;" to "\u1a7b\u06e1\u05a8", 731)
    put("Ll/\u1a79\u06d8;" to "\u06dc\u06e4\u06e2", -7087)
    put("Ll/\u1a7a\u0736;" to "\u06eb\u06da\u1a7b", 4484)
    put("Ll/\u1a7b\u1a75;" to "\u06eb\u06df\u06dc", 9194)
    put("Ll/\u1a7b\u1a77;" to "\u1a7a\u06db\u06d6", 1435)
    put("Ll/\u06e4\u1a73;" to "\u06e8\u06d8\u073f", -6213)
    put("Ll/\u1a73;" to "\u06dc\u06e2\u06e2", -5250)
}

private val BOOLS: Map<Pair<String, String>, Int> = buildMap {
    put("Ll/\u06d9\u06db;" to "\u06ec\u06e7\u1a7b", 0)
    put("Ll/\u06eb;" to "\u06e4\u05a8\u06dc", 1)
    put("Ll/\u06db\u0730;" to "\u06e1\u06eb\u1a7a", 1)
    put("Ll/\u06e4\u1a74;" to "\u0736\u06d8\u0730", 0)
    put("Ll/\u073a\u06eb;" to "\u06e1\u06df\u06e0", 0)
    put("Ll/\u1a78\u06e0;" to "\u073d\u06e1\u06e8", 1)
    put("Ll/\u06e1;" to "\u06e4\u1a74\u06da", 0)
    put("Ll/\u1a74\u1a74;" to "\u06e0\u06d6\u1a79", 1)
    put("Ll/\u06d7\u1a76;" to "\u06d6\u06da\u06d9", 0)
    put("Ll/\u1a76\u06e0;" to "\u06d6\u06d8\u06ec", 1)
    put("Ll/\u06df\u1a77;" to "\u1a78\u06e1\u1a79", 0)
    put("Ll/\u1a74\u1a73;" to "\u06db\u06eb\u06db", 1)
}

private fun keyValueFor(defClass: String, name: String): Int? = KEYS[defClass to name]

private fun boolValueFor(defClass: String, name: String): Int? = BOOLS[defClass to name]

package app.logm1lo.patches.mtmanager.premium

import app.logm1lo.patches.shared.COMPATIBILITY_MTMANAGER
import app.morphe.patcher.patch.bytecodePatch
import app.morphe.patcher.util.proxy.mutableTypes.MutableMethod
import com.android.tools.smali.dexlib2.AccessFlags
import com.android.tools.smali.dexlib2.Opcode
import com.android.tools.smali.dexlib2.builder.MutableMethodImplementation
import com.android.tools.smali.dexlib2.builder.instruction.BuilderInstruction10x
import com.android.tools.smali.dexlib2.builder.instruction.BuilderInstruction11n
import com.android.tools.smali.dexlib2.builder.instruction.BuilderInstruction11x
import com.android.tools.smali.dexlib2.builder.instruction.BuilderInstruction21s
import com.android.tools.smali.dexlib2.builder.instruction.BuilderInstruction22c
import com.android.tools.smali.dexlib2.builder.instruction.BuilderInstruction35c
import com.android.tools.smali.dexlib2.immutable.ImmutableMethod
import com.android.tools.smali.dexlib2.immutable.reference.ImmutableFieldReference
import com.android.tools.smali.dexlib2.immutable.reference.ImmutableMethodReference

/**
 * Stub all native methods in `l.*` classes.
 *
 * MT Manager's native gate: `libmtprotect.so` verifies the APK signing cert in
 * JNI_OnLoad; on a re-signed (patched) APK it silently skips RegisterNatives
 * for ALL `l.*` natives. The first native call then throws UnsatisfiedLinkError
 * and the app crashes at startup.
 *
 * Proven fix (mtmanager_2.26.8_vip_unlocked.apk): remove ACC_NATIVE from every
 * native method in `l.*` classes and give it a safe default implementation
 * based on return type:
 *   - void -> return-void
 *   - Z/B/S/C/I -> const/4 v0, 0x0; return v0
 *   - J/D -> const-wide/16 v0, 0x0; return-wide v0
 *   - objects -> const/4 v0, 0x0; return-object v0
 *   - <init>([B...) -> invoke-super <init>; store [B param into [B field; return-void
 *
 * Skips <clinit> (handled by the key patch) and the m43976 registration driver.
 */
@Suppress("unused")
val mtmanagerStubNativeMethodsPatch = bytecodePatch(
    name = "Stub native methods",
    description = "Replaces all l.* native methods with safe Java defaults so the app no longer depends on native registration.",
    default = true
) {
    compatibleWith(COMPATIBILITY_MTMANAGER)

    execute {
        var stubCount = 0

        classDefForEach { cls ->
            val type = cls.type
            if (!type.startsWith("Ll/")) return@classDefForEach

            val mutable = mutableClassDefBy(cls)
            // Snapshot the method set (it is a mutable copy).
            val methods = mutable.methods.toList()
            for (method in methods) {
                val flags = method.accessFlags
                if ((flags and AccessFlags.NATIVE.value) == 0) continue
                val name = method.name

                // Skip <clinit> (K2M7 canaries handled by key patch) and the
                // m43976 registration driver l.᩹ܶ֡.ۛ᩶ۗ(I)V
                if (name == "<clinit>") continue
                if (name == "\u06db\u1a76\u06d7" && // ۛ᩶ۗ
                    method.returnType == "V" &&
                    method.parameterTypes.size == 1 &&
                    method.parameterTypes[0].toString() == "I"
                ) continue

                val newFlags = flags and AccessFlags.NATIVE.value.inv()

                // Register count: 'this' (non-static) + params, wide params count 2
                var regCount = 0
                val isStatic = (newFlags and AccessFlags.STATIC.value) != 0
                if (!isStatic) regCount++
                for (p in method.parameterTypes) {
                    regCount++
                    if (p.toString() == "J" || p.toString() == "D") regCount++
                }
                if (regCount == 0) regCount = 1
                val rt = method.returnType
                if ((rt == "J" || rt == "D") && regCount < 2) regCount = 2

                val impl = MutableMethodImplementation(regCount)

                if (name == "<init>") {
                    val superCls = cls.superclass
                    if (superCls != null) {
                        impl.addInstruction(
                            BuilderInstruction35c(
                                Opcode.INVOKE_DIRECT, 1, 0, 0, 0, 0, 0,
                                ImmutableMethodReference(superCls, "<init>", emptyList(), "V")
                            )
                        )
                    }
                    val bField = cls.fields.firstOrNull { it.type == "[B" }
                    val firstIsB = method.parameterTypes.isNotEmpty() &&
                        method.parameterTypes[0].toString() == "[B"
                    if (bField != null && firstIsB) {
                        impl.addInstruction(
                            BuilderInstruction22c(
                                Opcode.IPUT_OBJECT, 1, 0,
                                ImmutableFieldReference(cls.type, bField.name, "[B")
                            )
                        )
                    }
                    impl.addInstruction(BuilderInstruction10x(Opcode.RETURN_VOID))
                } else {
                    when (rt) {
                        "V" -> impl.addInstruction(BuilderInstruction10x(Opcode.RETURN_VOID))
                        "Z", "B", "S", "C", "I" -> {
                            impl.addInstruction(BuilderInstruction11n(Opcode.CONST_4, 0, 0))
                            impl.addInstruction(BuilderInstruction11x(Opcode.RETURN, 0))
                        }
                        "J", "D" -> {
                            impl.addInstruction(BuilderInstruction21s(Opcode.CONST_WIDE_16, 0, 0))
                            impl.addInstruction(BuilderInstruction11x(Opcode.RETURN_WIDE, 0))
                        }
                        else -> {
                            impl.addInstruction(BuilderInstruction11n(Opcode.CONST_4, 0, 0))
                            impl.addInstruction(BuilderInstruction11x(Opcode.RETURN_OBJECT, 0))
                        }
                    }
                }

                // Build a new immutable method with the stub body and swap it in.
                val newMethod = ImmutableMethod(
                    method.definingClass, method.name, method.parameters, method.returnType,
                    newFlags, method.annotations, method.hiddenApiRestrictions,
                    impl
                )
                mutable.methods.remove(method)
                mutable.methods.add(MutableMethod(newMethod))
                stubCount++
            }
        }

        println("MT Manager: stubbed $stubCount native methods")
    }
}

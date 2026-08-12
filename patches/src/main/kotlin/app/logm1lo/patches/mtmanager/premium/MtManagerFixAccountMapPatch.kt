package app.logm1lo.patches.mtmanager.premium

import app.logm1lo.patches.shared.COMPATIBILITY_MTMANAGER
import app.morphe.patcher.patch.bytecodePatch
import com.android.tools.smali.dexlib2.Opcode
import com.android.tools.smali.dexlib2.builder.MutableMethodImplementation
import com.android.tools.smali.dexlib2.builder.instruction.BuilderInstruction10x
import com.android.tools.smali.dexlib2.builder.instruction.BuilderInstruction11x
import com.android.tools.smali.dexlib2.builder.instruction.BuilderInstruction21c
import com.android.tools.smali.dexlib2.builder.instruction.BuilderInstruction22c
import com.android.tools.smali.dexlib2.immutable.ImmutableMethod
import com.android.tools.smali.dexlib2.immutable.reference.ImmutableTypeReference

/**
 * Fix account-data getters on `l.ۨ᩸ܰ` that return null after native stubbing.
 *
 * The native Map getter `۟()Ljava/util/Map;` is used by the Purchase VIP /
 * account screen `l.ܳ᩻ܰ` (and others) as `map.containsKey(...)`. With the
 * native stubbed to `return null`, that call throws
 *   NullPointerException: Attempt to invoke interface method
 *   'boolean java.util.Map.containsKey(...)' on a null object reference
 * and the activity crashes (QA finding 2026-08-12).
 *
 * Fix: replace the body with `new-instance v0, Ljava/util/HashMap;`,
 * `invoke-direct {v0}, <init>()V`, `return-object v0` so callers get an
 * empty (non-null) Map. String getters (`ۧ()`, `۬()`, `ܶ()`) are left
 * returning null — callers guard those with null-checks.
 */
@Suppress("unused")
val mtmanagerFixAccountMapPatch = bytecodePatch(
    name = "Fix account map getter",
    description = "Makes l.ۨ᩸ܰ.۟()Ljava/util/Map; return an empty HashMap instead of null to prevent Purchase-VIP / account screen crashes.",
    default = true
) {
    compatibleWith(COMPATIBILITY_MTMANAGER)
    dependsOn(mtmanagerStubNativeMethodsPatch)

    execute {
        val vipClass = mutableClassDefByOrNull("Ll/\u06e8\u1a78\u0730;") ?: return@execute
        val target = vipClass.methods.firstOrNull {
            it.name == "\u06df" && // ۟
                it.returnType == "Ljava/util/Map;" &&
                it.parameterTypes.isEmpty()
        } ?: return@execute

        val impl = MutableMethodImplementation(1)
        // new-instance v0, Ljava/util/HashMap;
        impl.addInstruction(
            BuilderInstruction21c(
                Opcode.NEW_INSTANCE, 0,
                ImmutableTypeReference("Ljava/util/HashMap;")
            )
        )
        // invoke-direct {v0}, Ljava/util/HashMap;-><init>()V
        impl.addInstruction(
            com.android.tools.smali.dexlib2.builder.instruction.BuilderInstruction35c(
                Opcode.INVOKE_DIRECT, 1, 0, 0, 0, 0, 0,
                com.android.tools.smali.dexlib2.immutable.reference.ImmutableMethodReference(
                    "Ljava/util/HashMap;", "<init>", emptyList(), "V"
                )
            )
        )
        // return-object v0 (Format11x — takes register v0)
        impl.addInstruction(BuilderInstruction11x(Opcode.RETURN_OBJECT, 0))

        val newMethod = ImmutableMethod(
            target.definingClass, target.name, target.parameters, target.returnType,
            target.accessFlags, target.annotations, target.hiddenApiRestrictions,
            impl
        )
        vipClass.methods.remove(target)
        vipClass.methods.add(app.morphe.patcher.util.proxy.mutableTypes.MutableMethod(newMethod))
        println("MT Manager: l.ۨ᩸ܰ.۟()Ljava/util/Map; now returns empty HashMap")
    }
}

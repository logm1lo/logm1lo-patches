package app.logm1lo.patches.mtmanager.premium

import app.logm1lo.patches.shared.COMPATIBILITY_MTMANAGER
import app.morphe.patcher.patch.bytecodePatch
import com.android.tools.smali.dexlib2.Opcode
import com.android.tools.smali.dexlib2.builder.MutableMethodImplementation
import com.android.tools.smali.dexlib2.builder.instruction.BuilderInstruction11x
import com.android.tools.smali.dexlib2.builder.instruction.BuilderInstruction35c
import com.android.tools.smali.dexlib2.immutable.ImmutableMethod
import com.android.tools.smali.dexlib2.immutable.reference.ImmutableMethodReference

/**
 * Fix the VIP list variant so the full VIP unlock (including ۘ()=true) does
 * not break file listing.
 *
 * Why: `l.᩷ۛܰ` is the file-list data provider. It exposes two variants:
 *   - ܳ()  the NORMAL list variant (works with stubbed natives)
 *   - ܿ()  the VIP list variant (returns empty / throws with stubbed natives)
 *
 * Several call sites choose between them on ۘ():
 *   - `l.ۨ᩺ܰ.ܳ()`  file-list adapter  (if ۘ() true -> ܿ() VIP variant -> "Folders: 0")
 *   - `l.᩻᩺ܰ`       dex util           (same ۘ()-gated ܿ()/ܳ() selection, 2 sites)
 * and 6 more classes call ܿ() unconditionally (l.ۤۘۧ, l.ܳۘ, l.᩹ۘ,
 * l.֨ۗ֡, l.ۗ᩷ܰ, l.ۚۗ֡) — those are already broken in the 6-getter build.
 *
 * Fix: replace the body of `l.᩷ۛܰ.ܿ()Ll/֨ۛܰ;` with a direct delegation to
 * `l.᩷ۛܰ.ܳ()Ll/֨ۛܰ;`. Every caller of the VIP variant now receives the
 * working normal result, so forcing ۘ() true can no longer break listing.
 *
 * Method is non-static, so p0 = this.
 */
@Suppress("unused")
val mtmanagerVipListVariantFixPatch = bytecodePatch(
    name = "Fix VIP list variant",
    description = "Makes l.᩷ۛܰ.ܿ() (VIP file-list variant) delegate to the working normal variant so ۘ()=true keeps file listing intact.",
    default = true
) {
    compatibleWith(COMPATIBILITY_MTMANAGER)

    execute {
        val cls = mutableClassDefByOrNull("Ll/\u1a77\u06db\u0730;") ?: return@execute // l.᩷ۛܰ
        val target = cls.methods.firstOrNull {
            it.name == "\u073f" && // ܿ
                it.returnType == "Ll/\u05a8\u06db\u0730;" // Ll/֨ۛܰ;
        } ?: return@execute

        val impl = MutableMethodImplementation(2)
        // Non-static, no params: p0 (this) = v1, v0 is a free local.
        // invoke-virtual {v1}, Ll/᩷ۛܰ;->ܳ()Ll/֨ۛܰ;
        impl.addInstruction(
            BuilderInstruction35c(
                Opcode.INVOKE_VIRTUAL, 1, 1, 0, 0, 0, 0,
                ImmutableMethodReference(
                    "Ll/\u1a77\u06db\u0730;", // Ll/᩷ۛܰ;
                    "\u0733", // ܳ
                    emptyList(),
                    "Ll/\u05a8\u06db\u0730;" // Ll/֨ۛܰ;
                )
            )
        )
        // move-result-object v0
        impl.addInstruction(BuilderInstruction11x(Opcode.MOVE_RESULT_OBJECT, 0))
        // return-object v0 (Format11x — takes register v0)
        impl.addInstruction(BuilderInstruction11x(Opcode.RETURN_OBJECT, 0))

        val newMethod = ImmutableMethod(
            target.definingClass, target.name, target.parameters, target.returnType,
            target.accessFlags, target.annotations, target.hiddenApiRestrictions,
            impl
        )
        cls.methods.remove(target)
        cls.methods.add(app.morphe.patcher.util.proxy.mutableTypes.MutableMethod(newMethod))
        println("MT Manager: VIP list variant l.᩷ۛܰ.ܿ() now delegates to normal l.᩷ۛܰ.ܳ()")
    }
}

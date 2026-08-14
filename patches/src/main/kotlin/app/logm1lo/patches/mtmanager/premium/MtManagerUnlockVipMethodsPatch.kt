package app.logm1lo.patches.mtmanager.premium

import app.logm1lo.patches.shared.COMPATIBILITY_MTMANAGER
import app.morphe.patcher.patch.bytecodePatch
import app.morphe.patcher.util.proxy.mutableTypes.MutableMethod
import com.android.tools.smali.dexlib2.AccessFlags
import com.android.tools.smali.dexlib2.Opcode
import com.android.tools.smali.dexlib2.builder.MutableMethodImplementation
import com.android.tools.smali.dexlib2.builder.instruction.BuilderInstruction11n
import com.android.tools.smali.dexlib2.builder.instruction.BuilderInstruction11x
import com.android.tools.smali.dexlib2.immutable.ImmutableMethod

/**
 * Unlock VIP by REPLACING the getter method bodies on `l.ۨ᩸ܰ`.
 *
 * Proven approach (PatchVipMethods2.java in the working build): instead of
 * patching every call site, replace each `()Z` getter method's body with
 * `const/4 v0, 0x1; return v0`. Every call site then sees true with no
 * move-result interaction risk.
 *
 * ALL SEVEN getters are now unlocked (2026-08-12 fresh hunt):
 *   ֡()Z login-confirm, ۠()Z UI/themes, ܰ()Z misc, ܺ()Z FTP/editors,
 *   ᩶()Z misc, ᩷()Z misc, and ۘ()Z MT_Protector/tools.
 *
 * ۘ() was previously left false because forcing it made the file listing
 * show "Folders: 0" — the file-list adapter `l.ۨ᩺ܰ.ܳ()` selects the VIP
 * list variant `l.᩷ۛܰ.ܿ()` when ۘ() is true, and that variant returns
 * empty with stubbed natives. The companion patch
 * `mtmanagerVipListVariantFixPatch` makes `l.᩷ۛܰ.ܿ()` delegate to the
 * working normal variant `l.᩷ۛܰ.ܳ()`, so forcing ۘ() true is now safe.
 */
@Suppress("unused")
val mtmanagerUnlockVipMethodsPatch = bytecodePatch(
    name = "Unlock VIP (methods)",
    description = "Replaces all seven VIP getter methods on l.ۨ᩸ܰ to always return true.",
    default = true
) {
    compatibleWith(COMPATIBILITY_MTMANAGER)

    execute {
        val vipClass = mutableClassDefByOrNull("Ll/\u06e8\u1a78\u0730;") ?: return@execute
        val targets = setOf(
            "\u05a1", // ֡ login-confirm
            "\u06d8", // ۘ MT_Protector / tool gate (safe via VIP-list-variant fix)
            "\u06e0", // ۠ UI/themes
            "\u0730", // ܰ misc
            "\u073a", // ܺ FTP
            "\u1a76", // ᩶ misc
            "\u1a77", // ᩷ misc
        )
        var patched = 0
        val methods = vipClass.methods.toList()
        for (method in methods) {
            if (method.name !in targets) continue
            if (method.returnType != "Z") continue
            if (method.parameterTypes.isNotEmpty()) continue

            val impl = MutableMethodImplementation(1)
            impl.addInstruction(BuilderInstruction11n(Opcode.CONST_4, 0, 1))
            impl.addInstruction(com.android.tools.smali.dexlib2.builder.instruction.BuilderInstruction11x(Opcode.RETURN, 0))

            // CRITICAL (fix19): clear the NATIVE access flag. Keeping it set leaves
            // a method that "has code, but is marked native" → dex verification fails
            // (ClassNotFoundException at Application instantiation). This mirrors the
            // StubNativeMethodsPatch flag clearing.
            val newFlags = method.accessFlags and AccessFlags.NATIVE.value.inv()

            val newMethod = ImmutableMethod(
                method.definingClass, method.name, method.parameters, method.returnType,
                newFlags, method.annotations, method.hiddenApiRestrictions,
                impl
            )
            vipClass.methods.remove(method)
            vipClass.methods.add(MutableMethod(newMethod))
            patched++
        }
        println("MT Manager: replaced $patched VIP getter methods to return true")
    }
}

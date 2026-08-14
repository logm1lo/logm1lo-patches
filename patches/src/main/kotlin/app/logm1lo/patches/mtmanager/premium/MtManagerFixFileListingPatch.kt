package app.logm1lo.patches.mtmanager.premium

import app.logm1lo.patches.shared.COMPATIBILITY_MTMANAGER
import app.morphe.patcher.patch.bytecodePatch
import com.android.tools.smali.dexlib2.Opcode
import com.android.tools.smali.dexlib2.builder.MutableMethodImplementation
import com.android.tools.smali.dexlib2.builder.instruction.BuilderInstruction11n
import com.android.tools.smali.dexlib2.builder.instruction.BuilderInstruction11x
import com.android.tools.smali.dexlib2.immutable.ImmutableMethod

/**
 * Fix the empty file listing so folders and file pickers populate.
 *
 * MT Manager's file-list pipeline has native-hollow gates that reject every
 * entry with stubbed natives:
 *
 * GATE 1 — node factory mount check: `l.֨۫ۧ.ܿ(Ljava/lang/String;)Z`
 *   decides whether the factory builds the direct `l.᩸۫ۧ` node (File.list —
 *   works, returns 22 entries) or the native-backed hollow `l.֡ܶܳ` node.
 *
 * GATE 2 — node validation: `l.֨۫ۧ.ܿ(Ll/ܳ۫ۧ;)Z`
 *   rejects a node unless it passes the provider path-check
 *   (`provider.۟(path)`, a deep native state machine) or the native-node
 *   path `ۘ()` (which deliberately THROWS RuntimeException in the stub build).
 *   With stubbed natives every node is rejected -> empty list.
 *
 * FIX: force BOTH gates to return true:
 *   - `l.֨۫ۧ.ܿ(Ljava/lang/String;)Z`  -> const/4 v0,0x1; return v0
 *   - `l.֨۫ۧ.ܿ(Ll/ܳ۫ۧ;)Z`            -> const/4 v0,0x1; return v0
 *
 * IMPORTANT: do NOT force `l.ܳ۫ۧ.۬ܿ()Z` (per-item displayable) — the factory
 * uses it as a switch: true routes to the throwing native-node `ۘ()`. Forcing
 * it true causes ExceptionInInitializerError. The node-validation gate fix
 * bypasses that whole branch.
 *
 * Must run AFTER the native-stub patch (so the stub patch doesn't clobber it).
 */
@Suppress("unused")
val mtmanagerFixFileListingPatch = bytecodePatch(
    name = "Fix file listing (direct FileSource)",
    description = "Forces the node-factory mount check and node validation to pass so folders and file pickers populate.",
    default = true
) {
    compatibleWith(COMPATIBILITY_MTMANAGER)
    // NOTE (fix19): removed dependsOn(mtmanagerStubNativeMethodsPatch). The splice
    // approach delivers working natives; pulling the stub patch in under
    // --exclusive stubbed natives the app needs (NoSuchMethodError at launch).
    // This patch forces the file-listing gates independently.

    execute {
        val factory = mutableClassDefByOrNull("Ll/\u05a8\u06eb\u06e7;") ?: return@execute // l.֨۫ۧ
        var patched = 0

        // GATE 1: ܿ(String)Z -> true (always build direct FileSource node)
        val mountCheck = factory.methods.firstOrNull {
            it.name == "\u073f" && // ܿ
                it.returnType == "Z" &&
                it.parameterTypes.size == 1 &&
                it.parameterTypes[0].toString() == "Ljava/lang/String;"
        }
        if (mountCheck != null) {
            replaceWithTrue(factory, mountCheck)
            patched++
        }

        // GATE 2: ܿ(Ll/ܳ۫ۧ;)Z -> true (node validation)
        val nodeValidation = factory.methods.firstOrNull {
            it.name == "\u073f" && // ܿ
                it.returnType == "Z" &&
                it.parameterTypes.size == 1 &&
                it.parameterTypes[0].toString() == "Ll/\u0733\u06eb\u06e7;" // Ll/ܳ۫ۧ;
        }
        if (nodeValidation != null) {
            replaceWithTrue(factory, nodeValidation)
            patched++
        }

        // GATE 3: l.ۘ֫ܳ.۬ܿ()Z -> true (per-item displayable check)
        // The loader produces items of type l/ۡ֡۬ (extends l/ۘ֫ܳ which implements
        // the display interface l/᩶᩺ܳ). The adapter feed skips items whose ۬ܿ()
        // is false. It returns (۟۟() || field ܳ۟:Z), both false with stubbed natives
        // -> every item skipped -> empty list. Force true.
        val itemClass = mutableClassDefByOrNull("Ll/\u06d8\u05ab\u0733;") ?: return@execute // l.ۘ֫ܳ
        val itemDisp = itemClass.methods.firstOrNull {
            it.name == "\u06ec\u073f" && // ۬ܿ
                it.returnType == "Z" &&
                it.parameterTypes.isEmpty()
        }
        if (itemDisp != null) {
            replaceWithTrue(itemClass, itemDisp)
            patched++
        }

        // GATE 4: l.᩶֨ܳ.ۢ()Z -> true (feed early-exit gate)
        // The adapter feed starts with node.᩺() -> l.ۤۚܳ.ۢ() -> l.᩶֨ܳ.ۢ() which is a
        // HARDCODED `const/4 v0,0x0; return v0` stub. When false the feed returns
        // before iterating the items, so the ListViews stay empty. Force true so
        // the feed actually processes the displayable items.
        val feedGateClass = mutableClassDefByOrNull("Ll/\u1a76\u05a8\u0733;") ?: return@execute // l.᩶֨ܳ
        val feedGate = feedGateClass.methods.firstOrNull {
            it.name == "\u06e2" && // ۢ
                it.returnType == "Z" &&
                it.parameterTypes.isEmpty()
        }
        if (feedGate != null) {
            replaceWithTrue(feedGateClass, feedGate)
            patched++
        }

        println("MT Manager: forced $patched file-listing gate(s) to true (mount-check + node validation + item displayable + feed gate)")
    }
}

private fun replaceWithTrue(
    cls: app.morphe.patcher.util.proxy.mutableTypes.MutableClass,
    method: app.morphe.patcher.util.proxy.mutableTypes.MutableMethod
) {
    val impl = MutableMethodImplementation(1)
    impl.addInstruction(BuilderInstruction11n(Opcode.CONST_4, 0, 1))
    impl.addInstruction(BuilderInstruction11x(Opcode.RETURN, 0))
    val newMethod = ImmutableMethod(
        method.definingClass, method.name, method.parameters, method.returnType,
        method.accessFlags, method.annotations, method.hiddenApiRestrictions,
        impl
    )
    cls.methods.remove(method)
    cls.methods.add(app.morphe.patcher.util.proxy.mutableTypes.MutableMethod(newMethod))
}

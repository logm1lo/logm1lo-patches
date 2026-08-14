package app.logm1lo.patches.mtmanager.premium

import app.logm1lo.patches.shared.COMPATIBILITY_MTMANAGER
import app.morphe.patcher.patch.bytecodePatch
import app.morphe.patcher.util.smali.InlineSmaliCompiler
import com.android.tools.smali.dexlib2.builder.MutableMethodImplementation
import com.android.tools.smali.dexlib2.immutable.ImmutableMethod

/**
 * Force the REAL file-browser ListView to LAY OUT its rows.
 *
 * ROOT CAUSE: `l.ܳۚܰ.layoutChildren()` (the two-pane file-browser ListView's
 * row layout method) is wrapped in the same 1330+ instruction obfuscated
 * native-state machine as `draw()`. On the splice-bypassed build the native
 * state makes it early-return before laying out rows, so the file rows are
 * present in the adapter but never laid out/painted -> blank pane.
 *
 * FIX: rebuild `l.ܳۚܰ.layoutChildren()` to simply invoke the super
 * implementation (android.widget.ListView.layoutChildren) and return, so the
 * standard ListView layout runs against the (real, populated) adapter and
 * lays out all rows. Combined with the draw() super-patch, rows should then
 * both layout AND paint.
 *
 * Applies ON TOP of the gate-passing fix16 build (single-method change via
 * --exclusive on the already-patched APK) to avoid disturbing the native gate.
 */
@Suppress("unused")
val mtmanagerForceListViewLayoutPatch = bytecodePatch(
    name = "Force file-browser ListView to layout rows",
    description = "Replaces l.ܳۚܰ.layoutChildren with a plain super call so the file browser's ListView lays out all its rows.",
    default = true
) {
    compatibleWith(COMPATIBILITY_MTMANAGER)

    execute {
        val cls = mutableClassDefByOrNull("Ll/\u0733\u06da\u0730;") ?: return@execute // l.ܳۚܰ
        val method = cls.methods.firstOrNull {
            it.name == "layoutChildren" &&
                it.parameterTypes.isEmpty() &&
                it.returnType == "V"
        } ?: return@execute

        // Non-static 0-param: .registers 1 -> p0 = this = v0
        val smali = "invoke-super {p0}, Landroid/widget/ListView;->layoutChildren()V\nreturn-void\n"
        val impl = MutableMethodImplementation(1)
        val compiled = InlineSmaliCompiler.Companion.compile(smali, "", 1, false)
        compiled.forEach { impl.addInstruction(it) }

        val newMethod = ImmutableMethod(
            method.definingClass, method.name, method.parameters, method.returnType,
            method.accessFlags, method.annotations, method.hiddenApiRestrictions, impl
        )
        cls.methods.remove(method)
        cls.methods.add(app.morphe.patcher.util.proxy.mutableTypes.MutableMethod(newMethod))
        println("MT Manager: l.ܳۚܰ.layoutChildren simplified to super (real ListView layout target)")
    }
}

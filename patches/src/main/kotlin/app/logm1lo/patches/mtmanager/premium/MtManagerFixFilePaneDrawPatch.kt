package app.logm1lo.patches.mtmanager.premium

import app.logm1lo.patches.shared.COMPATIBILITY_MTMANAGER
import app.morphe.patcher.patch.bytecodePatch
import app.morphe.patcher.util.smali.InlineSmaliCompiler
import com.android.tools.smali.dexlib2.builder.MutableMethodImplementation
import com.android.tools.smali.dexlib2.immutable.ImmutableMethod

/**
 * Make the visible file-browser pane (slide panel file list) always DRAW.
 *
 * ROOT CAUSE: the file browser's file pane `l.֡ۚܰ` (a RecyclerView variant)
 * overrides `dispatchDraw(Canvas)` with a huge conditional body full of early
 * `return-void` branches. On the patched build one of those branches is taken,
 * so the file rows are laid out with real text but never painted -> white page
 * below the tabs even though the hierarchy contains the rows.
 *
 * FIX: rebuild `l.֡ۚܰ.dispatchDraw(Landroid/graphics/Canvas;)V` to simply
 * invoke the super implementation (ViewGroup.dispatchDraw) and return, so all
 * child rows are always drawn.
 */
@Suppress("unused")
val mtmanagerFixFilePaneDrawPatch = bytecodePatch(
    name = "Force file-browser pane to draw rows",
    description = "Replaces l.֡ۚܰ.dispatchDraw with a plain super call so the visible file list always paints its rows.",
    default = true
) {
    compatibleWith(COMPATIBILITY_MTMANAGER)

    execute {
        val cls = mutableClassDefByOrNull("Ll/\u05a1\u06da\u0730;") ?: return@execute // l.֡ۚܰ
        val method = cls.methods.firstOrNull {
            it.name == "dispatchDraw" &&
                it.parameterTypes.size == 1 &&
                it.parameterTypes[0] == "Landroid/graphics/Canvas;" &&
                it.returnType == "V"
        } ?: return@execute

        // Non-static 1-param (Canvas): .registers 2 -> p0 = this = v1, p1 = canvas = v0
        val smali = "invoke-super {p0, p1}, Landroid/view/ViewGroup;->dispatchDraw(Landroid/graphics/Canvas;)V\nreturn-void\n"
        val impl = MutableMethodImplementation(2)
        val compiled = InlineSmaliCompiler.Companion.compile(smali, "Landroid/graphics/Canvas;", 2, false)
        compiled.forEach { impl.addInstruction(it) }

        val newMethod = ImmutableMethod(
            method.definingClass, method.name, method.parameters, method.returnType,
            method.accessFlags, method.annotations, method.hiddenApiRestrictions, impl
        )
        cls.methods.remove(method)
        cls.methods.add(app.morphe.patcher.util.proxy.mutableTypes.MutableMethod(newMethod))
        println("MT Manager: l.֡ۚܰ.dispatchDraw simplified to always draw children")
    }
}

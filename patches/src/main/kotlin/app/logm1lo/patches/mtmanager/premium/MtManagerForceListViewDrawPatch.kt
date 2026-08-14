package app.logm1lo.patches.mtmanager.premium

import app.logm1lo.patches.shared.COMPATIBILITY_MTMANAGER
import app.morphe.patcher.patch.bytecodePatch
import app.morphe.patcher.util.smali.InlineSmaliCompiler
import com.android.tools.smali.dexlib2.builder.MutableMethodImplementation
import com.android.tools.smali.dexlib2.immutable.ImmutableMethod

/**
 * Force the REAL file-browser ListView to always draw its children.
 *
 * ROOT CAUSE (render wall): the file browser's ListView is `l.ܳۚܰ`
 * (codepoints \u0733 \u06da \u0730, extends android.widget.ListView).
 * It overrides `draw(Landroid/graphics/Canvas;)V` with a huge 1330-instruction
 * obfuscated body full of early `return-void` branches and a buried
 * `invoke-super` at instruction 1266. On the patched/native-keyed build one of
 * the early-return branches is taken, so the ListView's children (the file
 * rows) are laid out but NEVER painted -> the file browser renders a white/
 * blank pane even though the view hierarchy contains all rows with real data.
 *
 * FIX: rebuild `l.ܳۚܰ.draw(Canvas)V` to simply invoke the super
 * implementation (android.widget.ListView.draw) and return, so the ListView
 * always paints its children. This is the correct target for the "force the
 * file-browser pane to draw rows" fix (the earlier l.֡ۚܰ.dispatchDraw patch
 * targeted a class that does not block drawing).
 */
@Suppress("unused")
val mtmanagerForceListViewDrawPatch = bytecodePatch(
    name = "Force file-browser ListView to draw rows (real target)",
    description = "Replaces l.ܳۚܰ.draw with a plain super call so the file browser's ListView always paints its rows.",
    default = true
) {
    compatibleWith(COMPATIBILITY_MTMANAGER)

    execute {
        val cls = mutableClassDefByOrNull("Ll/\u0733\u06da\u0730;") ?: return@execute // l.ܳۚܰ
        val method = cls.methods.firstOrNull {
            it.name == "draw" &&
                it.parameterTypes.size == 1 &&
                it.parameterTypes[0] == "Landroid/graphics/Canvas;" &&
                it.returnType == "V"
        } ?: return@execute

        // Non-static 1-param (Canvas): .registers 2 -> p0 = this = v1, p1 = canvas = v0
        val smali = "invoke-super {p0, p1}, Landroid/widget/ListView;->draw(Landroid/graphics/Canvas;)V\nreturn-void\n"
        val impl = MutableMethodImplementation(2)
        val compiled = InlineSmaliCompiler.Companion.compile(smali, "Landroid/graphics/Canvas;", 2, false)
        compiled.forEach { impl.addInstruction(it) }

        val newMethod = ImmutableMethod(
            method.definingClass, method.name, method.parameters, method.returnType,
            method.accessFlags, method.annotations, method.hiddenApiRestrictions, impl
        )
        cls.methods.remove(method)
        cls.methods.add(app.morphe.patcher.util.proxy.mutableTypes.MutableMethod(newMethod))
        println("MT Manager: l.ܳۚܰ.draw simplified to always draw children (real ListView target)")
    }
}

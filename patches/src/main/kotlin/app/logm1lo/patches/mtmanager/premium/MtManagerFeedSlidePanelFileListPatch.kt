package app.logm1lo.patches.mtmanager.premium

import app.logm1lo.patches.shared.COMPATIBILITY_MTMANAGER
import app.morphe.patcher.patch.bytecodePatch
import app.morphe.patcher.util.smali.InlineSmaliCompiler
import com.android.tools.smali.dexlib2.builder.MutableMethodImplementation
import com.android.tools.smali.dexlib2.immutable.ImmutableMethod

/**
 * Feed the slide-panel file browser list (l/ۗ᩺ܰ) with real file items.
 *
 * ROOT CAUSE (white page): the visible file browser is the slide panel with
 * History/Bookmarks tabs. Its file list adapter (l/ۘ᩸ܳ) gets its item count
 * from the tab object l/᩹ۛܳ.۟()I and items from l/᩹ۛܳ.ܿ(I). Both delegate to
 * a file source (l/֡ۛܳ via l/۠ۛܳ.ܿ(index)) whose path field is the internal
 * "HIS" marker ("\rH\rI\rS\r") — NOT the real directory. On re-signed builds
 * that source only contains the breadcrumb item, so the panel renders a white
 * page below the tabs.
 *
 * FIX: rebuild both getters on l/᩹ۛܳ to self-feed from the browser's CURRENT
 * directory path (reachable as this.۟.᩶.ۛ — adapter -> browser -> path):
 *
 *   l/᩹ۛܳ.۟()I  -> 1 (breadcrumb) + MtTools.feedFileItems(path).size()
 *   l/᩹ۛܳ.ܿ(I)  -> index 0: the original breadcrumb item (source item 0)
 *                  index >0: MtTools.feedFileItems(path).get(I-1)
 *
 * The adapter's ܳ() populate loop calls l/᩹ۛܳ.۟() + l/᩹ۛܳ.ܿ(I), so it will
 * fill its internal list with breadcrumb + real file rows, which then render.
 *
 * Requires `extendWith("extensions/mtmanager.mpe")` so MtTools is present.
 */
@Suppress("unused")
val mtmanagerFeedSlidePanelFileListPatch = bytecodePatch(
    name = "Feed slide-panel file list (extension)",
    description = "Seeds the visible file browser's tab getters with the real directory listing via the MtTools extension, so the slide panel renders all files instead of only the breadcrumb.",
    default = true
) {
    compatibleWith(COMPATIBILITY_MTMANAGER)

    extendWith("extensions/mtmanager.mpe")

    execute {
        val cls = mutableClassDefByOrNull("Ll/\u1a79\u06db\u0733;") ?: return@execute // l.᩹ۛܳ

        // ---- 1. Rebuild l/᩹ۛܳ.۟()I : count = 1 + real count ----
        val countMethod = cls.methods.firstOrNull {
            it.name == "\u06df" && // ۟
                it.returnType == "I" &&
                it.parameterTypes.isEmpty()
        }
        if (countMethod != null) {
            // Non-static 0-param: .registers 8 -> p0 (this) = v7; scratch v0..v6.
            val smali = buildString {
                // v0 = this.۟.᩶.ۛ (browser current path)
                append("iget-object v0, p0, Ll/\u1a79\u06db\u0733;->\u06df:Ll/\u06d8\u1a78\u0733;\n") // this.۟ adapter
                append("iget-object v0, v0, Ll/\u06d8\u1a78\u0733;->\u1a76:Ll/\u06d9\u1a78\u0733;\n") // .᩶ browser
                append("iget-object v0, v0, Ll/\u06d9\u1a78\u0733;->\u06db:Ljava/lang/String;\n") // .ۛ path
                // if path null -> fallback to original source count
                append("if-eqz v0, :orig\n")
                // reflection: Class.forName("app.morphe.extension.mtmanager.MtTools")
                append("invoke-virtual {p0}, Ljava/lang/Object;->getClass()Ljava/lang/Class;\n")
                append("move-result-object v1\n")
                append("invoke-virtual {v1}, Ljava/lang/Class;->getClassLoader()Ljava/lang/ClassLoader;\n")
                append("move-result-object v1\n")
                append("const-string v2, \"app.morphe.extension.mtmanager.MtTools\"\n")
                append("const/4 v3, 0x1\n")
                append("invoke-static {v2, v3, v1}, Ljava/lang/Class;->forName(Ljava/lang/String;ZLjava/lang/ClassLoader;)Ljava/lang/Class;\n")
                append("move-result-object v1\n")
                // getMethod("feedFileItems", String.class)
                append("const/4 v3, 0x1\n")
                append("new-array v2, v3, [Ljava/lang/Class;\n")
                append("const-class v3, Ljava/lang/String;\n")
                append("const/4 v4, 0x0\n")
                append("aput-object v3, v2, v4\n")
                append("const-string v3, \"feedFileItems\"\n")
                append("invoke-virtual {v1, v3, v2}, Ljava/lang/Class;->getMethod(Ljava/lang/String;[Ljava/lang/Class;)Ljava/lang/reflect/Method;\n")
                append("move-result-object v1\n")
                // method.invoke(null, path)
                append("const/4 v3, 0x1\n")
                append("new-array v2, v3, [Ljava/lang/Object;\n")
                append("const/4 v4, 0x0\n")
                append("aput-object v0, v2, v4\n")
                append("const/4 v3, 0x0\n")
                append("invoke-virtual {v1, v3, v2}, Ljava/lang/reflect/Method;->invoke(Ljava/lang/Object;[Ljava/lang/Object;)Ljava/lang/Object;\n")
                append("move-result-object v0\n")
                // if null or empty -> original source count
                append("if-eqz v0, :orig\n")
                append("invoke-interface {v0}, Ljava/util/List;->isEmpty()Z\n")
                append("move-result v1\n")
                append("if-nez v1, :orig\n")
                // return 1 + feedList.size()
                append("invoke-interface {v0}, Ljava/util/List;->size()I\n")
                append("move-result v0\n")
                append("add-int/lit8 v0, v0, 0x1\n")
                append("return v0\n")
                // :orig -> original: this.ܿ index -> l/۠ۛܳ.ܿ(I) -> l/֡ۛܳ.ܶ()
                append(":orig\n")
                append("iget v0, p0, Ll/\u1a79\u06db\u0733;->\u073f:I\n")
                append("invoke-static {v0}, Ll/\u06e0\u06db\u0733;->\u073f(I)Ll/\u05a1\u06db\u0733;\n")
                append("move-result-object v0\n")
                append("invoke-virtual {v0}, Ll/\u05a1\u06db\u0733;->\u0736()I\n")
                append("move-result v0\n")
                append("return v0\n")
            }
            val impl = MutableMethodImplementation(8)
            val compiled = InlineSmaliCompiler.Companion.compile(smali, "", 8, false)
            compiled.forEach { impl.addInstruction(it) }
            val newMethod = ImmutableMethod(
                countMethod.definingClass, countMethod.name, countMethod.parameters, countMethod.returnType,
                countMethod.accessFlags, countMethod.annotations, countMethod.hiddenApiRestrictions, impl
            )
            cls.methods.remove(countMethod)
            cls.methods.add(app.morphe.patcher.util.proxy.mutableTypes.MutableMethod(newMethod))
            println("MT Manager: l.᩹ۛܳ.۟() rebuilt self-feeding count")
        }

        // ---- 2. Rebuild l/᩹ۛܳ.ܿ(I) : breadcrumb at 0, real items after ----
        val itemMethod = cls.methods.firstOrNull {
            it.name == "\u073f" && // ܿ
                it.returnType == "Ll/\u073f\u06db\u0733;" && // l/ܿۛܳ
                it.parameterTypes.size == 1 &&
                it.parameterTypes[0] == "I"
        }
        if (itemMethod != null) {
            // Non-static 1-param: .registers 8 -> p0 (this)=v7, p1 (index)=v6; scratch v0..v5.
            val smali = buildString {
                // if p1 (index) == 0 -> return original source item 0 (breadcrumb)
                append("if-eqz p1, :orig\n")
                // v0 = this.۟.᩶.ۛ (browser current path)
                append("iget-object v0, p0, Ll/\u1a79\u06db\u0733;->\u06df:Ll/\u06d8\u1a78\u0733;\n")
                append("iget-object v0, v0, Ll/\u06d8\u1a78\u0733;->\u1a76:Ll/\u06d9\u1a78\u0733;\n")
                append("iget-object v0, v0, Ll/\u06d9\u1a78\u0733;->\u06db:Ljava/lang/String;\n")
                append("if-eqz v0, :orig\n")
                // reflection Class.forName
                append("invoke-virtual {p0}, Ljava/lang/Object;->getClass()Ljava/lang/Class;\n")
                append("move-result-object v1\n")
                append("invoke-virtual {v1}, Ljava/lang/Class;->getClassLoader()Ljava/lang/ClassLoader;\n")
                append("move-result-object v1\n")
                append("const-string v2, \"app.morphe.extension.mtmanager.MtTools\"\n")
                append("const/4 v3, 0x1\n")
                append("invoke-static {v2, v3, v1}, Ljava/lang/Class;->forName(Ljava/lang/String;ZLjava/lang/ClassLoader;)Ljava/lang/Class;\n")
                append("move-result-object v1\n")
                append("const/4 v3, 0x1\n")
                append("new-array v2, v3, [Ljava/lang/Class;\n")
                append("const-class v3, Ljava/lang/String;\n")
                append("const/4 v4, 0x0\n")
                append("aput-object v3, v2, v4\n")
                append("const-string v3, \"feedFileItems\"\n")
                append("invoke-virtual {v1, v3, v2}, Ljava/lang/Class;->getMethod(Ljava/lang/String;[Ljava/lang/Class;)Ljava/lang/reflect/Method;\n")
                append("move-result-object v1\n")
                append("const/4 v3, 0x1\n")
                append("new-array v2, v3, [Ljava/lang/Object;\n")
                append("const/4 v4, 0x0\n")
                append("aput-object v0, v2, v4\n")
                append("const/4 v3, 0x0\n")
                append("invoke-virtual {v1, v3, v2}, Ljava/lang/reflect/Method;->invoke(Ljava/lang/Object;[Ljava/lang/Object;)Ljava/lang/Object;\n")
                append("move-result-object v0\n")
                append("if-eqz v0, :orig\n")
                append("invoke-interface {v0}, Ljava/util/List;->isEmpty()Z\n")
                append("move-result v1\n")
                append("if-nez v1, :orig\n")
                // index p1 is 1-based into real list (breadcrumb is index 0)
                append("add-int/lit8 v2, p1, -0x1\n")
                append("invoke-interface {v0, v2}, Ljava/util/List;->get(I)Ljava/lang/Object;\n")
                append("move-result-object v0\n")
                append("check-cast v0, Ll/\u073f\u06db\u0733;\n")
                append("return-object v0\n")
                // :orig -> original: this.ܿ index -> l/۠ۛܳ.ܿ(I) -> l/֡ۛܳ.ܿ(I)
                append(":orig\n")
                append("iget v0, p0, Ll/\u1a79\u06db\u0733;->\u073f:I\n")
                append("invoke-static {v0}, Ll/\u06e0\u06db\u0733;->\u073f(I)Ll/\u05a1\u06db\u0733;\n")
                append("move-result-object v0\n")
                append("invoke-virtual {v0, p1}, Ll/\u05a1\u06db\u0733;->\u073f(I)Ll/\u073f\u06db\u0733;\n")
                append("move-result-object v0\n")
                append("return-object v0\n")
            }
            val impl = MutableMethodImplementation(8)
            // The item getter has 1 int param -> pass "I" so the compiler maps p1 correctly
            // (non-static, .registers 8: p0 = this = v7, p1 = index = v6, locals v0..v5).
            val compiled = InlineSmaliCompiler.Companion.compile(smali, "I", 8, false)
            compiled.forEach { impl.addInstruction(it) }
            val newMethod = ImmutableMethod(
                itemMethod.definingClass, itemMethod.name, itemMethod.parameters, itemMethod.returnType,
                itemMethod.accessFlags, itemMethod.annotations, itemMethod.hiddenApiRestrictions, impl
            )
            cls.methods.remove(itemMethod)
            cls.methods.add(app.morphe.patcher.util.proxy.mutableTypes.MutableMethod(newMethod))
            println("MT Manager: l.᩹ۛܳ.ܿ(I) rebuilt self-feeding item getter")
        }
    }
}

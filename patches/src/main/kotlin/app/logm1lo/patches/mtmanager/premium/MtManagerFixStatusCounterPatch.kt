package app.logm1lo.patches.mtmanager.premium

import app.logm1lo.patches.shared.COMPATIBILITY_MTMANAGER
import app.morphe.patcher.patch.bytecodePatch
import app.morphe.patcher.util.smali.InlineSmaliCompiler
import com.android.tools.smali.dexlib2.builder.MutableMethodImplementation
import com.android.tools.smali.dexlib2.immutable.ImmutableMethod

/**
 * Fix the file-browser status-bar counter ("Folders: 0  Files: 0").
 *
 * ROOT CAUSE: the counter text is built by `l.ۤۚܳ.ܺ۟()` which iterates
 * `this.᩷()`. `᩷()` is gated by a flattened state machine and returns the
 * EMPTY static list on the native-keyed build -> counts 0/0.
 *
 * IMPORTANT: `᩷()` is ALSO called by the file-browser load path (smali
 * lines 3548 / 4712 / 5171). A naive gate force makes `᩷()` return
 * `this.֡۟`, which is NULL on fresh install -> unmodifiableList(null) NPE
 * -> the browser fails to render. (Verified on-device: blank pane.)
 *
 * FIX: fully rebuild `l.ۤۚܳ.᩷()` (new .registers 12, p0=v11) to be
 * NULL-SAFE and SELF-FEEDING:
 *
 *   1. If `this.֡۟` is non-null && !isEmpty -> return it (native warm state).
 *   2. Otherwise resolve current path (`this.᩸۟` -> `ۜ()`) and call the
 *      MtTools extension `MtTools.feedStatusItems(path)` via reflection.
 *      If non-null -> return it (real counts on fresh installs).
 *   3. Fallback -> return Collections.emptyList() (never null, no NPE).
 *
 * Requires `extendWith("extensions/mtmanager.mpe")` so MtTools is present.
 */
@Suppress("unused")
val mtmanagerFixStatusCounterPatch = bytecodePatch(
    name = "Fix file-browser status counter (self-feeding)",
    description = "Rebuilds l.ۤۚܳ.᩷() to return the real file list (native warm state) or feed real items via the MtTools extension on fresh installs, never NPEing the browser.",
    default = true
) {
    compatibleWith(COMPATIBILITY_MTMANAGER)

    extendWith("extensions/mtmanager.mpe")

    execute {
        val cls = mutableClassDefByOrNull("Ll/\u06e4\u06da\u0733;") ?: return@execute // l.ۤۚܳ
        val method = cls.methods.firstOrNull {
            it.name == "\u1a77" && // ᩷
                it.returnType == "Ljava/util/List;" &&
                it.parameterTypes.isEmpty()
        } ?: return@execute

        // Compile the new body as smali text. Non-static 0-param:
        // .registers 12 -> p0 (this) = v11; scratch v0..v10.
        val smali = buildString {
            // Diagnostic log: confirm ᩷() is called
            append("const-string v0, \"MtTools\"\n")
            append("const-string v1, \"MT-DEBUG: l.ۤۚܳ.᩷() called\"\n")
            append("invoke-static {v0, v1}, Landroid/util/Log;->i(Ljava/lang/String;Ljava/lang/String;)I\n")
            // Always feed via MtTools (skip this.֡۟ warm-state shortcut — it can
            // hold stale native placeholder items "Android" x42 that never get
            // replaced, so the file-browser ListView would keep showing them).
            // 1) v3 = this.᩸۟ (current dir holder); v0 = v3.᩵() (real path)
            append(":feed\n")
            append("iget-object v3, p0, Ll/\u06e4\u06da\u0733;->\u1a78\u06df:Ll/\u1a76\u05a8\u0733;\n")
            append("if-eqz v3, :empty\n")
            append("invoke-virtual {v3}, Ll/\u1a76\u05a8\u0733;->\u1a75()Ljava/lang/String;\n")
            append("move-result-object v0\n")
            append("if-eqz v0, :empty\n")
            // Diagnostic: log resolved path (v0 = path)
            append("const-string v1, \"MtTools\"\n")
            append("const-string v2, \"MT-DEBUG: ᩷() path=\"\n")
            append("invoke-virtual {v2, v0}, Ljava/lang/String;->concat(Ljava/lang/String;)Ljava/lang/String;\n")
            append("move-result-object v2\n")
            append("invoke-static {v1, v2}, Landroid/util/Log;->i(Ljava/lang/String;Ljava/lang/String;)I\n")
            // 2) reflection: MtTools.feedStatusItems(path)
            append("move-object v6, v11\n")
            append("invoke-virtual {v6}, Ljava/lang/Object;->getClass()Ljava/lang/Class;\n")
            append("move-result-object v6\n")
            append("invoke-virtual {v6}, Ljava/lang/Class;->getClassLoader()Ljava/lang/ClassLoader;\n")
            append("move-result-object v6\n")
            append("const-string v1, \"app.morphe.extension.mtmanager.MtTools\"\n")
            append("const/4 v2, 0x1\n")
            append("invoke-static {v1, v2, v6}, Ljava/lang/Class;->forName(Ljava/lang/String;ZLjava/lang/ClassLoader;)Ljava/lang/Class;\n")
            append("move-result-object v1\n")
            append("const/4 v5, 0x1\n")
            append("new-array v4, v5, [Ljava/lang/Class;\n")
            append("const-class v5, Ljava/lang/String;\n")
            append("const/4 v7, 0x0\n")
            append("aput-object v5, v4, v7\n")
            append("const-string v2, \"feedStatusItems\"\n")
            append("invoke-virtual {v1, v2, v4}, Ljava/lang/Class;->getMethod(Ljava/lang/String;[Ljava/lang/Class;)Ljava/lang/reflect/Method;\n")
            append("move-result-object v1\n")
            append("const/4 v5, 0x1\n")
            append("new-array v4, v5, [Ljava/lang/Object;\n")
            append("const/4 v7, 0x0\n")
            append("aput-object v0, v4, v7\n")
            append("const/4 v2, 0x0\n")
            append("invoke-virtual {v1, v2, v4}, Ljava/lang/reflect/Method;->invoke(Ljava/lang/Object;[Ljava/lang/Object;)Ljava/lang/Object;\n")
            append("move-result-object v0\n")
            append("if-eqz v0, :empty\n")
            append("return-object v0\n")
            // 3) fallback: emptyList()
            append(":empty\n")
            append("invoke-static {}, Ljava/util/Collections;->emptyList()Ljava/util/List;\n")
            append("move-result-object v0\n")
            append("return-object v0\n")
        }

        val impl = MutableMethodImplementation(12)
        // compile with explicit params ("" = non-static 0-param) and register count 12,
        // so `p0` is typed as `this` (v11) by the verifier.
        val compiled = InlineSmaliCompiler.Companion.compile(smali, "", 12, false)
        compiled.forEach { impl.addInstruction(it) }

        val newMethod = ImmutableMethod(
            method.definingClass, method.name, method.parameters, method.returnType,
            method.accessFlags, method.annotations, method.hiddenApiRestrictions, impl
        )
        cls.methods.remove(method)
        cls.methods.add(app.morphe.patcher.util.proxy.mutableTypes.MutableMethod(newMethod))
        println("MT Manager: l.ۤۚܳ.᩷() rebuilt self-feeding (native list -> MtTools.feedStatusItems -> emptyList)")

        // ---- Also rebuild l/ۤۚܳ.۟(I) (item getter) to self-feed ----
        // The file-browser row adapter binds via l/ۤۚܳ.۟(I) which reads this.֡۟.
        // On a fresh install that list is empty, so rows get null items -> no
        // file names. Mirror ᩷(): return this.֡۟.get(I) when populated, else
        // feed via MtTools.feedStatusItems(path) and return list.get(I).
        val itemMethod = cls.methods.firstOrNull {
            it.name == "\u06df" && // ۟
                it.returnType == "Ll/\u1a76\u1a7a\u0733;" // l/᩶᩺ܳ
        }
        if (itemMethod != null) {
            println("MT Manager: l.ۤۚܳ.۟(I) item getter found, params=" + itemMethod.parameterTypes)
            val itemSmali = buildString {
                // Diagnostic log: confirm ۟(I) is called (use v0..v5 free locals only)
                append("const-string v0, \"MtTools\"\n")
                append("const-string v1, \"MT-DEBUG: ۟(I) ENTER p1=\"\n")
                append("invoke-static {p1}, Ljava/lang/String;->valueOf(I)Ljava/lang/String;\n")
                append("move-result-object v2\n")
                append("invoke-virtual {v1, v2}, Ljava/lang/String;->concat(Ljava/lang/String;)Ljava/lang/String;\n")
                append("move-result-object v2\n")
                append("invoke-static {v0, v2}, Landroid/util/Log;->i(Ljava/lang/String;Ljava/lang/String;)I\n")
                // Save the index param (p1) into a free local v5 — the reflection
                // below needs a scratch constant for array indices and would
                // otherwise clobber p1 (which IS the index in .registers 8).
                append("move v5, p1\n")
                // Always feed via MtTools (skip this.֡۟ warm-state shortcut).
                // 1) :feed -> resolve path, call MtTools.feedStatusItems
                append(":feed\n")
                append("iget-object v3, p0, Ll/\u06e4\u06da\u0733;->\u1a78\u06df:Ll/\u1a76\u05a8\u0733;\n")
                append("if-eqz v3, :null\n")
                append("invoke-virtual {v3}, Ll/\u1a76\u05a8\u0733;->\u1a75()Ljava/lang/String;\n")
                append("move-result-object v0\n")
                append("if-eqz v0, :null\n")
                append("move-object v6, p0\n")
                append("invoke-virtual {v6}, Ljava/lang/Object;->getClass()Ljava/lang/Class;\n")
                append("move-result-object v6\n")
                append("invoke-virtual {v6}, Ljava/lang/Class;->getClassLoader()Ljava/lang/ClassLoader;\n")
                append("move-result-object v6\n")
                append("const-string v1, \"app.morphe.extension.mtmanager.MtTools\"\n")
                append("const/4 v2, 0x1\n")
                append("invoke-static {v1, v2, v6}, Ljava/lang/Class;->forName(Ljava/lang/String;ZLjava/lang/ClassLoader;)Ljava/lang/Class;\n")
                append("move-result-object v1\n")
                append("const/4 v2, 0x1\n")
                append("new-array v4, v2, [Ljava/lang/Class;\n")
                append("const-class v3, Ljava/lang/String;\n")
                append("const/4 v2, 0x0\n")
                append("aput-object v3, v4, v2\n")
                append("const-string v2, \"feedStatusItems\"\n")
                append("invoke-virtual {v1, v2, v4}, Ljava/lang/Class;->getMethod(Ljava/lang/String;[Ljava/lang/Class;)Ljava/lang/reflect/Method;\n")
                append("move-result-object v1\n")
                append("const/4 v2, 0x1\n")
                append("new-array v4, v2, [Ljava/lang/Object;\n")
                append("const/4 v2, 0x0\n")
                append("aput-object v0, v4, v2\n")
                append("const/4 v2, 0x0\n")
                append("invoke-virtual {v1, v2, v4}, Ljava/lang/reflect/Method;->invoke(Ljava/lang/Object;[Ljava/lang/Object;)Ljava/lang/Object;\n")
                append("move-result-object v0\n")
                append("if-eqz v0, :null\n")
                append("invoke-interface {v0}, Ljava/util/List;->size()I\n")
                append("move-result v1\n")
                append("if-le v1, v5, :null\n")
                append("invoke-interface {v0, v5}, Ljava/util/List;->get(I)Ljava/lang/Object;\n")
                append("move-result-object v0\n")
                append("check-cast v0, Ll/\u1a76\u1a7a\u0733;\n")
                // Diagnostic: log returned item toString + index
                append("const-string v1, \"MtTools\"\n")
                append("const-string v2, \"MT-DEBUG: ۟(I) idx=\"\n")
                append("invoke-static {v5}, Ljava/lang/String;->valueOf(I)Ljava/lang/String;\n")
                append("move-result-object v3\n")
                append("invoke-virtual {v2, v3}, Ljava/lang/String;->concat(Ljava/lang/String;)Ljava/lang/String;\n")
                append("move-result-object v3\n")
                append("const-string v2, \" item=\"\n")
                append("invoke-virtual {v3, v2}, Ljava/lang/String;->concat(Ljava/lang/String;)Ljava/lang/String;\n")
                append("move-result-object v3\n")
                append("invoke-virtual {v0}, Ljava/lang/Object;->toString()Ljava/lang/String;\n")
                append("move-result-object v2\n")
                append("invoke-virtual {v3, v2}, Ljava/lang/String;->concat(Ljava/lang/String;)Ljava/lang/String;\n")
                append("move-result-object v3\n")
                append("invoke-static {v1, v3}, Landroid/util/Log;->i(Ljava/lang/String;Ljava/lang/String;)I\n")
                append("return-object v0\n")
                // 2) :null -> return null
                append(":null\n")
                append("const/4 v0, 0x0\n")
                append("return-object v0\n")
            }
            val itemImpl = MutableMethodImplementation(8)
            // non-static 1-param: .registers 8 -> p0=this=v7, p1=index=v6
            val compiledItem = InlineSmaliCompiler.Companion.compile(itemSmali, "I", 8, false)
            compiledItem.forEach { itemImpl.addInstruction(it) }
            val newItemMethod = ImmutableMethod(
                itemMethod.definingClass, itemMethod.name, itemMethod.parameters, itemMethod.returnType,
                itemMethod.accessFlags, itemMethod.annotations, itemMethod.hiddenApiRestrictions, itemImpl
            )
            cls.methods.remove(itemMethod)
            cls.methods.add(app.morphe.patcher.util.proxy.mutableTypes.MutableMethod(newItemMethod))
            println("MT Manager: l.ۤۚܳ.۟(I) rebuilt self-feeding item getter")
        }
    }
}

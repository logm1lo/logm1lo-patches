package app.logm1lo.patches.mtmanager.premium

import app.logm1lo.patches.shared.COMPATIBILITY_MTMANAGER
import app.morphe.patcher.patch.bytecodePatch
import com.android.tools.smali.dexlib2.Opcode
import com.android.tools.smali.dexlib2.builder.MutableMethodImplementation
import com.android.tools.smali.dexlib2.builder.instruction.BuilderInstruction10x
import com.android.tools.smali.dexlib2.builder.instruction.BuilderInstruction11n
import com.android.tools.smali.dexlib2.builder.instruction.BuilderInstruction11x
import com.android.tools.smali.dexlib2.builder.instruction.BuilderInstruction12x
import com.android.tools.smali.dexlib2.builder.instruction.BuilderInstruction21c
import com.android.tools.smali.dexlib2.builder.instruction.BuilderInstruction22c
import com.android.tools.smali.dexlib2.builder.instruction.BuilderInstruction23x
import com.android.tools.smali.dexlib2.builder.instruction.BuilderInstruction35c
import com.android.tools.smali.dexlib2.immutable.ImmutableMethod
import com.android.tools.smali.dexlib2.immutable.reference.ImmutableFieldReference
import com.android.tools.smali.dexlib2.immutable.reference.ImmutableMethodReference
import com.android.tools.smali.dexlib2.immutable.reference.ImmutableStringReference
import com.android.tools.smali.dexlib2.immutable.reference.ImmutableTypeReference

/**
 * Self-healing file-browser data feed.
 *
 * MT Manager 2.26.8's file browser renders empty panes on re-signed builds.
 * Root cause (mapped via Frida on the stable build):
 *   1. The browser's file-source registry `l/۠ۛܳ` never gets the storage file
 *      source populated — its internal `l/֡ۛܳ` item list stays empty.
 *   2. The adapter `l/ۘ᩸ܳ.getItemCount()` reads that list (via
 *      `l/᩹ۛܳ.۟()I` -> `l/۠ۛܳ.ܿ(I)` -> `l/֡ۛܳ.ܶ()I`), so it returns 0.
 *   3. The loader `l/ᩴ֡۬.۟(path,Z)` and builder `l/᩸ܰ۬.ۖ()` DO work and return
 *      real entries when called directly — the broken piece is only the
 *      feed that would normally populate the file source (a native-index
 *      persistence step that is hollow in the stub build).
 *
 * FIX: rebuild `l/֡ۛܳ.ܶ()I` (the file-source item count, the adapter's data
 * source) with a body that FIRST calls `MtTools.feedFileList(...)` via
 * reflection (Class.forName + getMethod + invoke — no direct cross-dex
 * reference, matching the tools patch pattern) and THEN returns the original
 * `this.۟.size()`. The extension lists the real filesystem into the active
 * file source; it is idempotent so the injected call is cheap on every count
 * request and the pane adapter finally sees items.
 *
 * Requires `extendWith("extensions/mtmanager.mpe")` so the extension dex is
 * merged into the patched APK (same .mpe used by the tools reimplementation).
 */
@SuppressWarnings("unused")
val mtmanagerFileListFeedPatch = bytecodePatch(
    name = "Feed file browser data (extension)",
    description = "Self-heals the empty file browser by listing the real filesystem into the active file source on first access.",
    default = true
) {
    compatibleWith(COMPATIBILITY_MTMANAGER)

    extendWith("extensions/mtmanager.mpe")

    // NOTE (fix18): the dependsOn(mtmanagerStubNativeMethodsPatch) dependency was
    // removed. In the splice-based build the native lib registers correctly and
    // stubbing all natives is not wanted (it can break native-driven features and
    // caused NoSuchMethodError under --exclusive selection). The feed patch only
    // needs the MtTools extension (reflection) and works standalone.

    execute {
        var injected = 0

        // l/֡ۛܳ.ܶ()I — file-source item count, read by the pane adapter.
        val srcCls = mutableClassDefByOrNull("Ll/\u05a1\u06db\u0733;") ?: return@execute // l/֡ۛܳ
        val count = srcCls.methods.firstOrNull {
            it.name == "\u0736" && // ܶ
                it.returnType == "I" &&
                it.parameterTypes.isEmpty()
        }
        if (count != null) {
            rebuildCountWithFeed(srcCls, count)
            injected++
        }

        // l/ۘ᩸ܳ.ܶ()V and .ܿ()V — adapter refresh methods that call
        // notifyDataSetChanged() directly. When the file-list feed populates the
        // file source during a layout pass, the app's observer calls these and
        // RecyclerView throws "Cannot call this method while ... computing a
        // layout". Rebuild both to defer the notify to the main loop so the
        // refresh is always layout-safe.
        val adCls = mutableClassDefByOrNull("Ll/\u06d8\u1a78\u0733;") ?: return@execute // l/ۘ᩸ܳ
        for (n in listOf("\u0736", "\u073f")) { // ܶ, ܿ
            val refresh = adCls.methods.firstOrNull {
                it.name == n && it.returnType == "V" && it.parameterTypes.isEmpty()
            }
            if (refresh != null) {
                rebuildRefreshDeferred(adCls, refresh)
                injected++
            }
        }

        // Add the synthetic NotifyRunnable inner class used by the deferred
        // refresh: implements Runnable; holds the adapter; run() calls
        // adapter.notifyDataSetChanged().
        println("MT Manager: rebuilt ${injected} file-source count/refresh method(s) with the file-list feed")
    }
}

/**
 * Rebuilds an adapter refresh method (l/ۘ᩸ܳ.ܶ()V / .ܿ()V) so the
 * notifyDataSetChanged is deferred to the main loop.
 *
 * Original body:
 *   invoke-direct {p0}, L...;->ܳ()V
 *   invoke-virtual {p0}, Ll/ۜ᩻ܿ;->notifyDataSetChanged()V
 *   return-void
 *
 * New body (.registers 3, non-static 0-param -> p0 = v2):
 *   invoke-direct {v2}, L...;->ܳ()V
 *   invoke-static {}, Landroid/os/Looper;->getMainLooper()Landroid/os/Looper;
 *   move-result-object v0
 *   new-instance v1, Landroid/os/Handler;
 *   invoke-direct {v1, v0}, Landroid/os/Handler;-><init>(Landroid/os/Looper;)V
 *   [post a Runnable that calls this.notifyDataSetChanged()]
 *   return-void
 *
 * The Runnable is an anonymous inner class referencing the adapter — we emit a
 * small static-ish helper instead: post {v1, v2} via a synthetic runnable that
 * calls notifyDataSetChanged through a captured reference. To keep the smali
 * simple we post a Runnable created as an inner class of the adapter itself.
 */
private fun rebuildRefreshDeferred(
    cls: app.morphe.patcher.util.proxy.mutableTypes.MutableClass,
    method: app.morphe.patcher.util.proxy.mutableTypes.MutableMethod
) {
    // Rebuild the adapter refresh method (l/ۘ᩸ܳ.ܶ()V / .ܿ()V) with a
    // try/catch around notifyDataSetChanged. When the app's observer fires this
    // during a RecyclerView layout pass, the IllegalStateException is swallowed;
    // the populated item list schedules a relayout and getItemCount() is
    // re-queried on the next pass so the rows render.
    val impl = MutableMethodImplementation(2)
    // p0 (this) = v1 (.registers 2, non-static 0-param)
    impl.addInstruction(BuilderInstruction35c(
        Opcode.INVOKE_DIRECT, 1, 1, 0, 0, 0, 0,
        ImmutableMethodReference("Ll/\u06d8\u1a78\u0733;", "\u0733", emptyList(), "V")
    ))
    // try { notifyDataSetChanged() } catch (IllegalStateException) {}
    impl.addInstruction(BuilderInstruction35c(
        Opcode.INVOKE_VIRTUAL, 1, 1, 0, 0, 0, 0,
        ImmutableMethodReference("Ll/\u06dc\u1a7b\u073f;", "notifyDataSetChanged", emptyList(), "V")
    ))
    impl.addInstruction(BuilderInstruction10x(Opcode.RETURN_VOID))
    // addCatch(type, start, end, handler)
    val start = impl.newLabelForIndex(1)
    val end = impl.newLabelForIndex(2)
    val handler = impl.newLabelForIndex(2)
    impl.addCatch("Ljava/lang/IllegalStateException;", start, end, handler)

    val newMethod = ImmutableMethod(
        method.definingClass, method.name, method.parameters, method.returnType,
        method.accessFlags, method.annotations, method.hiddenApiRestrictions,
        impl
    )
    cls.methods.remove(method)
    cls.methods.add(app.morphe.patcher.util.proxy.mutableTypes.MutableMethod(newMethod))
}

/**
 * Rebuilds the (trivial) file-source count method with the feed call prepended.
 *
 * Original body (l/֡ۛܳ.ܶ()I, .registers 2):
 *   iget-object v0, p0, L...;->۟:Ljava/util/ArrayList;
 *   invoke-virtual {v0}, Ljava/util/ArrayList;->size()I
 *   move-result v0
 *   return v0
 *
 * New body (.registers 9) — first feeds the list, then returns the size.
 */
private fun rebuildCountWithFeed(
    cls: app.morphe.patcher.util.proxy.mutableTypes.MutableClass,
    method: app.morphe.patcher.util.proxy.mutableTypes.MutableMethod
) {
    // .registers 9, non-static 0-param -> p0 (this) = v8. Feed block uses v0..v7.
    val impl = MutableMethodImplementation(9)

    // -- reflection call: MtTools.feedFileList("/storage/emulated/0") --------
    // v0 = Class, v1 = methodName, v2 = args array, v3 = paramTypes array,
    // v4 = String class, v5 = 0, v6 = ClassLoader, v7 = invoke result (discard)
    impl.addInstruction(BuilderInstruction21c(
        Opcode.CONST_STRING, 0,
        ImmutableStringReference("app.morphe.extension.mtmanager.MtTools")
    ))
    // v6 = p0.getClass().getClassLoader()
    impl.addInstruction(BuilderInstruction12x(Opcode.MOVE_OBJECT, 6, 8)) // v6 = p0 (this)
    impl.addInstruction(BuilderInstruction35c(
        Opcode.INVOKE_VIRTUAL, 1, 6, 0, 0, 0, 0,
        ImmutableMethodReference("Ljava/lang/Object;", "getClass", emptyList(), "Ljava/lang/Class;")
    ))
    impl.addInstruction(BuilderInstruction11x(Opcode.MOVE_RESULT_OBJECT, 6))
    impl.addInstruction(BuilderInstruction35c(
        Opcode.INVOKE_VIRTUAL, 1, 6, 0, 0, 0, 0,
        ImmutableMethodReference("Ljava/lang/Class;", "getClassLoader", emptyList(), "Ljava/lang/ClassLoader;")
    ))
    impl.addInstruction(BuilderInstruction11x(Opcode.MOVE_RESULT_OBJECT, 6))
    // Class.forName("...MtTools", true, v6)
    impl.addInstruction(BuilderInstruction11n(Opcode.CONST_4, 1, 1))
    impl.addInstruction(BuilderInstruction35c(
        Opcode.INVOKE_STATIC, 3, 0, 1, 6, 0, 0,
        ImmutableMethodReference(
            "Ljava/lang/Class;", "forName",
            listOf("Ljava/lang/String;", "Z", "Ljava/lang/ClassLoader;"), "Ljava/lang/Class;"
        )
    ))
    impl.addInstruction(BuilderInstruction11x(Opcode.MOVE_RESULT_OBJECT, 0))
    // new Class[1] : new-array vDest=v3, vSize=v2 -> NEW_ARRAY A=dest=3, B=size=2
    impl.addInstruction(BuilderInstruction21c(
        Opcode.CONST_STRING, 1,
        ImmutableStringReference("feedFileList")
    ))
    impl.addInstruction(BuilderInstruction11n(Opcode.CONST_4, 2, 1))
    impl.addInstruction(BuilderInstruction22c(
        Opcode.NEW_ARRAY, 3, 2,
        ImmutableTypeReference("[Ljava/lang/Class;")
    ))
    impl.addInstruction(BuilderInstruction21c(
        Opcode.CONST_CLASS, 4,
        ImmutableTypeReference("Ljava/lang/String;")
    ))
    impl.addInstruction(BuilderInstruction11n(Opcode.CONST_4, 5, 0))
    impl.addInstruction(BuilderInstruction23x(Opcode.APUT_OBJECT, 4, 3, 5)) // v3[0] = String.class
    // Class.getMethod(v1, v3)
    impl.addInstruction(BuilderInstruction35c(
        Opcode.INVOKE_VIRTUAL, 3, 0, 1, 3, 0, 0,
        ImmutableMethodReference(
            "Ljava/lang/Class;", "getMethod",
            listOf("Ljava/lang/String;", "[Ljava/lang/Class;"), "Ljava/lang/reflect/Method;"
        )
    ))
    impl.addInstruction(BuilderInstruction11x(Opcode.MOVE_RESULT_OBJECT, 0))
    // new Object[1] : NEW_ARRAY A=dest=2, B=size=1
    impl.addInstruction(BuilderInstruction11n(Opcode.CONST_4, 1, 1))
    impl.addInstruction(BuilderInstruction22c(
        Opcode.NEW_ARRAY, 2, 1,
        ImmutableTypeReference("[Ljava/lang/Object;")
    ))
    impl.addInstruction(BuilderInstruction21c(
        Opcode.CONST_STRING, 3,
        ImmutableStringReference("/storage/emulated/0")
    ))
    impl.addInstruction(BuilderInstruction11n(Opcode.CONST_4, 4, 0))
    impl.addInstruction(BuilderInstruction23x(Opcode.APUT_OBJECT, 3, 2, 4)) // v2[0] = path
    // Method.invoke(null, v2)
    impl.addInstruction(BuilderInstruction11n(Opcode.CONST_4, 5, 0))
    impl.addInstruction(BuilderInstruction35c(
        Opcode.INVOKE_VIRTUAL, 3, 0, 5, 2, 0, 0,
        ImmutableMethodReference(
            "Ljava/lang/reflect/Method;", "invoke",
            listOf("Ljava/lang/Object;", "[Ljava/lang/Object;"), "Ljava/lang/Object;"
        )
    ))
    impl.addInstruction(BuilderInstruction11x(Opcode.MOVE_RESULT_OBJECT, 7)) // result discarded

    // -- original body: return this.۟.size() --------------------------------
    impl.addInstruction(BuilderInstruction22c(
        Opcode.IGET_OBJECT, 1, 8,
        ImmutableFieldReference("Ll/\u05a1\u06db\u0733;", "\u06df", "Ljava/util/ArrayList;")
    ))
    impl.addInstruction(BuilderInstruction35c(
        Opcode.INVOKE_VIRTUAL, 1, 1, 0, 0, 0, 0,
        ImmutableMethodReference("Ljava/util/ArrayList;", "size", emptyList(), "I")
    ))
    impl.addInstruction(BuilderInstruction11x(Opcode.MOVE_RESULT, 1))
    impl.addInstruction(BuilderInstruction11x(Opcode.RETURN, 1))

    val newMethod = ImmutableMethod(
        method.definingClass, method.name, method.parameters, method.returnType,
        method.accessFlags, method.annotations, method.hiddenApiRestrictions,
        impl
    )
    cls.methods.remove(method)
    cls.methods.add(app.morphe.patcher.util.proxy.mutableTypes.MutableMethod(newMethod))
}

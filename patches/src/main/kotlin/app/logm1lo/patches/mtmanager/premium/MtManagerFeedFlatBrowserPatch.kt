package app.logm1lo.patches.mtmanager.premium

import app.logm1lo.patches.shared.COMPATIBILITY_MTMANAGER
import app.morphe.patcher.extensions.InstructionExtensions.addInstructionsWithLabels
import app.morphe.patcher.patch.bytecodePatch

/**
 * Feed the FLAT file browser (dual-pane, stock layout) with the real directory
 * listing via MtTools.feedFlatIndex(fragment).
 *
 * The flat browser fragment l/ܰۨܶ renders rows from the native file-index
 * (l/֡ۨܶ.ܳ -> l/ᩴܿ᩶ -> l/ۚܶܶ store) which is NOT rebuilt on splice builds
 * (stale "Android" placeholder rows). The extension helper populates the store
 * with real file entries via reflection.
 *
 * Splice-safe: pure Java injection, no native methods touched.
 * Pairs with removing the slide-panel patches so the app shows the stock
 * flat dual-pane layout (hamburger + path bar, no History/Bookmarks tabs).
 */
@SuppressWarnings("unused")
val mtmanagerFeedFlatBrowserPatch = bytecodePatch(
    name = "Feed flat file browser (extension)",
    description = "Populates the flat dual-pane browser's native file-index with the real directory listing via the MtTools extension.",
    default = true
) {
    compatibleWith(COMPATIBILITY_MTMANAGER)

    extendWith("extensions/mtmanager.mpe")

    execute {
        val cls = mutableClassDefByOrNull("Ll/\u0730\u06e8\u0736;") ?: return@execute // l/ܰۨܶ
        val onCreate = cls.methods.firstOrNull {
            it.name == "onCreate" && it.parameterTypes.size == 1
        } ?: return@execute
        val impl = onCreate.implementation ?: return@execute

        // Inject the reflection feed call at position 1 (right after the initial
        // invoke-super). addInstructionsWithLabels performs register allocation,
        // so the smali can reference p0/p1 and arbitrary v-registers safely.
        val smali = """
            invoke-virtual {p0}, Ljava/lang/Object;->getClass()Ljava/lang/Class;
            move-result-object v0
            invoke-virtual {v0}, Ljava/lang/Class;->getClassLoader()Ljava/lang/ClassLoader;
            move-result-object v0
            const-string v1, "app.morphe.extension.mtmanager.MtTools"
            const/4 v2, 0x1
            invoke-static {v1, v2, v0}, Ljava/lang/Class;->forName(Ljava/lang/String;ZLjava/lang/ClassLoader;)Ljava/lang/Class;
            move-result-object v0
            const/4 v2, 0x1
            new-array v1, v2, [Ljava/lang/Class;
            const-class v2, Ljava/lang/Object;
            const/4 v3, 0x0
            aput-object v2, v1, v3
            const-string v2, "feedFlatIndex"
            invoke-virtual {v0, v2, v1}, Ljava/lang/Class;->getMethod(Ljava/lang/String;[Ljava/lang/Class;)Ljava/lang/reflect/Method;
            move-result-object v0
            const/4 v2, 0x1
            new-array v1, v2, [Ljava/lang/Object;
            const/4 v2, 0x0
            aput-object p0, v1, v2
            const/4 v2, 0x0
            invoke-virtual {v0, v2, v1}, Ljava/lang/reflect/Method;->invoke(Ljava/lang/Object;[Ljava/lang/Object;)Ljava/lang/Object;
        """.trimIndent()
        onCreate.addInstructionsWithLabels(1, smali)
        println("MT Manager: injected feedFlatIndex into l/ܰۨܶ.onCreate")
    }
}

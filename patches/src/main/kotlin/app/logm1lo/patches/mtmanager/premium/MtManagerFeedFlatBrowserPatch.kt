package app.logm1lo.patches.mtmanager.premium

import app.logm1lo.patches.shared.COMPATIBILITY_MTMANAGER
import app.morphe.patcher.extensions.InstructionExtensions.addInstruction
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
 * INJECTION APPROACH (v2): direct `invoke-static` to MtTools, NOT reflection.
 * The v1 reflection chain used a scratch register (v3) that collides with the
 * `this` parameter register (p0 = v3) in onCreate's .registers 5 layout,
 * clobbering `this` to null. A direct static call uses only p0 (arg) + v0
 * (move-result), with zero register collision.
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

        // Inject a DIRECT static call to MtTools.feedFlatIndex(this) at position
        // 1 (right after invoke-super). feedFlatIndex(Object) returns int, so we
        // consume it with move-result v0. Direct reference resolves because the
        // MtTools extension is merged into the same dex (classes7) as this class.
        onCreate.addInstruction(1, "invoke-static {p0}, Lapp/morphe/extension/mtmanager/MtTools;->feedFlatIndex(Ljava/lang/Object;)I")
        onCreate.addInstruction(2, "move-result v0")

        println("MT Manager: injected feedFlatIndex direct call into l/ܰۨܶ.onCreate")
    }
}

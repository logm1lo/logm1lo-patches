package app.logm1lo.patches.mtmanager.premium

import app.logm1lo.patches.shared.COMPATIBILITY_MTMANAGER
import app.morphe.patcher.extensions.InstructionExtensions.addInstructionsWithLabels
import app.morphe.patcher.patch.bytecodePatch

/**
 * Re-implements MT Manager's hollow native conversion tools via the bundled
 * `extensions/mtmanager.mpe` Java extension.
 *
 * The Morphe patch stubs all `l.*` natives (see Stub native methods patch),
 * which leaves the conversion tools "hollow" — Dex2Smali, Dex2Jar and Sign
 * open but never produce output because their kernels were native.
 *
 * This patch wires the app's existing dispatch points to pure-Java
 * reimplementations shipped in the .mpe extension:
 *
 * 1. Conversion bridge `Ll/᩸ۖ֡;->۟(Ll/᩸ۖ֡;Ljava/lang/String;Ljava/lang/String;Ll/ܺܽܺ;)V`
 *    Both the Dex2Smali task (`l/ۙۖ֡`) and the Dex2Jar task (`l/ۛۖ֡`) call this
 *    static bridge with the resolved input and output paths. We inject a call
 *    to `MtTools.dispatch(in, out)` at the top. The extension chooses the tool
 *    by output extension (.jar -> Dex2Jar, .zip -> Dex2Smali, .apk -> Sign)
 *    and performs the conversion; if it returns true we skip the broken
 *    native flow entirely.
 *
 * 2. File-tool dispatcher `Ll/᩸ۖ֡;->ܿ(Ll/᩸ۖ֡;Ljava/lang/String;)V`
 *    Called with a selected file path before routing to the tool chooser.
 *    We inject `MtTools.dispatch(path, path)` so an .apk selection is signed
 *    immediately (sibling `_signed.apk`) instead of entering the hollow
 *    chooser. Non-apk paths fall through unchanged.
 *
 * Requires `extendWith("extensions/mtmanager.mpe")` so the extension dex is
 * merged into the patched APK.
 */
@Suppress("unused")
val mtmanagerToolsExtensionPatch = bytecodePatch(
    name = "Reimplement native tools (extension)",
    description = "Reroutes Dex2Smali / Dex2Jar / Sign to pure-Java reimplementations bundled in the mtmanager extension.",
    default = true
) {
    compatibleWith(COMPATIBILITY_MTMANAGER)

    extendWith("extensions/mtmanager.mpe")

    execute {
        // --- 1. Conversion bridge -------------------------------------------
        var bridgeInjected = 0
        MtConversionBridgeFingerprint.matchAllOrNull()?.forEach { match ->
            val method = match.method
            val impl = method.implementation ?: return@forEach
            // The bridge is tiny: v0 is free at entry (register count 10).
            method.addInstructionsWithLabels(
                0,
                """
                    invoke-static {p1, p2}, Lcom/morphe/extension/mtmanager/MtTools;->dispatch(Ljava/lang/String;Ljava/lang/String;)Z
                    move-result v0
                    if-eqz v0, :original
                    return-void
                    :original
                    nop
                """
            )
            bridgeInjected++
        }
        println("MT Manager Tools Extension: injected dispatch into $bridgeInjected conversion bridge(s)")

        // --- 2. File-tool dispatcher (Sign on .apk selection) ---------------
        var dispatcherInjected = 0
        MtFileDispatcherFingerprint.matchAllOrNull()?.forEach { match ->
            val method = match.method
            method.addInstructionsWithLabels(
                0,
                """
                    invoke-static {p1, p1}, Lcom/morphe/extension/mtmanager/MtTools;->dispatch(Ljava/lang/String;Ljava/lang/String;)Z
                    move-result v0
                    if-eqz v0, :original
                    return-void
                    :original
                    nop
                """
            )
            dispatcherInjected++
        }
        println("MT Manager Tools Extension: injected dispatch into $dispatcherInjected file-tool dispatcher(s)")
    }
}

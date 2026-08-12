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
 * reimplementations shipped in the .mpe extension.
 *
 * Frida-verified dispatch path (Dex2Smali on test.dex.bak via "Open with"):
 *
 *   l/ᩴۜܶ.<init>(descriptor)              <- tool runner built with descriptor
 *   l/ᩴۜܶ.ܳ()                             <- executor: reads descriptor, writes output
 *   l/ۙۧ᩶.<init>(test.dex_smali.zip)      <- native writer (stubbed -> empty zip)
 *
 * The single choke point is `Ll/ᩴۜܶ;->ܳ()V`. At its entry the descriptor
 * (field `ۤ`, type `Ll/۫ۜܶ;`) is already set, so we can read:
 *   - descriptor.ۧ()  -> tool name ("Dex2Smali" / "Dex2Jar" / ...)
 *   - descriptor.۟()  -> selected file node (Ll/ܳ۫ۧ;)
 *   - file.֫۟()       -> input path string
 * and hand both to `MtTools.dispatchByName(name, path)` which performs the
 * pure-Java conversion and writes the real output file. If it returns true
 * we return-void, skipping the broken native flow entirely.
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
        // --- 1. Universal tool runner Ll/ᩴۜܶ;->ܳ()V ------------------------
        var runnerInjected = 0
        MtToolRunnerFingerprint.matchAllOrNull()?.forEach { match ->
            val method = match.method
            // .registers 28 -> v0..v6 free at index 0. We call MtTools via
            // reflection (Class.forName with the runner's OWN classloader) so
            // the runner dex does NOT reference the extension class directly —
            // this avoids cross-dex verifier failures on MT Manager's
            // protected classloader while still resolving the class at runtime.
            method.addInstructionsWithLabels(
                0,
                """
                    move-object/from16 v1, p0
                    invoke-virtual {v1}, Ljava/lang/Object;->getClass()Ljava/lang/Class;
                    move-result-object v0
                    invoke-virtual {v0}, Ljava/lang/Class;->getClassLoader()Ljava/lang/ClassLoader;
                    move-result-object v6
                    const-string v0, "app.morphe.extension.mtmanager.MtTools"
                    const/4 v2, 0x1
                    invoke-static {v0, v2, v6}, Ljava/lang/Class;->forName(Ljava/lang/String;ZLjava/lang/ClassLoader;)Ljava/lang/Class;
                    move-result-object v0
                    const-string v2, "dispatchRunner"
                    const/4 v3, 0x1
                    new-array v3, v3, [Ljava/lang/Class;
                    const-class v4, Ljava/lang/Object;
                    const/4 v5, 0x0
                    aput-object v4, v3, v5
                    invoke-virtual {v0, v2, v3}, Ljava/lang/Class;->getMethod(Ljava/lang/String;[Ljava/lang/Class;)Ljava/lang/reflect/Method;
                    move-result-object v0
                    const/4 v2, 0x1
                    new-array v2, v2, [Ljava/lang/Object;
                    aput-object v1, v2, v5
                    const/4 v1, 0x0
                    invoke-virtual {v0, v1, v2}, Ljava/lang/reflect/Method;->invoke(Ljava/lang/Object;[Ljava/lang/Object;)Ljava/lang/Object;
                    move-result-object v0
                    if-eqz v0, :original
                    check-cast v0, Ljava/lang/Boolean;
                    invoke-virtual {v0}, Ljava/lang/Boolean;->booleanValue()Z
                    move-result v0
                    if-eqz v0, :original
                    return-void
                    :original
                    nop
                """
            )
            runnerInjected++
        }
        println("MT Manager Tools Extension: injected dispatch into $runnerInjected tool runner(s)")

        // --- 3. Sign tool dispatcher Ll/ܿۜܶ;->۟(p1,p2,...) -------------------
        var signInjected = 0
        MtSignDispatcherFingerprint.matchAllOrNull()?.forEach { match ->
            val method = match.method
            // .registers 60 -> v0..v7 free at index 0. Reflection call via
            // MtTools.dispatchSign(inputNode, outputNode) so the dispatcher dex
            // does not reference the extension class directly (avoids the
            // cross-dex verifier failure on MT Manager's classloader).
            // Registers used: v0=input, v1=output, v2=Class, v3=methodName,
            // v4=paramTypes, v5=Object, v6=ClassLoader, v7=0/result.
            method.addInstructionsWithLabels(
                0,
                """
                    move-object/from16 v0, p1
                    move-object/from16 v1, p2
                    invoke-virtual {v0}, Ljava/lang/Object;->getClass()Ljava/lang/Class;
                    move-result-object v6
                    invoke-virtual {v6}, Ljava/lang/Class;->getClassLoader()Ljava/lang/ClassLoader;
                    move-result-object v6
                    const-string v2, "app.morphe.extension.mtmanager.MtTools"
                    const/4 v3, 0x1
                    invoke-static {v2, v3, v6}, Ljava/lang/Class;->forName(Ljava/lang/String;ZLjava/lang/ClassLoader;)Ljava/lang/Class;
                    move-result-object v2
                    const-string v3, "dispatchSign"
                    const/4 v4, 0x2
                    new-array v4, v4, [Ljava/lang/Class;
                    const-class v5, Ljava/lang/Object;
                    const/4 v7, 0x0
                    aput-object v5, v4, v7
                    const/4 v7, 0x1
                    aput-object v5, v4, v7
                    invoke-virtual {v2, v3, v4}, Ljava/lang/Class;->getMethod(Ljava/lang/String;[Ljava/lang/Class;)Ljava/lang/reflect/Method;
                    move-result-object v2
                    const/4 v3, 0x2
                    new-array v3, v3, [Ljava/lang/Object;
                    const/4 v7, 0x0
                    aput-object v0, v3, v7
                    const/4 v7, 0x1
                    aput-object v1, v3, v7
                    const/4 v0, 0x0
                    invoke-virtual {v2, v0, v3}, Ljava/lang/reflect/Method;->invoke(Ljava/lang/Object;[Ljava/lang/Object;)Ljava/lang/Object;
                    move-result-object v0
                    if-eqz v0, :original
                    check-cast v0, Ljava/lang/Boolean;
                    invoke-virtual {v0}, Ljava/lang/Boolean;->booleanValue()Z
                    move-result v0
                    if-eqz v0, :original
                    return-void
                    :original
                    nop
                """
            )
            signInjected++
        }
        println("MT Manager Tools Extension: injected dispatch into $signInjected sign dispatcher(s)")

        // --- 4. Conversion bridge (fallback) ---------------------------------
        var bridgeInjected = 0
        MtConversionBridgeFingerprint.matchAllOrNull()?.forEach { match ->
            val method = match.method
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

        // --- 5. File-tool dispatcher (Sign on .apk selection) ----------------
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

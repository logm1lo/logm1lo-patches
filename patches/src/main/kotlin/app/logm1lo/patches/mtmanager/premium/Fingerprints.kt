package app.logm1lo.patches.mtmanager.premium

import app.morphe.patcher.Fingerprint
import app.morphe.patcher.methodCall

/**
 * Native state class `l.ۨ᩸ܰ` (jadx: C10878) — backed by libmt1.so/libmtprotect.so.
 * Every feature gate is a smali call site: `invoke-static {}, Ll/ۨ᩸ܰ;->X()Z`
 * followed by `move-result vN`. Forcing each result register to 1 unlocks the
 * feature or bypasses the login check regardless of native state.
 *
 * --- VIP feature getters ---
 * ۘ()Z — MT_Protector, FTP menu, theme/settings
 * ܺ()Z — FTP/SFTP/SMB/WebDav (shows "(VIP)" disabled items when false)
 * ۠()Z — UI spans / theme features
 * ᩶()Z — misc VIP gates
 * ܰ()Z — misc VIP gates
 * ᩷()Z — misc VIP gates
 *
 * --- Login-state getter ---
 * ֡()Z — login confirmation flag (called right after native setter stores credentials)
 */
internal object VipGetter1Fingerprint : Fingerprint(
    filters = listOf(methodCall(smali = "Ll/ۨ᩸ܰ;->ۘ()Z"))
)

internal object VipGetter2Fingerprint : Fingerprint(
    filters = listOf(methodCall(smali = "Ll/ۨ᩸ܰ;->ܺ()Z"))
)

internal object VipGetter3Fingerprint : Fingerprint(
    filters = listOf(methodCall(smali = "Ll/ۨ᩸ܰ;->۠()Z"))
)

internal object VipGetter4Fingerprint : Fingerprint(
    filters = listOf(methodCall(smali = "Ll/ۨ᩸ܰ;->᩶()Z"))
)

internal object VipGetter5Fingerprint : Fingerprint(
    filters = listOf(methodCall(smali = "Ll/ۨ᩸ܰ;->ܰ()Z"))
)

internal object VipGetter6Fingerprint : Fingerprint(
    filters = listOf(methodCall(smali = "Ll/ۨ᩸ܰ;->᩷()Z"))
)

/** Login confirmation: forced true by Login Removal / Bypass patch. */
internal object LoginConfirmedFingerprint : Fingerprint(
    filters = listOf(methodCall(smali = "Ll/ۨ᩸ܰ;->֡()Z"))
)

/**
 * Conversion-tool dispatch bridge `Ll/᩸ۖ֡;->۟(Ll/᩸ۖ֡;Ljava/lang/String;Ljava/lang/String;Ll/ܺܽܺ;)V`.
 *
 * Both the Dex2Smali task (`l/ۙۖ֡`) and the Dex2Jar task (`l/ۛۖ֡`) call this
 * static bridge with the resolved input path (p1) and output path (p2).
 * It then opens the conversion dialog and drives the (native, stubbed)
 * conversion. We inject at the top of this method and hand the paths to the
 * Java reimplementation; when the extension handles the conversion we skip
 * the broken native flow entirely.
 */
internal object MtConversionBridgeFingerprint : Fingerprint(
    definingClass = "Ll/\u1a78\u06d6\u05a1;", // Ll/᩸ۖ֡;
    name = "\u06df", // ۟
    returnType = "V",
    parameters = listOf(
        "Ll/\u1a78\u06d6\u05a1;", // Ll/᩸ۖ֡; parent fragment
        "Ljava/lang/String;",
        "Ljava/lang/String;",
        "Ll/\u073a\u073d\u073a;", // Ll/ܺܽܺ; callback
    ),
    filters = listOf(
        methodCall(smali = "Ll/\u1a78\u06d6\u05a1;->\u06df(ILjava/lang/String;Ljava/lang/String;ZLl/\u073a\u073d\u073a;)V")
    )
)

/**
 * File-tool dispatcher `Ll/᩸ۖ֡;->ܿ(Ll/᩸ۖ֡;Ljava/lang/String;)V` (synthetic).
 * Receives a selected file path and creates the `l/᩹ܽ֡` Runnable that routes
 * to the correct tool task by extension. We inject before the routing so a
 * .apk selection can be re-routed to the extension's Sign tool.
 */
internal object MtFileDispatcherFingerprint : Fingerprint(
    definingClass = "Ll/\u1a78\u06d6\u05a1;", // Ll/᩸ۖ֡;
    name = "\u073f", // ܿ
    returnType = "V",
    parameters = listOf(
        "Ll/\u1a78\u06d6\u05a1;",
        "Ljava/lang/String;",
    )
)

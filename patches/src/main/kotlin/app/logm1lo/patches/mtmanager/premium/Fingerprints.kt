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

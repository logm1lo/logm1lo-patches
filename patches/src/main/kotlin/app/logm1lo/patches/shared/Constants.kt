package app.logm1lo.patches.shared

import app.morphe.patcher.patch.ApkFileType
import app.morphe.patcher.patch.AppTarget
import app.morphe.patcher.patch.Compatibility

val COMPATIBILITY_CALISTREE = Compatibility(
    name = "Calistree",
    packageName = "com.calistree.calistree",
    appIconColor = 0x4CAF50,
    apkFileType = ApkFileType.APK,
    targets = listOf(
        AppTarget(version = "5.8.5"),
    )
)

val COMPATIBILITY_ZALO = Compatibility(
    name = "Zalo",
    packageName = "com.zing.zalo",
    appIconColor = 0x0180C7,
    apkFileType = ApkFileType.APK,
    targets = listOf(
        AppTarget(version = "26.08.01"),
    )
)

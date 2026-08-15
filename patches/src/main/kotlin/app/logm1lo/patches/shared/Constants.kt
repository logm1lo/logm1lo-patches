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

val COMPATIBILITY_CUBESOLVER = Compatibility(
    name = "Cube Solver",
    packageName = "com.jeffprod.cubesolver",
    appIconColor = 0x4CAF50,
    apkFileType = ApkFileType.APK,
    targets = listOf(
        AppTarget(version = "5.0.3"),
    )
)

val COMPATIBILITY_MTMANAGER = Compatibility(
    name = "MT Manager",
    packageName = "bin.mt.plus",
    appIconColor = 0x2196F3,
    apkFileType = ApkFileType.APK,
    targets = listOf(
        AppTarget(version = "2.26.8"),
    )
)

val COMPATIBILITY_LOCKET = Compatibility(
    name = "Locket",
    packageName = "com.locket.Locket",
    appIconColor = 0xF6C945,
    apkFileType = ApkFileType.APKS,
    targets = listOf(
        AppTarget(version = "1.235.0"),
    )
)

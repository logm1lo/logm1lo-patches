package util

import com.google.gson.GsonBuilder
import java.io.File

fun main() {
    val gradleProps = File("gradle.properties").readLines()
    val version = gradleProps.firstOrNull { it.startsWith("version=") }
        ?.substringAfter("=") ?: "1.0.0"

    val patchList = mapOf(
        "NOTE" to "Do NOT manually edit this file.",
        "version" to version,
        "patches" to listOf(
            mapOf(
                "name" to "Premium Unlock",
                "description" to "Unlocks all Calistree PRO features. Overrides 5 RevenueCat Java methods paired with 10 Dart AOT hex patches in libapp.so for PRO state persistence, plan limit bypass, promotional gate removal, and restart-proof initial state.",
                "default" to true,
                "dependencies" to emptyList<String>(),
                "compatiblePackages" to listOf(
                    mapOf(
                        "packageName" to "com.calistree.calistree",
                        "name" to "Calistree",
                        "apkFileType" to "APK",
                        "targets" to listOf(
                            mapOf(
                                "version" to "5.8.5",
                                "isExperimental" to false,
                            )
                        ),
                    )
                ),
                "options" to emptyList<Map<String, Any>>(),
            )
        )
    )

    val gson = GsonBuilder().setPrettyPrinting().disableHtmlEscaping().create()
    val outputFile = File("patches-list.json")
    outputFile.writeText(gson.toJson(patchList))
    println("Generated patches-list.json (version: $version, patches: 1)")
}

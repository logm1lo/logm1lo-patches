package util

import com.google.gson.GsonBuilder
import java.io.File
import java.util.jar.JarFile

fun main() {
    val buildDir = File("patches/build/libs")
    val mppFile = buildDir.listFiles()?.firstOrNull { it.name.endsWith(".mpp") }
        ?: error("No .mpp file found in $buildDir")

    val version = JarFile(mppFile).manifest.mainAttributes.getValue("Version") ?: "unknown"

    val gson = GsonBuilder().setPrettyPrinting().disableHtmlEscaping().create()
    val json = gson.toJson(mapOf("version" to version, "patches" to emptyList<Any>()))
    File("patches-list.json").writeText(json)
    println("Generated patches-list.json (version: $version)")
}

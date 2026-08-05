group = "app.logm1lo"

patches {
    about {
        name = "Logm1lo Patches"
        description = "Custom Morphe patches for various apps."
        source = "https://github.com/logm1lo/logm1lo-patches"
        author = "Logm1lo"
        contact = "https://github.com/logm1lo"
        website = "https://github.com/logm1lo/logm1lo-patches"
        license = "GPL-3.0"
    }
}

kotlin {
    compilerOptions {
        freeCompilerArgs.add("-Xcontext-parameters")
    }
}

val patchListGeneratorClasspath = configurations.create("patchListGeneratorClasspath")

dependencies {
    compileOnly(libs.gson)
    patchListGeneratorClasspath(libs.gson)
}

tasks {
    register<JavaExec>("generatePatchesList") {
        description = "Generate patches-list.json from built MPP"
        dependsOn("buildAndroid")
        classpath = sourceSets["main"].runtimeClasspath + patchListGeneratorClasspath
        mainClass.set("util.PatchListGeneratorKt")
        workingDir = rootProject.projectDir
    }

    publish {
        dependsOn("generatePatchesList")
    }
}

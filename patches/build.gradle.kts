group = "app.rivanced"

tasks.withType<Jar>().configureEach {
    includeEmptyDirs = false
}

patches {
    about {
        name = "RIVanced Universal Patches"
        description = "Universal patches ported from ReVanced and adapted for Morphe"
        source = "git@github.com:rushiranpise/RI-Vanced-Universal-Morphe-Patches.git"
        author = "RIVanced"
        contact = "na"
        website = "https://github.com/rushiranpise/RI-Vanced-Universal-Morphe-Patches"
        license = "GNU General Public License v3.0"
    }
}

dependencies {
    compileOnly(libs.gson)

    // Required due to smali, or build fails. Can be removed once smali is bumped.
    implementation(libs.guava)

    implementation(libs.apksig)

    // Android API stubs defined here.
    compileOnly(project(":patches:stub"))
}

kotlin {
    compilerOptions {
        freeCompilerArgs.addAll(
            "-Xcontext-parameters"
        )
    }
}

// Separate configuration so gson is available at runtime for the
// generatePatchesList task but never bundled into the APK.
val patchListGeneratorClasspath: Configuration by configurations.creating

dependencies {
    patchListGeneratorClasspath(libs.gson)
}

tasks {
    register<JavaExec>("generatePatchesList") {
        description = "Build patch with patch list"

        dependsOn(build)

        classpath = sourceSets["main"].runtimeClasspath + patchListGeneratorClasspath
        mainClass.set("util.PatchListGeneratorKt")
    }

    // Used by gradle-semantic-release-plugin.
    publish {
        dependsOn("generatePatchesList")
    }
}

apply(from = "strings-processing.gradle.kts")

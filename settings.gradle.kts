rootProject.name = "rivanced-patches"

pluginManagement {
    repositories {
        gradlePluginPortal()
        google()
        maven {
            name = "GitHubPackages"
            url = uri("https://maven.pkg.github.com/MorpheApp/registry")
            credentials {
                username = providers.gradleProperty("gpr.user").orNull ?: System.getenv("GITHUB_ACTOR")
                password = providers.gradleProperty("gpr.key").orNull ?: System.getenv("GITHUB_TOKEN")
            }
        }
        // TODO: Remove once https://github.com/google/protobuf-gradle-plugin/pull/797 is merged.
        maven { url = uri("https://jitpack.io") }
    }
    // TODO: Remove once https://github.com/google/protobuf-gradle-plugin/pull/797 is merged.
    resolutionStrategy {
        eachPlugin {
            if (requested.id.id == "com.google.protobuf") {
                useModule("com.github.ReVanced:protobuf-gradle-plugin:${requested.version}")
            }
        }
    }
}

plugins {
    id("app.morphe.patches") version "1.3.3"
}

settings {
    extensions {
        defaultNamespace = "app.morphe.extension"

        // Must resolve to an absolute path (not relative),
        // otherwise the extensions in subfolders will fail to find the proguard config.
        proguardFiles(rootProject.projectDir.resolve("extensions/proguard-rules.pro").toString())
    }
}

include(":patches:stub")

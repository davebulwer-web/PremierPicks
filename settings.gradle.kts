pluginManagement {
    repositories {
        google()
        mavenCentral()
        gradlePluginPortal()
    }
}

dependencyResolutionManagement {
    repositories {
        google()
        mavenCentral()
    }
}

rootProject.name = "premier-picks"

// The game rules are a standalone Kotlin/JVM build, so they can be tested with
// `./gradlew -p engine test` on machines without the Android SDK.
includeBuild("engine")
include(":app")

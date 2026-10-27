pluginManagement {
    repositories {
        gradlePluginPortal()
        mavenCentral()
    }
}

dependencyResolutionManagement {
    repositories {
        mavenCentral()
    }
}

rootProject.name = "premier-picks"

// Pure-Kotlin game rules, testable on the JVM with no Android SDK.
include(":engine")
// The Jetpack Compose app module (":app") is added in milestone 2.

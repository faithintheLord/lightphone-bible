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
        maven { url = uri("https://jitpack.io") }
    }
}

rootProject.name = "lightphone-bible"
includeBuild("vendor/light-sdk/plugin")
include(":lint-rules")
project(":lint-rules").projectDir = file("vendor/light-sdk/lint-rules")

include(":sdk:shared", ":sdk:ui", ":sdk:client")
project(":sdk").projectDir = file("vendor/light-sdk/sdk")
for (module in listOf("shared", "ui", "client")) {
    project(":sdk:$module").projectDir = file("vendor/light-sdk/sdk/$module")
}

if (providers.gradleProperty("includeLightEmulator").orNull == "true") {
    for (module in listOf("trust", "server", "emulator")) {
        include(":sdk:$module")
        project(":sdk:$module").projectDir = file("vendor/light-sdk/sdk/$module")
    }
}
include(":tool")

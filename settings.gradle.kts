import java.util.Properties

pluginManagement {
    repositories {
        google()
        mavenCentral()
        gradlePluginPortal()
    }
}

val localProperties = Properties()
val localPropertiesFile = file("local.properties")
if (localPropertiesFile.exists()) {
    localPropertiesFile.inputStream().use { localProperties.load(it) }
}
val ghUsername = localProperties.getProperty("gpr.user") ?: System.getenv("GH_PACKAGES_USER")
val ghPassword = localProperties.getProperty("gpr.key") ?: System.getenv("GH_PACKAGES_TOKEN")

dependencyResolutionManagement {
    repositories {
        google()
        mavenCentral()
        maven {
            name = "GitHubPackages-Keyboard"
            url = uri("https://maven.pkg.github.com/lightphone/light-keyboard")
            credentials {
                username = ghUsername
                password = ghPassword
            }
        }
    }
}

rootProject.name = "lightphone-bible"

// Build keyboard UI from source when GitHub Packages credentials are unavailable locally.
includeBuild("vendor/light-keyboard") {
    dependencySubstitution {
        substitute(module("com.thelightphone.lp3keyboard:ui")).using(project(":ui"))
    }
}

includeBuild("vendor/light-sdk/plugin")
include(":lint-rules")
project(":lint-rules").projectDir = file("vendor/light-sdk/lint-rules")
include(":sdk:shared")
include(":sdk:ui")
include(":sdk:client")
project(":sdk").projectDir = file("vendor/light-sdk/sdk")
project(":sdk:shared").projectDir = file("vendor/light-sdk/sdk/shared")
project(":sdk:ui").projectDir = file("vendor/light-sdk/sdk/ui")
project(":sdk:client").projectDir = file("vendor/light-sdk/sdk/client")
include(":sdk:server")
project(":sdk:server").projectDir = file("vendor/light-sdk/sdk/server")
include(":sdk:emulator")
project(":sdk:emulator").projectDir = file("vendor/light-sdk/sdk/emulator")
include(":bible")

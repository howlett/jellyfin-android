import java.util.*

include(":app")

pluginManagement {
    val kotlinVersion: String by settings
    resolutionStrategy {
        eachPlugin {
            if (requested.id.namespace?.startsWith("org.jetbrains.kotlin") == true)
                useVersion(kotlinVersion)
        }
    }
}

// Load properties from local.properties
val properties = Properties().apply {
    val location = File("local.properties")
    if (location.exists())
        load(location.inputStream())
}

// Get value for dependency substitution
val enableDependencySubstitution = properties.getProperty("enable.dependency.substitution", "true").equals("true", true)

// Replace SDK dependency with local version
val apiclientLocation = "../jellyfin-sdk-kotlin"
if (File(apiclientLocation).exists() && enableDependencySubstitution) {
    includeBuild(apiclientLocation) {
        dependencySubstitution {
            substitute(module("org.jellyfin.sdk:jellyfin-platform-android")).with(project(":jellyfin-platform-android"))
        }
    }
}

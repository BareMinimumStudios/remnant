rootProject.name = "Remnant"

pluginManagement {
    repositories {
        // Fetch standard Kotlin and Arrow libraries from their primary repository.
        exclusiveContent {
            forRepository { mavenCentral() }
            filter {
                includeGroup("org.jetbrains.kotlin")
                includeGroup("org.jetbrains.kotlinx")
                includeGroup("io.arrow-kt")
            }
        }
        maven("https://maven.muon.rip/releases")
        maven("https://maven.neoforged.net/releases")
        maven("https://libraries.minecraft.net")
        maven("https://maven.fabricmc.net/")
        maven("https://maven.msrandom.net/repository/cloche/")
        mavenCentral()
        gradlePluginPortal()
    }
}

dependencyResolutionManagement {
    versionCatalogs.create("libs") {
        from(files("libraries.toml"))
    }
}

rootProject.name = "Remnant"

pluginManagement {
    repositories {
        maven("https://maven.neoforged.net/releases")
        maven("https://libraries.minecraft.net")
        maven("https://maven.fabricmc.net/")
        maven("https://maven.msrandom.net/repository/cloche/")
        mavenCentral()
        gradlePluginPortal()
    }
}
plugins {
    id("org.gradle.toolchains.foojay-resolver-convention") version "1.0.0"
}

dependencyResolutionManagement {
    versionCatalogs.create("libs") {
        from(files("libraries.toml"))
    }
}
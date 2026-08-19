plugins {
    `maven-publish`
    kotlin("jvm") version libs.versions.kotlin
    alias(libs.plugins.cloche)
}

group = "net.bms.remnant"
version = "1.0.0"

repositories {
    cloche.librariesMinecraft()

    mavenCentral()

    cloche {
        main()
        mavenFabric()
        mavenForge()
        mavenNeoforgedMeta()
        mavenNeoforged()
        mavenParchment()
    }

    maven("https://api.modrinth.com/maven")
    maven("https://maven.nucleoid.xyz")
    maven("https://maven.terraformersmc.com/")
    maven("https://maven.ladysnake.org/releases")
    maven("https://maven.wispforest.io/releases")
    maven("https://maven.shedaniel.me/")
    maven("https://thedarkcolour.github.io/KotlinForForge/")
}

cloche {
    metadata {
        modId = "remnant"
        name = "remnant"
        description = "Allows the ability to cache player data to the server based on ledger keys."
        license = "BML-1.0"

        author {
            name = "karuzumi"
            contact = "https://github.com/karuzumi"
        }

        url = "https://github.com/BareMinimumStudios/OfflinePlayerCache"
        sources = "https://github.com/BareMinimumStudios/OfflinePlayerCache"
        issues = "https://github.com/BareMinimumStudios/OfflinePlayerCache/issues"

        icon = "assets/remnant/icon.png"
    }

    common {
        mappings {
            official()
        }


        dependencies {
        }
    }

    fabric("fabric:1.21.1") {
        minecraftVersion = "1.21.1"
        loaderVersion = libs.versions.fabric.loader


        includedClient()

        runs {
            server()
            client()
        }

        dependencies {
            fabricApi(libs.versions.fabric.api.get1211())

            modImplementation(libs.fabric.language.kotlin)
        }

        metadata {
            dependencies {
                dependency {
                    modId = "fabric-api"
                    version(libs.versions.fabric.api.get1211().get())
                }
                dependency {
                    modId = "fabric-language-kotlin"
                    version(libs.versions.fabric.language.kotlin.get())
                }
            }

            entrypoint("main") {
                adapter.set("kotlin")
                value.set("net.bms.remnant.RemnantFabricEntrypoint")
            }
        }
    }

    neoforge("neoforge:1.21.1") {
        minecraftVersion = "1.21.1"
        loaderVersion = libs.versions.neoforge.loader


        runs {
            server()
            client()
        }

        dependencies {
            modImplementation(libs.neoforge.language.kotlin)
        }

        metadata {
            modLoader = "kotlinforforge"
            loaderVersion {
                start = libs.versions.neoforge.language.kotlin.get()
            }
            blurLogo = false
            dependencies {
                dependency {
                    modId = "kotlinforforge"
                    version(libs.versions.neoforge.language.kotlin.get())
                }
            }
        }
    }
}

kotlin {
    compilerOptions {
        freeCompilerArgs = listOf("-Xmulti-platform", "-Xno-check-actual", "-Xexpect-actual-classes")
    }
}
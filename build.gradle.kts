plugins {
    kotlin("jvm") version libs.versions.kotlin
    alias(libs.plugins.cloche)
    alias(libs.plugins.mixinmcp.decompile)
}

group = providers.gradleProperty("maven_group").get()
version = providers.gradleProperty("mod_version").get()

repositories {
    mavenCentral()

    cloche {
        librariesMinecraft()
        main()
        mavenFabric()
        mavenNeoforgedMeta()
        mavenNeoforged()
        mavenParchment()
    }

    maven("https://thedarkcolour.github.io/KotlinForForge/") {
        content {
            includeGroup("thedarkcolour")
        }
    }
}

cloche {
    metadata {
        modId = "remnant"
        name = "Remnant"
        description = "Keeps registered player data available while players are offline."
        license = "BML-1.0"

        author {
            name = "karuzumi"
            contact = "https://github.com/karuzumi"
        }

        url = "https://github.com/BareMinimumStudios/remnant"
        sources = "https://github.com/BareMinimumStudios/remnant"
        issues = "https://github.com/BareMinimumStudios/remnant/issues"

        icon = "assets/remnant/icon.png"
    }

    common {
        mappings {
            official()
            parchment(libs.versions.parchment)
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
            fabricApi(libs.versions.fabric.api)
            modImplementation(libs.fabric.language.kotlin)
        }

        metadata {
            dependencies {
                dependency {
                    modId = "fabric-api"
                    version(libs.versions.fabric.api.get())
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
    jvmToolchain(21)
    compilerOptions {
        languageVersion.set(org.jetbrains.kotlin.gradle.dsl.KotlinVersion.KOTLIN_2_2)
        apiVersion.set(org.jetbrains.kotlin.gradle.dsl.KotlinVersion.KOTLIN_2_2)
    }
}

// Cloche's Fabric transforms need this generated mapping archive before resolving dependencies.
tasks.matching {
    it.name == "accessWidenFabric1211CommonMinecraft" ||
        it.name == "accessWidenFabric1211Minecraft" ||
        it.name == "createCommonApiStub"
}.configureEach {
    dependsOn("generateFabric1211MappingsArtifact")
}

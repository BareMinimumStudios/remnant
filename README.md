![Offline Player Cache Banner](https://cdn.modrinth.com/data/cached_images/8bf7b045806b81dba417cabafe08bed2d4fd4a1c.png)
[![GitHub license](https://img.shields.io/badge/MIT-MIT?style=for-the-badge&label=LICENCE&labelColor=1A1A1A&color=FFFFFF&link=https%3A%2F%2Fgithub.com%2FPlayerEXDirectorsCut%2Foffline-player-cache%2Fblob%2F1.20.1%2Fmain%2FLICENSE)](https://github.com/PlayerEXDirectorsCut/offline-player-cache/blob/1.20.1/main/LICENSE)
[![GitHub stars](https://img.shields.io/github/stars/PlayerEXDirectorsCut/offline-player-cache?style=for-the-badge&logo=github&labelColor=1A1A1A&color=FFFFFF&link=https%3A%2F%2Fgithub.com%2FPlayerEXDirectorsCut%2Foffline-player-cache%2Fstargazers
)](https://github.com/PlayerEXDirectorsCut/offline-player-cache/stargazers)
[![GitHub forks](https://img.shields.io/github/forks/PlayerEXDirectorsCut/offline-player-cache?style=for-the-badge&logo=github&labelColor=1A1A1A&color=FFFFFF&link=https%3A%2F%2Fgithub.com%2FPlayerEXDirectorsCut%2Foffline-player-cache%2Fforks
)](https://github.com/PlayerEXDirectorsCut/offline-player-cache/forks)
[![GitHub issues](https://img.shields.io/github/issues/PlayerEXDirectorsCut/offline-player-cache?style=for-the-badge&logo=github&label=ISSUES&labelColor=1A1A1A&link=https%3A%2F%2Fgithub.com%2FPlayerEXDirectorsCut%2Foffline-player-cache%2Fissues
)](https://github.com/PlayerEXDirectorsCut/offline-player-cache/issues)

[![docs](https://cdn.jsdelivr.net/npm/@intergrav/devins-badges@3/assets/cozy/documentation/generic_vector.svg)](https://playerexdirectorscut.github.io/Bare-Minimum-Docs/)
![mkdocs](https://cdn.jsdelivr.net/npm/@intergrav/devins-badges@3/assets/cozy/built-with/mkdocs_vector.svg)
![java](https://cdn.jsdelivr.net/npm/@intergrav/devins-badges@3/assets/cozy/built-with/java_vector.svg)
[![curseforge](https://cdn.jsdelivr.net/npm/@intergrav/devins-badges@3/assets/cozy/available/curseforge_vector.svg)](https://www.curseforge.com/minecraft/mc-mods/opc-directors-cut)
[![modrinth](https://cdn.jsdelivr.net/npm/@intergrav/devins-badges@3/assets/cozy/available/modrinth_vector.svg)](https://modrinth.com/mod/opc-directors-cut)

![fabric](https://cdn.jsdelivr.net/npm/@intergrav/devins-badges@3/assets/cozy/supported/fabric_vector.svg)
![quilt](https://cdn.jsdelivr.net/npm/@intergrav/devins-badges@3/assets/cozy/supported/quilt_vector.svg)

### Preamble 📝

**Offline Player Cache: Directors Cut** has been completely redone from the ground up, and its framework predates `1.*` and below.
This was developed in mind to have **persistent leaderboards** for servers for their offline players.

### Content 📦
Modders may register `Record`'s linked with an id and a `Codec` to serialize/deserialize it.

Upon a player's disconnection from the server, their cached data is stored into the servers `level` data, which then can be accessed through code or through the commands the mod provides.

Upon a player's reconnection to the server, their cached data is deleted.

### Commands

#### `/opc get <uuid>|<name> <key>`
Provides details about the current player value. If they are online, it will provide their **current** value, but if they are offline, it will provide their **cached** value.

#### `/opc remove <uuid>|<name> <key>`
If the player with the associated username or UUID is offline, it will remove that players **cached** value based on the selected key.
If the player is **online**, nothing will occur with this command.

#### `/opc list <uuid>|<name>`
Lists all the keys and values this player has stored if they are offline, or if they are online, their current ones.

## Developers Guide

### Setup
Offline Player Cache has a [**Modrinth**](https://modrinth.com/mod/opc-directors-cut) and [**Curseforge**](https://curseforge.com/minecraft/mc-mods/opc-directors-cut) page.

In order to develop with the API, please add the following:

**`gradle.properties`**

```properties
opc_version=...
```

**`build.gradle`**

```groovy
repositories {
    maven {
        name = "Modrinth"
        url = "https://api.modrinth.com/maven"
        content {
            includeGroup "maven.modrinth"
        }
    }
}

dependencies {
    modImplementation "maven.modrinth:opc-directors-cut:${project.opc_version}"
    // include this if you do not want to force your users to install the mod.
    include "maven.modrinth:opc-directors-cut:${project.opc_version}"
}
```

<details><summary>Alternatively, if you are using Kotlin DSL:</summary>

**`build.gradle.kts`**

```kotlin
repositories {
    maven {
        name = "Modrinth"
        url = uri("https://api.modrinth.com/maven")
        content {
            includeGroup("maven.modrinth")
        }
    }
}

dependencies {
    modImplementation("maven.modrinth:opc-directors-cut:${properties["opc_version"]}")
    // include this if you do not want to force your users to install the mod.
    include("maven.modrinth:opc-directors-cut:${properties["opc_version"]}")
}
```

</details>

### Migration to 2.0.0

- A considerable amount of internal and external changes have happened with the mod (again).
- It's record based, so you are able to register any `Record` with an associated identifier.
- More docs coming soon...

### Using the Cache API
- In order to utilize the cache, utilize the new `OfflinePlayerCacheAPI`, which will allow you to register `CachedPlayerKeys` to the cache, and permit you to obtain data based on the keys provided.

### Registering & Using Keys

```kotlin
val LEVEL_KEY = ResourceLocation.tryBuild("opc", "level")!!

@JvmRecord
data class Level(val level: Int) {
    companion object {
        val CODEC: Codec<Level> = RecordCodecBuilder.create {
            it.group(Codec.INT.fieldOf("level").forGetter(Level::level)).apply(it, ::Level)
        }
    }
}

// static init
init {
    OfflinePlayerCacheAPI.register(LEVEL_KEY, TestingKeys.Level::class.java, TestingKeys.Level.CODEC) { player ->
        TestingKeys.Level(player.experienceLevel)
    }
}
```
Once your key is registered, you are good to go!

Keys will automatically be managed by the cache, and you should be able to see these keys through the commands we provide.

---

<p><img src="https://i.imgur.com/V38aMzY.png" alt="Sponsor Banner"/></p>
<p><b>Use code &quot;BAREMINIMUM&quot; to get 15% off your first month!</b></p>

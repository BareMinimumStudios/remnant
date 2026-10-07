![Remnant banner](https://raw.githubusercontent.com/BareMinimumStudios/remnant/HEAD/docs/assets/remnant-banner.png)

[![BML 1.0](https://img.shields.io/badge/LICENSE-BML--1.0-FFFFFF?style=for-the-badge&labelColor=1A1A1A)](https://github.com/BareMinimumStudios/remnant/blob/HEAD/LICENSE)
[![Build](https://img.shields.io/github/actions/workflow/status/BareMinimumStudios/remnant/build.yml?style=for-the-badge&logo=githubactions&logoColor=white&label=BUILD&labelColor=1A1A1A)](https://github.com/BareMinimumStudios/remnant/actions/workflows/build.yml)
[![GitHub](https://cdn.jsdelivr.net/npm/@intergrav/devins-badges@3/assets/cozy/available/github_vector.svg)](https://github.com/BareMinimumStudios/remnant)
[![CurseForge](https://cdn.jsdelivr.net/npm/@intergrav/devins-badges@3/assets/cozy/available/curseforge_vector.svg)](https://www.curseforge.com/minecraft/mc-mods/opc-directors-cut)

![Fabric](https://cdn.jsdelivr.net/npm/@intergrav/devins-badges@3/assets/cozy/supported/fabric_vector.svg)
![NeoForge](https://cdn.jsdelivr.net/npm/@intergrav/devins-badges@3/assets/cozy/supported/neoforge_vector.svg)

## Remnant

Remnant lets mods read registered player data after a player logs out. It is a server-side library for Minecraft 1.21.1 on Fabric and NeoForge, continuing **Offline Player Cache (OPC)**.

An integrating mod registers a value, a Mojang `Codec`, and a serializer for reading that value from a live player. On disconnect, Remnant snapshots every registered value into world `SavedData`. When the player comes back, the stale offline snapshot is removed and reads return to the live player.

Use it for leaderboards, statistics, progression, and server tools that need player data while someone is offline. Remnant stores the values registered by integrating mods.

## Requirements

- **Fabric 1.21.1** — requires Fabric API and Fabric Language Kotlin
- **NeoForge 1.21.1** — requires Kotlin for Forge
- **Java 21**

Install the JAR for your loader. Remnant does not add player-facing features on its own; other mods use its API.

## Commands

All commands require permission level 2.

```text
/remnant keys
/remnant players

/remnant get name <name> <key>
/remnant get uuid <uuid> <key>

/remnant list name <name>
/remnant list uuid <uuid>

/remnant remove name <name> <key>
/remnant remove uuid <uuid> <key>

/remnant clear name <name>
/remnant clear uuid <uuid>
```

`get` and `list` can inspect either a live player or an offline snapshot. `remove` and `clear` only modify offline cache data.

## For developers

A registered value is identified by a `ResourceLocation`, serialized with a Mojang `Codec`, and produced from a live `Player`.

```java
public static final PlayerLedgerKey<Contract> CONTRACT = PlayerLedger.register(
    ResourceLocation.fromNamespaceAndPath("example", "contract"),
    Contract.CODEC,
    player -> new Contract(player.getName().getString(), true)
);
```

Read through `PlayerLedger.getPlayer(...)` to get either a live `LedgerPlayer.Online` or a cached `LedgerPlayer.Offline`.

For Modrinth Maven, use the project ID so the coordinate stays stable if the public slug changes:

```kotlin
repositories {
    exclusiveContent {
        forRepository {
            maven("https://api.modrinth.com/maven")
        }
        filter {
            includeGroup("maven.modrinth")
        }
    }
}

dependencies {
    implementation("maven.modrinth:oLPaySSb:<version>")
}
```

Full API examples, source, development notes, and release history are available on the [main GitHub repository](https://github.com/BareMinimumStudios/remnant).

## Links

- [Source](https://github.com/BareMinimumStudios/remnant)
- [Issues](https://github.com/BareMinimumStudios/remnant/issues)
- [Modrinth](https://modrinth.com/mod/remnant)
- [CurseForge](https://www.curseforge.com/minecraft/mc-mods/opc-directors-cut)

## Sponsor

[![Sponsor Banner](https://www.bisecthosting.com/partners/custom-banners/db76a74a-a111-4660-98b7-5a75c15a5951.png)](https://bisecthosting.com/bareminimum)

Use code **`bareminimum`** to get **25% off your first month**.

## License

Remnant is licensed under the **Bare Minimum License (BML) v1.0**. See the [full license](https://github.com/BareMinimumStudios/remnant/blob/HEAD/LICENSE).

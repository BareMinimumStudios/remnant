<p align="center" style="text-align: center;">
<img src="https://raw.githubusercontent.com/BareMinimumStudios/remnant/HEAD/docs/assets/remnant-banner.png" alt="Remnant banner" width="1080">
</p>

<p align="center" style="text-align: center;">
<a href="https://modrinth.com/mod/remnant"><img src="https://img.shields.io/badge/Modrinth-133329?logo=modrinth&amp;logoColor=8CFFCF&amp;style=for-the-badge" alt="Modrinth" height="28"></a>
<a href="https://www.curseforge.com/minecraft/mc-mods/remnant-opc"><img src="https://img.shields.io/badge/CurseForge-39232B?logo=curseforge&amp;logoColor=FFA880&amp;style=for-the-badge" alt="CurseForge" height="28"></a>
<a href="https://github.com/BareMinimumStudios/remnant"><img src="https://img.shields.io/badge/Source-242B49?logo=github&amp;logoColor=EAF0FF&amp;style=for-the-badge" alt="GitHub source" height="28"></a>
<a href="https://github.com/BareMinimumStudios/remnant/issues"><img src="https://img.shields.io/github/issues/BareMinimumStudios/remnant?style=for-the-badge&amp;labelColor=242B49&amp;color=364775" alt="Report an issue" height="28"></a>
<a href="https://github.com/BareMinimumStudios/remnant/forks"><img src="https://img.shields.io/github/forks/BareMinimumStudios/remnant?style=for-the-badge&amp;labelColor=242B49&amp;color=364775" alt="Forks" height="28"></a>
<a href="https://github.com/BareMinimumStudios/remnant/stargazers"><img src="https://img.shields.io/github/stars/BareMinimumStudios/remnant?style=for-the-badge&amp;labelColor=242B49&amp;color=364775" alt="Stars" height="28"></a>
</p>

<p align="center" style="text-align: center;"><strong>Minecraft 1.21.1 · Fabric + NeoForge · Java 21</strong></p>

Remnant keeps player data available to mods after players log out. It continues **Offline Player Cache (OPC)** as a server-side library for leaderboards, statistics, progression, and other integrations.

## How it works

- **Save data on logout.** Integrating mods register the values they need. Remnant stores captured values in Minecraft world data, so snapshots survive server restarts.
- **Return to live data on login.** Remnant clears the offline snapshot when the player rejoins. Integrations can look up players by UUID or case-insensitive username.
- **Manage snapshots in game.** Operators can inspect saved values, remove a key, or clear a player's offline snapshot.

## Installation

- **Fabric 1.21.1** — requires Fabric API and Fabric Language Kotlin
- **NeoForge 1.21.1** — requires Kotlin for Forge
- **Java 21**

Install the JAR for your loader and the dependencies above on the server. Integrating mods supply the values used by leaderboards, stats, and other features.

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

`get` and `list` read live values for online players and saved values for offline players. Lookups support UUIDs and case-insensitive usernames. `remove` and `clear` only modify offline snapshots.

## For developers

A registered value is identified by a `ResourceLocation`, serialized with a Mojang `Codec`, and produced from a live `Player`.

```java
public static final PlayerLedgerKey<Contract> CONTRACT = PlayerLedger.register(
    ResourceLocation.fromNamespaceAndPath("example", "contract"),
    Contract.CODEC,
    player -> new Contract(player.getName().getString(), true)
);
```

Use `PlayerLedger.getPlayer(...)` with a UUID or username to read a live player or an offline snapshot. It throws `IllegalStateException` when neither is available. Use `getOfflinePlayer(...)` for a cache-only lookup that returns `null` when there is no snapshot.

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
    implementation("maven.modrinth:oLPaySSb:3.0.0+1.21.1-fabric") /* For NeoForge, use 3.0.0+1.21.1-neoforge. */
}
```

Full API examples, source, development notes, and release history are available on the [main GitHub repository](https://github.com/BareMinimumStudios/remnant).

## License

Remnant is licensed under the **Bare Minimum License (BML) v1.1**. See the [full license](https://github.com/BareMinimumStudios/remnant/blob/HEAD/LICENSE).

## Sponsor

[![Sponsor Banner](https://www.bisecthosting.com/partners/custom-banners/db76a74a-a111-4660-98b7-5a75c15a5951.png)](https://bisecthosting.com/bareminimum)

Use code **`bareminimum`** to get **25% off your first month**!

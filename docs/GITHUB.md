<div align="center">

<img src="https://raw.githubusercontent.com/BareMinimumStudios/remnant/HEAD/docs/assets/remnant-banner.png" alt="Remnant banner" width="100%">

<br>

<a href="https://github.com/BareMinimumStudios/remnant/blob/HEAD/LICENSE"><img alt="BML 1.0" src="https://img.shields.io/badge/LICENSE-BML--1.0-FFFFFF?style=for-the-badge&labelColor=1A1A1A"></a>
<a href="https://github.com/BareMinimumStudios/remnant/actions/workflows/build.yml"><img alt="Build" src="https://img.shields.io/github/actions/workflow/status/BareMinimumStudios/remnant/build.yml?style=for-the-badge&logo=githubactions&logoColor=white&label=BUILD&labelColor=1A1A1A"></a>
<a href="https://github.com/BareMinimumStudios/remnant/stargazers"><img alt="GitHub stars" src="https://img.shields.io/github/stars/BareMinimumStudios/remnant?style=for-the-badge&logo=github&labelColor=1A1A1A&color=FFFFFF"></a>
<a href="https://github.com/BareMinimumStudios/remnant/forks"><img alt="GitHub forks" src="https://img.shields.io/github/forks/BareMinimumStudios/remnant?style=for-the-badge&logo=github&labelColor=1A1A1A&color=FFFFFF"></a>
<a href="https://github.com/BareMinimumStudios/remnant/issues"><img alt="GitHub issues" src="https://img.shields.io/github/issues/BareMinimumStudios/remnant?style=for-the-badge&logo=github&label=ISSUES&labelColor=1A1A1A"></a>

<br><br>

<img alt="Fabric" height="56" src="https://cdn.jsdelivr.net/npm/@intergrav/devins-badges@3/assets/cozy/supported/fabric_vector.svg">
<img alt="NeoForge" height="56" src="https://cdn.jsdelivr.net/npm/@intergrav/devins-badges@3/assets/cozy/supported/neoforge_vector.svg">

<br>

<a href="https://modrinth.com/mod/remnant"><img alt="Available on Modrinth" height="56" src="https://cdn.jsdelivr.net/npm/@intergrav/devins-badges@3/assets/cozy/available/modrinth_vector.svg"></a>
<a href="https://www.curseforge.com/minecraft/mc-mods/opc-directors-cut"><img alt="Available on CurseForge" height="56" src="https://cdn.jsdelivr.net/npm/@intergrav/devins-badges@3/assets/cozy/available/curseforge_vector.svg"></a>
<a href="https://github.com/BareMinimumStudios/remnant"><img alt="Available on GitHub" height="56" src="https://cdn.jsdelivr.net/npm/@intergrav/devins-badges@3/assets/cozy/available/github_vector.svg"></a>
<a href="https://github.com/BareMinimumStudios/remnant/blob/HEAD/DEVELOPMENT.md"><img alt="Documentation" height="56" src="https://cdn.jsdelivr.net/npm/@intergrav/devins-badges@3/assets/cozy/documentation/generic_vector.svg"></a>

<br>

<img alt="Java" height="56" src="https://cdn.jsdelivr.net/npm/@intergrav/devins-badges@3/assets/cozy/built-with/java_vector.svg">
<img alt="Gradle" height="56" src="https://cdn.jsdelivr.net/npm/@intergrav/devins-badges@3/assets/cozy/built-with/gradle_vector.svg">
<a href="https://discord.gg/pcRw79hwey"><img alt="Bare Minimum Studios Discord" height="56" src="https://cdn.jsdelivr.net/npm/@intergrav/devins-badges@3/assets/cozy/social/discord-plural_vector.svg"></a>

</div>

---

## What is Remnant?

Remnant lets mods read registered player data after a player logs out. It is a server-side library for Minecraft 1.21.1 on Fabric and NeoForge, continuing **Offline Player Cache (OPC)**.

Mods register a value, a Mojang `Codec`, and a serializer that knows how to read that value from a live player. When the player disconnects, Remnant takes a snapshot and stores it in the world. When they return, the stale offline copy is removed and reads go back to the live player.

Use it for leaderboards, statistics, progression, or server tools that need player data while someone is offline. Integrating mods choose which values to register; Remnant stores and reads those values.

### At a glance

- **Minecraft 1.21.1**
- **Fabric + NeoForge** from one Cloche project
- **Kotlin** with a Java-friendly API
- **Java 21**
- World-backed offline snapshots using Minecraft `SavedData`
- Codec-based values instead of hard-coded cache fields
- Commands for inspecting and maintaining cached data

## Platform support

| Loader | Runtime dependencies |
| --- | --- |
| Fabric | Fabric API, Fabric Language Kotlin |
| NeoForge | Kotlin for Forge |

Remnant is designed for server-side use. Integrating mods decide what gets registered and how each value is produced.

## Commands

All Remnant administration commands require permission level 2.

| Command | Purpose |
| --- | --- |
| `/remnant keys` | Lists every registered ledger key. |
| `/remnant players` | Lists cached offline players and their stored key counts. |
| `/remnant get name <name> <key>` | Reads one value by player name. |
| `/remnant get uuid <uuid> <key>` | Reads one value by UUID. |
| `/remnant list name <name>` | Lists all current/cached values for a name. |
| `/remnant list uuid <uuid>` | Lists all current/cached values for a UUID. |
| `/remnant remove name <name> <key>` | Removes one cached value for an offline player. |
| `/remnant remove uuid <uuid> <key>` | Removes one cached value for an offline player. |
| `/remnant clear name <name>` | Clears the full offline snapshot for a name. |
| `/remnant clear uuid <uuid>` | Clears the full offline snapshot for a UUID. |

`get` and `list` work with both online and offline players. Online reads are created from the live player at command time; offline reads come from the stored ledger snapshot. `remove` and `clear` only touch offline cache data.

## Developer API

### 1. Define a value

Remnant 3.x stores registered payloads as Java `Record` values. A small value and codec can look like this:

```java
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

public record Contract(String label, boolean signed) {
    public static final Codec<Contract> CODEC = RecordCodecBuilder.create(instance ->
        instance.group(
            Codec.STRING.fieldOf("label").forGetter(Contract::label),
            Codec.BOOL.fieldOf("signed").forGetter(Contract::signed)
        ).apply(instance, Contract::new)
    );
}
```

### 2. Register it

Register ledger keys during normal mod initialization:

```java
import net.bms.remnant.api.PlayerLedger;
import net.bms.remnant.api.PlayerLedgerKey;
import net.minecraft.resources.ResourceLocation;

public static final PlayerLedgerKey<Contract> CONTRACT = PlayerLedger.register(
    ResourceLocation.fromNamespaceAndPath("example", "contract"),
    Contract.CODEC,
    player -> new Contract(player.getName().getString(), true)
);
```

Registration IDs are unique. Attempting to register the same ID twice throws immediately instead of silently replacing the first entry.

### 3. Read a player

```java
import net.bms.remnant.api.PlayerLedger;
import net.bms.remnant.player.LedgerPlayer;

LedgerPlayer player = PlayerLedger.getPlayer(server, uuid);

if (player instanceof LedgerPlayer.Offline offline) {
    Contract contract = offline.getPlayer().entry(CONTRACT);
}
```

`PlayerLedger.getPlayer(...)` prefers the live `ServerPlayer` when the player is online. If they are offline and Remnant has a snapshot, it returns `LedgerPlayer.Offline`. `getOfflinePlayer(...)` is also available when an integration specifically wants cached data only.

## Adding Remnant to a project

Published builds are available through [Modrinth](https://modrinth.com/mod/remnant) and [CurseForge](https://www.curseforge.com/minecraft/mc-mods/opc-directors-cut).

For projects that consume Modrinth Maven artifacts directly, using the project ID keeps the coordinate stable even if the public project slug changes:

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

Use the dependency configuration that matches your loader/toolchain (`modImplementation`, `implementation`, etc.).

## Building and development

```bash
./gradlew clean build
```

The project produces Fabric and NeoForge artifacts from the same source tree. Local development notes, IntelliJ setup, and the optional MixinMCP workflow live in [`DEVELOPMENT.md`](https://github.com/BareMinimumStudios/remnant/blob/HEAD/DEVELOPMENT.md).

## Publishing

Tagged releases are handled by [`.github/workflows/publish.yml`](https://github.com/BareMinimumStudios/remnant/blob/HEAD/.github/workflows/publish.yml) with [`Kira-NT/mc-publish`](https://github.com/Kira-NT/mc-publish). Fabric and NeoForge publish independently to Modrinth and CurseForge while GitHub receives a single release containing both jars.

The release workflow expects:

- `MODRINTH_TOKEN`
- `CURSEFORGE_TOKEN`

For this release, `mod_version` is `3.0.0`. Update `CHANGELOG.md` before publishing and set its release date to the day you publish. Open **Actions → publish → Run workflow**, select your source branch, and choose `all`. You can also publish by pushing the matching tag `v3.0.0+1.21.1`. For a partial failure, run the workflow again with only the failed destination selected; do not repeat successful uploads.

GitHub uses its automatic `GITHUB_TOKEN`; no `REPOSITORY_TOKEN` secret is needed. Organization marketplace secrets work when they grant this repository access. The workflow extracts only the current release section from `CHANGELOG.md` for release notes.

The public release naming is intentionally consistent with the existing project scheme:

- marketplace display names: `3.0.0+1.21.1-fabric` and `3.0.0+1.21.1-neoforge`
- GitHub display name: `Remnant 3.0.0+1.21.1`
- Fabric version: `3.0.0+1.21.1-fabric`
- NeoForge version: `3.0.0+1.21.1-neoforge`
- release channel: **Release**

## Project lineage

Remnant grew out of Offline Player Cache and keeps that project's original offline-data idea while moving the code, branding, API, and maintenance under Bare Minimum Studios. The icon keeps OPC's offline and database symbols.

## Sponsor

[![Sponsor Banner](https://www.bisecthosting.com/partners/custom-banners/db76a74a-a111-4660-98b7-5a75c15a5951.png)](https://bisecthosting.com/bareminimum)

Use code **`bareminimum`** to get **25% off your first month**.

## License

Remnant is licensed under the **Bare Minimum License (BML) v1.0**. See the [license on the main repository](https://github.com/BareMinimumStudios/remnant/blob/HEAD/LICENSE) for the complete terms.

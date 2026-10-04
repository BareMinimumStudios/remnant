# Remnant

Remnant is a small server-side API/library for **Minecraft 1.21.1** that lets mods register player data which is snapshotted when a player disconnects and remains queryable while that player is offline.

The 1.21.1 branch is written in Kotlin and targets both **Fabric** and **NeoForge** from one Cloche project.

## Platform support

| Platform | Minecraft | Runtime dependency |
| --- | --- | --- |
| Fabric | 1.21.1 | Fabric API, Fabric Language Kotlin |
| NeoForge | 1.21.1 | Kotlin for Forge |

Java 21 is required.

## How the cache works

Integrating mods register a `PlayerLedgerKey` with:

- a `ResourceLocation` id;
- a Mojang `Codec` used to persist the value;
- a serializer which reads the live `Player` and produces the registered value.

When a player disconnects, Remnant evaluates every registered serializer and writes the resulting ledger snapshot to world `SavedData`. When that player reconnects, the offline snapshot is removed so online reads come from the live player instead of stale cached data.

A failure in one registered serializer is logged and no longer prevents the remaining keys from being cached.

## Commands

All Remnant administration commands require permission level 2.

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

`get` and `list` work with both online and offline players. Online values are serialized from the current player; offline values come from the saved ledger snapshot.

`remove` and `clear` only modify offline cache data. `players` lists cached offline players and the number of registered values stored for each one.

## Developer API

### Register a ledger value

Remnant 2.x currently stores payloads as Java `Record` values. A small record and codec can look like this:

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

Register the value during normal mod initialization:

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

Duplicate ids are rejected during registration.

### Read a player

```java
import net.bms.remnant.api.PlayerLedger;
import net.bms.remnant.player.LedgerPlayer;

LedgerPlayer player = PlayerLedger.getPlayer(server, uuid);

if (player instanceof LedgerPlayer.Offline offline) {
    Contract contract = offline.getPlayer().entry(CONTRACT);
}
```

`PlayerLedger.getPlayer(...)` returns a live `LedgerPlayer.Online` when the player is connected and a `LedgerPlayer.Offline` when an offline snapshot exists. It throws when neither can be found. `getOfflinePlayer(...)` is available when only an offline result is wanted.

### Maven consumption

Published Remnant files are available through the project pages on [Modrinth](https://modrinth.com/mod/opc-directors-cut) and [CurseForge](https://www.curseforge.com/minecraft/mc-mods/opc-directors-cut).

For projects that consume Modrinth Maven artifacts directly:

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
    // Use the dependency configuration appropriate for your loader/toolchain.
    implementation("maven.modrinth:opc-directors-cut:<version>")
}
```

## Building

```bash
./gradlew clean build
```

Release jars are produced for both loader targets. The project uses Java 21, Kotlin, and Cloche.

## Publishing

Publishing is handled by `.github/workflows/publish.yml` using `Kira-NT/mc-publish`.

The repository needs these Actions secrets:

- `MODRINTH_TOKEN`
- `CURSEFORGE_TOKEN`

GitHub Releases use the workflow-provided `GITHUB_TOKEN`; a separate personal access token is not required.

For a normal tagged release:

1. Set `mod_version` in `gradle.properties`.
2. Commit the version/changelog changes.
3. Push a matching tag such as `v2.0.0-beta.5`.

The workflow verifies that the tag matches `mod_version`, builds both loader jars once, verifies that exactly one Fabric and one NeoForge release artifact exist, and uploads those jars as a short-lived Actions artifact. Independent jobs then publish Fabric/NeoForge to Modrinth and CurseForge and create one GitHub Release containing both jars.

Keeping each destination in its own job is intentional: if one upload fails after another succeeds, GitHub can re-run only the failed job instead of trying to recreate an already-published Modrinth/CurseForge version. The workflow can also be started manually with `workflow_dispatch`; choose the specific failed destination for recovery rather than `all` when part of a release already succeeded.

## License

Remnant is licensed under the **Bare Minimum License (BML) v1.0**. See [`LICENSE`](LICENSE) for the complete terms.

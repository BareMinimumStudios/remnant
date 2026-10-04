package net.bms.remnant.cache

import com.google.common.collect.BiMap
import com.google.common.collect.HashBiMap
import com.mojang.serialization.Codec
import net.bms.remnant.Remnant
import net.bms.remnant.api.PlayerLedger
import net.bms.remnant.api.PlayerLedgerKey
import net.minecraft.core.HolderLookup
import net.minecraft.nbt.CompoundTag
import net.minecraft.nbt.NbtOps
import net.minecraft.resources.ResourceLocation
import net.minecraft.server.MinecraftServer
import net.minecraft.util.datafix.DataFixTypes
import net.minecraft.world.entity.player.Player
import net.minecraft.world.level.saveddata.SavedData
import org.jetbrains.annotations.ApiStatus
import java.util.UUID
import kotlin.jvm.optionals.getOrNull

@ApiStatus.Internal
class LedgerCache : SavedData(), ILedgerCache {
    private val usernamesToUUID: BiMap<String, UUID> = HashBiMap.create()
    private val playerCache: MutableMap<UUID, MutableMap<PlayerLedgerKey<out Record>, Record>> = mutableMapOf()

    companion object {
        private const val DATA_NAME = "remnant-ledger"

        fun getOrCreate(server: MinecraftServer): LedgerCache {
            return server.overworld().dataStorage.computeIfAbsent(
                Factory(::LedgerCache, ::load, DataFixTypes.LEVEL),
                DATA_NAME,
            )
        }

        fun load(tag: CompoundTag, provider: HolderLookup.Provider): LedgerCache {
            val cache = LedgerCache()

            val usernamesCompound = tag.getCompound("usernames")
            for (username in usernamesCompound.allKeys) {
                runCatching { usernamesCompound.getUUID(username) }
                    .onSuccess { uuid -> cache.usernamesToUUID.forcePut(username, uuid) }
                    .onFailure { error ->
                        Remnant.LOGGER.warn("Skipping invalid cached username mapping for '{}'.", username, error)
                    }
            }

            val playersCompound = tag.getCompound("players")
            for (uuidString in playersCompound.allKeys) {
                val uuid = runCatching { UUID.fromString(uuidString) }
                    .onFailure { error ->
                        Remnant.LOGGER.warn("Skipping invalid cached player UUID '{}'.", uuidString, error)
                    }
                    .getOrNull() ?: continue

                val playerCompound = playersCompound.getCompound(uuidString)
                val ledger = mutableMapOf<PlayerLedgerKey<out Record>, Record>()

                for (keyString in playerCompound.allKeys) {
                    val id = ResourceLocation.tryParse(keyString) ?: run {
                        Remnant.LOGGER.warn("Skipping invalid player ledger id '{}' for {}.", keyString, uuid)
                        continue
                    }
                    val key = PlayerLedger.registeredKeys[id] ?: continue
                    val nbt = playerCompound.get(keyString) ?: continue

                    @Suppress("UNCHECKED_CAST")
                    val codec = key.codec as Codec<Record>
                    val record = runCatching {
                        codec.decode(NbtOps.INSTANCE, nbt)
                            .resultOrPartial { message ->
                                Remnant.LOGGER.error("Failed to decode ledger entry '{}' for {}: {}", id, uuid, message)
                            }
                            .getOrNull()
                            ?.first
                    }.onFailure { error ->
                        Remnant.LOGGER.error("Codec threw while decoding ledger entry '{}' for {}.", id, uuid, error)
                    }.getOrNull() ?: continue

                    ledger[key] = record
                }

                cache.playerCache[uuid] = ledger
            }

            return cache
        }
    }

    override fun save(tag: CompoundTag, provider: HolderLookup.Provider): CompoundTag {
        val playersCompound = CompoundTag()

        playerCache.forEach { (uuid, ledger) ->
            val playerCompound = CompoundTag()

            ledger.forEach { (key, record) ->
                @Suppress("UNCHECKED_CAST")
                val codec = key.codec as Codec<Record>
                val encoded = runCatching {
                    codec.encodeStart(NbtOps.INSTANCE, record)
                        .resultOrPartial { message ->
                            Remnant.LOGGER.error("Failed to encode ledger entry '{}' for {}: {}", key.id, uuid, message)
                        }
                        .getOrNull()
                }.onFailure { error ->
                    Remnant.LOGGER.error("Codec threw while encoding ledger entry '{}' for {}.", key.id, uuid, error)
                }.getOrNull()

                if (encoded != null) {
                    playerCompound.put(key.id.toString(), encoded)
                }
            }

            playersCompound.put(uuid.toString(), playerCompound)
        }

        tag.put("players", playersCompound)

        val usernamesCompound = CompoundTag()
        usernamesToUUID.forEach(usernamesCompound::putUUID)
        tag.put("usernames", usernamesCompound)

        return tag
    }

    override val uuids: Collection<UUID>
        get() = playerCache.keys

    override val usernames: Collection<String>
        get() = usernamesToUUID.keys

    override fun isPlayerCached(uuid: UUID): Boolean = playerCache.containsKey(uuid)

    override fun isPlayerCached(username: String): Boolean {
        val uuid = getUUIDFromUsername(username) ?: return false
        return playerCache.containsKey(uuid)
    }

    override fun getUsernameFromUUID(uuid: UUID): String? = usernamesToUUID.inverse()[uuid]

    override fun getUUIDFromUsername(username: String): UUID? {
        return usernamesToUUID[username]
            ?: usernamesToUUID.entries.firstOrNull { (cachedName, _) ->
                cachedName.equals(username, ignoreCase = true)
            }?.value
    }

    @Suppress("UNCHECKED_CAST")
    override fun <R : Record> getEntry(key: PlayerLedgerKey<R>, uuid: UUID): R? {
        return playerCache[uuid]?.get(key) as? R
    }

    override fun <R : Record> getEntry(key: PlayerLedgerKey<R>, username: String): R? {
        val uuid = getUUIDFromUsername(username) ?: return null
        return getEntry(key, uuid)
    }

    override fun cache(player: Player): Boolean {
        val username = player.gameProfile.name ?: return false
        val ledger = mutableMapOf<PlayerLedgerKey<out Record>, Record>()

        PlayerLedger.registeredKeys.values.forEach { key ->
            runCatching { key.serializer.invoke(player) }
                .onSuccess { record -> ledger[key] = record }
                .onFailure { error ->
                    Remnant.LOGGER.error("Failed to cache ledger entry '{}' for {} ({}).", key.id, username, player.uuid, error)
                }
        }

        playerCache[player.uuid] = ledger

        // A UUID may have a stale username after a Minecraft name change. Remove that
        // reverse mapping before inserting the current profile name to keep the BiMap valid.
        usernamesToUUID.inverse().remove(player.uuid)
        usernamesToUUID.forcePut(username, player.uuid)

        setDirty()
        return true
    }

    override fun uncache(player: Player): Boolean = uncache(player.uuid)

    override fun uncache(uuid: UUID): Boolean {
        val removedPlayer = playerCache.remove(uuid) != null
        val removedUsername = usernamesToUUID.inverse().remove(uuid) != null

        if (removedPlayer || removedUsername) {
            setDirty()
            return true
        }

        return false
    }

    override fun uncache(username: String): Boolean {
        return uncache(getUUIDFromUsername(username) ?: return false)
    }

    override fun <R : Record> uncacheEntry(key: PlayerLedgerKey<R>, player: Player): Boolean {
        return uncacheEntry(key, player.uuid)
    }

    override fun <R : Record> uncacheEntry(key: PlayerLedgerKey<R>, username: String): Boolean {
        return uncacheEntry(key, getUUIDFromUsername(username) ?: return false)
    }

    override fun <R : Record> uncacheEntry(key: PlayerLedgerKey<R>, uuid: UUID): Boolean {
        val removed = playerCache[uuid]?.remove(key) != null
        if (removed) setDirty()
        return removed
    }

    override fun getPlayerCache(uuid: UUID): PlayerLedgerEntry? = playerCache[uuid]?.toMap()

    override fun getPlayerCache(username: String): PlayerLedgerEntry? {
        val uuid = getUUIDFromUsername(username) ?: return null
        return playerCache[uuid]?.toMap()
    }
}

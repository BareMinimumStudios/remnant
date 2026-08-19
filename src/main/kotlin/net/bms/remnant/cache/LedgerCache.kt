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
import net.minecraft.server.MinecraftServer
import net.minecraft.util.datafix.DataFixTypes
import net.minecraft.world.entity.player.Player
import net.minecraft.world.level.saveddata.SavedData
import org.jetbrains.annotations.ApiStatus
import java.util.UUID
import kotlin.collections.component1
import kotlin.collections.component2
import kotlin.collections.get
import kotlin.collections.set
import kotlin.jvm.optionals.getOrNull

@ApiStatus.Internal
class LedgerCache : SavedData(), ILedgerCache {
    val usernamesToUUID: BiMap<String, UUID> = HashBiMap.create()
    val playerCache: MutableMap<UUID, MutableMap<PlayerLedgerKey<out Record>, Record>> = mutableMapOf()

    companion object {
        fun getOrCreate(server: MinecraftServer): LedgerCache {
            return server.overworld().dataStorage.computeIfAbsent(Factory(::LedgerCache, ::load, DataFixTypes.LEVEL), "remnant-ledger")
        }

        fun load(tag: CompoundTag, provider: HolderLookup.Provider): LedgerCache {
            val cache = LedgerCache()

            val usernamesCompound = tag.getCompound("usernames")
            for (username in usernamesCompound.allKeys) {
                cache.usernamesToUUID[username] = usernamesCompound.getUUID(username)
            }

            val playersCompound = tag.getCompound("players")
            for (uuidStr in playersCompound.allKeys) {
                val uuid = UUID.fromString(uuidStr)
                val playerCompound = playersCompound.getCompound(uuidStr)
                val ledger: MutableMap<PlayerLedgerKey<out Record>, Record> = mutableMapOf()

                for (keyStr in playerCompound.allKeys) {
                    val key = PlayerLedger.registeredKeys.values.find { it.id.toString() == keyStr } ?: continue
                    val nbt = playerCompound.get(keyStr) ?: continue

                    @Suppress("UNCHECKED_CAST")
                    val codec = key.codec as Codec<Record>
                    val record = codec.decode(NbtOps.INSTANCE, nbt)
                        .resultOrPartial(Remnant.LOGGER::error)
                        .getOrNull()
                        ?.first ?: continue

                    ledger[key] = record
                }

                cache.playerCache[uuid] = ledger
            }

            return cache
        }
    }

    override fun save(tag: CompoundTag, provider: HolderLookup.Provider): CompoundTag {
        val playersCompound = CompoundTag()

        playerCache.forEach { (key, ledger) ->
            val playerCompound = CompoundTag()

            ledger.forEach { (key, record) ->
                @Suppress("UNCHECKED_CAST")
                val codec = key.codec as Codec<Record>
                val encoded = codec.encodeStart(NbtOps.INSTANCE, record)
                    ?.resultOrPartial(Remnant.LOGGER::error)
                    ?.getOrNull()
                if (encoded != null) playerCompound.put(key.id.toString(), encoded)
            }

            playersCompound.put(key.toString(), playerCompound)
        }

        tag.put("players", playersCompound)

        val usernamesCompound = CompoundTag()
        usernamesToUUID.forEach(usernamesCompound::putUUID)
        tag.put("usernames", usernamesCompound)

        return tag
    }

    override val uuids: Collection<UUID>
        get() = usernamesToUUID.values

    override val usernames: Collection<String>
        get() = usernamesToUUID.keys

    override fun isPlayerCached(uuid: UUID): Boolean {
        return usernamesToUUID.containsValue(uuid)
    }

    override fun isPlayerCached(username: String): Boolean {
        return usernamesToUUID.containsKey(username)
    }

    override fun getUsernameFromUUID(uuid: UUID): String? {
        return usernamesToUUID.inverse()[uuid]
    }

    override fun getUUIDFromUsername(username: String): UUID? {
        return usernamesToUUID[username]
    }

    @Suppress("UNCHECKED_CAST")
    override fun <R : Record> getEntry(key: PlayerLedgerKey<R>, uuid: UUID): R? {
        val cached = playerCache[uuid] ?: return null

        val entry = cached[key] ?: return null

        return entry as R?
    }

    override fun <R : Record> getEntry(key: PlayerLedgerKey<R>, username: String): R? {
        val uuid = usernamesToUUID[username] ?: return null
        return getEntry(key, uuid)
    }

    override fun cache(player: Player): Boolean {
        if (player.gameProfile.name == null) return false
        this.playerCache[player.uuid] = PlayerLedger.registeredKeys.entries.associate { (_, key) -> key to key.serializer.invoke(player) }.toMutableMap()
        this.usernamesToUUID[player.gameProfile.name] = player.uuid
        this.setDirty()
        return true
    }

    override fun uncache(player: Player): Boolean {
        return uncache(player.uuid)
    }

    override fun uncache(uuid: UUID): Boolean {
        if (this.playerCache.remove(uuid) == null) return false
        this.usernamesToUUID.inverse().remove(uuid)
        this.setDirty()
        return true
    }

    override fun uncache(username: String): Boolean {
        return uncache(usernamesToUUID[username] ?: return false)
    }

    override fun <R : Record> uncacheEntry(key: PlayerLedgerKey<R>, player: Player): Boolean {
        return this.uncacheEntry(key, player.uuid)
    }

    override fun <R : Record> uncacheEntry(key: PlayerLedgerKey<R>, username: String): Boolean {
        return this.uncacheEntry(key, usernamesToUUID[username] ?: return false)
    }

    override fun <R : Record> uncacheEntry(key: PlayerLedgerKey<R>, uuid: UUID): Boolean {
        val isRemoved = this.playerCache[uuid]?.remove(key) != null
        if (isRemoved) this.setDirty()
        return isRemoved
    }

    override fun getPlayerCache(uuid: UUID): PlayerLedgerEntry? {
        return this.playerCache[uuid]?.toMap()
    }

    override fun getPlayerCache(username: String): PlayerLedgerEntry? {
        return this.playerCache[usernamesToUUID[username]]?.toMap()
    }
}
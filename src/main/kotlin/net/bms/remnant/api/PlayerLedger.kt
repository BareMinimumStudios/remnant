package net.bms.remnant.api

import com.google.common.collect.BiMap
import com.google.common.collect.HashBiMap
import com.google.common.collect.Maps
import com.mojang.authlib.GameProfile
import com.mojang.serialization.Codec
import net.bms.remnant.cache.LedgerCache
import net.bms.remnant.player.OfflinePlayer
import net.minecraft.resources.ResourceLocation
import net.minecraft.world.entity.player.Player
import org.jetbrains.annotations.ApiStatus
import net.bms.remnant.player.LedgerPlayer
import net.minecraft.server.MinecraftServer
import java.util.UUID

typealias PlayerSerializer<R> = (Player) -> R

/**
 * The API for the [LedgerCache] that allows for the registering and accessing of offline player data.
 *
 * @author karuzumi, DataEncoded, OverlordsIII
 */
@ApiStatus.AvailableSince("1.0")
object PlayerLedger {
    private val mutableRegisteredKeys: BiMap<ResourceLocation, PlayerLedgerKey<out Record>> = HashBiMap.create()

    /** Read-only view of all ledger keys currently registered with the API. */
    @JvmStatic
    val registeredKeys: BiMap<ResourceLocation, PlayerLedgerKey<out Record>> = Maps.unmodifiableBiMap(mutableRegisteredKeys)

    /**
     * Register a [Record] key based on a given
     * [net.minecraft.resources.ResourceLocation] (used for identifying in commands, etc...)
     *
     * This should be statically registered in your mod.
     *
     * @param id The [net.minecraft.resources.ResourceLocation] used to look up the [Record] entry.
     * @param codec A given [Codec] to ser/de the data.
     * @param serializer The function used to transform data on the [Player] to the given [Record].
     *
     * @throws IllegalStateException If the given [id] is already registered.
     */
    @JvmStatic
    fun <R : Record> register(id: ResourceLocation, codec: Codec<R>, serializer: PlayerSerializer<R>): PlayerLedgerKey<R> {
        check(!mutableRegisteredKeys.containsKey(id)) { "A player ledger key is already registered for id '$id'." }

        val key = PlayerLedgerKey(id, codec, serializer)
        mutableRegisteredKeys[id] = key
        return key
    }

    /**
     * Gets the currently cached entry related to this player by the unique identifier of a user.
     *
     * @param server A logical [MinecraftServer].
     * @param uuid The [UUID] used for the lookup operation for the player.
     *
     * @return [LedgerPlayer] A sealed-class implementation that provide an [OfflinePlayer] if that player is offline, and an [Player] if online.
     */
    @JvmStatic
    fun getPlayer(server: MinecraftServer, uuid: UUID): LedgerPlayer {
        server.playerList.players.firstOrNull { it.uuid == uuid }?.let { player ->
            return LedgerPlayer.Online(player)
        }

        val ledger = LedgerCache.getOrCreate(server)
        val cache = ledger.getPlayerCache(uuid) ?: throw IllegalStateException("Player not found: $uuid")
        val username = ledger.getUsernameFromUUID(uuid) ?: "unknown"
        return LedgerPlayer.Offline(OfflinePlayer(server, cache, GameProfile(uuid, username)))
    }

    /**
     * Gets the currently cached entry related to this player by the unique identifier of a user.
     *
     * @param server A logical [MinecraftServer].
     * @param username The username used for the lookup operation for the player.
     *
     * @return [LedgerPlayer] A sealed-class implementation that provide an [OfflinePlayer] if that player is offline, and an [Player] if online.
     */
    @JvmStatic
    fun getPlayer(server: MinecraftServer, username: String): LedgerPlayer {
        server.playerList.players.firstOrNull { it.gameProfile.name?.equals(username, ignoreCase = true) == true }?.let { player ->
            return LedgerPlayer.Online(player)
        }

        val ledger = LedgerCache.getOrCreate(server)
        val uuid = ledger.getUUIDFromUsername(username) ?: throw IllegalStateException("Player not found: $username")
        val cache = ledger.getPlayerCache(uuid) ?: throw IllegalStateException("Player not found: $username")
        val cachedUsername = ledger.getUsernameFromUUID(uuid) ?: username
        return LedgerPlayer.Offline(OfflinePlayer(server, cache, GameProfile(uuid, cachedUsername)))
    }

    /**
     * Gets the currently cached entry related to this player by the unique username of a user.
     *
     * @param server A logical [MinecraftServer].
     * @param uuid The [UUID] used for the lookup operation for the cached [OfflinePlayer].
     *
     * @return [OfflinePlayer] A pseudo-class of [Player] that is an offline 'entity'.
     */
    @JvmStatic
    fun getOfflinePlayer(server: MinecraftServer, uuid: UUID): OfflinePlayer? {
        val ledger = LedgerCache.getOrCreate(server)
        val cache = ledger.getPlayerCache(uuid) ?: return null
        val username = ledger.getUsernameFromUUID(uuid) ?: "unknown"
        return OfflinePlayer(server, cache, GameProfile(uuid, username))
    }

    /**
     * Gets the currently cached entry related to this player by the unique username of a user.
     *
     * @param server A logical [MinecraftServer].
     * @param username The [GameProfile.getName] used for the lookup operation for the cached [OfflinePlayer].
     *
     * @return [OfflinePlayer] A pseudo-class of [Player] that is an offline 'entity'.
     */
    @JvmStatic
    fun getOfflinePlayer(server: MinecraftServer, username: String): OfflinePlayer? {
        val ledger = LedgerCache.getOrCreate(server)
        val uuid = ledger.getUUIDFromUsername(username) ?: return null
        val cache = ledger.getPlayerCache(uuid) ?: return null
        val cachedUsername = ledger.getUsernameFromUUID(uuid) ?: username
        return OfflinePlayer(server, cache, GameProfile(uuid, cachedUsername))
    }

    /**
     * All offline player records currently in the cache.
     *
     * @param server A logical [MinecraftServer].
     *
     * @return [OfflinePlayer] A pseudo-class of [Player] that is an offline 'entity'.
     */
    @JvmStatic
    fun getOfflinePlayers(server: MinecraftServer): Collection<OfflinePlayer> {
        return LedgerCache.getOrCreate(server).uuids.mapNotNull { uuid ->
            getOfflinePlayer(server, uuid)
        }
    }
}
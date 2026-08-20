package net.bms.remnant.api

import com.google.common.collect.BiMap
import com.google.common.collect.HashBiMap
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
    /** The static registry of keys. Contains a view of what is currently registered in the API. */
    @JvmStatic
    val registeredKeys: BiMap<ResourceLocation, PlayerLedgerKey<out Record>> = HashBiMap.create()

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
    inline fun <reified R : Record> register(id: ResourceLocation, codec: Codec<R>, noinline serializer: PlayerSerializer<R>): PlayerLedgerKey<R> {
        val key = PlayerLedgerKey(id, codec, serializer)

        registeredKeys.computeIfAbsent(id) { key }

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
        val ledger = LedgerCache.getOrCreate(server)
        val cache = ledger.getPlayerCache(uuid)
        return if (cache == null) {
            // we will attempt to route to an existing player in the server, if there is none, then throw an exception!

            val player = server.playerList.players.find { player -> player.gameProfile.id == uuid } ?: throw IllegalStateException("Player not found: $uuid")

            LedgerPlayer.Online(player)
        }
        else {
            LedgerPlayer.Offline(OfflinePlayer(server, cache, GameProfile(uuid, ledger.getUsernameFromUUID(uuid))))
        }
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
        val ledger = LedgerCache.getOrCreate(server)
        val cache = ledger.getPlayerCache(username)
        return if (cache == null) {
            // we will attempt to route to an existing player in the server, if there is none, then throw an exception!

            val player = server.playerList.players.find { player -> player.gameProfile.name == username } ?: throw IllegalStateException("Player not found: $username")

            LedgerPlayer.Online(player)
        }
        else {
            LedgerPlayer.Offline(OfflinePlayer(server, cache, GameProfile(ledger.getUUIDFromUsername(username), username)))
        }
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
        val cache = ledger.getPlayerCache(uuid)
        return if (cache != null) OfflinePlayer(server, cache, GameProfile(uuid, ledger.getUsernameFromUUID(uuid)))
        else null
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
        val cache = ledger.getPlayerCache(username)
        return if (cache != null) OfflinePlayer(server, cache, GameProfile(ledger.getUUIDFromUsername(username), username))
        else null
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
        return LedgerCache.getOrCreate(server).playerCache.keys.map {
            uuid -> getOfflinePlayer(server, uuid) ?: throw IllegalStateException("failed to get offline player with uuid: $uuid")
        }
    }
}
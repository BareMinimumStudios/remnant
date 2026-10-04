package net.bms.remnant.command

import com.mojang.brigadier.CommandDispatcher
import com.mojang.brigadier.arguments.StringArgumentType
import com.mojang.brigadier.context.CommandContext
import net.bms.remnant.Remnant
import net.bms.remnant.api.PlayerLedger
import net.bms.remnant.api.PlayerLedgerKey
import net.bms.remnant.cache.LedgerCache
import net.bms.remnant.player.LedgerPlayer
import net.minecraft.ChatFormatting
import net.minecraft.commands.CommandSourceStack
import net.minecraft.commands.Commands
import net.minecraft.commands.arguments.ResourceLocationArgument
import net.minecraft.commands.arguments.UuidArgument
import net.minecraft.network.chat.Component
import net.minecraft.network.chat.MutableComponent
import java.util.UUID

/**
 * Administrative commands for inspecting and maintaining the Remnant player ledger.
 */
object RemnantCommands {
    fun register(dispatcher: CommandDispatcher<CommandSourceStack>) {
        dispatcher.register(
            Commands.literal("remnant")
                .requires { it.hasPermission(2) }
                .then(
                    Commands.literal("get")
                        .then(
                            Commands.literal("name")
                                .then(
                                    Commands.argument("name", StringArgumentType.string())
                                        .suggests(RemnantCommandSuggestions.Names)
                                        .then(
                                            Commands.argument("key", ResourceLocationArgument.id())
                                                .suggests(RemnantCommandSuggestions.Keys)
                                                .executes { context ->
                                                    executeGetKey(context) { ctx -> StringArgumentType.getString(ctx, "name") }
                                                },
                                        ),
                                ),
                        )
                        .then(
                            Commands.literal("uuid")
                                .then(
                                    Commands.argument("uuid", UuidArgument.uuid())
                                        .suggests(RemnantCommandSuggestions.Uuids)
                                        .then(
                                            Commands.argument("key", ResourceLocationArgument.id())
                                                .suggests(RemnantCommandSuggestions.Keys)
                                                .executes { context ->
                                                    executeGetKey(context) { ctx -> UuidArgument.getUuid(ctx, "uuid") }
                                                },
                                        ),
                                ),
                        ),
                )
                .then(Commands.literal("keys").executes(::executeGetKeys))
                .then(Commands.literal("players").executes(::executeGetPlayers))
                .then(
                    Commands.literal("remove")
                        .then(
                            Commands.literal("name")
                                .then(
                                    Commands.argument("name", StringArgumentType.string())
                                        .suggests(RemnantCommandSuggestions.Names)
                                        .then(
                                            Commands.argument("key", ResourceLocationArgument.id())
                                                .suggests(RemnantCommandSuggestions.Keys)
                                                .executes { context ->
                                                    executeRemoveKey(context) { ctx -> StringArgumentType.getString(ctx, "name") }
                                                },
                                        ),
                                ),
                        )
                        .then(
                            Commands.literal("uuid")
                                .then(
                                    Commands.argument("uuid", UuidArgument.uuid())
                                        .suggests(RemnantCommandSuggestions.Uuids)
                                        .then(
                                            Commands.argument("key", ResourceLocationArgument.id())
                                                .suggests(RemnantCommandSuggestions.Keys)
                                                .executes { context ->
                                                    executeRemoveKey(context) { ctx -> UuidArgument.getUuid(ctx, "uuid") }
                                                },
                                        ),
                                ),
                        ),
                )
                .then(
                    Commands.literal("clear")
                        .then(
                            Commands.literal("name")
                                .then(
                                    Commands.argument("name", StringArgumentType.string())
                                        .suggests(RemnantCommandSuggestions.Names)
                                        .executes { context ->
                                            executeRemoveAllCachedTo(context) { ctx -> StringArgumentType.getString(ctx, "name") }
                                        },
                                ),
                        )
                        .then(
                            Commands.literal("uuid")
                                .then(
                                    Commands.argument("uuid", UuidArgument.uuid())
                                        .suggests(RemnantCommandSuggestions.Uuids)
                                        .executes { context ->
                                            executeRemoveAllCachedTo(context) { ctx -> UuidArgument.getUuid(ctx, "uuid") }
                                        },
                                ),
                        ),
                )
                .then(
                    Commands.literal("list")
                        .then(
                            Commands.literal("name")
                                .then(
                                    Commands.argument("name", StringArgumentType.string())
                                        .suggests(RemnantCommandSuggestions.Names)
                                        .executes { context ->
                                            executeListKeys(context) { ctx -> StringArgumentType.getString(ctx, "name") }
                                        },
                                ),
                        )
                        .then(
                            Commands.literal("uuid")
                                .then(
                                    Commands.argument("uuid", UuidArgument.uuid())
                                        .suggests(RemnantCommandSuggestions.Uuids)
                                        .executes { context ->
                                            executeListKeys(context) { ctx -> UuidArgument.getUuid(ctx, "uuid") }
                                        },
                                ),
                        ),
                ),
        )
    }

    private fun <T> executeListKeys(
        ctx: CommandContext<CommandSourceStack>,
        input: (CommandContext<CommandSourceStack>) -> T,
    ): Int {
        val id = input(ctx)
        val ledgerPlayer = resolvePlayer(ctx, id) ?: return 0
        val entries = snapshotEntries(ledgerPlayer)

        ctx.source.sendSuccess(fetchingMessage(id), false)

        if (entries.isEmpty()) {
            ctx.source.sendSuccess({ Component.literal("No values for: $id").withStyle(ChatFormatting.GRAY) }, false)
            return 1
        }

        ctx.source.sendSuccess({ Component.literal("Found: ${ledgerPlayer.name}").withStyle(ChatFormatting.GREEN) }, false)
        ctx.source.sendSuccess({ Component.literal("Listing [${entries.size}] value(s):").withStyle(ChatFormatting.GREEN) }, false)
        entries.forEach { (key, value) ->
            ctx.source.sendSuccess({ Component.literal("${key.id} = $value").withStyle(ChatFormatting.WHITE) }, false)
        }

        return 1
    }

    private fun executeGetPlayers(ctx: CommandContext<CommandSourceStack>): Int {
        val cache = LedgerCache.getOrCreate(ctx.source.server)
        ctx.source.sendSuccess(
            { Component.literal("Listing Offline Players [${cache.uuids.size}]:").withStyle(ChatFormatting.BOLD) },
            false,
        )

        cache.uuids.forEach { uuid ->
            val username = cache.getUsernameFromUUID(uuid) ?: "unknown"
            val keyCount = cache.getPlayerCache(uuid)?.size ?: 0
            ctx.source.sendSuccess({ Component.literal("$username ($uuid) [$keyCount keys]") }, false)
        }

        return 1
    }

    private fun executeGetKeys(ctx: CommandContext<CommandSourceStack>): Int {
        if (PlayerLedger.registeredKeys.isEmpty()) {
            ctx.source.sendSuccess({ Component.literal("There are no registered ledger keys.") }, false)
            return 1
        }

        ctx.source.sendSuccess(
            { Component.literal("Registered Keys [${PlayerLedger.registeredKeys.size}]:").withStyle(ChatFormatting.BOLD) },
            false,
        )
        PlayerLedger.registeredKeys.keys.forEach { location ->
            ctx.source.sendSuccess({ Component.literal(location.toString()) }, false)
        }

        return 1
    }

    private fun <T> executeRemoveKey(
        ctx: CommandContext<CommandSourceStack>,
        input: (CommandContext<CommandSourceStack>) -> T,
    ): Int {
        val id = input(ctx)
        val identifier = ResourceLocationArgument.getId(ctx, "key")
        val key = PlayerLedger.registeredKeys[identifier]

        if (key == null) {
            ctx.source.sendFailure(Component.literal("Unknown ledger key: $identifier").withStyle(ChatFormatting.RED))
            return 0
        }

        val cache = LedgerCache.getOrCreate(ctx.source.server)
        val removed = when (id) {
            is String -> cache.uncacheEntry(key, id)
            is UUID -> cache.uncacheEntry(key, id)
            else -> false
        }

        if (!removed) {
            ctx.source.sendFailure(Component.literal("No cached '$identifier' value exists for $id.").withStyle(ChatFormatting.RED))
            return 0
        }

        ctx.source.sendSuccess({ Component.literal("$id: removed [$identifier]").withStyle(ChatFormatting.WHITE) }, false)
        return 1
    }

    private fun <T> executeRemoveAllCachedTo(
        ctx: CommandContext<CommandSourceStack>,
        input: (CommandContext<CommandSourceStack>) -> T,
    ): Int {
        val id = input(ctx)
        val cache = LedgerCache.getOrCreate(ctx.source.server)
        val removed = when (id) {
            is String -> cache.uncache(id)
            is UUID -> cache.uncache(id)
            else -> false
        }

        if (!removed) {
            ctx.source.sendFailure(Component.literal("No offline cache exists for $id.").withStyle(ChatFormatting.RED))
            return 0
        }

        ctx.source.sendSuccess({ Component.literal("$id: cleared").withStyle(ChatFormatting.WHITE) }, false)
        return 1
    }

    private fun <T> executeGetKey(
        ctx: CommandContext<CommandSourceStack>,
        input: (CommandContext<CommandSourceStack>) -> T,
    ): Int {
        val id = input(ctx)
        val identifier = ResourceLocationArgument.getId(ctx, "key")
        val key = PlayerLedger.registeredKeys[identifier]

        if (key == null) {
            ctx.source.sendFailure(Component.literal("Unknown ledger key: $identifier").withStyle(ChatFormatting.RED))
            return 0
        }

        val ledgerPlayer = resolvePlayer(ctx, id) ?: return 0
        val value = when (ledgerPlayer) {
            is LedgerPlayer.Online -> runCatching { key.serializer.invoke(ledgerPlayer.player) }
                .onFailure { error ->
                    Remnant.LOGGER.error("Failed to serialize ledger key '{}' for online player {}.", identifier, ledgerPlayer.uuid, error)
                }
                .getOrNull()

            is LedgerPlayer.Offline -> ledgerPlayer.player.ledger[key]
        }

        ctx.source.sendSuccess(fetchingMessage(id), false)
        ctx.source.sendSuccess({ Component.literal("Found: ${ledgerPlayer.name} (${ledgerPlayer.uuid})").withStyle(ChatFormatting.GREEN) }, false)

        if (value == null) {
            ctx.source.sendFailure(Component.literal("No value exists for ledger key '$identifier'.").withStyle(ChatFormatting.RED))
            return 0
        }

        ctx.source.sendSuccess({ Component.literal("$identifier = $value").withStyle(ChatFormatting.WHITE) }, false)
        return 1
    }

    private fun <T> resolvePlayer(ctx: CommandContext<CommandSourceStack>, id: T): LedgerPlayer? {
        val player = runCatching {
            when (id) {
                is String -> PlayerLedger.getPlayer(ctx.source.server, id)
                is UUID -> PlayerLedger.getPlayer(ctx.source.server, id)
                else -> null
            }
        }.getOrNull()

        if (player == null) {
            ctx.source.sendFailure(Component.literal("Player not found: $id").withStyle(ChatFormatting.RED))
        }

        return player
    }

    private fun snapshotEntries(player: LedgerPlayer): Map<PlayerLedgerKey<out Record>, Record> {
        return when (player) {
            is LedgerPlayer.Offline -> player.player.ledger
            is LedgerPlayer.Online -> buildMap {
                PlayerLedger.registeredKeys.values.forEach { key ->
                    runCatching { key.serializer.invoke(player.player) }
                        .onSuccess { value -> put(key, value) }
                        .onFailure { error ->
                            Remnant.LOGGER.error("Failed to serialize ledger key '{}' for online player {}.", key.id, player.uuid, error)
                        }
                }
            }
        }
    }

    private fun <T> fetchingMessage(id: T): () -> MutableComponent = {
        Component.literal("Fetching: $id").withStyle(ChatFormatting.GOLD)
    }
}

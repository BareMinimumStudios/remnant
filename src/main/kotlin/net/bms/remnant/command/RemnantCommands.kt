package net.bms.remnant.command

import com.mojang.brigadier.CommandDispatcher
import com.mojang.brigadier.arguments.StringArgumentType
import com.mojang.brigadier.context.CommandContext
import net.minecraft.ChatFormatting
import net.minecraft.commands.CommandSourceStack
import net.minecraft.commands.Commands
import net.minecraft.commands.arguments.ResourceLocationArgument
import net.minecraft.commands.arguments.UuidArgument
import net.minecraft.network.chat.Component
import net.minecraft.network.chat.MutableComponent
import net.bms.remnant.api.PlayerLedger
import net.bms.remnant.cache.LedgerCache
import java.util.UUID
import kotlin.to

/**
 * The command-tree for the **`OfflinePlayerCache`**.
 *
 * Was converted to the tree-format of `brigadier` for a cleaner look.
 *
 * @author OverlordsIII, bibi-reden
 * */
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
                            .executes { context -> executeGetKey(context) { ctx -> StringArgumentType.getString(ctx, "name") } }
                        )
                    )
                )
                .then(
                    Commands.literal("uuid")
                    .then(
                        Commands.argument("uuid", UuidArgument.uuid())
                            .suggests(RemnantCommandSuggestions.Uuids)
                            .then(
                                Commands.argument("key", ResourceLocationArgument.id())
                                .suggests(RemnantCommandSuggestions.Keys)
                                .executes { context -> executeGetKey(context) {
                                    ctx -> UuidArgument.getUuid(ctx, "uuid")
                                }
                            }
                        )
                    )
                )
            )
            .then(
                Commands.literal("remove").then(
                    Commands.literal("name").then(
                        Commands.argument("name", StringArgumentType.string()).suggests(RemnantCommandSuggestions.Names)
                        .then(
                            Commands.argument("key", ResourceLocationArgument.id())
                            .suggests(RemnantCommandSuggestions.Keys)
                            .executes { context ->
                                executeRemoveKey(context) { ctx -> StringArgumentType.getString(ctx, "name") }
                            }
                        )
                    )
                )
                .then(
                    Commands.literal("uuid")
                    .then(
                        Commands.argument("uuid", UuidArgument.uuid()).suggests(RemnantCommandSuggestions.Uuids)
                        .then(
                            Commands.argument("key", ResourceLocationArgument.id())
                            .suggests(RemnantCommandSuggestions.Keys)
                            .executes { context ->
                                executeRemoveKey(context) { ctx: CommandContext<CommandSourceStack> -> UuidArgument.getUuid(ctx,"uuid") }
                            }
                        )
                    )
                )
            )
            .then(
                Commands.literal("clear").then(
                    Commands.literal("name").then(
                        Commands.argument("name", StringArgumentType.string())
                            .suggests(RemnantCommandSuggestions.Names)
                            .executes { context ->
                                executeRemoveAllCachedTo(context) {
                                    ctx -> StringArgumentType.getString(ctx, "name")
                                }
                            }
                    )
                )
                    .then(
                        Commands.literal("uuid")
                            .then(
                                Commands.argument("uuid", UuidArgument.uuid())
                                    .suggests(RemnantCommandSuggestions.Uuids)
                                    .executes { context ->
                                        executeRemoveAllCachedTo(context) { ctx -> UuidArgument.getUuid(ctx, "uuid") }
                                    }
                            )
                    )
                )
            .then(
                Commands.literal("list")
                .then(
                    Commands.literal("name")
                    .then(
                        Commands.argument("name", StringArgumentType.string())
                        .suggests(RemnantCommandSuggestions.Names)
                        .executes { context ->
                            executeListKeys(context) { ctx -> StringArgumentType.getString(ctx, "name")}
                        }
                    )
                )
                .then(
                    Commands.literal("uuid")
                    .then(
                        Commands.argument("uuid", UuidArgument.uuid())
                        .suggests(RemnantCommandSuggestions.Uuids)
                        .executes { context ->
                            executeListKeys(context) { ctx -> UuidArgument.getUuid(ctx, "uuid")}
                        }
                    )
                )
            )
            .then(
                Commands.literal("clear").then(
                    Commands.literal("name")
                        .then(
                            Commands.argument("name", StringArgumentType.string())
                                .suggests(RemnantCommandSuggestions.Names)
                                .executes { context ->
                                    executeListKeys(context) { ctx -> StringArgumentType.getString(ctx, "name")}
                                }
                        )
                )
                .then(
                    Commands.literal("uuid")
                        .then(
                            Commands.argument("uuid", UuidArgument.uuid())
                                .suggests(RemnantCommandSuggestions.Uuids)
                                .executes { context ->
                                    executeListKeys(context) { ctx -> UuidArgument.getUuid(ctx, "uuid")}
                                }
                        )
                )
            )
        )
    }

    private fun <T> executeListKeys(ctx: CommandContext<CommandSourceStack>, input: (CommandContext<CommandSourceStack>) -> T): Int {
        val id = input(ctx)

        val offlinePlayer = when (id) {
            is String -> PlayerLedger.getOfflinePlayer(ctx.source.server, id)!!
            is UUID -> PlayerLedger.getOfflinePlayer(ctx.source.server, id)!!
            else -> return -1
        }

        ctx.source.sendSuccess(fetchingMessage(id), false)

        if (offlinePlayer.entries().isEmpty()) {
            ctx.source.sendSuccess({ Component.literal("No values for: $id").withStyle(ChatFormatting.GRAY)}, false)
        }
        else {
            ctx.source.sendSuccess({ Component.literal("Found: $id").withStyle(ChatFormatting.GREEN)}, false)
            ctx.source.sendSuccess({ Component.literal("Listing [${offlinePlayer.values().size}] value(s):").withStyle(ChatFormatting.GREEN)}, false)
            offlinePlayer.entries().forEach { (key, value) ->
                ctx.source.sendSuccess({
                    Component.literal( "${PlayerLedger.registeredKeys.inverse()[key]} = $value").withStyle(
                        ChatFormatting.WHITE)}, false)
            }
        }

        return 1;
    }

    private fun <T> executeRemoveKey(ctx: CommandContext<CommandSourceStack>, input: (CommandContext<CommandSourceStack>) -> T): Int {
        val id = input(ctx)
        val identifier = ResourceLocationArgument.getId(ctx, "key")

        val value = PlayerLedger.registeredKeys[identifier]

        if (value == null) {
            ctx.source.sendSuccess(nullKeyMessage(id), false)
            return -1
        }

        val opc = LedgerCache.getOrCreate(ctx.source.server)

        when (id) {
            is String -> opc.uncacheEntry(value, id)
            is UUID -> opc.uncacheEntry(value, id)
        }

        ctx.source.sendSuccess({ Component.literal("$id: un-cached [$identifier]").withStyle(ChatFormatting.WHITE) }, false)

        return 1
    }

    private fun <T> executeRemoveAllCachedTo(context: CommandContext<CommandSourceStack>, input: (CommandContext<CommandSourceStack>) -> T): Int {
        val uuidOrPlayer = input(context)
        val opc = LedgerCache.getOrCreate(context.source.server)

        val executed = when (uuidOrPlayer) {
            is String -> opc.uncache(uuidOrPlayer)
            is UUID -> opc.uncache(uuidOrPlayer)
            else -> false;
        }

        context.source.sendSuccess({ Component.literal( "$uuidOrPlayer: cleared" ).withStyle(ChatFormatting.WHITE) }, false)

        return if (executed) 1 else -1
    }

    private fun <T> executeGetKey(ctx: CommandContext<CommandSourceStack>, input: (CommandContext<CommandSourceStack>) -> T): Int {
        val id = input(ctx)
        val identifier = ResourceLocationArgument.getId(ctx, "key")
        val key = PlayerLedger.registeredKeys[identifier]

        if (key == null) {
            ctx.source.sendSuccess(nullKeyMessage(id), false)
            return -1
        }

        val server = ctx.source.server

        val api = LedgerCache.getOrCreate(server)

        val (value, otherId) = when (id) {
            is String -> (api.getEntry(key, id) to api.getUUIDFromUsername(id))
            is UUID -> (api.getEntry(key, id) to api.getUsernameFromUUID(id))
            else -> null
        } ?: return -1

        ctx.source.sendSuccess(fetchingMessage(id), false)
        ctx.source.sendSuccess({ Component.literal("Found: $otherId").withStyle(ChatFormatting.GREEN)}, false)
        if (value != null) {
            ctx.source.sendSuccess({ Component.literal("$identifier = $value").withStyle(ChatFormatting.WHITE)}, false)
        }
        else {
            ctx.source.sendSuccess(nullKeyMessage(identifier), false)
        }

        return 1
    }

    private fun <T> fetchingMessage(id: T): () -> MutableComponent = { Component.literal("Fetching: $id").withStyle(
        ChatFormatting.GOLD) }
    private fun <T> nullKeyMessage(id: T): () -> MutableComponent = { Component.literal("$id -> <null_key>").withStyle(
        ChatFormatting.RED) }
}
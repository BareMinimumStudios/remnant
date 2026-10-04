package net.bms.remnant.command

import com.mojang.brigadier.suggestion.SuggestionProvider
import net.bms.remnant.api.PlayerLedger
import net.bms.remnant.cache.LedgerCache
import net.minecraft.commands.CommandSourceStack
import net.minecraft.commands.SharedSuggestionProvider

object RemnantCommandSuggestions {
    @JvmStatic
    val Keys = SuggestionProvider<CommandSourceStack> { _, builder ->
        SharedSuggestionProvider.suggestResource(PlayerLedger.registeredKeys.keys, builder)
        builder.buildFuture()
    }

    @JvmStatic
    val Names = SuggestionProvider<CommandSourceStack> { ctx, builder ->
        val names = buildSet {
            addAll(LedgerCache.getOrCreate(ctx.source.server).usernames)
            ctx.source.server.playerList.players.mapNotNullTo(this) { it.gameProfile.name }
        }
        SharedSuggestionProvider.suggest(names, builder)
        builder.buildFuture()
    }

    @JvmStatic
    val Uuids = SuggestionProvider<CommandSourceStack> { ctx, builder ->
        val uuids = buildSet {
            addAll(LedgerCache.getOrCreate(ctx.source.server).uuids)
            ctx.source.server.playerList.players.mapTo(this) { it.uuid }
        }
        uuids.forEach { builder.suggest(it.toString()) }
        builder.buildFuture()
    }
}

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
        LedgerCache.getOrCreate(ctx.source.server).usernames.forEach(builder::suggest)
        builder.buildFuture()
    }
    @JvmStatic
    val Uuids = SuggestionProvider<CommandSourceStack> { ctx, builder ->
        LedgerCache.getOrCreate(ctx.source.server).uuids.forEach { builder.suggest(it.toString()) }
        builder.buildFuture()
    }
}
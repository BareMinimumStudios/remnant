package net.bms.remnant

import net.fabricmc.api.ModInitializer
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents
import net.bms.remnant.cache.LedgerCache
import net.bms.remnant.command.RemnantCommands

class RemnantFabricEntrypoint : ModInitializer {
    override fun onInitialize() {
        Remnant.init()
        
        CommandRegistrationCallback.EVENT.register { dispatcher, _, _ ->
            RemnantCommands.register(dispatcher)
        }

        ServerPlayConnectionEvents.JOIN.register { impl, _, server ->
            LedgerCache.getOrCreate(server).uncache(impl.player)
        }

        ServerPlayConnectionEvents.DISCONNECT.register { impl, server ->
            LedgerCache.getOrCreate(server).cache(impl.player)
        }
    }
}
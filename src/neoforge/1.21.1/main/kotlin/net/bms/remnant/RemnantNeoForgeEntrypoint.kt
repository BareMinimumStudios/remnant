package net.bms.remnant

import net.bms.remnant.cache.LedgerCache
import net.bms.remnant.command.RemnantCommands
import net.neoforged.fml.common.Mod
import net.neoforged.neoforge.event.RegisterCommandsEvent
import net.neoforged.neoforge.event.entity.player.PlayerEvent
import thedarkcolour.kotlinforforge.neoforge.forge.FORGE_BUS

@Mod(Remnant.MOD_ID)
object RemnantNeoForgeEntrypoint {
    init {
        Remnant.init()

        // Register common server events on both dedicated and integrated servers.
        // Physical-side gating (Dist.CLIENT/DEDICATED_SERVER) is incorrect for these
        // logical-server events because single-player runs on the client distribution.
        FORGE_BUS.addListener(::onPlayerConnect)
        FORGE_BUS.addListener(::onPlayerDisconnect)
        FORGE_BUS.addListener(::registerCommands)
    }

    private fun onPlayerConnect(event: PlayerEvent.PlayerLoggedInEvent) {
        val server = event.entity.server ?: return
        LedgerCache.getOrCreate(server).uncache(event.entity)
    }

    private fun onPlayerDisconnect(event: PlayerEvent.PlayerLoggedOutEvent) {
        val server = event.entity.server ?: return
        LedgerCache.getOrCreate(server).cache(event.entity)
    }

    private fun registerCommands(event: RegisterCommandsEvent) {
        RemnantCommands.register(event.dispatcher)
    }
}

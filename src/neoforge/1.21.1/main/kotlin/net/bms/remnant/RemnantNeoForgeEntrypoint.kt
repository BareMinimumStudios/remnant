package net.bms.remnant

import net.bms.remnant.cache.LedgerCache
import net.bms.remnant.command.RemnantCommands
import net.neoforged.api.distmarker.Dist
import net.neoforged.bus.api.EventPriority
import net.neoforged.bus.api.SubscribeEvent
import net.neoforged.fml.common.EventBusSubscriber
import net.neoforged.fml.common.Mod

import net.neoforged.neoforge.event.RegisterCommandsEvent
import net.neoforged.neoforge.event.entity.player.PlayerEvent

import thedarkcolour.kotlinforforge.neoforge.forge.FORGE_BUS
import thedarkcolour.kotlinforforge.neoforge.forge.runForDist

@Mod("remnant")
@EventBusSubscriber
object RemnantNeoForgeEntrypoint {
    init {
        Remnant.init()

        runForDist(
            serverTarget = {
                FORGE_BUS.addListener(::onPlayerConnect)
                FORGE_BUS.addListener(::onPlayerDisconnect)
                FORGE_BUS.addListener(::registerCommands)
                "remnant"
            },
            clientTarget = {}
        )
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    fun onPlayerConnect(event: PlayerEvent.PlayerLoggedInEvent) {
        LedgerCache.getOrCreate(event.entity.server!!).uncache(event.entity)
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    fun onPlayerDisconnect(event: PlayerEvent.PlayerLoggedOutEvent) {
        LedgerCache.getOrCreate(event.entity.server!!).cache(event.entity)
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    fun registerCommands(event: RegisterCommandsEvent) {
        RemnantCommands.register(event.dispatcher)
    }
}
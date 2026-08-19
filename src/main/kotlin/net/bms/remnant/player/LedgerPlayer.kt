package net.bms.remnant.player

import com.mojang.authlib.GameProfile
import net.minecraft.server.level.ServerPlayer
import java.util.UUID

sealed class LedgerPlayer {
    abstract val profile: GameProfile

    val uuid: UUID get() = profile.id
    val name: String get() = profile.name

    data class Online(val player: ServerPlayer, override val profile: GameProfile = player.gameProfile) : LedgerPlayer()
    data class Offline(val player: OfflinePlayer, override val profile: GameProfile = player.gameProfile) : LedgerPlayer()
}
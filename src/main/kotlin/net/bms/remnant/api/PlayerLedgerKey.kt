package net.bms.remnant.api

import com.mojang.serialization.Codec
import net.minecraft.resources.ResourceLocation

data class PlayerLedgerKey<R : Record>(val id: ResourceLocation, val codec: Codec<R>, val serializer: PlayerSerializer<R>)
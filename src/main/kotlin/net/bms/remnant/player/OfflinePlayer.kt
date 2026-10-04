package net.bms.remnant.player

import com.mojang.authlib.GameProfile
import net.bms.remnant.cache.PlayerLedgerEntry
import net.bms.remnant.api.PlayerLedgerKey
import net.minecraft.core.BlockPos
import net.minecraft.nbt.CompoundTag
import net.minecraft.nbt.NbtAccounter
import net.minecraft.nbt.NbtIo
import net.minecraft.server.MinecraftServer
import net.minecraft.world.entity.player.Player
import net.minecraft.world.level.storage.LevelResource
import org.jetbrains.annotations.ApiStatus
import net.bms.remnant.Remnant
import net.minecraft.Util
import java.io.File
import java.io.FileOutputStream

/**
 * A pseudo 'offline' player subclassing the actual [Player] instance.
 */
@ApiStatus.AvailableSince("1.0")
class OfflinePlayer(server: MinecraftServer, val ledger: PlayerLedgerEntry, profile: GameProfile) : Player(server.overworld(), BlockPos.ZERO, 0.0F, profile), AutoCloseable {
    private val playerDataDirectory = server.getWorldPath(LevelResource.PLAYER_DATA_DIR).toFile()

    init {
        val file = File(playerDataDirectory, "$stringUUID.dat")

        if (file.exists()) {
            try {
                val tag = NbtIo.readCompressed(file.toPath(), NbtAccounter.unlimitedHeap())

                val posList = tag.getList("Pos", 6)
                val rotList = tag.getList("Rotation", 5)

                if (posList.size == 3) {
                    setPosRaw(posList.getDouble(0), posList.getDouble(1), posList.getDouble(2))
                }

                if (rotList.size == 2) {
                    yRot = rotList.getFloat(0)
                    xRot = rotList.getFloat(1)
                }

                load(tag)
            } catch (why: Exception) {
                Remnant.LOGGER.warn("Could not read player data for $stringUUID", why)
            }
        }
    }

    @Suppress("UNCHECKED_CAST")
    fun <R : Record> entry(key: PlayerLedgerKey<R>): R? {
        return ledger[key] as? R
    }

    fun entries(): Set<Map.Entry<PlayerLedgerKey<out Record>, Record>> {
        return ledger.entries
    }

    fun keys(): Collection<PlayerLedgerKey<*>> = ledger.keys

    fun values(): Collection<Record> = ledger.values

    override fun isSpectator(): Boolean = false

    override fun isCreative(): Boolean = false

    override fun close() {
        // override a save into uuid.dat

        try {
            val file = File.createTempFile("${stringUUID}.tmp", ".dat", playerDataDirectory)

            FileOutputStream(file).use {
                val tag = saveWithoutId(CompoundTag())
                NbtIo.writeCompressed(tag, it)
            }

            val outbound = File(playerDataDirectory, "${stringUUID}.dat")
            val inbound = File(playerDataDirectory, "${stringUUID}.dat_old")

            Util.safeReplaceFile(outbound.toPath(), file.toPath(), inbound.toPath())
        }
        catch (why: Exception) {
            Remnant.LOGGER.warn("Could not write offline player data to: $playerDataDirectory", why)
        }
    }
}
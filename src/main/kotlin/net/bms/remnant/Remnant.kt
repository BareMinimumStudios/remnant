package net.bms.remnant

import net.bms.remnant.api.PlayerLedger
import com.mojang.logging.LogUtils
import org.slf4j.Logger

object Remnant {
    /** The mod id for  opc.  */
    const val MOD_ID: String = "remnant"

    /** The logger for opc.  */
    val LOGGER: Logger = LogUtils.getLogger()

    /**
     * Initializes the mod.
     */
    @JvmStatic
    fun init() {
        LOGGER.info("registered [${PlayerLedger.registeredKeys.size}] keys in the ledger.")
    }
}

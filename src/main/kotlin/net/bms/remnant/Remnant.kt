package net.bms.remnant

import com.mojang.logging.LogUtils
import org.slf4j.Logger

object Remnant {
    const val MOD_ID: String = "remnant"

    val LOGGER: Logger = LogUtils.getLogger()

    @JvmStatic
    fun init() {
        LOGGER.info("Remnant initialized.")
    }
}

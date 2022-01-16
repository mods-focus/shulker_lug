package place.imphi.mods.focus

import net.fabricmc.api.ModInitializer
import place.imphi.mods.focus.blocks.ShulkerLugBlock
import org.apache.logging.log4j.LogManager

class ShulkerLugMod : ModInitializer {
    override fun onInitialize() {
        // This code runs as soon as Minecraft is in a mod-load-ready state.
        // However, some things (like resources) may still be uninitialized.
        // Proceed with mild caution.
        LOGGER.info("Hi from the focus!")
        ShulkerLugBlock.register()
    }

    companion object {
        // This logger is used to write text to the console and the log file.
        // It is considered best practice to use your mod id as the logger's name.
        // That way, it's clear which mod wrote info, warnings, and errors.
		@JvmField
		val LOGGER = LogManager.getLogger("focus_shulker_lug")
    }
}
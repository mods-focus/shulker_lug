package place.imphi.mods.focus

import net.fabricmc.api.ClientModInitializer
import place.imphi.mods.focus.blocks.ShulkerLugBlock

class ShulkerLugModClient : ClientModInitializer {
    override fun onInitializeClient() {
        ShulkerLugBlock.registerClient()
    }
}
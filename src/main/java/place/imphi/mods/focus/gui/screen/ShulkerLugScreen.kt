package place.imphi.mods.focus.gui.screen

import io.github.cottonmc.cotton.gui.client.CottonInventoryScreen
import place.imphi.mods.focus.gui.description.ShulkerLugGuiDescription
import net.minecraft.entity.player.PlayerInventory
import net.minecraft.text.Text

/**
 * Extremely simple inventory screen, provided with <3 by cottonmc
 */
class ShulkerLugScreen(description: ShulkerLugGuiDescription?, inventory: PlayerInventory?, title: Text?) :
    CottonInventoryScreen<ShulkerLugGuiDescription>(description, inventory, title) {
}
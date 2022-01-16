package place.imphi.mods.focus.gui.description

import io.github.cottonmc.cotton.gui.SyncedGuiDescription
import io.github.cottonmc.cotton.gui.widget.WGridPanel
import io.github.cottonmc.cotton.gui.widget.WItemSlot
import io.github.cottonmc.cotton.gui.widget.data.Insets
import net.minecraft.entity.player.PlayerInventory
import net.minecraft.item.ItemStack
import net.minecraft.screen.ScreenHandlerContext
import net.minecraft.screen.ScreenHandlerType
import net.minecraft.screen.slot.Slot
import place.imphi.mods.focus.block_entities.ShulkerLugBlockEntity

/**
 * Extremely simple GUI description, provided with <3 by cottonmc
 * It's a glorified dispenser interface, ngl
 */
class ShulkerLugGuiDescription(syncId: Int, playerInventory: PlayerInventory, context: ScreenHandlerContext) :
    SyncedGuiDescription(ScreenHandlerType.GENERIC_3X3, syncId, playerInventory, getBlockInventory(context, ShulkerLugBlockEntity.INVENTORY_SIZE), getBlockPropertyDelegate(context)) {
    init {
        val root = WGridPanel()
        setRootPanel(root)
        root.setSize(300, 200)
        root.insets = Insets.ROOT_PANEL
        val itemSlot = WItemSlot.of(blockInventory, 0, 3, 3)
        itemSlot.setFilter { toInsert -> toInsert.item.canBeNested() }
        root.add(itemSlot, 4, 1)
        root.add(createPlayerInventoryPanel(), 0, 3)
        root.validate(this)
    }
}
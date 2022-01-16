package place.imphi.mods.focus.block_entities

import place.imphi.mods.focus.blocks.ShulkerLugBlock
import place.imphi.mods.focus.gui.description.ShulkerLugGuiDescription
import place.imphi.mods.focus.utils.simpleRandom
import net.minecraft.block.Block
import net.minecraft.block.BlockState
import net.minecraft.block.ShulkerBoxBlock
import net.minecraft.block.entity.LootableContainerBlockEntity
import net.minecraft.entity.player.PlayerEntity
import net.minecraft.entity.player.PlayerInventory
import net.minecraft.inventory.Inventories
import net.minecraft.inventory.SidedInventory
import net.minecraft.item.BlockItem
import net.minecraft.item.ItemStack
import net.minecraft.nbt.NbtCompound
import net.minecraft.screen.NamedScreenHandlerFactory
import net.minecraft.screen.ScreenHandler
import net.minecraft.screen.ScreenHandlerContext
import net.minecraft.screen.ShulkerBoxScreenHandler
import net.minecraft.text.LiteralText
import net.minecraft.text.Text
import net.minecraft.util.collection.DefaultedList
import net.minecraft.util.math.BlockPos
import net.minecraft.util.math.Direction
import java.util.stream.IntStream

/**
 * Definition of the Shulker Lug Block Entity,
 * in charge of handling the inventory-side of the block
 */
class ShulkerLugBlockEntity(pos: BlockPos?, state: BlockState?) :
    LootableContainerBlockEntity(ShulkerLugBlock.SHULKER_LUG_BLOCK_ENTITY, pos, state), SidedInventory,
    NamedScreenHandlerFactory {
    companion object {
        const val INVENTORY_SIZE = 9
        const val ITEMS_KEY = "Items"
        const val NEXT_SEED_KEY = "Seed"

        /** Verbose method to return the NBT data of the stack */
        private fun getNBT(stack: ItemStack): NbtCompound {
            return BlockItem.getBlockEntityNbt(stack)!!
        }

        /**
         * Returns an instance of a DefaultedList.
         * It's important to work with the same "inventory" over the
         * entire flow of working with the lug's inventory.
         * Otherwise, you may not actually change the lug's content
         * when you save the NBT data
         */
        fun getInventory(stack: ItemStack): DefaultedList<ItemStack>? {
            val nbt = getNBT(stack)
            if (nbt.contains(ITEMS_KEY, 9)) {
                val inventory = DefaultedList.ofSize(INVENTORY_SIZE, ItemStack.EMPTY)
                Inventories.readNbt(nbt, inventory)
                return inventory
            }
            return null
        }

        /**
         * Returns the next item in the pseudorandom line
         */
        fun getNext(inventory: DefaultedList<ItemStack>, stack: ItemStack): ItemStack? {
            val nbt = getNBT(stack)
            val valid = inventory.filter { testStack -> !testStack.isEmpty && testStack.item is BlockItem }
            if (valid.isNotEmpty()) {
                val seed = nbt.getLong(NEXT_SEED_KEY)
                val random = simpleRandom(seed)
                return valid.elementAt((random % valid.size).toInt())
            }
            return null
        }

        /**
         * Consumes one of the next item in the pseudorandom line
         * and writes it to the item's NBT data
         */
        fun consume(inventory: DefaultedList<ItemStack>, stack: ItemStack) {
            val next = getNext(inventory, stack) ?: return
            val nbt = getNBT(stack)

            val valid = inventory.filter { inventoryStack -> !inventoryStack.isEmpty }

            next.decrement(1)

            Inventories.writeNbt(nbt, inventory, valid.count() == 1 && next.count == 0)
            BlockItem.setBlockEntityNbt(stack, ShulkerLugBlock.SHULKER_LUG_BLOCK_ENTITY, nbt)
        }

        /**
         * Generates the next pseudorandom value and
         * writes it to the item's NBT data
         */
        fun step(stack: ItemStack) {
            val nbt = getNBT(stack)
            val seed = nbt.getLong(NEXT_SEED_KEY)
            val random = simpleRandom(seed)
            nbt.putLong(NEXT_SEED_KEY, random)
            BlockItem.setBlockEntityNbt(stack, ShulkerLugBlock.SHULKER_LUG_BLOCK_ENTITY, nbt)
        }
    }

    val AVAILABLE_SLOTS: IntArray = IntStream.range(0, INVENTORY_SIZE).toArray()
    var inventory = DefaultedList.ofSize(INVENTORY_SIZE, ItemStack.EMPTY)
    var seed = Math.random().toLong()

    override fun readNbt(nbt: NbtCompound) {
        super.readNbt(nbt)
        readInventoryNbt(nbt)
        seed = nbt.getLong(NEXT_SEED_KEY)
    }

    override fun writeNbt(nbt: NbtCompound) {
        super.writeNbt(nbt)
        if (!serializeLootTable(nbt)) {
            Inventories.writeNbt(nbt, this.inventory, false)
        }
        nbt.putLong(NEXT_SEED_KEY, seed)
    }

    fun readInventoryNbt(nbt: NbtCompound) {
        this.inventory = DefaultedList.ofSize(size(), ItemStack.EMPTY)
        if (!deserializeLootTable(nbt) && nbt.contains(ITEMS_KEY, 9)) {
            Inventories.readNbt(nbt, this.inventory)
        }
    }

    override fun size(): Int {
        return inventory.size
    }

    override fun getAvailableSlots(side: Direction?): IntArray {
        return AVAILABLE_SLOTS
    }

    override fun canInsert(slot: Int, stack: ItemStack, dir: Direction?): Boolean {
        return stack.item.canBeNested()
    }

    override fun isValid(slot: Int, stack: ItemStack): Boolean {
        return stack.item.canBeNested()
    }

    override fun canExtract(slot: Int, stack: ItemStack?, dir: Direction?): Boolean {
        return true
    }

    override fun createMenu(syncId: Int, inv: PlayerInventory, player: PlayerEntity): ScreenHandler {
        return ShulkerLugGuiDescription(syncId, inv, ScreenHandlerContext.create(world, pos))
    }

    override fun getDisplayName(): Text {
        return LiteralText("Shulker Lug")
    }

    override fun getContainerName(): Text {
        return LiteralText("Shulker Lug")
    }

    override fun createScreenHandler(syncId: Int, playerInventory: PlayerInventory?): ScreenHandler {
        return ShulkerBoxScreenHandler(syncId, playerInventory, this)
    }

    override fun getInvStackList(): DefaultedList<ItemStack> {
        return this.inventory
    }

    override fun setInvStackList(list: DefaultedList<ItemStack>) {
        this.inventory = list
    }

}
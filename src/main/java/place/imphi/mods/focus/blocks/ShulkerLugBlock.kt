package place.imphi.mods.focus.blocks

import net.fabricmc.fabric.api.`object`.builder.v1.block.FabricBlockSettings
import net.fabricmc.fabric.api.`object`.builder.v1.block.entity.FabricBlockEntityTypeBuilder
import net.fabricmc.fabric.api.client.screenhandler.v1.ScreenRegistry
import net.fabricmc.fabric.api.item.v1.FabricItemSettings
import net.fabricmc.fabric.api.screenhandler.v1.ScreenHandlerRegistry
import net.minecraft.advancement.criterion.Criteria
import net.minecraft.block.*
import net.minecraft.block.entity.BlockEntityType
import net.minecraft.block.piston.PistonBehavior
import net.minecraft.client.item.TooltipContext
import net.minecraft.entity.ItemEntity
import net.minecraft.entity.player.PlayerEntity
import net.minecraft.inventory.Inventories
import net.minecraft.item.*
import net.minecraft.loot.context.LootContext
import net.minecraft.loot.context.LootContextParameters
import net.minecraft.screen.ScreenHandlerContext
import net.minecraft.screen.ScreenHandlerType
import net.minecraft.server.network.ServerPlayerEntity
import net.minecraft.sound.SoundCategory
import net.minecraft.state.property.Property
import net.minecraft.text.LiteralText
import net.minecraft.text.Text
import net.minecraft.text.TranslatableText
import net.minecraft.util.ActionResult
import net.minecraft.util.Formatting
import net.minecraft.util.Hand
import net.minecraft.util.collection.DefaultedList
import net.minecraft.util.hit.BlockHitResult
import net.minecraft.util.math.BlockPos
import net.minecraft.util.registry.Registry
import net.minecraft.world.BlockView
import net.minecraft.world.World
import net.minecraft.world.event.GameEvent
import place.imphi.mods.focus.block_entities.ShulkerLugBlockEntity
import place.imphi.mods.focus.gui.description.ShulkerLugGuiDescription
import place.imphi.mods.focus.gui.screen.ShulkerLugScreen
import place.imphi.mods.focus.utils.Identifier
import java.util.function.Consumer

/**
 * Definition of the Shulker Lug Block
 * This block works as a Shulker Box of sorts, allowing the deployment of block inside it
 */
class ShulkerLugBlock(settings: Settings?) : BlockWithEntity(settings) {
    companion object {
        val SHULKER_LUG_BLOCK = ShulkerLugBlock(FabricBlockSettings.of(Material.SHULKER_BOX).strength(4.0f))
        val ID = Identifier("shulker_lug")
        val CONTENTS = Identifier("contents")
        val SHULKER_LUG_ITEM = ShulkerLugItem(SHULKER_LUG_BLOCK, FabricItemSettings().group(ItemGroup.MISC).maxCount(1))
        lateinit var SHULKER_LUG_BLOCK_ENTITY: BlockEntityType<ShulkerLugBlockEntity>
        lateinit var SHULKER_LUG_SCREEN_HANDLER_TYPE: ScreenHandlerType<ShulkerLugGuiDescription>

        /**
         * The server side of the block registration happens here
         */
        fun register() {
            // Block and item register
            Registry.register(Registry.BLOCK, ID, SHULKER_LUG_BLOCK)
            Registry.register(Registry.ITEM, ID, SHULKER_LUG_ITEM)

            // Screen handler register
            SHULKER_LUG_SCREEN_HANDLER_TYPE = ScreenHandlerRegistry.registerSimple(ID) { syncId, inventory ->
                ShulkerLugGuiDescription(
                    syncId, inventory, ScreenHandlerContext.EMPTY
                )
            }

            // Block entity register
            SHULKER_LUG_BLOCK_ENTITY =
                Registry.register(
                    Registry.BLOCK_ENTITY_TYPE, Identifier("shulker_lug"), FabricBlockEntityTypeBuilder.create(
                        { pos, state -> ShulkerLugBlockEntity(pos, state) },
                        SHULKER_LUG_BLOCK
                    ).build(null)
                )
        }

        /**
         * Client side block registration
         */
        fun registerClient() {
            // Screen handler register
            ScreenRegistry.register(SHULKER_LUG_SCREEN_HANDLER_TYPE) { gui, inventory, title ->
                ShulkerLugScreen(
                    gui,
                    inventory,
                    title
                )
            }

        }
    }

    /**
     * Definition of the Shulker Lug item.
     * Maybe it shouldn't be internal?
     */
    class ShulkerLugItem(block: Block?, settings: Settings?) : AliasedBlockItem(block, settings) {
        /** Disgusting redefinition of private method */
        private fun <T : Comparable<T>?> with(state: BlockState, property: Property<T>, name: String): BlockState {
            return property.parse(name).map { value: T ->
                state.with(
                    property,
                    value
                ) as BlockState
            }.orElse(state) as BlockState
        }

        /** Disgusting redefinition of private method */
        private fun placeFromTag(pos: BlockPos, world: World, stack: ItemStack, state: BlockState): BlockState {
            var blockState = state
            val nbtCompound = stack.nbt
            if (nbtCompound != null) {
                val nbtCompound2 = nbtCompound.getCompound("BlockStateTag")
                val stateManager = state.block.stateManager
                val var9: Iterator<*> = nbtCompound2.keys.iterator()
                while (var9.hasNext()) {
                    val string = var9.next() as String
                    val property = stateManager.getProperty(string)
                    if (property != null) {
                        val string2 = nbtCompound2[string]!!.asString()
                        blockState = with(blockState, property, string2)
                    }
                }
            }
            if (blockState !== state) {
                world.setBlockState(pos, blockState, 2)
            }
            return blockState
        }

        /**
         * This method extends super.place() adding the option of
         * __not actually__ placing the block
         */
        private fun fakePlace(context: ItemPlacementContext?, place: Boolean): ActionResult {
            return if (!context!!.canPlace()) {
                ActionResult.FAIL
            } else {
                val itemPlacementContext = getPlacementContext(context)
                if (itemPlacementContext == null) {
                    ActionResult.FAIL
                } else {
                    val blockState = getPlacementState(itemPlacementContext)
                    if (blockState == null) {
                        ActionResult.FAIL
                    } else if (!this.place(itemPlacementContext, blockState)) {
                        ActionResult.FAIL
                    } else {
                        val blockPos = itemPlacementContext.blockPos
                        val world = itemPlacementContext.world
                        val playerEntity = itemPlacementContext.player
                        val itemStack = itemPlacementContext.stack
                        var blockState2 = world.getBlockState(blockPos)
                        if (blockState2.isOf(blockState.block)) {
                            blockState2 = placeFromTag(blockPos, world, itemStack, blockState2)
                            postPlacement(blockPos, world, playerEntity, itemStack, blockState2)
                            blockState2.block.onPlaced(world, blockPos, blockState2, playerEntity, itemStack)
                            if (playerEntity is ServerPlayerEntity && place) {
                                Criteria.PLACED_BLOCK.trigger(playerEntity as ServerPlayerEntity?, blockPos, itemStack)
                            }
                        }
                        val blockSoundGroup = blockState2.soundGroup
                        world.playSound(
                            playerEntity,
                            blockPos,
                            getPlaceSound(blockState2),
                            SoundCategory.BLOCKS,
                            (blockSoundGroup.getVolume() + 1.0f) / 2.0f,
                            blockSoundGroup.getPitch() * 0.8f
                        )
                        if (place) world.emitGameEvent(playerEntity, GameEvent.BLOCK_PLACE, blockPos)
                        if ((playerEntity == null || !playerEntity.abilities.creativeMode) && place) {
                            itemStack.decrement(1)
                        }
                        ActionResult.success(world.isClient)
                    }
                }
            }
        }

        // Disgusting hack to place another block instead of the Shulker Lug at hand
        var nextBlock: Block? = null
        override fun getBlock(): Block {
            return nextBlock ?: super.getBlock()
        }

        override fun place(context: ItemPlacementContext): ActionResult {
            // Is sneaking, place the lug
            if (context.player?.isSneaking == true) return super.place(context)
            val stack = context.player?.getStackInHand(context.hand)
            if (stack?.hasNbt() == true) {
                val inventory = ShulkerLugBlockEntity.getInventory(stack) ?: return super.place(context)
                // Prepare the lug to provide the next block in the pseudorandom line
                ShulkerLugBlockEntity.step(stack)
                val nextStack = ShulkerLugBlockEntity.getNext(inventory, stack) ?: return super.place(context)
                val nextItem = nextStack.item
                if (nextItem is BlockItem) {
                    // Set the block to be placed to the one selected...
                    nextBlock = nextItem.block
                    // ... place it...
                    val res = fakePlace(context, false)
                    // ... remove it from the internal NBT stack ...
                    ShulkerLugBlockEntity.consume(inventory, stack)
                    // ... and stop the block overriding
                    nextBlock = null
                    return res
                }
            }
            // If the lug's empty, place it
            return super.place(context)
        }

        override fun canBeNested(): Boolean {
            return false
        }
    }

    override fun createBlockEntity(pos: BlockPos?, state: BlockState?): ShulkerLugBlockEntity {
        return ShulkerLugBlockEntity(pos, state)
    }

    override fun getPistonBehavior(state: BlockState?): PistonBehavior {
        return PistonBehavior.DESTROY
    }

    override fun getRenderType(state: BlockState?): BlockRenderType {
        return BlockRenderType.MODEL
    }

    override fun onUse(
        state: BlockState?,
        world: World,
        pos: BlockPos,
        player: PlayerEntity,
        hand: Hand,
        hit: BlockHitResult?
    ): ActionResult {
        // On use open the lug's interface
        player.openHandledScreen(state?.createScreenHandlerFactory(world, pos))
        return ActionResult.SUCCESS
    }

    /**
     * This method's been copied straight from the ShulkerBoxBlock definition,
     * and I've got no idea about what it does or how it does it, but it
     * works :D
     */
    override fun onBreak(world: World, pos: BlockPos, state: BlockState?, player: PlayerEntity) {
        val blockEntity = world.getBlockEntity(pos)
        if (blockEntity is ShulkerLugBlockEntity) {
            if (!world.isClient && !blockEntity.isEmpty) {
                // If it's not empty, spawn entity with items
                val itemStack = ItemStack(SHULKER_LUG_BLOCK)
                blockEntity.setStackNbt(itemStack)
                if (blockEntity.hasCustomName()) {
                    itemStack.setCustomName(blockEntity.customName)
                }
                val itemEntity =
                    ItemEntity(world, pos.x.toDouble() + 0.5, pos.y.toDouble() + 0.5, pos.z.toDouble() + 0.5, itemStack)
                itemEntity.setToDefaultPickupDelay()
                world.spawnEntity(itemEntity)
            } else if (!world.isClient && blockEntity.isEmpty) {
                // If it's  empty, spawn entity without nbt
                val itemStack = ItemStack(SHULKER_LUG_BLOCK)
                val itemEntity =
                    ItemEntity(world, pos.x.toDouble() + 0.5, pos.y.toDouble() + 0.5, pos.z.toDouble() + 0.5, itemStack)
                itemEntity.setToDefaultPickupDelay()
                world.spawnEntity(itemEntity)
            } else {
                blockEntity.checkLootInteraction(player)
            }
        }

        super.onBreak(world, pos, state, player)
    }


    override fun getPickStack(world: BlockView, pos: BlockPos?, state: BlockState?): ItemStack? {
        println("PICK")
        val itemStack = super.getPickStack(world, pos, state)
        world.getBlockEntity(pos, SHULKER_LUG_BLOCK_ENTITY).ifPresent { blockEntity: ShulkerLugBlockEntity ->
            blockEntity.setStackNbt(
                itemStack
            )
        }
        return itemStack
    }

    /**
     * This method's been copied straight from the ShulkerBoxBlock definition,
     * and I've got no idea about what it does or how it does it, but it
     * works :D
     */
    override fun getDroppedStacks(state: BlockState?, initialBuilder: LootContext.Builder): List<ItemStack?>? {
        val builder = initialBuilder
        val blockEntity = builder.getNullable(LootContextParameters.BLOCK_ENTITY) as ShulkerLugBlockEntity?
        if (blockEntity is ShulkerLugBlockEntity) {
            println(blockEntity.size())
            builder.putDrop(CONTENTS) { _, consumer: Consumer<ItemStack?> ->
                for (i in 0 until blockEntity.size()) {
                    println(blockEntity.getStack(i))
                    consumer.accept(blockEntity.getStack(i))
                }
            }
        }
        println(builder)
        @Suppress("DEPRECATION")
        return super.getDroppedStacks(state, builder)
    }

    /**
     * If the player's shifting while empty-handed, instamine
     */
    override fun calcBlockBreakingDelta(
        state: BlockState?,
        player: PlayerEntity?,
        world: BlockView?,
        pos: BlockPos?
    ): Float {
        // IMPORTANT: Do NOT use mainHandStack == ItemStack.EMPTY, USE .isEmpty!!
        val instaMine = player?.mainHandStack?.isEmpty == true && player.isSneaking
        @Suppress("DEPRECATION")
        return if (instaMine) 1F else return super.calcBlockBreakingDelta(state, player, world, pos)
    }

    /**
     * This method's been copied straight from the ShulkerBoxBlock definition,
     * and I've got no idea about what it does or how it does it, but it
     * works :D
     */
    override fun appendTooltip(
        stack: ItemStack?,
        world: BlockView?,
        tooltip: MutableList<Text?>,
        options: TooltipContext?
    ) {
        super.appendTooltip(stack, world, tooltip, options)
        val nbtCompound = BlockItem.getBlockEntityNbt(stack)
        if (nbtCompound != null) {
            if (nbtCompound.contains("LootTable", 8)) {
                tooltip.add(LiteralText("???????"))
            }
            if (nbtCompound.contains("Items", 9)) {
                val defaultedList = DefaultedList.ofSize(27, ItemStack.EMPTY)
                Inventories.readNbt(nbtCompound, defaultedList)
                var i = 0
                var j = 0
                for (itemStack in defaultedList) {
                    if (itemStack.isEmpty) continue
                    ++j
                    if (i > 4) continue
                    ++i
                    val mutableText = itemStack.name.shallowCopy()
                    mutableText.append(" x").append(itemStack.count.toString())
                    tooltip.add(mutableText)
                }
                if (j - i > 0) {
                    tooltip.add(TranslatableText("container.shulkerBox.more", j - i).formatted(Formatting.ITALIC))
                }
            }
        }
    }

}
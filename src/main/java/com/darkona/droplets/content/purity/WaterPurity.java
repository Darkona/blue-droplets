package com.darkona.droplets.content.purity;

import com.darkona.droplets.BlueDroplets;
import com.darkona.droplets.content.registry.ItemInit;
import com.darkona.droplets.content.registry.ThirstComponent;
import com.darkona.droplets.foundation.common.event.RegisterThirstValueEvent;
import com.darkona.droplets.foundation.config.CommonConfig;
import net.minecraft.advancements.CriteriaTriggers;
import net.minecraft.stats.Stats;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.AreaEffectCloud;
import net.minecraft.world.entity.boss.enderdragon.EnderDragon;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.dispenser.BlockSource;
import net.minecraft.core.dispenser.DefaultDispenseItemBehavior;
import net.minecraft.core.dispenser.DispenseItemBehavior;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.Style;
import net.minecraft.network.chat.contents.PlainTextContents;
import net.minecraft.network.chat.contents.TranslatableContents;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.TickTask;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.FluidTags;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ItemUtils;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.alchemy.PotionContents;
import net.minecraft.world.item.alchemy.Potions;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.BucketPickup;
import net.minecraft.world.level.block.DispenserBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.level.gameevent.GameEvent;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.ModList;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.event.entity.player.ItemTooltipEvent;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;
import net.neoforged.neoforge.fluids.FluidStack;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.Random;


@SuppressWarnings({"SpellCheckingInspection","unused"})
@EventBusSubscriber
public class WaterPurity
{
    private static final List<ContainerWithPurity> codeContainers = new CopyOnWriteArrayList<>();
    private static volatile List<ContainerWithPurity> waterContainers = List.of();
    private static final List<Block> fillablesWithPurity = new ArrayList<>();
    public static final int MIN_PURITY = 0;
    public static final int MAX_PURITY = 3;

    /**
     * Specifies the purity of a block filled with water. Has to be incremented by one
     * number because while using Mixins, generally every block that
     * implements water purity has a mixin-able "createBlockStateDefinition" function,
     * but doesn't have an as-accessible "setDefaultState" function. Thus i am forced to
     * use 0 as the "null" value for the block purity.
     * <br><br>
     * On the bright side, there is a function in this class which takes in a BlockState and
     * returns the already-modified purity
     * */
    public static final IntegerProperty BLOCK_PURITY = IntegerProperty.create("purity", 0, 4);

    public static void init()
    {
        registerDispenserBehaviours();
        registerContainers();
        registerFillables();

        if(ModList.get().isLoaded("farmersrespite"))
        {
            registerFarmersRespiteContainers();
//            fillablesWithPurity.add(FRBlocks.KETTLE.get());
        }

        if(ModList.get().isLoaded("brewinandchewin"))
        {
//            registerBrewinAndChewinContainers();
        }
    }

    private static void registerContainers()
    {
        addContainer(new ContainerWithPurity(Items.GLASS_BOTTLE,
                PotionContents.createItemStack(Items.POTION,Potions.WATER).getItem()).setEqualsFilled(itemStack ->
                itemStack.is(Items.POTION) && itemStack.get(DataComponents.POTION_CONTENTS).is(Potions.WATER)));
        addContainer(new ContainerWithPurity(ItemInit.TERRACOTTA_BOWL.get(),
                ItemInit.TERRACOTTA_WATER_BOWL.get()));
        addContainer(new ContainerWithPurity(Items.BUCKET,
                Items.WATER_BUCKET, false).canHarvestRunningWater(false));
    }

    private static void registerFillables()
    {
        fillablesWithPurity.add(Blocks.CAULDRON);
        fillablesWithPurity.add(Blocks.WATER_CAULDRON);
    }

    private static void registerFarmersRespiteContainers()
    {
//        waterContainers.add(new ContainerWithPurity(FRItems.GREEN_TEA));
//        waterContainers.add(new ContainerWithPurity(FRItems.YELLOW_TEA));
//        waterContainers.add(new ContainerWithPurity(FRItems.BLACK_TEA));
//        waterContainers.add(new ContainerWithPurity(FRItems.ROSE_HIP_TEA));
//        waterContainers.add(new ContainerWithPurity(FRItems.DANDELION_TEA));
//        waterContainers.add(new ContainerWithPurity(FRItems.COFFEE));
//        waterContainers.add(new ContainerWithPurity(FRItems.GAMBLERS_TEA));
//        waterContainers.add(new ContainerWithPurity(FRItems.PURULENT_TEA));
    }

//    private static void registerBrewinAndChewinContainers()
//    {
//        waterContainers.add(new ContainerWithPurity(BCItems.BEER.get()));
//        waterContainers.add(new ContainerWithPurity(BCItems.VODKA.get()));
//        waterContainers.add(new ContainerWithPurity(BCItems.RICE_WINE.get()));
//        waterContainers.add(new ContainerWithPurity(BCItems.STRONGROOT_ALE.get()));
//        waterContainers.add(new ContainerWithPurity(BCItems.PALE_JANE.get()));
//        waterContainers.add(new ContainerWithPurity(BCItems.SALTY_FOLLY.get()));
//        waterContainers.add(new ContainerWithPurity(BCItems.STEEL_TOE_STOUT.get()));
//        waterContainers.add(new ContainerWithPurity(BCItems.GLITTERING_GRENADINE.get()));
//        waterContainers.add(new ContainerWithPurity(BCItems.BLOODY_MARY.get()));
//        waterContainers.add(new ContainerWithPurity(BCItems.RED_RUM.get()));
//        waterContainers.add(new ContainerWithPurity(BCItems.WITHERING_DROSS.get()));
//        waterContainers.add(new ContainerWithPurity(BCItems.KOMBUCHA.get()));
//    }

    @SubscribeEvent
    static void fillablesHandler(PlayerInteractEvent.RightClickBlock event)
    {
        if (event.getEntity() instanceof ServerPlayer player && isWaterFilledContainer(event.getItemStack()))
        {
            ServerLevel level = player.serverLevel();
            BlockPos pos = event.getHitVec().getBlockPos();
            BlockState blockState = level.getBlockState(pos);
            //Trying to make compat with unregistered fluid container
            BlockEntity entity = level.getBlockEntity(pos);

            if (isFillableBlock(blockState) ||(entity != null && Capabilities.FluidHandler.BLOCK.getCapability(level,pos,blockState,entity,null) != null))
            {
                int purity = getPurity(event.getItemStack());

                int blockPurity = !blockState.hasProperty(BLOCK_PURITY) ?
                        3 : (blockState.getValue(BLOCK_PURITY) - 1 < 0 ?
                            3 : blockState.getValue(BLOCK_PURITY) - 1);

                MinecraftServer server = level.getServer();
                server.tell(new TickTask(server.getTickCount(), () -> {
                    BlockState blockState1 = level.getBlockState(pos);

                    if(!blockState1.hasProperty(BLOCK_PURITY))
                        return;

                    level.setBlock(
                            pos,
                            blockState1.setValue(BLOCK_PURITY, Math.min(purity, blockPurity) + 1),
                            0
                    );
                }));
            }
        }

    }
    /**
     * Registers new custom water container
     * the container will be taken into consider of purity
     * Don't use it directly. Trying to subscribe #{@link RegisterThirstValueEvent}
     */
    @Deprecated
    public static void addContainer(ContainerWithPurity container)
    {
        codeContainers.add(container);
        waterContainers = merge(waterContainers, List.of(container));
    }

    /**
     * Replaces the data-driven containers (config and {@link RegisterThirstValueEvent}); code containers are kept.
     * One container per filled item: the first registered wins.
     */
    public static void setContainers(List<ContainerWithPurity> containers)
    {
        waterContainers = merge(codeContainers, containers);
    }

    private static List<ContainerWithPurity> merge(List<ContainerWithPurity> first, List<ContainerWithPurity> second)
    {
        Map<Item, ContainerWithPurity> byFilled = new LinkedHashMap<>();
        for (ContainerWithPurity container : first)
            byFilled.putIfAbsent(container.getFilledItem(), container);
        for (ContainerWithPurity container : second)
            byFilled.putIfAbsent(container.getFilledItem(), container);
        return List.copyOf(byFilled.values());
    }

    /**
     * Returns the filled equivalent of the water container given in input.
     * The second parameter specifies if the container inputted is the empty or
     * filled version
     */
    public static ItemStack getFilledContainer(ItemStack container, boolean fromFilled)
    {
        for (ContainerWithPurity waterContainer : waterContainers)
            if ((!fromFilled && waterContainer.equalsEmpty(container)) || (fromFilled && waterContainer.equalsFilled(container)))
                return waterContainer.getFilledItem().getDefaultInstance();

        return ItemStack.EMPTY.copy();
    }

    /**
     * Fills empty buckets, glass bottles and terracotta bowls from water in the world, with the purity of that water.
     * Replaces vanilla {@code use} for those cases; dragon breath and everything else is left to vanilla.
     */
    @SubscribeEvent
    static void fillWithPurity(PlayerInteractEvent.RightClickItem event)
    {
        ItemStack item = event.getItemStack();
        boolean bucket = item.is(Items.BUCKET);
        boolean bottle = item.is(Items.GLASS_BOTTLE);
        if (!bucket && !bottle && !item.is(ItemInit.TERRACOTTA_BOWL.get()))
            return;

        Player player = event.getEntity();
        Level level = event.getLevel();
        if (bottle && !level.getEntitiesOfClass(AreaEffectCloud.class, player.getBoundingBox().inflate(2.0),
                cloud -> cloud.isAlive() && cloud.getOwner() instanceof EnderDragon).isEmpty())
            return;

        BlockHitResult hit = pickFluid(player, bucket ? ClipContext.Fluid.SOURCE_ONLY : ClipContext.Fluid.ANY);
        if (hit.getType() != HitResult.Type.BLOCK)
            return;
        BlockPos pos = hit.getBlockPos();
        BlockState state = level.getBlockState(pos);
        if (!state.getFluidState().is(FluidTags.WATER) || !level.mayInteract(player, pos))
            return;

        int purity = getBlockPurity(level, pos);
        ItemStack filled;
        if (bucket)
        {
            if (!(state.getBlock() instanceof BucketPickup pickup) || !player.mayUseItemAt(pos.relative(hit.getDirection()), hit.getDirection(), item))
                return;
            filled = pickup.pickupBlock(player, level, pos, state);
            if (filled.isEmpty())
                return;
            addPurity(filled, purity);
            pickup.getPickupSound(state).ifPresent(sound -> player.playSound(sound, 1.0F, 1.0F));
            if (player instanceof ServerPlayer serverPlayer)
                CriteriaTriggers.FILLED_BUCKET.trigger(serverPlayer, filled);
        }
        else
        {
            filled = addPurity(bottle ? PotionContents.createItemStack(Items.POTION, Potions.WATER) : new ItemStack(ItemInit.TERRACOTTA_WATER_BOWL.get()), purity);
            level.playSound(player, player.getX(), player.getY(), player.getZ(), bottle ? SoundEvents.BOTTLE_FILL : SoundEvents.BUCKET_FILL, SoundSource.NEUTRAL, 1.0F, 1.0F);
        }

        level.gameEvent(player, GameEvent.FLUID_PICKUP, pos);
        player.awardStat(Stats.ITEM_USED.get(item.getItem()));
        player.setItemInHand(event.getHand(), ItemUtils.createFilledResult(item, player, filled));
        event.setCancellationResult(InteractionResult.sidedSuccess(level.isClientSide()));
        event.setCanceled(true);
    }

    /**
     * Same ray as vanilla {@code Item.getPlayerPOVHitResult}: from the eyes, along the view, up to the block interaction range.
     */
    public static BlockHitResult pickFluid(Player player, ClipContext.Fluid fluid)
    {
        Vec3 eye = player.getEyePosition();
        Vec3 end = eye.add(player.getViewVector(1.0F).scale(player.blockInteractionRange()));
        return player.level().clip(new ClipContext(eye, end, ClipContext.Block.OUTLINE, fluid, player));
    }

    /**
     * Renders the client-side tooltip for items that have a water
     * purity tag
     */
    @SubscribeEvent
    static void renderPurityTooltip(ItemTooltipEvent event)
    {
        if(isWaterFilledContainer(event.getItemStack()))
        {
            int purity = getPurity(event.getItemStack());
            if(purity >= MIN_PURITY && purity <= MAX_PURITY)
            {
                String purityText = getPurityText(purity);

                int purityColor = getPurityColor(purity);

                assert purityText != null;
                event.getToolTip()
                        .add(MutableComponent
                                .create(new PlainTextContents.LiteralContents(purityText))
                                .setStyle(Style.EMPTY.withColor(purityColor)));
            }
        }
    }

    public static boolean isWaterFilledContainer(ItemStack item)
    {
        for (ContainerWithPurity waterContainer : waterContainers)
            if (waterContainer.equalsFilled(item))
                return true;

        return false;
    }

    public static boolean isEmptyWaterContainer(ItemStack item)
    {
        for (ContainerWithPurity waterContainer : waterContainers)
            if (waterContainer.equalsEmpty(item))
                return true;

        return false;
    }

    static boolean isFillableBlock(Block block)
    {
        for (Block fillable : fillablesWithPurity)
        {
            if (fillable == block)
                return true;
        }

        return false;
    }

    static boolean isFillableBlock(BlockState blockState)
    {
        return isFillableBlock(blockState.getBlock());
    }

    /**
     * Reads the purity from an item without modifying it; missing or invalid purity reads as the default purity
     */
    public static Integer getPurity(ItemStack item)
    {
        return sanitizePurity(item.get(ThirstComponent.PURITY));
    }

    /**
     * Reads the purity from a fluid without modifying it; missing or invalid purity reads as the default purity
     */
    public static Integer getPurity(FluidStack fluid)
    {
        return sanitizePurity(fluid.get(ThirstComponent.PURITY));
    }

    public static int sanitizePurity(@Nullable Integer purity)
    {
        return purity == null || purity < MIN_PURITY || purity > MAX_PURITY ? CommonConfig.DEFAULT_PURITY.get() : purity;
    }

    /**
     * Returns the purity string in the language selected by the player
     */
    public static String getPurityText(int purity)
    {
        String purityText = purity == 0 ? "dirty" :
                purity == 1 ? "slightly_dirty" :
                        purity == 2 ? "acceptable" : "purified";

        return MutableComponent.create(new TranslatableContents(BlueDroplets.ID + ".purity." + purityText,purityText,TranslatableContents.NO_ARGS)).getString();
    }

    /**
     * Returns the purity color in decimal format
     */
    public static int getPurityColor(int purity)
    {
        return purity == 0 ? 11028517 :
                purity == 1 ? 7957617 :
                purity == 2 ? 6128285 : 2208255;
    }

    /**
     * Returns the already-adjusted water purity level of a
     * block with the BLOCK_PURITY tag
     */
    public static int getBlockPurity(BlockState blockState)
    {
        return sanitizePurity(blockState.hasProperty(BLOCK_PURITY) ? blockState.getValue(BLOCK_PURITY) - 1 : null);
    }

    public static boolean hasPurity(ItemStack item)
    {
        return item.get(ThirstComponent.PURITY) != null;
    }

    public static boolean hasPurity(FluidStack fluid)
    {
        return fluid.get(ThirstComponent.PURITY) != null;
    }

    /**
     * Shorthand for adding purity to an item if in a context where the block
     * the player is pointing at is accessible
     */
    public static ItemStack addPurity(ItemStack item, BlockPos pos, Level level)
    {
        return addPurity(item, getBlockPurity(level, pos));
    }


    /**
     * Sets the purity component on an item; it is always stored, also for the default purity.
     * Invalid values are stored as the default purity.
     */
    public static ItemStack addPurity(ItemStack item, int purity)
    {
        item.set(ThirstComponent.PURITY, sanitizePurity(purity));
        return item;
    }

    /**
     * Sets the purity component on a fluid; it is always stored, also for the default purity.
     * Invalid values are stored as the default purity.
     */
    public static FluidStack addPurity(FluidStack fluid, int purity)
    {
        fluid.set(ThirstComponent.PURITY, sanitizePurity(purity));
        return fluid;
    }


    /**
     * Calculates the water purity of a specific block in the level
     */
    public static int getBlockPurity(Level level, BlockPos pos)
    {
        int purity = (pos.getY() > CommonConfig.MOUNTAINS_Y.get().intValue() || pos.getY() < CommonConfig.CAVES_Y.get().intValue())
                && pos.getY() < CommonConfig.MOUNTAINS_Y.get().intValue() - 32 ? 1 : 0;

        if(level.getFluidState(pos).is(FluidTags.WATER))
        {
            if(!level.getFluidState(pos).isSource())
                purity = Math.min(purity + CommonConfig.RUNNING_WATER_PURIFICATION_AMOUNT.get().intValue(), MAX_PURITY);

            return purity;
        }
        else if(level.getBlockState(pos).is(Blocks.WATER_CAULDRON))
        {
            return getBlockPurity(level.getBlockState(pos));
        }
        else
            return CommonConfig.DEFAULT_PURITY.get();
    }

    /**
     * Gives the player effects based on the purity of the water just drunk (missing purity reads as the default)
     * and returns whether thirst and quenched should be added or not
     */
    public static boolean givePurityEffects(Player player, ItemStack item)
    {
        return !isWaterFilledContainer(item) || givePurityEffects(player, getPurity(item));
    }

    /**
     * Calculates purity-derived effects
     */
    public static boolean givePurityEffects(Player player, int purity)
    {
        boolean shouldRegenerate = true;
        Random random = new Random();
        float chance = random.nextFloat();

        switch (purity) {
            case 0 -> {
                if (chance < CommonConfig.DIRTY_NAUSEA_PERCENTAGE.get().intValue() / 100.0f) {
                    if(player instanceof ServerPlayer)
                    {
                        player.addEffect(new MobEffectInstance(MobEffects.CONFUSION, 20 * 5, 0));
                        player.addEffect(new MobEffectInstance(MobEffects.HUNGER, 20 * 30, 0));
                    }

                }

                if (chance <= CommonConfig.DIRTY_POISON_PERCENTAGE.get().intValue() / 100.0f) {
                    if(player instanceof ServerPlayer)
                    {
                        player.addEffect(new MobEffectInstance(MobEffects.POISON, 20 * 10, 0));
                    }
                    shouldRegenerate = false;
                }

            }
            case 1 -> {
                if (chance < CommonConfig.SLIGHTLY_DIRTY_NAUSEA_PERCENTAGE.get().intValue() / 100.0f) {
                    if(player instanceof ServerPlayer)
                    {
                        player.addEffect(new MobEffectInstance(MobEffects.CONFUSION, 20 * 5, 0));
                        player.addEffect(new MobEffectInstance(MobEffects.HUNGER, 20 * 30, 0));
                    }

                }

                if (chance <= CommonConfig.SLIGHTLY_DIRTY_POISON_PERCENTAGE.get().intValue() / 100.0f) {
                    if(player instanceof ServerPlayer)
                    {
                        player.addEffect(new MobEffectInstance(MobEffects.POISON, 20 * 10, 0));
                    }
                    shouldRegenerate = false;
                }

            }
            case 2 -> {
                if (chance < CommonConfig.ACCEPTABLE_NAUSEA_PERCENTAGE.get().intValue() / 100.0f) {
                    if(player instanceof ServerPlayer)
                    {
                        player.addEffect(new MobEffectInstance(MobEffects.CONFUSION, 20 * 5, 0));
                        player.addEffect(new MobEffectInstance(MobEffects.HUNGER, 20 * 30, 0));
                    }

                }

                if (chance <= CommonConfig.ACCEPTABLE_POISON_PERCENTAGE.get().intValue() / 100.0f) {
                    if(player instanceof ServerPlayer)
                    {
                        player.addEffect(new MobEffectInstance(MobEffects.POISON, 20 * 10, 0));
                    }
                    shouldRegenerate = false;
                }

            }
            case 3 -> {
                if (chance < CommonConfig.PURIFIED_NAUSEA_PERCENTAGE.get().intValue() / 100.0f) {
                    if(player instanceof ServerPlayer)
                    {
                        player.addEffect(new MobEffectInstance(MobEffects.CONFUSION, 20 * 5, 0));
                        player.addEffect(new MobEffectInstance(MobEffects.HUNGER, 20 * 30, 0));
                    }

                }

                if (chance <= CommonConfig.PURIFIED_POISON_PERCENTAGE.get().intValue() / 100.0f) {
                    if(player instanceof ServerPlayer)
                    {
                        player.addEffect(new MobEffectInstance(MobEffects.POISON, 20 * 10, 0));
                    }
                    shouldRegenerate = false;
                }

            }
        }

        return shouldRegenerate || CommonConfig.QUENCH_THIRST_WHEN_DEBUFFED.get();
    }


    static void registerDispenserBehaviours()
    {
        DispenseItemBehavior bucketDefaultBehaviour = DispenserBlock.DISPENSER_REGISTRY.get(Items.BUCKET);
        DispenseItemBehavior bottleDefaultBehaviour = DispenserBlock.DISPENSER_REGISTRY.get(Items.GLASS_BOTTLE);

        DispenserBlock.registerBehavior(Items.BUCKET, (block, item) ->
        {
            Level level = block.level();
            BlockPos blockpos = block.pos().relative(block.state().getValue(DispenserBlock.FACING));
            BlockState state = level.getBlockState(blockpos);
            if(state.getFluidState().is(FluidTags.WATER) && state.getFluidState().isSource() && state.getBlock() instanceof BucketPickup pickup)
            {
                ItemStack result = new ItemStack(Items.WATER_BUCKET);
                return getStack(block, item, level, blockpos, result, pickup);
            }
            else
                return bucketDefaultBehaviour.dispense(block,item);

        });

        DispenserBlock.registerBehavior(Items.GLASS_BOTTLE, (block, item) ->
        {
            Level level = block.level();
            BlockPos blockpos = block.pos().relative(block.state().getValue(DispenserBlock.FACING));

            if(level.getFluidState(blockpos).is(FluidTags.WATER))
            {
                ItemStack result = PotionContents.createItemStack(Items.POTION,Potions.WATER);
                return getStack(block, item, level, blockpos, result, null);
            }
            else
                return bottleDefaultBehaviour.dispense(block,item);

        });
    }

    @NotNull
    private static ItemStack getStack(BlockSource block, ItemStack item, Level level, BlockPos blockpos, ItemStack result, @Nullable BucketPickup pickup) {
        level.gameEvent(null, GameEvent.FLUID_PICKUP, blockpos);
        addPurity(result, blockpos, level);

        if(pickup != null)
            pickup.pickupBlock(null,level, blockpos, level.getBlockState(blockpos));

        item.shrink(1);
        if (item.isEmpty()) {
            return result;
        } else
        {
            if (block.blockEntity().insertItem(result)!=result)
            {
                new DefaultDispenseItemBehavior().dispense(block, result);
            }

            return item;
        }
    }
}

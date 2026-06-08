package com.darkona.droplets.content.purity;

import com.darkona.droplets.foundation.config.ClientConfig;
import com.darkona.droplets.foundation.config.PurityConfig;
import com.darkona.droplets.foundation.config.SyncedValues;
import com.darkona.droplets.BlueDroplets;
import com.darkona.droplets.api.event.PurityEffectEvent;
import com.darkona.droplets.content.thirst.ThirstHelper;
import com.darkona.droplets.content.data.BiomeWater;
import com.darkona.droplets.content.data.DimensionWater;
import com.darkona.droplets.content.data.DropletsDataMaps;
import com.darkona.droplets.content.data.DropletsTags;
import com.darkona.droplets.content.registry.EffectInit;
import com.darkona.droplets.content.registry.ItemInit;
import com.darkona.droplets.content.registry.ThirstComponent;
import com.darkona.droplets.core.NumberRows;
import com.darkona.droplets.foundation.common.event.RegisterThirstValueEvent;
import net.minecraft.advancements.CriteriaTriggers;
import net.minecraft.stats.Stats;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.ItemInteractionResult;
import net.minecraft.world.entity.AreaEffectCloud;
import net.minecraft.world.entity.boss.enderdragon.EnderDragon;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraft.core.BlockPos;
import net.minecraft.core.cauldron.CauldronInteraction;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.dispenser.BlockSource;
import net.minecraft.core.dispenser.DefaultDispenseItemBehavior;
import net.minecraft.core.dispenser.DispenseItemBehavior;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.network.chat.Style;
import net.minecraft.network.chat.contents.PlainTextContents;
import net.minecraft.network.chat.contents.TranslatableContents;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.FluidTags;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ItemUtils;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.alchemy.PotionContents;
import net.minecraft.world.item.alchemy.Potions;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.BucketPickup;
import net.minecraft.world.level.block.DispenserBlock;
import net.minecraft.world.level.block.LayeredCauldronBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.gameevent.GameEvent;
import net.minecraft.world.level.material.FluidState;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.entity.player.ItemTooltipEvent;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;
import net.neoforged.neoforge.fluids.FluidStack;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.Supplier;


@SuppressWarnings({"SpellCheckingInspection","unused"})
@EventBusSubscriber
public class WaterPurity
{
    private static final List<ContainerWithPurity> codeContainers = new CopyOnWriteArrayList<>();
    private static volatile Map<Item, ContainerWithPurity> waterContainers = Map.of();
    private static volatile List<ContainerWithPurity> dataContainers = List.of();
    public static final int MIN_PURITY = 0;
    public static final int MAX_PURITY = 3;
    public static final int CAULDRON_PURITY = 1;
    public static final int HEATED_CAULDRON_PURITY = 2;

    public static void init()
    {
        registerDispenserBehaviours();
        registerContainers();
    }

    /**
     * Main thread only: the interaction maps are not thread-safe.
     */
    public static void registerCauldronInteractions()
    {
        CauldronInteraction.WATER.map().put(ItemInit.TERRACOTTA_BOWL.get(), fillFromCauldron(() -> new ItemStack(ItemInit.TERRACOTTA_WATER_BOWL.get()), SoundEvents.BUCKET_FILL));
    }

    /**
     * Takes one level of water from a water cauldron into {@code filled}, with the cauldron's purity.
     */
    public static CauldronInteraction fillFromCauldron(Supplier<ItemStack> filled, SoundEvent sound)
    {
        return (state, level, pos, player, hand, stack) -> {
            if (!level.isClientSide())
            {
                player.setItemInHand(hand, ItemUtils.createFilledResult(stack, player, addPurity(filled.get(), pos, level)));
                player.awardStat(Stats.USE_CAULDRON);
                player.awardStat(Stats.ITEM_USED.get(stack.getItem()));
                LayeredCauldronBlock.lowerFillLevel(state, level, pos);
                level.playSound(null, pos, sound, SoundSource.BLOCKS, 1.0F, 1.0F);
                level.gameEvent(null, GameEvent.FLUID_PICKUP, pos);
            }
            return ItemInteractionResult.sidedSuccess(level.isClientSide());
        };
    }

    private static void registerContainers()
    {
        addContainer(new ContainerWithPurity(Items.GLASS_BOTTLE,
                PotionContents.createItemStack(Items.POTION,Potions.WATER).getItem()).setEqualsFilled(itemStack ->
                itemStack.is(Items.POTION) && isWater(itemStack.get(DataComponents.POTION_CONTENTS))));
        addContainer(new ContainerWithPurity(ItemInit.TERRACOTTA_BOWL.get(),
                ItemInit.TERRACOTTA_WATER_BOWL.get()));
        addContainer(new ContainerWithPurity(Items.BUCKET,
                Items.WATER_BUCKET, false).canHarvestRunningWater(false));
    }

    private static boolean isWater(@Nullable PotionContents contents)
    {
        return contents != null && contents.is(Potions.WATER);
    }

    /**
     * Registers a water container for the whole session.
     *
     * @deprecated use {@code DropletsAPI.registerContainer}
     */
    @Deprecated
    public static void addContainer(ContainerWithPurity container)
    {
        codeContainers.add(container);
        waterContainers = merge(waterContainers.values(), List.of(container));
    }

    /**
     * Replaces the data-driven containers (config and {@link RegisterThirstValueEvent}); code containers are kept.
     * One container per filled item: the first registered wins.
     */
    public static void setContainers(List<ContainerWithPurity> containers)
    {
        dataContainers = List.copyOf(containers);
        waterContainers = merge(codeContainers, containers);
    }

    /**
     * Filled items of the data-driven containers (config and {@link RegisterThirstValueEvent}), as sent to clients.
     */
    public static List<Item> dataContainerItems()
    {
        return dataContainers.stream().map(ContainerWithPurity::getFilledItem).toList();
    }

    /**
     * {@code defaultPurity} of the server; on a client of a remote server it replaces the local config value.
     */
    public static int defaultPurity()
    {
        return SyncedValues.defaultPurity();
    }

    /**
     * {@code purity.enabled} (the server's on remote clients): when false nothing stores, shows or rolls purity.
     */
    public static boolean enabled()
    {
        return SyncedValues.purityEnabled();
    }

    private static Map<Item, ContainerWithPurity> merge(Collection<ContainerWithPurity> first, List<ContainerWithPurity> second)
    {
        Map<Item, ContainerWithPurity> byFilled = new HashMap<>();
        for (ContainerWithPurity container : first)
            byFilled.putIfAbsent(container.getFilledItem(), container);
        for (ContainerWithPurity container : second)
            byFilled.putIfAbsent(container.getFilledItem(), container);
        return Map.copyOf(byFilled);
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

        BlockHitResult hit = pickFluid(player, bucket || !SyncedValues.canFillFromFlowingWater() ? ClipContext.Fluid.SOURCE_ONLY : ClipContext.Fluid.ANY);
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
        if(enabled() && isWaterFilledContainer(event.getItemStack()) && (event.getFlags().hasShiftDown() || !ClientConfig.ONLY_SHOW_PURITY_WHEN_SHIFTING.get()))
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

    /**
     * One map lookup by the stack's item, then that container's own check (e.g. water bottle vs. other potions).
     */
    public static boolean isWaterFilledContainer(ItemStack item)
    {
        ContainerWithPurity container = waterContainers.get(item.getItem());
        return container != null && !item.is(DropletsTags.PURITY_OPT_OUT) && container.equalsFilled(item);
    }

    /**
     * Reads the purity from an item without modifying it; missing purity reads as the drink's data map purity if it has
     * one, missing or invalid purity as the default purity
     */
    public static Integer getPurity(ItemStack item)
    {
        Integer stored = item.get(ThirstComponent.PURITY);
        if (stored == null)
        {
            int fixed = ThirstHelper.getDrinkPurity(item);
            if (fixed >= MIN_PURITY)
                return fixed;
        }
        return sanitizePurity(stored);
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
        return purity == null || purity < MIN_PURITY || purity > MAX_PURITY ? defaultPurity() : purity;
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

    public static boolean hasPurity(ItemStack item)
    {
        return item.get(ThirstComponent.PURITY) != null && !item.is(DropletsTags.PURITY_OPT_OUT);
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
     * Invalid values are stored as the default purity. Items in {@code bluedroplets:purity_opt_out}, or any item with {@code purity.enabled=false}, are left unchanged.
     */
    public static ItemStack addPurity(ItemStack item, int purity)
    {
        if (enabled() && !item.is(DropletsTags.PURITY_OPT_OUT))
            item.set(ThirstComponent.PURITY, sanitizePurity(purity));
        return item;
    }

    /**
     * Sets the purity component on a fluid; it is always stored, also for the default purity.
     * Invalid values are stored as the default purity; nothing is stored with {@code purity.enabled=false}.
     */
    public static FluidStack addPurity(FluidStack fluid, int purity)
    {
        if (enabled())
            fluid.set(ThirstComponent.PURITY, sanitizePurity(purity));
        return fluid;
    }


    /**
     * Calculates the water purity of a specific block in the level
     */
    public static int getBlockPurity(Level level, BlockPos pos)
    {
        FluidState fluid = level.getFluidState(pos);
        if (fluid.is(FluidTags.WATER))
            return getWaterPurity(level, pos, fluid.isSource());
        return level.getBlockState(pos).is(Blocks.WATER_CAULDRON) ? cauldronPurity(level, pos) : defaultPurity();
    }

    /**
     * Purity of water taken from a water cauldron. The cauldron stores nothing: the purity is decided when the water
     * leaves it, {@value #CAULDRON_PURITY}, or {@value #HEATED_CAULDRON_PURITY} while it stands on a heat source (block
     * tag {@code bluedroplets:cauldron_heat_sources}; blocks with a {@code lit} property only when lit). Never purified:
     * that is what filters are for.
     */
    public static int cauldronPurity(BlockGetter level, BlockPos pos)
    {
        BlockState below = level.getBlockState(pos.below());
        return below.is(DropletsTags.CAULDRON_HEAT_SOURCES) && below.getOptionalValue(BlockStateProperties.LIT).orElse(true)
                ? HEATED_CAULDRON_PURITY : CAULDRON_PURITY;
    }

    /**
     * Purity of water in the world: salt water rule, then base (biome data map, biome tag, dimension data map,
     * {@code worldWaterBasePurity}), plus altitude, running water and biome deltas, capped by the biome's {@code max}.
     */
    public static int getWaterPurity(Level level, BlockPos pos, boolean source)
    {
        return getWaterPurity(level, pos, source, null);
    }

    /**
     * Same, writing each step to {@code trace} when given ({@code /bluedroplets debug purity}).
     */
    public static int getWaterPurity(Level level, BlockPos pos, boolean source, @Nullable List<String> trace)
    {
        Holder<Biome> biome = level.getBiome(pos);
        int salt = PurityConfig.SALT_WATER_PURITY.get();
        if (salt >= MIN_PURITY && biome.is(DropletsTags.SALT_WATER))
        {
            if (trace != null)
                trace.add("salt water biome: fixed " + salt);
            return salt;
        }

        BiomeWater biomeWater = biome.getData(DropletsDataMaps.BIOME_WATER);
        int purity = basePurity(level, biome, biomeWater, trace);
        int altitude = altitudeDelta(level, pos.getY());
        int flow = source ? PurityConfig.STILL_WATER_PURIFICATION_AMOUNT.get() : PurityConfig.RUNNING_WATER_PURIFICATION_AMOUNT.get();
        int delta = biomeWater != null ? biomeWater.delta() : 0;
        int max = biomeWater != null ? biomeWater.max() : MAX_PURITY;
        if (trace != null)
        {
            trace.add("altitude band: " + signed(altitude));
            trace.add((source ? "still" : "running") + " water: " + signed(flow));
            trace.add("biome_water delta: " + signed(delta) + ", max " + max);
        }
        return Math.max(MIN_PURITY, Math.min(purity + altitude + flow + delta, max));
    }

    private static String signed(int value)
    {
        return value >= 0 ? "+" + value : String.valueOf(value);
    }

    private static final NumberRows ALTITUDE_BANDS = new NumberRows(3);

    /**
     * Delta of the first {@code altitudeBands} entry containing {@code y}; parsed once per config value.
     */
    private static int altitudeDelta(Level level, int y)
    {
        if (PurityConfig.ALTITUDE_RELATIVE_TO_SEA_LEVEL.get())
            y -= level.getSeaLevel();
        return (int) NumberRows.band(ALTITUDE_BANDS.get(PurityConfig.ALTITUDE_BANDS.get()), y, 0);
    }

    private static int basePurity(Level level, Holder<Biome> biome, @Nullable BiomeWater biomeWater, @Nullable List<String> trace)
    {
        String from;
        int purity;
        DimensionWater dimensionWater;
        if (biomeWater != null && biomeWater.base().isPresent())
        {
            from = "biome_water data map";
            purity = biomeWater.base().get();
        }
        else if ((purity = taggedPurity(biome)) >= MIN_PURITY)
            from = "biome tag bluedroplets:water_purity/" + purity;
        else if ((dimensionWater = level.dimensionTypeRegistration().getData(DropletsDataMaps.DIMENSION_WATER)) != null && dimensionWater.base().isPresent())
        {
            from = "dimension_water data map";
            purity = dimensionWater.base().get();
        }
        else
        {
            from = "worldWaterBasePurity";
            purity = PurityConfig.WORLD_WATER_BASE_PURITY.get();
        }
        if (trace != null)
            trace.add("base " + purity + " from " + from);
        return purity;
    }

    private static int taggedPurity(Holder<Biome> biome)
    {
        for (int purity = MAX_PURITY; purity >= MIN_PURITY; purity--)
            if (biome.is(DropletsTags.WATER_PURITY[purity]))
                return purity;
        return -1;
    }

    /**
     * Purity a drink rolls effects for: the stored purity of a water container, the fixed purity of other drinks, or -1.
     */
    public static int drinkPurity(ItemStack item)
    {
        return isWaterFilledContainer(item) ? getPurity(item) : ThirstHelper.getDrinkPurity(item);
    }

    /**
     * One entry of a {@code purity.toml} effect list; {@code chance} is 0-1.
     */
    public record PurityEffect(Holder<MobEffect> effect, int duration, int amplifier, float chance, boolean blocksHydration) {}

    private record EffectTables(List<List<? extends String>> source, PurityEffect[][] byPurity) {}

    private static volatile EffectTables effectTables = new EffectTables(List.of(), new PurityEffect[0][]);

    /**
     * Rolls once, collects every effect of this purity's list whose chance is above the roll, adds Dehydration for dirty water in a hot climate ({@code hotDirtyWater}), lets
     * {@link PurityEffectEvent} change them, and applies them (effects only on the server). Returns whether the drink
     * should still restore thirst.
     */
    public static boolean givePurityEffects(Player player, int purity)
    {
        if (!enabled() || purity < MIN_PURITY || purity > MAX_PURITY)
            return true;
        boolean hydrate = true;
        List<MobEffectInstance> effects = new ArrayList<>();
        float roll = player.getRandom().nextFloat();
        for (PurityEffect effect : effectTable(purity))
        {
            if (roll >= effect.chance())
                continue;
            effects.add(new MobEffectInstance(effect.effect(), effect.duration(), effect.amplifier()));
            hydrate &= !effect.blocksHydration();
        }
        if (PurityConfig.HOT_DIRTY_WATER.get() && purity <= PurityConfig.HOT_DIRTY_WATER_MAX_PURITY.get() && player instanceof ServerPlayer && ThirstHelper.isHotClimate(player))
            effects.add(new MobEffectInstance(EffectInit.DEHYDRATION, PurityConfig.HOT_DIRTY_WATER_DURATION.get(), PurityConfig.HOT_DIRTY_WATER_AMPLIFIER.get()));
        PurityEffectEvent event = NeoForge.EVENT_BUS.post(new PurityEffectEvent(player, purity, effects, hydrate || PurityConfig.QUENCH_WHEN_DEBUFFED.get()));
        if (event.isCanceled())
            return true;
        if (player instanceof ServerPlayer)
            for (MobEffectInstance effect : event.getEffects())
                player.addEffect(effect);
        return event.hydrates();
    }

    /**
     * Parsed {@code effects} list of a purity, cached until the config changes; unknown effect ids are left out.
     */
    public static PurityEffect[] effectTable(int purity)
    {
        EffectTables cached = effectTables;
        List<List<? extends String>> source = cached.source();
        boolean stale = source.size() != PurityConfig.EFFECTS.size();
        for (int i = 0; !stale && i < source.size(); i++)
            stale = source.get(i) != PurityConfig.EFFECTS.get(i).get();
        if (stale)
        {
            List<List<? extends String>> lists = new ArrayList<>();
            PurityEffect[][] byPurity = new PurityEffect[PurityConfig.EFFECTS.size()][];
            for (int i = 0; i < byPurity.length; i++)
            {
                List<? extends String> list = PurityConfig.EFFECTS.get(i).get();
                lists.add(list);
                byPurity[i] = list.stream().map(WaterPurity::parseEffect).filter(Objects::nonNull).toArray(PurityEffect[]::new);
            }
            effectTables = cached = new EffectTables(lists, byPurity);
        }
        return cached.byPurity()[purity];
    }

    private static @Nullable PurityEffect parseEffect(String entry)
    {
        if (!PurityConfig.isValidEffect(entry))
            return null;
        String[] parts = entry.split(",");
        Holder<MobEffect> effect = BuiltInRegistries.MOB_EFFECT.getHolder(ResourceLocation.parse(parts[0].trim())).orElse(null);
        if (effect == null)
            return null;
        return new PurityEffect(effect, Integer.parseInt(parts[1].trim()), Integer.parseInt(parts[2].trim()),
                (float) (Double.parseDouble(parts[3].trim()) / 100.0), parts.length == 5 && parts[4].trim().equals("true"));
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

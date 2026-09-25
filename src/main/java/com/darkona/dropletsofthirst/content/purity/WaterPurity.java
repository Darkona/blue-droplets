package com.darkona.dropletsofthirst.content.purity;

import net.minecraft.world.item.alchemy.Potion;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.TextComponent;
import com.darkona.dropletsofthirst.foundation.config.ClientConfig;
import com.darkona.dropletsofthirst.foundation.config.PurityConfig;
import com.darkona.dropletsofthirst.foundation.config.SyncedValues;
import com.darkona.dropletsofthirst.api.PurityLevel;
import com.darkona.dropletsofthirst.api.event.PurityEffectEvent;
import com.darkona.dropletsofthirst.content.thirst.ThirstHelper;
import com.darkona.dropletsofthirst.content.data.BiomeWater;
import com.darkona.dropletsofthirst.content.data.DimensionWater;
import com.darkona.dropletsofthirst.content.data.DropletsDataMaps;
import com.darkona.dropletsofthirst.content.data.DropletsTags;
import com.darkona.dropletsofthirst.content.registry.EffectInit;
import com.darkona.dropletsofthirst.content.registry.ItemInit;
import com.darkona.dropletsofthirst.content.registry.ThirstComponent;
import com.darkona.dropletsofthirst.core.NumberRows;
import com.darkona.dropletsofthirst.foundation.common.event.RegisterThirstValueEvent;
import net.minecraft.stats.Stats;
import net.minecraft.world.InteractionResult;
import net.minecraft.locale.Language;
import net.minecraft.network.chat.Component;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraft.core.BlockPos;
import net.minecraft.core.cauldron.CauldronInteraction;
import net.minecraft.core.Holder;
import net.minecraft.core.Registry;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.network.chat.Style;
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
import net.minecraft.world.item.alchemy.PotionUtils;
import net.minecraft.world.item.alchemy.Potions;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.DispenserBlock;
import net.minecraft.world.level.block.LayeredCauldronBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.gameevent.GameEvent;
import net.minecraft.world.level.material.FluidState;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod.EventBusSubscriber;
import com.darkona.dropletsofthirst.foundation.common.event.Events;
import net.minecraftforge.event.entity.player.ItemTooltipEvent;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.fml.loading.FMLEnvironment;
import com.darkona.dropletsofthirst.foundation.gui.ClientKeys;
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
    public static final int MIN_PURITY = PurityLevel.MIN;
    public static final int MAX_PURITY = PurityLevel.MAX;
    /** Purity of water taken from a water cauldron. */
    public static final int CAULDRON_PURITY = PurityLevel.MURKY.level();
    /** Purity of water taken from a water cauldron on a heat source. */
    public static final int HEATED_CAULDRON_PURITY = PurityLevel.CLEAN.level();

    public static void init()
    {
        registerContainers();
    }

    /**
     * Main thread only: the interaction maps are not thread-safe.
     */
    public static void registerCauldronInteractions()
    {
        CauldronInteraction.WATER.put(ItemInit.TERRACOTTA_BOWL.get(), fillFromCauldron(() -> new ItemStack(ItemInit.TERRACOTTA_WATER_BOWL.get()), SoundEvents.BUCKET_FILL));
    }

    /**
     * Takes one level of water from a water cauldron into {@code filled}; the purity is added by
     * {@link #applyCauldronPurity}, as for every other cauldron interaction.
     */
    public static CauldronInteraction fillFromCauldron(Supplier<ItemStack> filled, SoundEvent sound)
    {
        return (state, level, pos, player, hand, stack) -> {
            if (!level.isClientSide())
            {
                player.setItemInHand(hand, ItemUtils.createFilledResult(stack, player, filled.get()));
                player.awardStat(Stats.USE_CAULDRON);
                player.awardStat(Stats.ITEM_USED.get(stack.getItem()));
                LayeredCauldronBlock.lowerFillLevel(state, level, pos);
                level.playSound(null, pos, sound, SoundSource.BLOCKS, 1.0F, 1.0F);
                level.gameEvent(null, GameEvent.FLUID_PICKUP, pos);
            }
            return InteractionResult.sidedSuccess(level.isClientSide());
        };
    }

    private static void registerContainers()
    {
        addContainer(new ContainerWithPurity(Items.GLASS_BOTTLE, Items.POTION).setEqualsFilled(itemStack ->
                itemStack.is(Items.POTION) && isWaterPotion(itemStack)));
        addContainer(new ContainerWithPurity(ItemInit.TERRACOTTA_BOWL.get(),
                ItemInit.TERRACOTTA_WATER_BOWL.get()));
        addContainer(new ContainerWithPurity(Items.BUCKET,
                Items.WATER_BUCKET, false).canHarvestRunningWater(false));
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
     * Same ray as vanilla {@code Item.getPlayerPOVHitResult}: from the eyes, along the view, up to the block interaction range.
     */
    public static BlockHitResult pickFluid(Player player, ClipContext.Fluid fluid)
    {
        Vec3 eye = player.getEyePosition();
        Vec3 end = eye.add(player.getViewVector(1.0F).scale(player.getReachDistance()));
        return player.level.clip(new ClipContext(eye, end, ClipContext.Block.OUTLINE, fluid, player));
    }

    /**
     * Renders the client-side tooltip for items that have a water
     * purity tag
     */
    @SubscribeEvent
    static void renderPurityTooltip(ItemTooltipEvent event)
    {
        if(enabled() && isWaterFilledContainer(event.getItemStack()) && (!ClientConfig.ONLY_SHOW_PURITY_WHEN_SHIFTING.get() || FMLEnvironment.dist.isClient() && ClientKeys.shiftDown()))
        {
            int purity = getPurity(event.getItemStack());
            if(purity >= MIN_PURITY && purity <= MAX_PURITY)
                event.getToolTip().add(purityLine(purity));
        }
    }

    /** The last {@code Potion} name read, and whether it names water. */
    private record PotionName(String name, boolean water) {}

    private static volatile PotionName lastPotionName = new PotionName("", false);

    /**
     * Whether the stack's {@code Potion} tag names water, as {@code PotionUtils.getPotion(stack) == Potions.WATER}. That
     * parses the name into a resource location on every call, and stack sizes, item colours and tooltips ask all the
     * time; copies of a stack share the same name string, so the answer for the last name is kept.
     */
    public static boolean isWaterPotion(ItemStack stack)
    {
        CompoundTag tag = stack.getTag();
        if (tag == null)
            return false;
        String name = tag.getString("Potion");
        PotionName last = lastPotionName;
        if (!last.name().equals(name))
            lastPotionName = last = new PotionName(name, Potion.byName(name) == Potions.WATER);
        return last.water();
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
        Integer stored = ThirstComponent.get(item);
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
        return sanitizePurity(ThirstComponent.get(fluid));
    }

    public static int sanitizePurity(@Nullable Integer purity)
    {
        return purity == null || purity < MIN_PURITY || purity > MAX_PURITY ? defaultPurity() : purity;
    }

    /**
     * Returns the purity string in the language selected by the player; out-of-range values read as the default purity.
     */
    public static String getPurityText(int purity)
    {
        PurityLevel level = level(purity);
        Language language = Language.getInstance();
        return language.has(level.translationKey()) ? language.getOrDefault(level.translationKey()) : level.id();
    }

    /**
     * The purity line of a water container's tooltip: the purity text in the purity colour. The same instance while the
     * language stays the same, so the tooltip, rebuilt every frame, allocates nothing for it.
     */
    public static Component purityLine(int purity)
    {
        Language language = Language.getInstance();
        PurityLines cached = purityLines;
        if (cached == null || cached.language() != language)
        {
            Component[] lines = new Component[MAX_PURITY + 1];
            for (PurityLevel level : PurityLevel.values())
            {
                String text = getPurityText(level.level());
                lines[level.level()] = new TextComponent(text).setStyle(Style.EMPTY.withColor(level.color()));
            }
            purityLines = cached = new PurityLines(language, lines);
        }
        return cached.lines()[level(purity).level()];
    }

    /** Tooltip lines of every purity level in one language; a resource reload or a language change gives a new {@link Language}. */
    private record PurityLines(Language language, Component[] lines) {}

    private static volatile @Nullable PurityLines purityLines;

    /**
     * Returns the purity color, {@code 0xRRGGBB}
     */
    public static int getPurityColor(int purity)
    {
        return level(purity).color();
    }

    private static PurityLevel level(int purity)
    {
        PurityLevel level = PurityLevel.byLevel(purity);
        return level != null ? level : Objects.requireNonNull(PurityLevel.byLevel(sanitizePurity(purity)));
    }

    public static boolean hasPurity(ItemStack item)
    {
        return ThirstComponent.has(item) && !item.is(DropletsTags.PURITY_OPT_OUT);
    }

    public static boolean hasPurity(FluidStack fluid)
    {
        return ThirstComponent.get(fluid) != null;
    }


    /**
     * Sets the purity component on an item; it is always stored, also for the default purity.
     * Invalid values are stored as the default purity. Items in {@code droplets_of_thirst:purity_opt_out}, or any item with {@code purity.enabled=false}, are left unchanged.
     */
    public static ItemStack addPurity(ItemStack item, int purity)
    {
        if (enabled() && !item.is(DropletsTags.PURITY_OPT_OUT))
            ThirstComponent.set(item, sanitizePurity(purity));
        return item;
    }

    /**
     * Sets the purity component on a fluid; it is always stored, also for the default purity.
     * Invalid values are stored as the default purity; nothing is stored with {@code purity.enabled=false}.
     */
    public static FluidStack addPurity(FluidStack fluid, int purity)
    {
        if (enabled())
            ThirstComponent.set(fluid, sanitizePurity(purity));
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
     * Purity of water just taken from {@code pos} by a container, a machine or a dispenser: the water or water cauldron
     * still there, or else a source that was picked up. A picked up source keeps the purity of poured water
     * ({@link PouredWater#pickedUp}, which also hands it to the sources beside it); otherwise world water purity
     * depends on the biome and height, not on the block.
     */
    public static int takenWaterPurity(Level level, BlockPos pos)
    {
        FluidState fluid = level.getFluidState(pos);
        if (fluid.is(FluidTags.WATER))
            return getWaterPurity(level, pos, fluid.isSource());
        if (level.getBlockState(pos).is(Blocks.WATER_CAULDRON))
            return cauldronPurity(level, pos);
        int poured = PouredWater.pickedUp(level, pos);
        return poured >= MIN_PURITY ? poured : getWaterPurity(level, pos, true);
    }

    /**
     * A dispenser filled a container: a water container without a purity gets the purity of the water in front of it.
     */
    public static void dispenserFilled(Level level, BlockPos dispenser, BlockState state, ItemStack filled)
    {
        if (enabled() && state.hasProperty(DispenserBlock.FACING) && isWaterFilledContainer(filled) && !hasPurity(filled))
            addPurity(filled, takenWaterPurity(level, dispenser.relative(state.getValue(DispenserBlock.FACING))));
    }

    /** Purity for water leaving the water cauldron being used right now, -1 outside one. Server thread only. */
    private static int takingFromCauldron = -1;

    /**
     * Start of an item interaction with a cauldron: when it is a water cauldron on the server, water containers that
     * come out of it until {@link #doneTakingFromCauldron} get its purity. Returns whether it started.
     */
    public static boolean takingFromCauldron(BlockState state, Level level, BlockPos pos)
    {
        if (level.isClientSide() || !enabled() || !state.is(Blocks.WATER_CAULDRON))
            return false;
        takingFromCauldron = cauldronPurity(level, pos);
        return true;
    }

    public static void doneTakingFromCauldron()
    {
        takingFromCauldron = -1;
    }

    /**
     * During a water cauldron interaction, gives {@code filled} the cauldron purity if it is a water container with none.
     */
    public static void applyCauldronPurity(ItemStack filled)
    {
        if (takingFromCauldron >= MIN_PURITY && isWaterFilledContainer(filled) && !hasPurity(filled))
            addPurity(filled, takingFromCauldron);
    }

    /**
     * Purity of water taken from a water cauldron. The cauldron stores nothing: the purity is decided when the water
     * leaves it, {@link #CAULDRON_PURITY}, or {@link #HEATED_CAULDRON_PURITY} while it stands on a heat source (block
     * tag {@code droplets_of_thirst:cauldron_heat_sources}; blocks with a {@code lit} property only when lit). Never pure:
     * that is what filters and cooking are for.
     */
    public static int cauldronPurity(BlockGetter level, BlockPos pos)
    {
        BlockState below = level.getBlockState(pos.below());
        return below.is(DropletsTags.CAULDRON_HEAT_SOURCES) && below.getOptionalValue(BlockStateProperties.LIT).orElse(true)
                ? HEATED_CAULDRON_PURITY : CAULDRON_PURITY;
    }

    /**
     * Purity of water in the world: poured water ({@link PouredWater}), salt water rule, then base (biome data map, biome tag, dimension data map,
     * {@code worldWaterBasePurity}), plus altitude, running water and biome deltas, capped by the biome's {@code max}.
     */
    public static int getWaterPurity(Level level, BlockPos pos, boolean source)
    {
        return getWaterPurity(level, pos, source, null);
    }

    /**
     * Same, writing each step to {@code trace} when given ({@code /droplets_of_thirst debug purity}). A water source poured
     * into the world, or next to poured ones, has the purity of the water poured ({@link PouredWater}).
     */
    public static int getWaterPurity(Level level, BlockPos pos, boolean source, @Nullable List<String> trace)
    {
        if (source)
        {
            int poured = PouredWater.purityAt(level, pos);
            if (poured >= MIN_PURITY)
            {
                if (trace != null)
                    trace.add("poured water: fixed " + poured);
                return poured;
            }
        }
        return getWaterPurity(level, level.getBiome(pos), pos, source, trace);
    }

    /**
     * Same, for a given {@code biome} instead of the one at {@code pos} (the position still gives the altitude).
     */
    public static int getWaterPurity(Level level, Holder<Biome> biome, BlockPos pos, boolean source, @Nullable List<String> trace)
    {
        int salt = PurityConfig.SALT_WATER_PURITY.get();
        if (salt >= MIN_PURITY && biome.is(DropletsTags.SALT_WATER))
        {
            if (trace != null)
                trace.add("salt water biome: fixed " + salt);
            return salt;
        }

        BiomeWater biomeWater = DropletsDataMaps.BIOME_WATER.get(biome);
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
            from = "biome tag droplets_of_thirst:water_purity/" + purity;
        else if ((dimensionWater = DropletsDataMaps.DIMENSION_WATER.get(level.dimensionTypeRegistration())) != null && dimensionWater.base().isPresent())
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
     * Thirst added to water of this purity when drunk: {@code pureWater.thirstBonus} for pure water
     * ({@link PurityLevel#PURE}), 0 for any other purity or with purity off. Only for water, never for other drinks
     * with a purity.
     */
    public static int waterThirstBonus(int purity)
    {
        return purity == PurityLevel.PURE.level() && enabled() ? SyncedValues.pureThirstBonus() : 0;
    }

    /**
     * Quenched added to water of this purity when drunk, as {@link #waterThirstBonus}.
     */
    public static int waterQuenchedBonus(int purity)
    {
        return purity == PurityLevel.PURE.level() && enabled() ? SyncedValues.pureQuenchedBonus() : 0;
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
    public record PurityEffect(MobEffect effect, int duration, int amplifier, float chance, boolean blocksHydration) {}

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
            effects.add(new MobEffectInstance(EffectInit.DEHYDRATION.get(), PurityConfig.HOT_DIRTY_WATER_DURATION.get(), PurityConfig.HOT_DIRTY_WATER_AMPLIFIER.get()));
        PurityEffectEvent event = Events.post(new PurityEffectEvent(player, purity, effects, hydrate || PurityConfig.QUENCH_WHEN_DEBUFFED.get()));
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
        ResourceLocation id = ResourceLocation.tryParse(parts[0].trim());
        MobEffect effect = id == null ? null : Registry.MOB_EFFECT.getOptional(id).orElse(null);
        if (effect == null)
            return null;
        return new PurityEffect(effect, Integer.parseInt(parts[1].trim()), Integer.parseInt(parts[2].trim()),
                (float) (Double.parseDouble(parts[3].trim()) / 100.0), parts.length == 5 && parts[4].trim().equals("true"));
    }
}

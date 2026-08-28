package com.darkona.droplets.gametest;

import net.minecraft.network.chat.TextColor;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.Item;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.crafting.SingleRecipeInput;
import net.neoforged.neoforge.transfer.transaction.Transaction;
import net.neoforged.neoforge.transfer.fluid.FluidUtil;
import net.neoforged.neoforge.transfer.fluid.FluidResource;
import net.neoforged.neoforge.transfer.ResourceHandler;
import com.darkona.droplets.BlueDroplets;
import com.darkona.droplets.api.PurityLevel;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.biome.Biomes;
import com.darkona.droplets.content.purity.PurityTint;
import com.darkona.droplets.content.purity.WaterPurity;
import net.minecraft.world.item.alchemy.PotionContents;
import net.minecraft.world.item.alchemy.Potions;
import com.darkona.droplets.content.registry.ItemInit;
import com.darkona.droplets.content.registry.ThirstComponent;
import net.minecraft.core.BlockPos;
import net.minecraft.world.item.crafting.AbstractCookingRecipe;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.item.crafting.RecipeType;
import net.neoforged.fml.ModList;
import java.util.List;
import net.minecraft.world.level.material.Fluids;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.fluids.FluidStack;
import net.minecraft.core.Direction;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.GameType;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.LiquidBlock;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.block.DispenserBlock;
import net.minecraft.world.level.block.entity.DispenserBlockEntity;
import net.minecraft.core.dispenser.BlockSource;
import net.minecraft.world.level.block.LayeredCauldronBlock;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;

/**
 * Purity of water taken from cauldrons (murky, or clean on a heat source) and of water in the world.
 */
public class PurityTests
{
    @GameTest(template = "box")
    public static void cauldronPurityDependsOnHeat(GameTestHelper helper)
    {
        helper.assertValueEqual(WaterPurity.CAULDRON_PURITY, PurityLevel.MURKY.level(), "cauldron purity");
        helper.assertValueEqual(WaterPurity.HEATED_CAULDRON_PURITY, PurityLevel.CLEAN.level(), "heated cauldron purity");
        BlockPos pos = helper.absolutePos(new BlockPos(2, 2, 2));
        helper.getLevel().setBlockAndUpdate(pos, Blocks.WATER_CAULDRON.defaultBlockState().setValue(LayeredCauldronBlock.LEVEL, 3));
        helper.getLevel().setBlockAndUpdate(pos.below(), Blocks.STONE.defaultBlockState());
        helper.assertValueEqual(WaterPurity.getBlockPurity(helper.getLevel(), pos), WaterPurity.CAULDRON_PURITY, "cauldron on stone");
        helper.getLevel().setBlockAndUpdate(pos.below(), Blocks.CAMPFIRE.defaultBlockState());
        helper.assertValueEqual(WaterPurity.getBlockPurity(helper.getLevel(), pos), WaterPurity.HEATED_CAULDRON_PURITY, "cauldron on a lit campfire");
        helper.getLevel().setBlockAndUpdate(pos.below(), Blocks.CAMPFIRE.defaultBlockState().setValue(BlockStateProperties.LIT, false));
        helper.assertValueEqual(WaterPurity.getBlockPurity(helper.getLevel(), pos), WaterPurity.CAULDRON_PURITY, "cauldron on an unlit campfire");
        helper.succeed();
    }

    @GameTest(template = "box")
    public static void itemsTakeTheCauldronPurity(GameTestHelper helper)
    {
        BlockPos pos = helper.absolutePos(new BlockPos(2, 2, 2));
        helper.getLevel().setBlockAndUpdate(pos.below(), Blocks.CAMPFIRE.defaultBlockState());
        Player player = helper.makeMockPlayer(GameType.SURVIVAL);

        ItemStack bottle = useOnFullCauldron(helper, player, pos, new ItemStack(Items.GLASS_BOTTLE, 3));
        helper.assertValueEqual(bottle.getCount(), 2, "glass bottles left in hand");
        ItemStack water = ItemStack.EMPTY;
        for (ItemStack item : player.getInventory().getNonEquipmentItems())
            if (item.is(Items.POTION))
                water = item;
        helper.assertFalse(water.isEmpty(), "no water bottle in the inventory");
        helper.assertTrue(WaterPurity.hasPurity(water), "water bottle from a cauldron has no purity stored");
        helper.assertValueEqual(WaterPurity.getPurity(water), WaterPurity.HEATED_CAULDRON_PURITY, "water bottle from a heated cauldron");

        helper.getLevel().setBlockAndUpdate(pos.below(), Blocks.STONE.defaultBlockState());
        ItemStack bucket = useOnFullCauldron(helper, player, pos, new ItemStack(Items.BUCKET));
        helper.assertTrue(bucket.is(Items.WATER_BUCKET), "the bucket was not filled");
        helper.assertValueEqual(WaterPurity.getPurity(bucket), WaterPurity.CAULDRON_PURITY, "water bucket from a cauldron");

        ItemStack bowl = useOnFullCauldron(helper, player, pos, new ItemStack(ItemInit.TERRACOTTA_BOWL.get()));
        helper.assertTrue(bowl.is(ItemInit.TERRACOTTA_WATER_BOWL.get()), "the terracotta bowl was not filled");
        helper.assertValueEqual(WaterPurity.getPurity(bowl), WaterPurity.CAULDRON_PURITY, "terracotta water bowl from a cauldron");
        helper.succeed();
    }

    @GameTest(template = "box")
    public static void fluidCapabilityCarriesTheCauldronPurity(GameTestHelper helper)
    {
        BlockPos pos = helper.absolutePos(new BlockPos(2, 2, 2));
        helper.getLevel().setBlockAndUpdate(pos.below(), Blocks.CAMPFIRE.defaultBlockState());
        helper.getLevel().setBlockAndUpdate(pos, Blocks.WATER_CAULDRON.defaultBlockState().setValue(LayeredCauldronBlock.LEVEL, 3));
        ResourceHandler<FluidResource> handler = helper.getLevel().getCapability(Capabilities.Fluid.BLOCK, pos, null);
        helper.assertTrue(handler != null, "a water cauldron has no fluid handler");
        int heated = WaterPurity.HEATED_CAULDRON_PURITY;

        FluidResource inTank = handler.getResource(0);
        helper.assertTrue(WaterPurity.hasPurity(inTank), "tank contents have no purity");
        helper.assertValueEqual(WaterPurity.getPurity(inTank), heated, "purity of the tank contents");
        try (Transaction simulation = Transaction.openRoot())
        {
            helper.assertValueEqual(handler.extract(0, inTank, 1000, simulation), 1000, "amount of a simulated extraction of the tank contents");
        }
        helper.assertTrue(helper.getLevel().getBlockState(pos).is(Blocks.WATER_CAULDRON), "a simulated extraction emptied the cauldron");

        try (Transaction transaction = Transaction.openRoot())
        {
            helper.assertValueEqual(handler.extract(0, WaterPurity.waterResource(WaterPurity.CAULDRON_PURITY), 1000, transaction), 0, "extracted water of another purity");
            helper.assertValueEqual(handler.extract(0, FluidResource.of(Fluids.WATER).with(ThirstComponent.PURITY, heated), 1000, transaction), 1000, "amount extracted asking for the tank contents");
            transaction.commit();
        }
        helper.assertTrue(helper.getLevel().getBlockState(pos).is(Blocks.CAULDRON), "the cauldron was not emptied");
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void bucketsKeepPurityThroughTheFluidCapability(GameTestHelper helper)
    {
        ItemStack bucket = WaterPurity.addPurity(new ItemStack(Items.WATER_BUCKET), 0);
        FluidStack inBucket = FluidUtil.getFirstStackContained(bucket);
        helper.assertTrue(inBucket.is(Fluids.WATER), "no water in a water bucket");
        helper.assertTrue(WaterPurity.hasPurity(inBucket), "water read from a bucket has no purity");
        helper.assertValueEqual(WaterPurity.getPurity(inBucket), 0, "purity of water read from a bucket");

        ItemStack filled = TestSupport.fillBucket(helper, WaterPurity.waterResource(PurityLevel.PURE.level()));
        helper.assertTrue(filled.is(Items.WATER_BUCKET), "no water bucket for water with a purity");
        helper.assertValueEqual(WaterPurity.getPurity(filled), PurityLevel.PURE.level(), "purity of a bucket filled with water");
        helper.assertFalse(WaterPurity.hasPurity(TestSupport.fillBucket(helper, FluidResource.of(Fluids.WATER))), "a bucket of water without purity got one");
        helper.succeed();
    }

    @GameTest(template = "box")
    public static void dispensersFillWithWorldWaterPurity(GameTestHelper helper)
    {
        BlockPos dispenser = helper.absolutePos(new BlockPos(1, 2, 2));
        BlockPos water = dispenser.east();
        helper.getLevel().setBlockAndUpdate(dispenser, Blocks.DISPENSER.defaultBlockState().setValue(DispenserBlock.FACING, Direction.EAST));
        helper.getLevel().setBlockAndUpdate(water, Blocks.WATER.defaultBlockState());
        int expected = WaterPurity.getWaterPurity(helper.getLevel(), water, true);
        BlockSource source = new BlockSource(helper.getLevel(), dispenser, helper.getLevel().getBlockState(dispenser),
                (DispenserBlockEntity) helper.getLevel().getBlockEntity(dispenser));

        ItemStack bottle = DispenserBlock.DISPENSER_REGISTRY.get(Items.GLASS_BOTTLE).dispense(source, new ItemStack(Items.GLASS_BOTTLE));
        helper.assertTrue(bottle.is(Items.POTION), "the dispenser did not fill the bottle");
        helper.assertTrue(WaterPurity.hasPurity(bottle), "water bottle from a dispenser has no purity");
        helper.assertValueEqual(WaterPurity.getPurity(bottle), expected, "purity of a water bottle from a dispenser");

        ItemStack bucket = DispenserBlock.DISPENSER_REGISTRY.get(Items.BUCKET).dispense(source, new ItemStack(Items.BUCKET));
        helper.assertTrue(bucket.is(Items.WATER_BUCKET), "the dispenser did not fill the bucket");
        helper.assertTrue(helper.getLevel().getFluidState(water).isEmpty(), "the dispenser did not pick up the water");
        helper.assertValueEqual(WaterPurity.getPurity(bucket), expected, "purity of a water bucket from a dispenser");
        helper.succeed();
    }

    @GameTest(template = "box")
    public static void containersFillWithWorldWaterPurity(GameTestHelper helper)
    {
        BlockPos water = helper.absolutePos(new BlockPos(2, 2, 2));
        helper.getLevel().setBlockAndUpdate(water, Blocks.WATER.defaultBlockState());
        int still = WaterPurity.getWaterPurity(helper.getLevel(), water, true);
        ServerPlayer player = TestSupport.player(helper);
        player.snapTo(water.getX() + 0.5, water.getY() + 1, water.getZ() + 0.5, 0.0F, 90.0F);

        ItemStack bottle = useFromAbove(player, new ItemStack(Items.GLASS_BOTTLE));
        helper.assertTrue(bottle.is(Items.POTION), "the glass bottle was not filled");
        helper.assertTrue(WaterPurity.hasPurity(bottle), "water bottle from the world has no purity");
        helper.assertValueEqual(WaterPurity.getPurity(bottle), still, "purity of a water bottle from still water");

        ItemStack bowl = useFromAbove(player, new ItemStack(ItemInit.TERRACOTTA_BOWL.get()));
        helper.assertTrue(bowl.is(ItemInit.TERRACOTTA_WATER_BOWL.get()), "the terracotta bowl was not filled");
        helper.assertValueEqual(WaterPurity.getPurity(bowl), still, "purity of a terracotta water bowl from still water");
        helper.assertTrue(WaterPurity.hasPurity(bowl), "terracotta water bowl from the world has no purity");

        ItemStack bucket = useFromAbove(player, new ItemStack(Items.BUCKET));
        helper.assertTrue(bucket.is(Items.WATER_BUCKET), "the bucket was not filled");
        helper.assertTrue(WaterPurity.hasPurity(bucket), "water bucket from the world has no purity");
        helper.assertValueEqual(WaterPurity.getPurity(bucket), still, "purity of a water bucket from the world");

        helper.getLevel().setBlockAndUpdate(water, Blocks.WATER.defaultBlockState().setValue(LiquidBlock.LEVEL, 2));
        int running = WaterPurity.getWaterPurity(helper.getLevel(), water, false);
        ItemStack flowing = useFromAbove(player, new ItemStack(Items.GLASS_BOTTLE));
        helper.assertTrue(flowing.is(Items.POTION), "the glass bottle was not filled from flowing water");
        helper.assertValueEqual(WaterPurity.getPurity(flowing), running, "purity of a water bottle from flowing water");
        helper.succeed();
    }

    private static ItemStack useFromAbove(Player player, ItemStack stack)
    {
        player.setItemInHand(InteractionHand.MAIN_HAND, stack);
        return TestSupport.heldResult(stack.use(player.level(), player, InteractionHand.MAIN_HAND), stack);
    }

    @GameTest(template = "empty")
    public static void cookingReachesPure(GameTestHelper helper)
    {
        int max = -1;
        for (RecipeType<? extends AbstractCookingRecipe> type : List.of(RecipeType.SMELTING, RecipeType.CAMPFIRE_COOKING))
            for (RecipeHolder<? extends AbstractCookingRecipe> holder : helper.getLevel().getServer().getRecipeManager().recipeMap().byType(type))
                if (holder.id().identifier().getNamespace().equals(BlueDroplets.ID))
                {
                    Integer purity = holder.value().assemble(new SingleRecipeInput(ItemStack.EMPTY)).get(ThirstComponent.PURITY);
                    if (purity != null)
                        max = Math.max(max, purity);
                }
        helper.assertValueEqual(max, PurityLevel.PURE.level(), "highest purity from cooking water");
        helper.succeed();
    }

    private static ItemStack useOnFullCauldron(GameTestHelper helper, Player player, BlockPos pos, ItemStack stack)
    {
        helper.getLevel().setBlockAndUpdate(pos, Blocks.WATER_CAULDRON.defaultBlockState().setValue(LayeredCauldronBlock.LEVEL, 3));
        player.setItemInHand(InteractionHand.MAIN_HAND, stack);
        helper.getLevel().getBlockState(pos).useItemOn(stack, helper.getLevel(), player, InteractionHand.MAIN_HAND,
                new BlockHitResult(Vec3.atCenterOf(pos), Direction.UP, pos, false));
        return player.getItemInHand(InteractionHand.MAIN_HAND);
    }

    @GameTest(template = "box")
    public static void worldWaterHasAValidPurity(GameTestHelper helper)
    {
        BlockPos pos = helper.absolutePos(new BlockPos(2, 1, 2));
        helper.getLevel().setBlockAndUpdate(pos, Blocks.WATER.defaultBlockState());
        int purity = WaterPurity.getBlockPurity(helper.getLevel(), pos);
        helper.assertTrue(purity >= WaterPurity.MIN_PURITY && purity <= WaterPurity.MAX_PURITY, "world water purity " + purity);
        helper.succeed();
    }

    private static int biomePurity(GameTestHelper helper, ResourceKey<Biome> biome, boolean source)
    {
        var level = helper.getLevel();
        BlockPos pos = helper.absolutePos(new BlockPos(2, 1, 2));
        level.setBlockAndUpdate(pos, source ? Blocks.WATER.defaultBlockState() : Blocks.WATER.defaultBlockState().setValue(LiquidBlock.LEVEL, 1));
        var holder = level.registryAccess().lookupOrThrow(Registries.BIOME).getOrThrow(biome);
        return WaterPurity.getWaterPurity(level, holder, pos, source, null);
    }

    /**
     * Built-in {@code biome_water} data map: jungles and dark forests start at 1 like plains, taiga, birch and snowy
     * biomes at 2, mountains and windswept at 3, deserts and badlands at 0 capped at 2, swamps at 0 capped at 1.
     */
    @GameTest(template = "box")
    public static void defaultBiomeWaterValues(GameTestHelper helper)
    {
        // Plains still water is worldWaterBasePurity (1) plus the altitude band of the test position.
        int alt = biomePurity(helper, Biomes.PLAINS, true) - 1;
        helper.assertTrue(alt >= 0 && alt <= 3, "altitude delta " + alt);
        int max = PurityLevel.PURE.level();
        for (var biome : List.of(Biomes.PLAINS, Biomes.JUNGLE, Biomes.DARK_FOREST))
            base(helper, biome, 1, max, alt);
        for (var biome : List.of(Biomes.TAIGA, Biomes.BIRCH_FOREST, Biomes.SNOWY_PLAINS, Biomes.FROZEN_RIVER))
            base(helper, biome, 2, max, alt);
        for (var biome : List.of(Biomes.FROZEN_PEAKS, Biomes.JAGGED_PEAKS, Biomes.WINDSWEPT_HILLS))
            base(helper, biome, 3, max, alt);
        for (var biome : List.of(Biomes.DESERT, Biomes.BADLANDS))
            base(helper, biome, 0, 2, alt);
        for (var biome : List.of(Biomes.SWAMP, Biomes.MANGROVE_SWAMP))
            base(helper, biome, 0, 1, alt);
        helper.succeed();
    }

    /** Still water: base plus altitude; running water one more; both capped. */
    private static void base(GameTestHelper helper, ResourceKey<Biome> biome, int base, int max, int alt)
    {
        check(helper, biome.identifier() + " still", biomePurity(helper, biome, true), Math.min(max, base + alt));
        check(helper, biome.identifier() + " running", biomePurity(helper, biome, false), Math.min(max, base + alt + 1));
    }

    /** Default {@code altitudeBands}: +1, +2, +3 from 30, 60 and 100 blocks above sea level and from 16, 48 and 80 below it. */
    @GameTest(template = "box")
    public static void altitudeBandsAddUpToThree(GameTestHelper helper)
    {
        var level = helper.getLevel();
        int sea = level.getSeaLevel();
        int[][] bands = {{0, 0}, {29, 0}, {30, 1}, {59, 1}, {60, 2}, {99, 2}, {100, 3}, {-15, 0}, {-16, 1}, {-47, 1}, {-48, 2}, {-79, 2}, {-80, 3}};
        var plains = level.registryAccess().lookupOrThrow(Registries.BIOME).getOrThrow(Biomes.PLAINS);
        for (int[] band : bands)
        {
            BlockPos pos = new BlockPos(0, sea + band[0], 0);
            int purity = WaterPurity.getWaterPurity(level, plains, pos, true, null);
            check(helper, "still plains water " + band[0] + " from sea level", purity, 1 + band[1]);
        }
        helper.succeed();
    }

    /** The tint function is pure: only water containers with a stored purity change, and level 3 keeps vanilla blue. */
    @GameTest(template = "box")
    public static void tintChangesOnlyPurifiedWater(GameTestHelper helper)
    {
        int vanilla = 0xFF385DC6;
        for (int level = PurityLevel.MIN; level <= PurityLevel.MAX; level++)
        {
            ItemStack bottle = WaterPurity.addPurity(PotionContents.createItemStack(Items.POTION, Potions.WATER), level);
            ItemStack bowl = WaterPurity.addPurity(new ItemStack(ItemInit.TERRACOTTA_WATER_BOWL.get()), level);
            int expected = PurityTint.colorOf(level);
            check(helper, "bottle tint at level " + level, PurityTint.color(bottle, 0, vanilla), expected);
            check(helper, "bowl tint at level " + level, PurityTint.color(bowl, 0, vanilla), expected);
            check(helper, "glass layer at level " + level, PurityTint.color(bottle, 1, -1), -1);
        }
        check(helper, "level 3 keeps vanilla blue", PurityTint.colorOf(3), vanilla);
        ItemStack plain = PotionContents.createItemStack(Items.POTION, Potions.WATER);
        check(helper, "water without purity", PurityTint.color(plain, 0, vanilla), vanilla);
        ItemStack healing = WaterPurity.addPurity(PotionContents.createItemStack(Items.POTION, Potions.HEALING), 0);
        check(helper, "healing potion", PurityTint.color(healing, 0, 0xFFF82423), 0xFFF82423);
        helper.succeed();
    }

    /** Each level reads as its translation (the id when there is none); out-of-range values as the default purity. */
    @GameTest(template = "empty")
    public static void purityTextFollowsTheTranslation(GameTestHelper helper)
    {
        for (PurityLevel level : PurityLevel.values())
        {
            String text = WaterPurity.getPurityText(level.level());
            helper.assertValueEqual(text, Component.translatableWithFallback(level.translationKey(), level.id()).getString(), "text of " + level.id());
            helper.assertTrue(WaterPurity.getPurityText(level.level()) == text, "the text of " + level.id() + " is built again on every call");
            Component line = WaterPurity.purityLine(level.level());
            helper.assertValueEqual(line.getString(), text, "tooltip line of " + level.id());
            helper.assertValueEqual(line.getStyle().getColor(), TextColor.fromRgb(level.color()), "tooltip colour of " + level.id());
            helper.assertTrue(WaterPurity.purityLine(level.level()) == line, "the tooltip line of " + level.id() + " is built again on every call");
        }
        String fallback = WaterPurity.getPurityText(WaterPurity.defaultPurity());
        helper.assertValueEqual(WaterPurity.getPurityText(-1), fallback, "text of purity -1");
        helper.assertValueEqual(WaterPurity.getPurityText(PurityLevel.MAX + 1), fallback, "text of a purity past the maximum");
        helper.succeed();
    }

    /** Some mods build tooltips on a dedicated server, where the client config is not loaded. */
    @GameTest(template = "empty")
    public static void tooltipsBuildOnADedicatedServer(GameTestHelper helper)
    {
        ItemStack water = WaterPurity.addPurity(PotionContents.createItemStack(Items.POTION, Potions.WATER), PurityLevel.MURKY.level());
        water.getTooltipLines(Item.TooltipContext.of(helper.getLevel()), null, TooltipFlag.Default.NORMAL);
        new ItemStack(Items.APPLE).getTooltipLines(Item.TooltipContext.of(helper.getLevel()), null, TooltipFlag.Default.NORMAL);
        helper.succeed();
    }

    private static void check(GameTestHelper helper, String what, int actual, int expected)
    {
        helper.assertTrue(actual == expected, what + ": purity " + actual + ", expected " + expected);
    }
}

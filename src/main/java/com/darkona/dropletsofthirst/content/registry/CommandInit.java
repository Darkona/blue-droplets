package com.darkona.dropletsofthirst.content.registry;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.BoolArgumentType;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.tree.LiteralCommandNode;
import com.darkona.dropletsofthirst.DropletsOfThirst;
import com.darkona.dropletsofthirst.content.data.DimensionWater;
import com.darkona.dropletsofthirst.content.data.DropletsDataMaps;
import com.darkona.dropletsofthirst.content.purity.WaterPurity;
import com.darkona.dropletsofthirst.api.event.ThirstChangeEvent;
import com.darkona.dropletsofthirst.content.thirst.ExhaustionFactors;
import com.darkona.dropletsofthirst.content.thirst.PlayerThirst;
import com.darkona.dropletsofthirst.content.thirst.RecipeInference;
import com.darkona.dropletsofthirst.content.thirst.ThirstHelper;
import com.darkona.dropletsofthirst.foundation.config.ConfigCheck;
import com.darkona.dropletsofthirst.foundation.dev.ItemDump;
import com.darkona.dropletsofthirst.foundation.config.GameplayConfig;
import com.darkona.dropletsofthirst.core.ThirstConstants;
import com.darkona.dropletsofthirst.foundation.common.capability.IThirst;
import com.darkona.dropletsofthirst.foundation.common.capability.ModAttachment;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.commands.arguments.item.ItemArgument;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.ComponentUtils;
import net.minecraft.core.BlockPos;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.FluidTags;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.loading.FMLEnvironment;
import net.neoforged.neoforge.event.RegisterCommandsEvent;


import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Locale;
import java.util.Set;

@EventBusSubscriber(modid = DropletsOfThirst.ID)
public class CommandInit {

    @SubscribeEvent
    public static void RegisterCommand(RegisterCommandsEvent event){
        CommandDispatcher<CommandSourceStack> dispatcher=event.getDispatcher();
        LiteralCommandNode<CommandSourceStack> root = dispatcher.register(Commands.literal(DropletsOfThirst.ID)
                .requires(Commands.hasPermission(Commands.LEVEL_GAMEMASTERS))
                .then(Commands.literal("query").then(Commands.argument("Player", EntityArgument.player())
                        .executes(context -> {
                                    ServerPlayer player = EntityArgument.getPlayer(context,"Player");
                                    IThirst thirst = player.getData(ModAttachment.PLAYER_THIRST);
                                    int value = thirst.getThirst();
                                    int quenched = thirst.getQuenched();
                                    context.getSource().sendSuccess(() -> Component.translatable("command.droplets_of_thirst.query", value, quenched, player.getDisplayName()), false);
                                    return value;
                                }
                        )))
                .then(Commands.literal("set").then(Commands.argument("Player", EntityArgument.player())
                        .then(Commands.argument("thirst", IntegerArgumentType.integer(0, ThirstConstants.MAX_THIRST))
                                .then(Commands.argument("quenched", IntegerArgumentType.integer(0, ThirstConstants.MAX_THIRST))
                                        .executes(context -> {
                                            ServerPlayer player = EntityArgument.getPlayer(context,"Player");
                                            PlayerThirst thirst = player.getData(ModAttachment.PLAYER_THIRST);
                                            thirst.change(player, IntegerArgumentType.getInteger(context,"thirst"), IntegerArgumentType.getInteger(context,"quenched"), ThirstChangeEvent.Cause.COMMAND);
                                            // Quenched never exceeds thirst and listeners may cancel or change it: report what was set.
                                            int value = thirst.getThirst();
                                            int quenched = thirst.getQuenched();
                                            context.getSource().sendSuccess(() -> Component.translatable("command.droplets_of_thirst.set", value, quenched, player.getDisplayName()), true);
                                            return value;
                                        })))
                ))
                .then(Commands.literal("enable").then(Commands.argument("Player",EntityArgument.players())
                        .then(Commands.argument("bool", BoolArgumentType.bool())
                                .executes(context ->{
                                    Collection<ServerPlayer> players = EntityArgument.getPlayers(context,"Player");
                                    boolean shouldTick = BoolArgumentType.getBool(context,"bool");
                                    for(ServerPlayer player:players){
                                        IThirst thirstData = player.getData(ModAttachment.PLAYER_THIRST);
                                        thirstData.setShouldTickThirst(shouldTick);
                                        thirstData.updateThirstData(player);
                                    }
                                    Component names = ComponentUtils.formatList(players, ServerPlayer::getDisplayName);
                                    context.getSource().sendSuccess(() -> Component.translatable(shouldTick ? "command.droplets_of_thirst.enable" : "command.droplets_of_thirst.disable", names), true);
                                    return players.size();
                                }))))
                .then(Commands.literal("debug")
                        .then(Commands.literal("exhaustion")
                                .executes(context -> debugExhaustion(context.getSource(), context.getSource().getPlayerOrException()))
                                .then(Commands.argument("player", EntityArgument.player())
                                        .executes(context -> debugExhaustion(context.getSource(), EntityArgument.getPlayer(context, "player")))))
                        .then(Commands.literal("purity")
                                .executes(context -> debugPurity(context.getSource(), context.getSource().getPlayerOrException()))))
                .then(Commands.literal("config")
                        .then(Commands.literal("check")
                                .executes(context -> configCheck(context.getSource()))))
                .then(Commands.literal("infer")
                        .then(Commands.argument("item", ItemArgument.item(event.getBuildContext()))
                                .executes(context -> infer(context.getSource(), ItemArgument.getItem(context, "item").item().value()))))
        );
        dispatcher.register(Commands.literal("thirst").requires(Commands.hasPermission(Commands.LEVEL_GAMEMASTERS)).redirect(root));
        if (!FMLEnvironment.isProduction())
            dispatcher.register(Commands.literal(DropletsOfThirst.ID).requires(Commands.hasPermission(Commands.LEVEL_GAMEMASTERS))
                    .then(Commands.literal("dev")
                            .then(Commands.literal("dump_items")
                                    .then(Commands.argument("namespaces", StringArgumentType.greedyString())
                                            .executes(context -> dumpItems(context.getSource(), StringArgumentType.getString(context, "namespaces")))))));
    }

    /**
     * Development only: {@link ItemDump} of the items of the given namespaces (separated by spaces).
     */
    private static int dumpItems(CommandSourceStack source, String namespaces)
    {
        try
        {
            int rows = ItemDump.dump(Set.copyOf(Arrays.asList(namespaces.trim().split("\\s+"))));
            source.sendSuccess(() -> Component.literal("Wrote " + rows + " items to " + ItemDump.file()), false);
            return rows;
        }
        catch (IOException e)
        {
            source.sendFailure(Component.literal("Could not write " + ItemDump.file() + ": " + e.getMessage()));
            return 0;
        }
    }

    private static int debugExhaustion(CommandSourceStack source, ServerPlayer player)
    {
        PlayerThirst thirst = player.getData(ModAttachment.PLAYER_THIRST);
        float[] factors = new float[ExhaustionFactors.FACTORS.length];
        float cached = ExhaustionFactors.compute(player, factors);
        double drain = player.getAttributeValue(AttributeInit.THIRST_DRAIN);
        StringBuilder text = new StringBuilder("Thirst loss of ").append(player.getScoreboardName())
                .append(" (mode ").append(GameplayConfig.MODE.get()).append(")");
        for (int i = 0; i < factors.length; i++)
            text.append("\n  ").append(ExhaustionFactors.FACTORS[i]).append(": x").append(format(factors[i]))
                    .append(i == 0 ? " (" + climateSource(player) + ")" : "");
        text.append("\n  droplets_of_thirst:thirst_drain: x").append(format(drain))
                .append("\n  total: x").append(format(cached * drain))
                .append("\n  exhaustion ").append(format(thirst.getExhaustion())).append(" / ").append(format(GameplayConfig.EXHAUSTION_PER_POINT.get()))
                .append(", thirst ").append(thirst.getThirst()).append(", quenched ").append(thirst.getQuenched())
                .append(thirst.getShouldTickThirst() ? "" : " (thirst disabled for this player)");
        source.sendSuccess(() -> Component.literal(text.toString()), false);
        return 1;
    }

    private static String climateSource(ServerPlayer player)
    {
        DimensionWater dimension = player.level().dimensionTypeRegistration().getData(DropletsDataMaps.DIMENSION_WATER);
        if (dimension != null && dimension.thirstMultiplier().isPresent())
            return "dimension_water thirst_multiplier";
        if (ThirstHelper.isUltraWarm(player.level(), player.blockPosition()))
            return "netherMultiplier";
        return GameplayConfig.CLIMATE_FORMULA.get() + " formula";
    }

    /**
     * Purity of the water the player looks at, or of the block at their feet, with each step.
     */
    private static int debugPurity(CommandSourceStack source, ServerPlayer player)
    {
        if (!WaterPurity.enabled())
        {
            source.sendSuccess(() -> Component.literal("purity.enabled is false: water has no purity"), false);
            return 0;
        }
        Level level = player.level();
        BlockHitResult hit = WaterPurity.pickFluid(player, ClipContext.Fluid.ANY);
        BlockPos pos = hit.getType() == HitResult.Type.BLOCK ? hit.getBlockPos() : player.blockPosition();
        FluidState fluid = level.getFluidState(pos);
        List<String> trace = new ArrayList<>();
        int purity;
        if (fluid.is(FluidTags.WATER))
            purity = WaterPurity.getWaterPurity(level, pos, fluid.isSource(), trace);
        else
        {
            purity = WaterPurity.getBlockPurity(level, pos);
            trace.add("not water: " + (level.getBlockState(pos).is(Blocks.WATER_CAULDRON)
                    ? (purity == WaterPurity.HEATED_CAULDRON_PURITY ? "cauldron on a heat source" : "cauldron") : "defaultPurity"));
        }
        String biome = level.getBiome(pos).unwrapKey().map(key -> key.identifier().toString()).orElse("?");
        StringBuilder text = new StringBuilder("Purity at ").append(pos.toShortString()).append(" (").append(biome).append("): ")
                .append(purity).append(" ").append(WaterPurity.getPurityText(purity));
        for (String step : trace)
            text.append("\n  ").append(step);
        source.sendSuccess(() -> Component.literal(text.toString()), false);
        return purity;
    }

    private static int configCheck(CommandSourceStack source)
    {
        List<String> problems = ConfigCheck.problems();
        source.sendSuccess(() -> Component.literal(problems.isEmpty() ? "Droplets of Thirst config: no problems found"
                : "Droplets of Thirst config: " + problems.size() + " problem(s)\n  " + String.join("\n  ", problems)), false);
        return problems.size();
    }

    /**
     * Recomputes the estimate of one item from the current recipes and explains it; nothing is stored.
     */
    private static int infer(CommandSourceStack source, Item item)
    {
        MinecraftServer server = source.getServer();
        List<String> lines = RecipeInference.explain(item, ThirstHelper.inferenceInputs(), server.getRecipeManager(), server.registryAccess());
        source.sendSuccess(() -> Component.literal(String.join("\n", lines)), false);
        return lines.size();
    }

    private static String format(double value)
    {
        return String.format(Locale.ROOT, "%.3f", value);
    }
}

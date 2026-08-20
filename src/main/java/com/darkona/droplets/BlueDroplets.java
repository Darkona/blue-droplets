package com.darkona.droplets;

import com.darkona.droplets.api.DropletsAPI;
import com.darkona.droplets.compat.coldsweat.ColdSweatCompat;
import com.darkona.droplets.compat.create.CreateRegistry;
import com.darkona.droplets.compat.create.ponder.ThirstPonderPlugin;
import com.darkona.droplets.compat.reliquary.ReliquaryCompat;
import com.darkona.droplets.compat.delight.DelightCompat;
import com.darkona.droplets.compat.supernatural.SupernaturalCompat;
import com.darkona.droplets.compat.vampirism.VampirismCompat;
import com.darkona.droplets.content.DropletsServiceImpl;
import com.darkona.droplets.content.data.DropletsDataMaps;
import com.darkona.droplets.content.purity.WaterPurity;
import com.darkona.droplets.content.registry.AttributeInit;
import com.darkona.droplets.content.registry.ConditionInit;
import com.darkona.droplets.content.registry.EffectInit;
import com.darkona.droplets.content.registry.ItemInit;
import com.darkona.droplets.content.registry.LegacyIds;
import com.darkona.droplets.foundation.config.DiscouragedMods;
import com.darkona.droplets.content.registry.LootInit;
import com.darkona.droplets.content.thirst.PlayerThirst;
import com.darkona.droplets.content.thirst.PlayerThirstManager;
import com.darkona.droplets.foundation.common.capability.ModAttachment;
import com.darkona.droplets.foundation.config.*;
import com.darkona.droplets.foundation.gui.DrinkTooltip;
import com.darkona.droplets.foundation.gui.ThirstBarRenderer;
import com.darkona.droplets.foundation.gui.ThirstBarStyles;
import com.darkona.droplets.foundation.network.ThirstModPacketHandler;
import com.darkona.droplets.foundation.tab.ThirstTab;
import com.darkona.droplets.gametest.DropletsGameTests;
import com.darkona.droplets.compat.travelersbackpack.TravelersBackpackCompat;
import net.createmod.ponder.foundation.PonderIndex;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.PackType;
import net.minecraft.server.packs.PathPackResources;
import net.minecraft.server.packs.repository.Pack;
import net.minecraft.server.packs.repository.PackSource;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.AddPackFindersEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.ModList;
import net.minecraftforge.fml.ModLoadingContext;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.config.ModConfig;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import net.minecraftforge.fml.loading.FMLEnvironment;

import java.nio.file.Path;


@Mod(BlueDroplets.ID)
public class BlueDroplets
{
    public static final String ID = DropletsAPI.MOD_ID;

    public BlueDroplets()
    {
        IEventBus modBus = FMLJavaModLoadingContext.get().getModEventBus();
        DropletsAPI.setService(DropletsServiceImpl.INSTANCE);
        modBus.addListener(this::commonSetup);
        modBus.addListener(this::clientSetup);
        modBus.addListener(PlayerThirstManager::onConfigReloaded);
        modBus.addListener(BlueDroplets::addPacks);
        modBus.addListener(DropletsGameTests::register);
        MinecraftForge.EVENT_BUS.addListener(DropletsDataMaps::addReloadListeners);
        MinecraftForge.EVENT_BUS.addListener(EventPriority.HIGH, DropletsDataMaps::bind);
        ThirstModPacketHandler.register();
        DiscouragedMods.warn();
        TravelersBackpackCompat.init(modBus);
        ColdSweatCompat.init();
        ReliquaryCompat.init();
        VampirismCompat.init();
        DelightCompat.init();
        ModAttachment.register(modBus);

        if(FMLEnvironment.dist.isClient())
        {
            modBus.addListener(ThirstBarRenderer::registerLayer);
            ThirstBarStyles.registerBuiltIns();
            VampirismCompat.initClient();
            SupernaturalCompat.initClient();
            modBus.addListener(DrinkTooltip::registerFactory);
            MinecraftForge.EVENT_BUS.addListener(EventPriority.LOW, DrinkTooltip::gather);
        }

        ItemInit.register(modBus);
        EffectInit.register(modBus);
        AttributeInit.register(modBus);
        ConditionInit.register(modBus);
        LootInit.register(modBus);
        LegacyIds.register();

        if(ModList.get().isLoaded("create"))
            CreateRegistry.register();

        ThirstTab.register(modBus);
        LegacyConfigMigration.run();
        ModLoadingContext context = ModLoadingContext.get();
        context.registerConfig(ModConfig.Type.COMMON, GameplayConfig.SPEC, ID + "/gameplay.toml");
        context.registerConfig(ModConfig.Type.COMMON, PurityConfig.SPEC, ID + "/purity.toml");
        context.registerConfig(ModConfig.Type.COMMON, ItemsConfig.SPEC, ID + "/items.toml");
        context.registerConfig(ModConfig.Type.COMMON, CompatConfig.SPEC, ID + "/compat.toml");
        context.registerConfig(ModConfig.Type.CLIENT, ClientConfig.SPEC, ID + "/client.toml");
    }

    private void commonSetup(final FMLCommonSetupEvent event)
    {
        WaterPurity.init();
        event.enqueueWork(WaterPurity::registerCauldronInteractions);
        event.enqueueWork(EffectInit::registerBrewing);
    }

    private void clientSetup(final FMLClientSetupEvent event)
    {
        if(ModList.get().isLoaded("create")){
            event.enqueueWork(()-> new Object()
            {
                public void registerPonderPlugin(){
                    PonderIndex.addPlugin(new ThirstPonderPlugin());
                }
            }.registerPonderPlugin());
        }
    }

    /**
     * Optional built-in datapacks under {@code datapacks/} in the jar. {@code BUILT_IN} packs are enabled by default,
     * also in existing worlds; {@code FEATURE} packs must be enabled when creating the world or with {@code /datapack enable}.
     */
    private static void addPacks(AddPackFindersEvent event)
    {
        addPack(event, "purify_smelting", "Water purification: furnace", PackSource.BUILT_IN);
        addPack(event, "purify_campfire", "Water purification: campfire", PackSource.BUILT_IN);
        addPack(event, "purify_smoking", "Water purification: smoker", PackSource.FEATURE);
        addPack(event, "purify_cooking_pot", "Water purification: cooking pot", PackSource.BUILT_IN);
        addPack(event, "clean_water_cooking", "Clean water for cooking recipes", PackSource.BUILT_IN);
        addPack(event, "preset_casual", "casual preset", PackSource.FEATURE);
        addPack(event, "preset_hardcore", "hardcore preset", PackSource.FEATURE);
    }

    /**
     * Forge 1.20.1 has no helper for packs inside a mod jar: the pack is the jar's {@code datapacks/<name>} folder,
     * with the id {@code mod/blue_droplets:datapacks/<name>} that later versions give it.
     */
    private static void addPack(AddPackFindersEvent event, String name, String title, PackSource source)
    {
        if (event.getPackType() != PackType.SERVER_DATA)
            return;
        Path path = ModList.get().getModFileById(ID).getFile().findResource("datapacks/" + name);
        String id = "mod/" + ID + ":datapacks/" + name;
        event.addRepositorySource(packs -> {
            Pack pack = Pack.readMetaAndCreate(id, Component.literal("Blue Droplets: " + title), false,
                    packId -> new PathPackResources(packId, path, false), PackType.SERVER_DATA, Pack.Position.TOP, source);
            if (pack != null)
                packs.accept(pack);
        });
    }

    public static ResourceLocation asResource(String path)
    {
        return new ResourceLocation(ID, path);
    }
}

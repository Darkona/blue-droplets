package com.darkona.droplets;

import com.darkona.droplets.api.DropletsAPI;
import com.darkona.droplets.compat.create.CreateRegistry;
import com.darkona.droplets.compat.create.SandFilterBlockEntity;
import com.darkona.droplets.compat.create.ponder.ThirstPonderPlugin;
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
import com.darkona.droplets.content.registry.ThirstComponent;
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
import net.createmod.ponder.foundation.PonderIndex;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.PackType;
import net.minecraft.server.packs.repository.Pack;
import net.minecraft.server.packs.repository.PackSource;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.ModList;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.config.ModConfig;
import net.neoforged.fml.event.lifecycle.FMLClientSetupEvent;
import net.neoforged.fml.event.lifecycle.FMLCommonSetupEvent;
import net.neoforged.fml.loading.FMLEnvironment;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.AddPackFindersEvent;


@Mod(BlueDroplets.ID)
public class BlueDroplets
{
    public static final String ID = DropletsAPI.MOD_ID;

    public BlueDroplets(IEventBus modBus, ModContainer modContainer)
    {
        DropletsAPI.setService(DropletsServiceImpl.INSTANCE);
        modBus.addListener(this::commonSetup);
        modBus.addListener(this::clientSetup);
        modBus.addListener(DropletsDataMaps::register);
        modBus.addListener(PlayerThirstManager::onConfigReloaded);
        modBus.addListener(BlueDroplets::addPacks);
        modBus.addListener(ThirstModPacketHandler::register);
        modBus.addListener(DropletsGameTests::register);
        ModAttachment.ATTACHMENT_TYPES.register(modBus);
        ThirstComponent.DR.register(modBus);

        if(FMLEnvironment.dist.isClient())
        {
            modBus.addListener(ThirstBarRenderer::registerLayer);
            ThirstBarStyles.registerBuiltIns();
            VampirismCompat.initClient();
            SupernaturalCompat.initClient();
            modBus.addListener(DrinkTooltip::registerFactory);
            NeoForge.EVENT_BUS.addListener(DrinkTooltip::gather);
        }

        ItemInit.register(modBus);
        EffectInit.register(modBus);
        AttributeInit.register(modBus);
        ConditionInit.CONDITION_CODECS.register(modBus);
        ConditionInit.LOOT_CONDITIONS.register(modBus);
        LegacyIds.register(modBus);

        if(ModList.get().isLoaded("create"))
        {
            CreateRegistry.register();
            modBus.addListener(SandFilterBlockEntity::registerCapabilities);
        }

        ThirstTab.register(modBus);
        LegacyConfigMigration.run();
        modContainer.registerConfig(ModConfig.Type.COMMON, GameplayConfig.SPEC, ID + "/gameplay.toml");
        modContainer.registerConfig(ModConfig.Type.COMMON, PurityConfig.SPEC, ID + "/purity.toml");
        modContainer.registerConfig(ModConfig.Type.COMMON, ItemsConfig.SPEC, ID + "/items.toml");
        modContainer.registerConfig(ModConfig.Type.COMMON, CompatConfig.SPEC, ID + "/compat.toml");
        modContainer.registerConfig(ModConfig.Type.CLIENT, ClientConfig.SPEC, ID + "/client.toml");
    }

    private void commonSetup(final FMLCommonSetupEvent event)
    {
        WaterPurity.init();
        event.enqueueWork(WaterPurity::registerCauldronInteractions);
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
        addPack(event, "preset_casual", "casual preset", PackSource.FEATURE);
        addPack(event, "preset_hardcore", "hardcore preset", PackSource.FEATURE);
    }

    private static void addPack(AddPackFindersEvent event, String name, String title, PackSource source)
    {
        event.addPackFinders(asResource("datapacks/" + name), PackType.SERVER_DATA, Component.literal("Blue Droplets: " + title), source, false, Pack.Position.TOP);
    }

    public static ResourceLocation asResource(String path)
    {
        return ResourceLocation.fromNamespaceAndPath(ID, path);
    }
}

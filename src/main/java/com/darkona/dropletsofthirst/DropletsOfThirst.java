package com.darkona.dropletsofthirst;

import com.darkona.dropletsofthirst.api.DropletsAPI;
import com.darkona.dropletsofthirst.compat.coldsweat.ColdSweatCompat;
import com.darkona.dropletsofthirst.compat.reliquary.ReliquaryCompat;
import com.darkona.dropletsofthirst.compat.delight.DelightCompat;
import com.darkona.dropletsofthirst.compat.create.CreateRegistry;
import com.darkona.dropletsofthirst.compat.create.SandFilterBlockEntity;
import com.darkona.dropletsofthirst.compat.create.ponder.ThirstPonderPlugin;
import com.darkona.dropletsofthirst.compat.supernatural.SupernaturalCompat;
import com.darkona.dropletsofthirst.compat.vampirism.VampirismCompat;
import com.darkona.dropletsofthirst.content.DropletsServiceImpl;
import com.darkona.dropletsofthirst.content.data.DropletsDataMaps;
import com.darkona.dropletsofthirst.content.purity.WaterPurity;
import com.darkona.dropletsofthirst.content.registry.AttributeInit;
import com.darkona.dropletsofthirst.content.registry.ConditionInit;
import com.darkona.dropletsofthirst.content.registry.EffectInit;
import com.darkona.dropletsofthirst.content.registry.ItemInit;
import com.darkona.dropletsofthirst.content.registry.LegacyIds;
import com.darkona.dropletsofthirst.content.registry.ThirstComponent;
import com.darkona.dropletsofthirst.content.thirst.PlayerThirst;
import com.darkona.dropletsofthirst.content.thirst.PlayerThirstManager;
import com.darkona.dropletsofthirst.foundation.common.capability.ModAttachment;
import com.darkona.dropletsofthirst.foundation.config.*;
import com.darkona.dropletsofthirst.foundation.gui.DrinkTooltip;
import com.darkona.dropletsofthirst.foundation.gui.ThirstBarRenderer;
import com.darkona.dropletsofthirst.foundation.gui.ThirstBarStyles;
import com.darkona.dropletsofthirst.foundation.network.ThirstModPacketHandler;
import com.darkona.dropletsofthirst.foundation.tab.ThirstTab;
import com.darkona.dropletsofthirst.gametest.DropletsGameTests;
import net.createmod.ponder.foundation.PonderIndex;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.PackType;
import net.minecraft.server.packs.repository.Pack;
import net.minecraft.server.packs.repository.PackSource;
import com.darkona.dropletsofthirst.compat.travelersbackpack.TravelersBackpackCompat;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import com.darkona.dropletsofthirst.compat.create.CreateCompat;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.config.ModConfig;
import net.neoforged.fml.event.lifecycle.FMLClientSetupEvent;
import net.neoforged.fml.event.lifecycle.FMLCommonSetupEvent;
import net.neoforged.fml.loading.FMLEnvironment;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.AddPackFindersEvent;


@Mod(DropletsOfThirst.ID)
public class DropletsOfThirst
{
    public static final String ID = DropletsAPI.MOD_ID;

    public DropletsOfThirst(IEventBus modBus, ModContainer modContainer)
    {
        DropletsAPI.setService(DropletsServiceImpl.INSTANCE);
        modBus.addListener(this::commonSetup);
        modBus.addListener(this::clientSetup);
        modBus.addListener(DropletsDataMaps::register);
        modBus.addListener(PlayerThirstManager::onConfigReloaded);
        modBus.addListener(DropletsOfThirst::addPacks);
        modBus.addListener(ThirstModPacketHandler::register);
        modBus.addListener(DropletsGameTests::register);
        TravelersBackpackCompat.init(modBus);
        ColdSweatCompat.init();
        ReliquaryCompat.init();
        VampirismCompat.init();
        DelightCompat.init();
        ModAttachment.ATTACHMENT_TYPES.register(modBus);
        ThirstComponent.DR.register(modBus);

        if(FMLEnvironment.dist.isClient())
        {
            modBus.addListener(ThirstBarRenderer::registerLayer);
            ThirstBarStyles.registerBuiltIns();
            VampirismCompat.initClient();
            SupernaturalCompat.initClient();
            modBus.addListener(DrinkTooltip::registerFactory);
            NeoForge.EVENT_BUS.addListener(EventPriority.LOW, DrinkTooltip::gather);
            NeoForge.EVENT_BUS.addListener(WaterPurity::renderPurityTooltip);
            NeoForge.EVENT_BUS.addListener(PlayerThirstManager::estimatedTooltip);
        }

        ItemInit.register(modBus);
        EffectInit.register(modBus);
        AttributeInit.register(modBus);
        ConditionInit.CONDITION_CODECS.register(modBus);
        ConditionInit.LOOT_CONDITIONS.register(modBus);
        LegacyIds.register(modBus);

        if(CreateCompat.LOADED)
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
        if(CreateCompat.LOADED){
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

    private static void addPack(AddPackFindersEvent event, String name, String title, PackSource source)
    {
        event.addPackFinders(asResource("datapacks/" + name), PackType.SERVER_DATA, Component.literal("Droplets of Thirst: " + title), source, false, Pack.Position.TOP);
    }

    public static ResourceLocation asResource(String path)
    {
        return ResourceLocation.fromNamespaceAndPath(ID, path);
    }
}

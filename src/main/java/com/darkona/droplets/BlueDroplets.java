package com.darkona.droplets;

import com.darkona.droplets.compat.appleskin.AppleSkinCompat;
import com.darkona.droplets.compat.create.CreateRegistry;
import com.darkona.droplets.compat.create.ponder.ThirstPonderPlugin;
import com.darkona.droplets.content.purity.WaterPurity;
import com.darkona.droplets.content.registry.ConditionInit;
import com.darkona.droplets.content.registry.EffectInit;
import com.darkona.droplets.content.registry.ItemInit;
import com.darkona.droplets.content.registry.LegacyIds;
import com.darkona.droplets.content.registry.ThirstComponent;
import com.darkona.droplets.content.thirst.PlayerThirst;
import com.darkona.droplets.foundation.common.capability.ModAttachment;
import com.darkona.droplets.foundation.config.*;
import com.darkona.droplets.foundation.tab.ThirstTab;
import net.createmod.ponder.foundation.PonderIndex;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.ModList;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.event.lifecycle.FMLClientSetupEvent;
import net.neoforged.fml.event.lifecycle.FMLCommonSetupEvent;
import net.neoforged.fml.loading.FMLEnvironment;


@Mod(BlueDroplets.ID)
public class BlueDroplets
{
    public static final String ID = "bluedroplets";

    public BlueDroplets(IEventBus modBus, ModContainer modContainer)
    {

        modBus.addListener(this::commonSetup);
        modBus.addListener(this::clientSetup);
        ModAttachment.ATTACHMENT_TYPES.register(modBus);
        ThirstComponent.DR.register(modBus);

        if(FMLEnvironment.dist.isClient())
            AppleSkinCompat.initClient(modBus);

        ItemInit.register(modBus);
        EffectInit.register(modBus);
        ConditionInit.CONDITION_CODECS.register(modBus);
        LegacyIds.register(modBus);

        if(ModList.get().isLoaded("create"))
        {
            CreateRegistry.register();
        }

        ThirstTab.register(modBus);
        //configs
        ItemSettingsConfig.setup(modContainer);
        CommonConfig.setup(modContainer);
        ClientConfig.setup(modContainer);
        KeyWordConfig.setup(modContainer);
        ContainerConfig.setup(modContainer);
    }

    private void commonSetup(final FMLCommonSetupEvent event)
    {
        WaterPurity.init();

        if(ModList.get().isLoaded("tombstone"))
            PlayerThirst.checkTombstoneEffects = true;

        if(ModList.get().isLoaded("farmersdelight"))
            PlayerThirst.checkFDEffects = true;

        if(ModList.get().isLoaded("bakery"))
            PlayerThirst.checkLetsDoBakeryEffects = true;

        if(ModList.get().isLoaded("brewery"))
            PlayerThirst.checkLetsDoBreweryEffects = true;
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

    public static ResourceLocation asResource(String path)
    {
        return ResourceLocation.fromNamespaceAndPath(ID, path);
    }
}

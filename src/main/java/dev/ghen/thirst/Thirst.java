package dev.ghen.thirst;

import dev.ghen.thirst.compat.appleskin.AppleSkinCompat;
import dev.ghen.thirst.compat.create.CreateRegistry;
import dev.ghen.thirst.compat.create.ponder.ThirstPonderPlugin;
import dev.ghen.thirst.content.purity.WaterPurity;
import dev.ghen.thirst.content.registry.ConditionInit;
import dev.ghen.thirst.content.registry.EffectInit;
import dev.ghen.thirst.content.registry.ItemInit;
import dev.ghen.thirst.content.registry.ThirstComponent;
import dev.ghen.thirst.content.thirst.PlayerThirst;
import dev.ghen.thirst.foundation.common.capability.ModAttachment;
import dev.ghen.thirst.foundation.config.*;
import dev.ghen.thirst.foundation.tab.ThirstTab;
import net.createmod.ponder.foundation.PonderIndex;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.ModList;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.event.lifecycle.FMLClientSetupEvent;
import net.neoforged.fml.event.lifecycle.FMLCommonSetupEvent;
import net.neoforged.fml.loading.FMLEnvironment;


@Mod(Thirst.ID)
public class Thirst
{
    public static final String ID = "thirst";

    public Thirst(IEventBus modBus, ModContainer modContainer)
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

package com.darkona.droplets.content.purity;

import com.darkona.droplets.BlueDroplets;
import com.darkona.droplets.content.registry.ItemInit;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RegisterColorHandlersEvent;

@EventBusSubscriber(modid = BlueDroplets.ID, value = Dist.CLIENT)
public final class PurityTintClient
{
    /**
     * The terracotta water bowl draws its liquid with a grey layer, so it needs the vanilla water color by default;
     * the purity color replaces it in the {@code ItemColors} mixin.
     */
    @SubscribeEvent
    public static void registerItemColors(RegisterColorHandlersEvent.Item event)
    {
        event.register((stack, tintIndex) -> tintIndex == 0 ? PurityTint.VANILLA_WATER : -1, ItemInit.TERRACOTTA_WATER_BOWL.get());
    }

    private PurityTintClient() {}
}

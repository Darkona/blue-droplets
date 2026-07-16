package com.darkona.droplets.compat.reliquary;

import com.darkona.droplets.foundation.config.CompatConfig;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.fml.ModList;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.entity.living.LivingEntityUseItemEvent;

/**
 * Reliquary, without linking to it (items are matched by id). The Emperor's Chalice hydrates through the same path
 * as any drink: an entry in the {@code blue_droplets:drinks} data map, with purity 5 and no extra pure water bonus.
 * Its own effects (a point of food, the damage) stay as Reliquary has them. The Infernal Chalice has no entry, so it
 * does not hydrate. This class only adds the optional cooldown after a drink.
 */
public final class ReliquaryCompat
{
    public static final boolean LOADED = ModList.get().isLoaded("reliquary");
    public static final ResourceLocation EMPEROR_CHALICE = new ResourceLocation("reliquary", "emperor_chalice");

    private ReliquaryCompat() {}

    public static void init()
    {
        if (LOADED)
            MinecraftForge.EVENT_BUS.addListener(ReliquaryCompat::cooldownAfterDrinking);
    }

    private static void cooldownAfterDrinking(LivingEntityUseItemEvent.Finish event)
    {
        int cooldown = CompatConfig.RELIQUARY_EMPEROR_CHALICE_COOLDOWN.get();
        ItemStack item = event.getItem();
        if (cooldown > 0 && event.getEntity() instanceof ServerPlayer player && BuiltInRegistries.ITEM.getKey(item.getItem()).equals(EMPEROR_CHALICE))
            player.getCooldowns().addCooldown(item.getItem(), cooldown);
    }
}

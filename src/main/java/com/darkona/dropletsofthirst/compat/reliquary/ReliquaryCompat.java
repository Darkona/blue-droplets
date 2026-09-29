package com.darkona.dropletsofthirst.compat.reliquary;

import com.darkona.dropletsofthirst.foundation.config.CompatConfig;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.item.ItemStack;
import net.neoforged.fml.ModList;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.entity.living.LivingEntityUseItemEvent;

/**
 * Reliquary, without linking to it (items are matched by id). The Emperor's Chalice hydrates through the same path
 * as any drink: an entry in the {@code droplets_of_thirst:drinks} data map, with purity 5 and no extra pure water bonus.
 * Its own effects (a point of food, the damage) stay as Reliquary has them. The Infernal Chalice has no entry, so it
 * does not hydrate. This class only adds the optional cooldown after a drink.
 */
public final class ReliquaryCompat
{
    public static final boolean LOADED = ModList.get().isLoaded("reliquary");
    public static final Identifier EMPEROR_CHALICE = Identifier.fromNamespaceAndPath("reliquary", "emperor_chalice");

    private ReliquaryCompat() {}

    public static void init()
    {
        if (LOADED)
            NeoForge.EVENT_BUS.addListener(ReliquaryCompat::cooldownAfterDrinking);
    }

    private static void cooldownAfterDrinking(LivingEntityUseItemEvent.Finish event)
    {
        int cooldown = CompatConfig.RELIQUARY_EMPEROR_CHALICE_COOLDOWN.get();
        ItemStack item = event.getItem();
        if (cooldown > 0 && event.getEntity() instanceof ServerPlayer player && BuiltInRegistries.ITEM.getKey(item.getItem()).equals(EMPEROR_CHALICE))
            player.getCooldowns().addCooldown(item, cooldown);
    }
}

package com.darkona.droplets.content.registry;

import com.darkona.droplets.BlueDroplets;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.RangedAttribute;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.event.entity.EntityAttributeModificationEvent;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

public final class AttributeInit
{
    private static final DeferredRegister<Attribute> ATTRIBUTES = DeferredRegister.create(Registries.ATTRIBUTE, BlueDroplets.ID);

    /**
     * Multiplies every player's thirst loss (base 1.0). Items, enchantments ({@code minecraft:attributes}), effects,
     * Curios and commands change it with ordinary attribute modifiers.
     */
    public static final DeferredHolder<Attribute, Attribute> THIRST_DRAIN = ATTRIBUTES.register("thirst_drain",
            () -> new RangedAttribute("attribute.name.bluedroplets.thirst_drain", 1.0, 0.0, 10.0).setSyncable(true));

    private AttributeInit() {}

    public static void register(IEventBus modBus)
    {
        ATTRIBUTES.register(modBus);
        modBus.addListener(AttributeInit::addToPlayers);
    }

    private static void addToPlayers(EntityAttributeModificationEvent event)
    {
        event.add(EntityType.PLAYER, THIRST_DRAIN);
    }
}

package com.darkona.dropletsofthirst.content.registry;

import com.darkona.dropletsofthirst.DropletsOfThirst;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.RangedAttribute;
import net.minecraftforge.event.entity.EntityAttributeModificationEvent;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.RegistryObject;

public final class AttributeInit
{
    private static final DeferredRegister<Attribute> ATTRIBUTES = DeferredRegister.create(Registries.ATTRIBUTE, DropletsOfThirst.ID);

    /**
     * Multiplies every player's thirst loss (base 1.0). Items, enchantments, effects, Curios and commands change it
     * with ordinary attribute modifiers.
     */
    public static final RegistryObject<Attribute> THIRST_DRAIN = ATTRIBUTES.register("thirst_drain",
            () -> new RangedAttribute("attribute.name.droplets_of_thirst.thirst_drain", 1.0, 0.0, 10.0).setSyncable(true));

    private AttributeInit() {}

    public static void register(IEventBus modBus)
    {
        ATTRIBUTES.register(modBus);
        modBus.addListener(AttributeInit::addToPlayers);
    }

    private static void addToPlayers(EntityAttributeModificationEvent event)
    {
        event.add(EntityType.PLAYER, THIRST_DRAIN.get());
    }
}

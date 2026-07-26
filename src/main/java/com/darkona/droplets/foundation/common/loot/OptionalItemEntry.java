package com.darkona.droplets.foundation.common.loot;

import com.darkona.droplets.content.registry.LootInit;
import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonObject;
import com.google.gson.JsonSerializationContext;
import net.minecraft.core.Registry;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.GsonHelper;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.storage.loot.LootContext;
import net.minecraft.world.level.storage.loot.entries.LootPoolEntryType;
import net.minecraft.world.level.storage.loot.entries.LootPoolSingletonContainer;
import net.minecraft.world.level.storage.loot.functions.LootItemFunction;
import net.minecraft.world.level.storage.loot.predicates.LootItemCondition;

import java.util.function.Consumer;

/**
 * {@code blue_droplets:optional_item}: a {@code minecraft:item} entry whose item may belong to a mod that is not
 * installed; then it gives nothing. Forge 1.18.2 has no load conditions for loot tables, and an unknown item fails the
 * whole table, so the tables with other mods' drinks use it.
 */
public class OptionalItemEntry extends LootPoolSingletonContainer
{
    private final ResourceLocation name;
    private final Item item;

    OptionalItemEntry(ResourceLocation name, int weight, int quality, LootItemCondition[] conditions, LootItemFunction[] functions)
    {
        super(weight, quality, conditions, functions);
        this.name = name;
        this.item = Registry.ITEM.getOptional(name).orElse(Items.AIR);
    }

    @Override
    public LootPoolEntryType getType()
    {
        return LootInit.OPTIONAL_ITEM.get();
    }

    @Override
    protected void createItemStack(Consumer<ItemStack> output, LootContext context)
    {
        if (item != Items.AIR)
            output.accept(new ItemStack(item));
    }

    public static class Serializer extends LootPoolSingletonContainer.Serializer<OptionalItemEntry>
    {
        @Override
        public void serializeCustom(JsonObject json, OptionalItemEntry entry, JsonSerializationContext context)
        {
            super.serializeCustom(json, entry, context);
            json.addProperty("name", entry.name.toString());
        }

        @Override
        protected OptionalItemEntry deserialize(JsonObject json, JsonDeserializationContext context, int weight, int quality, LootItemCondition[] conditions, LootItemFunction[] functions)
        {
            return new OptionalItemEntry(new ResourceLocation(GsonHelper.getAsString(json, "name")), weight, quality, conditions, functions);
        }
    }
}

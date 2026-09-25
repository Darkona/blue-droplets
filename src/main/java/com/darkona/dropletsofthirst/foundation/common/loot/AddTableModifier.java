package com.darkona.dropletsofthirst.foundation.common.loot;

import com.google.gson.JsonObject;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.GsonHelper;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.storage.loot.LootContext;
import net.minecraft.world.level.storage.loot.predicates.LootItemCondition;
import net.minecraftforge.common.loot.GlobalLootModifierSerializer;
import net.minecraftforge.common.loot.LootModifier;
import org.jetbrains.annotations.NotNull;

import java.util.List;

/**
 * {@code droplets_of_thirst:add_table}: rolls another loot table into the generated loot, like NeoForge's
 * {@code neoforge:add_table} of later versions. A table that did not load adds nothing. Rolled raw, so the global loot
 * modifiers are not applied twice.
 */
public class AddTableModifier extends LootModifier
{
    private final ResourceLocation table;

    public AddTableModifier(LootItemCondition[] conditions, ResourceLocation table)
    {
        super(conditions);
        this.table = table;
    }

    @Override
    protected @NotNull List<ItemStack> doApply(List<ItemStack> generatedLoot, LootContext context)
    {
        context.getLootTable(table).getRandomItemsRaw(context, generatedLoot::add);
        return generatedLoot;
    }

    /**
     * Forge 1.18.2 reads loot modifiers from JSON with a serializer (later versions use a codec); the file format is the same.
     */
    public static final class Serializer extends GlobalLootModifierSerializer<AddTableModifier>
    {
        @Override
        public AddTableModifier read(ResourceLocation location, JsonObject json, LootItemCondition[] conditions)
        {
            return new AddTableModifier(conditions, new ResourceLocation(GsonHelper.getAsString(json, "table")));
        }

        @Override
        public JsonObject write(AddTableModifier instance)
        {
            JsonObject json = makeConditions(instance.conditions);
            json.addProperty("table", instance.table.toString());
            return json;
        }
    }
}

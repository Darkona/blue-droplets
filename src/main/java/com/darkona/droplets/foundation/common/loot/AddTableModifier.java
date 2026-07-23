package com.darkona.droplets.foundation.common.loot;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.storage.loot.LootContext;
import net.minecraft.world.level.storage.loot.predicates.LootItemCondition;
import net.minecraftforge.common.loot.IGlobalLootModifier;
import net.minecraftforge.common.loot.LootModifier;
import org.jetbrains.annotations.NotNull;

/**
 * {@code blue_droplets:add_table}: rolls another loot table into the generated loot, like NeoForge's
 * {@code neoforge:add_table} of later versions. A table that did not load adds nothing. Rolled raw, so the global loot
 * modifiers are not applied twice.
 */
public class AddTableModifier extends LootModifier
{
    public static final Codec<AddTableModifier> CODEC = RecordCodecBuilder.create(instance -> codecStart(instance)
            .and(ResourceLocation.CODEC.fieldOf("table").forGetter(modifier -> modifier.table))
            .apply(instance, AddTableModifier::new));

    private final ResourceLocation table;

    public AddTableModifier(LootItemCondition[] conditions, ResourceLocation table)
    {
        super(conditions);
        this.table = table;
    }

    @Override
    protected @NotNull ObjectArrayList<ItemStack> doApply(ObjectArrayList<ItemStack> generatedLoot, LootContext context)
    {
        context.getLootTable(table).getRandomItemsRaw(context, generatedLoot::add);
        return generatedLoot;
    }

    @Override
    public Codec<? extends IGlobalLootModifier> codec()
    {
        return CODEC;
    }
}

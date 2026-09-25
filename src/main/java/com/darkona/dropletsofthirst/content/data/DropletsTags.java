package com.darkona.dropletsofthirst.content.data;

import com.darkona.dropletsofthirst.DropletsOfThirst;
import com.darkona.dropletsofthirst.api.PurityLevel;
import net.minecraft.core.Registry;
import net.minecraft.tags.TagKey;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.material.Fluid;

public final class DropletsTags
{
    /** Index = purity: biomes in {@code droplets_of_thirst:water_purity/N} have water of base purity N (highest wins). */
    public static final TagKey<Biome>[] WATER_PURITY = waterPurityTags();
    public static final TagKey<Biome> SALT_WATER = TagKey.create(Registry.BIOME_REGISTRY, DropletsOfThirst.asResource("salt_water"));

    /** Fluids made from water that keep its purity (Create basins). */
    public static final TagKey<Fluid> CARRIES_PURITY = TagKey.create(Registry.FLUID_REGISTRY, DropletsOfThirst.asResource("carries_purity"));
    /** Items that never get or show a purity, e.g. other mods' water containers that compare components. */
    public static final TagKey<Item> PURITY_OPT_OUT = TagKey.create(Registry.ITEM_REGISTRY, DropletsOfThirst.asResource("purity_opt_out"));
    /** Drinks that carry a water purity (static containers: they can be filled by machines but not from the world). */
    public static final TagKey<Item> PURITY_CONTAINERS = TagKey.create(Registry.ITEM_REGISTRY, DropletsOfThirst.asResource("purity_containers"));
    /** Blocks that heat a water cauldron above them: its water comes out clean instead of murky; blocks with a {@code lit} property only when lit. */
    public static final TagKey<Block> CAULDRON_HEAT_SOURCES = TagKey.create(Registry.BLOCK_REGISTRY, DropletsOfThirst.asResource("cauldron_heat_sources"));
    /** Blocks filled with water by clicking that boil it, e.g. kettles: they refuse water below {@code compat.toml} {@code delight.kettleMinPurity}. */
    public static final TagKey<Block> REJECTS_DIRTY_WATER = TagKey.create(Registry.BLOCK_REGISTRY, DropletsOfThirst.asResource("rejects_dirty_water"));
    /** Items that never restore thirst, whatever the config, datapacks or other mods say. */
    public static final TagKey<Item> NO_THIRST = TagKey.create(Registry.ITEM_REGISTRY, DropletsOfThirst.asResource("no_thirst"));
    /** Items that take the {@code salty} penalties of {@code items.toml} when nothing else gives them values. */
    public static final TagKey<Item> SALTY = TagKey.create(Registry.ITEM_REGISTRY, DropletsOfThirst.asResource("salty"));
    /** The only items that hydrate a vampire, with their values in {@code droplets_of_thirst:drinks}. */
    public static final TagKey<Item> BLOOD = TagKey.create(Registry.ITEM_REGISTRY, DropletsOfThirst.asResource("blood"));

    /** Effects that pause thirst loss from activity while active, e.g. Farmer's Delight Nourishment. */
    public static final TagKey<MobEffect> PAUSES_THIRST = TagKey.create(Registry.MOB_EFFECT_REGISTRY, DropletsOfThirst.asResource("pauses_thirst"));
    /** Effects that stop the whole thirst tick while active, e.g. Corail Tombstone's Ghostly Shape. */
    public static final TagKey<MobEffect> STOPS_THIRST = TagKey.create(Registry.MOB_EFFECT_REGISTRY, DropletsOfThirst.asResource("stops_thirst"));

    private DropletsTags() {}

    @SuppressWarnings("unchecked")
    private static TagKey<Biome>[] waterPurityTags()
    {
        TagKey<Biome>[] tags = new TagKey[PurityLevel.MAX + 1];
        for (int purity = 0; purity < tags.length; purity++)
            tags[purity] = TagKey.create(Registry.BIOME_REGISTRY, DropletsOfThirst.asResource("water_purity/" + purity));
        return tags;
    }
}

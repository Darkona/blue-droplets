package com.darkona.droplets.content.data;

import com.darkona.droplets.BlueDroplets;
import net.minecraft.core.registries.Registries;
import net.minecraft.tags.TagKey;
import net.minecraft.world.level.biome.Biome;

public final class DropletsTags
{
    /** Index = purity: biomes in {@code bluedroplets:water_purity/N} have water of base purity N (highest wins). */
    public static final TagKey<Biome>[] WATER_PURITY = waterPurityTags();
    public static final TagKey<Biome> SALT_WATER = TagKey.create(Registries.BIOME, BlueDroplets.asResource("salt_water"));

    private DropletsTags() {}

    @SuppressWarnings("unchecked")
    private static TagKey<Biome>[] waterPurityTags()
    {
        TagKey<Biome>[] tags = new TagKey[4];
        for (int purity = 0; purity < tags.length; purity++)
            tags[purity] = TagKey.create(Registries.BIOME, BlueDroplets.asResource("water_purity/" + purity));
        return tags;
    }
}

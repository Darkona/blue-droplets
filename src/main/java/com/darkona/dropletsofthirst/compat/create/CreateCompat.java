package com.darkona.dropletsofthirst.compat.create;

import com.darkona.dropletsofthirst.DropletsOfThirst;
import com.darkona.dropletsofthirst.foundation.config.CompatConfig;
import net.minecraft.core.Registry;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;
import net.minecraftforge.fml.ModList;

/**
 * What the rest of the mod may ask about Create without loading any Create class.
 */
public final class CreateCompat
{
    public static final boolean LOADED = ModList.get().isLoaded("create");

    private CreateCompat() {}

    /**
     * Purity of water after one Sand Filter: {@code sandFilterFiltrationAmount} more, up to {@code sandFilterMaxPurity};
     * water that is already purer passes unchanged.
     */
    public static int sandFilterPurity(int purity)
    {
        int max = CompatConfig.SAND_FILTER_MAX_PURITY.get();
        return Math.max(purity, Math.min(purity + CompatConfig.SAND_FILTER_FILTRATION_AMOUNT.get(), max));
    }

    /**
     * The Sand Filter item, or air without Create.
     */
    public static Item sandFilter()
    {
        return LOADED ? Registry.ITEM.get(DropletsOfThirst.asResource("sand_filter")) : Items.AIR;
    }
}

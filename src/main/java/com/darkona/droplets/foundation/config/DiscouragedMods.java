package com.darkona.droplets.foundation.config;

import com.darkona.droplets.BlueDroplets;
import com.mojang.logging.LogUtils;
import net.minecraftforge.fml.ModList;
import net.minecraftforge.fml.ModLoader;
import net.minecraftforge.fml.ModLoadingContext;
import net.minecraftforge.fml.ModLoadingStage;
import net.minecraftforge.fml.ModLoadingWarning;
import org.slf4j.Logger;

import java.util.Map;

/**
 * Other thirst mods: the game starts, with a warning on the loading screen and in the log that both thirst systems
 * will run. Forge 1.20.1 has no {@code discouraged} dependency type in {@code mods.toml}, so the warning is added here.
 */
public final class DiscouragedMods
{
    private static final Logger LOGGER = LogUtils.getLogger();
    private static final Map<String, String> THIRST_MODS = Map.of(
            "toughasnails", "Tough As Nails",
            "legendarysurvivaloverhaul", "Legendary Survival Overhaul",
            "homeostatic", "Homeostatic",
            "survive", "Survive");

    private DiscouragedMods() {}

    public static void warn()
    {
        THIRST_MODS.forEach((id, name) -> {
            if (!ModList.get().isLoaded(id))
                return;
            LOGGER.warn("{} has its own thirst system. Both will run side by side; turn one of them off in its config if you only want one.", name);
            ModLoader.get().addWarning(new ModLoadingWarning(ModLoadingContext.get().getActiveContainer().getModInfo(), ModLoadingStage.CONSTRUCT,
                    BlueDroplets.ID + ".warning.other_thirst_mod", name));
        });
    }
}

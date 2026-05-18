package com.darkona.droplets.foundation.config;


import com.darkona.droplets.BlueDroplets;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.config.ModConfig;
import net.neoforged.fml.loading.FMLPaths;
import net.neoforged.neoforge.common.ModConfigSpec;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;

public class ContainerConfig {
    private static final ModConfigSpec SPEC;
    public static final  ModConfigSpec.Builder BUILDER = new  ModConfigSpec.Builder();
    public static final  ModConfigSpec.ConfigValue<List<? extends String>> CONTAINERS;

    static {
        BUILDER.push("Container");

        CONTAINERS = BUILDER.comment("Drinks that carry a water purity (added to the item tag bluedroplets:purity_containers, where the defaults live)"
                        ,"Format: [\"examplemod:example_item_1\", \"#examplemod:example_tag\"]")
                .<String>defineListAllowEmpty("Containers", List.of(), () -> "namespace:item", it -> it instanceof String);

        BUILDER.pop();

        SPEC = BUILDER.build();
    }

    public static void setup(ModContainer modContainer)
    {
        Path configPath = FMLPaths.CONFIGDIR.get();
        Path configFolder = Paths.get(configPath.toAbsolutePath().toString(), BlueDroplets.ID);

        try
        {
            Files.createDirectory(configFolder);
        }
        catch (Exception ignored) {}

        modContainer.registerConfig(ModConfig.Type.COMMON, SPEC, BlueDroplets.ID + "/container.toml");
    }
}

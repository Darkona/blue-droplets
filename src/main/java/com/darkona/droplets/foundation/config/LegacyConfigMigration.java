package com.darkona.droplets.foundation.config;

import com.darkona.droplets.BlueDroplets;
import com.darkona.droplets.content.registry.LegacyIds;
import com.mojang.logging.LogUtils;
import net.neoforged.fml.loading.FMLPaths;
import org.slf4j.Logger;

import java.io.IOException;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * Copies {@code config/thirst/*.toml} from Thirst Was Taken to {@code config/bluedroplets/} once,
 * when the new folder does not exist yet. Must run before any config is registered.
 */
public final class LegacyConfigMigration
{
    private static final Logger LOGGER = LogUtils.getLogger();

    private LegacyConfigMigration() {}

    public static void run()
    {
        Path configDir = FMLPaths.CONFIGDIR.get();
        Path target = configDir.resolve(BlueDroplets.ID);
        Path legacy = configDir.resolve(LegacyIds.LEGACY_NAMESPACE);
        if (Files.exists(target) || !Files.isDirectory(legacy))
            return;

        try
        {
            Files.createDirectories(target);
            try (DirectoryStream<Path> files = Files.newDirectoryStream(legacy, "*.toml"))
            {
                for (Path file : files)
                    Files.copy(file, target.resolve(file.getFileName()));
            }
            LOGGER.warn("Copied Thirst Was Taken config from {} to {}. The old folder is no longer read and can be deleted.", legacy, target);
        }
        catch (IOException e)
        {
            LOGGER.error("Could not copy Thirst Was Taken config from {} to {}; missing files will use defaults.", legacy, target, e);
        }
    }
}

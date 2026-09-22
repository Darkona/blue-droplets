package com.darkona.dropletsofthirst.compat.create.ponder;

import com.darkona.dropletsofthirst.DropletsOfThirst;
import net.createmod.ponder.api.registration.PonderPlugin;
import net.createmod.ponder.api.registration.PonderSceneRegistrationHelper;
import net.createmod.ponder.api.registration.PonderTagRegistrationHelper;
import net.minecraft.resources.ResourceLocation;
import org.jetbrains.annotations.NotNull;

public class ThirstPonderPlugin implements PonderPlugin {
    @Override
    public @NotNull String getModId() {
        return DropletsOfThirst.ID;
    }

    @Override
    public void registerScenes(@NotNull PonderSceneRegistrationHelper<ResourceLocation> helper) {
        ThirstPonders.registerScenes(helper);
    }

    @Override
    public void registerTags(@NotNull PonderTagRegistrationHelper<ResourceLocation> helper) {
        ThirstPonders.registerTags(helper);
    }
}

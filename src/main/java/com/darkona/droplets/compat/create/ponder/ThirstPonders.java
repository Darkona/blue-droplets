package com.darkona.droplets.compat.create.ponder;


import com.simibubi.create.infrastructure.ponder.AllCreatePonderTags;
import com.tterrag.registrate.util.entry.ItemProviderEntry;
import com.tterrag.registrate.util.entry.RegistryEntry;
import com.darkona.droplets.BlueDroplets;
import com.darkona.droplets.compat.create.CreateRegistry;
import com.darkona.droplets.compat.create.ponder.scene.SandFilterScene;
import net.createmod.ponder.api.registration.PonderSceneRegistrationHelper;
import net.createmod.ponder.api.registration.PonderTagRegistrationHelper;
import net.minecraft.resources.ResourceLocation;


public class ThirstPonders {
    public static final ResourceLocation PURIFICATION = BlueDroplets.asResource("purification");

    public static void registerTags(PonderTagRegistrationHelper<ResourceLocation> helper) {
        PonderTagRegistrationHelper<RegistryEntry<?,?>> HELPER = helper.withKeyFunction(RegistryEntry::getId);

        HELPER.registerTag(PURIFICATION)
                .addToIndex()
                .item(CreateRegistry.SAND_FILTER_BLOCK, true, false)
                .title("Purification")
                .description("Components which purifying water")
                .register();
    }

    public static void registerScenes(PonderSceneRegistrationHelper<ResourceLocation> helper) {
        PonderSceneRegistrationHelper<ItemProviderEntry<?,?>> HELPER = helper.withKeyFunction(RegistryEntry::getId);

        HELPER.addStoryBoard(
                CreateRegistry.SAND_FILTER_BLOCK,
                "sand_filter",
                SandFilterScene::filtering,
                AllCreatePonderTags.FLUIDS,
                PURIFICATION
        );
    }
}

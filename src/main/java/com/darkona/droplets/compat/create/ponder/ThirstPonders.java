package com.darkona.droplets.compat.create.ponder;


import com.darkona.droplets.BlueDroplets;
import com.darkona.droplets.compat.create.CreateRegistry;
import com.darkona.droplets.compat.create.ponder.scene.SandFilterScene;
import com.simibubi.create.foundation.ponder.PonderRegistrationHelper;
import com.simibubi.create.foundation.ponder.PonderRegistry;
import com.simibubi.create.foundation.ponder.PonderTag;
import com.simibubi.create.infrastructure.ponder.AllPonderTags;


/**
 * Create 0.5.1 keeps Ponder inside Create: tags and scenes go straight into its registry, on client setup, once the
 * Sand Filter item exists.
 */
public class ThirstPonders {
    public static PonderTag PURIFICATION;

    public static void register() {
        PURIFICATION = new PonderTag(BlueDroplets.asResource("purification"))
                .item(CreateRegistry.SAND_FILTER_BLOCK.get(), true, false)
                .defaultLang("Purification", "Components which purify water")
                .addToIndex();
        PonderRegistry.TAGS.forTag(PURIFICATION).add(CreateRegistry.SAND_FILTER_BLOCK);

        new PonderRegistrationHelper(BlueDroplets.ID).addStoryBoard(
                CreateRegistry.SAND_FILTER_BLOCK,
                "sand_filter",
                SandFilterScene::filtering,
                AllPonderTags.FLUIDS,
                PURIFICATION
        );
    }
}

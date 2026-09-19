package com.darkona.dropletsofthirst.foundation.common.capability;


import com.darkona.dropletsofthirst.DropletsOfThirst;
import com.darkona.dropletsofthirst.content.purity.PouredWater;
import com.darkona.dropletsofthirst.content.thirst.PlayerThirst;
import net.neoforged.neoforge.attachment.AttachmentType;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.registries.NeoForgeRegistries;

import java.util.function.Supplier;

/**
 * Internal. Other mods use {@link com.darkona.dropletsofthirst.api.DropletsAPI}.
 */
public class ModAttachment
{
    public static final DeferredRegister<AttachmentType<?>> ATTACHMENT_TYPES = DeferredRegister.create(NeoForgeRegistries.ATTACHMENT_TYPES, DropletsOfThirst.ID);
    public static final Supplier<AttachmentType<PlayerThirst>> PLAYER_THIRST = ATTACHMENT_TYPES.register(
            "player_thirst", () -> AttachmentType.serializable(PlayerThirst::new).copyOnDeath().build()
    );
    /** Poured water sources of a chunk; saved only when it has any, never synced. */
    public static final Supplier<AttachmentType<PouredWater>> POURED_WATER = ATTACHMENT_TYPES.register(
            "poured_water", () -> AttachmentType.builder(PouredWater::new).serialize(PouredWater.CODEC, water -> !water.isEmpty()).build()
    );
}

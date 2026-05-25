package com.darkona.droplets.foundation.common.capability;


import com.darkona.droplets.BlueDroplets;
import com.darkona.droplets.content.thirst.PlayerThirst;
import net.neoforged.neoforge.attachment.AttachmentType;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.registries.NeoForgeRegistries;

import java.util.function.Supplier;

/**
 * Internal. Other mods use {@link com.darkona.droplets.api.DropletsAPI}.
 */
public class ModAttachment
{
    public static final DeferredRegister<AttachmentType<?>> ATTACHMENT_TYPES = DeferredRegister.create(NeoForgeRegistries.ATTACHMENT_TYPES, BlueDroplets.ID);
    public static final Supplier<AttachmentType<PlayerThirst>> PLAYER_THIRST = ATTACHMENT_TYPES.register(
            "player_thirst", () -> AttachmentType.serializable(PlayerThirst::new).copyOnDeath().build()
    );

}

package com.darkona.droplets.foundation.common.capability;


import com.darkona.droplets.BlueDroplets;
import com.darkona.droplets.content.registry.LegacyIds;
import com.darkona.droplets.content.thirst.PlayerThirst;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.CapabilityManager;
import net.minecraftforge.common.capabilities.CapabilityToken;
import net.minecraftforge.common.capabilities.ICapabilitySerializable;
import net.minecraftforge.common.capabilities.RegisterCapabilitiesEvent;
import net.minecraftforge.common.util.LazyOptional;
import net.minecraftforge.event.AttachCapabilitiesEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.IEventBus;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Internal. Other mods use {@link com.darkona.droplets.api.DropletsAPI}.
 * <p>
 * A player's thirst is a capability ({@code blue_droplets:player_thirst}), kept through death and the End portal by
 * {@link #copyOnClone}. Thirst Was Taken saved it as {@code thirst:thirst} with the same keys: a player without
 * Blue Droplets data loads that one once, and saves under the new id from then on.
 */
public class ModAttachment
{
    public static final Capability<PlayerThirst> PLAYER_THIRST = CapabilityManager.get(new CapabilityToken<>() {});

    public static final ResourceLocation PLAYER_THIRST_ID = BlueDroplets.asResource("player_thirst");
    private static final ResourceLocation LEGACY_PLAYER_THIRST_ID = new ResourceLocation(LegacyIds.LEGACY_NAMESPACE, "thirst");

    private ModAttachment() {}

    public static void register(IEventBus modBus)
    {
        modBus.addListener(ModAttachment::registerCapabilities);
        MinecraftForge.EVENT_BUS.addGenericListener(Entity.class, ModAttachment::attach);
        MinecraftForge.EVENT_BUS.addListener(ModAttachment::copyOnClone);
    }

    /**
     * The thirst data of a player. Every player has it; a player whose capabilities were already invalidated (removed
     * from the world) gets a detached copy, so callers never check for null.
     */
    public static PlayerThirst thirst(Player player)
    {
        PlayerThirst thirst = player.getCapability(PLAYER_THIRST).orElse(null);
        return thirst != null ? thirst : new PlayerThirst();
    }

    private static void registerCapabilities(RegisterCapabilitiesEvent event)
    {
        event.register(PlayerThirst.class);
    }

    private static void attach(AttachCapabilitiesEvent<Entity> event)
    {
        if (!(event.getObject() instanceof Player))
            return;
        ThirstProvider provider = new ThirstProvider();
        event.addCapability(PLAYER_THIRST_ID, provider);
        event.addCapability(LEGACY_PLAYER_THIRST_ID, new LegacyThirstReader(provider));
    }

    /**
     * Death and the way back from the End create a new player: the data goes with it (the attachment's
     * {@code copyOnDeath} of later versions).
     */
    private static void copyOnClone(PlayerEvent.Clone event)
    {
        Player original = event.getOriginal();
        original.reviveCaps();
        PlayerThirst old = original.getCapability(PLAYER_THIRST).orElse(null);
        PlayerThirst now = event.getEntity().getCapability(PLAYER_THIRST).orElse(null);
        if (old != null && now != null)
            now.deserializeNBT(old.serializeNBT());
        original.invalidateCaps();
    }

    private static final class ThirstProvider implements ICapabilitySerializable<CompoundTag>
    {
        private final PlayerThirst thirst = new PlayerThirst();
        private final LazyOptional<PlayerThirst> optional = LazyOptional.of(() -> thirst);
        private boolean loaded;

        @Override
        public @NotNull <T> LazyOptional<T> getCapability(@NotNull Capability<T> cap, @Nullable Direction side)
        {
            return PLAYER_THIRST.orEmpty(cap, optional);
        }

        @Override
        public CompoundTag serializeNBT()
        {
            return thirst.serializeNBT();
        }

        @Override
        public void deserializeNBT(CompoundTag nbt)
        {
            thirst.deserializeNBT(nbt);
            loaded = true;
        }
    }

    /**
     * Reads Thirst Was Taken's {@code thirst:thirst} into the player's data when it has none of its own (it is
     * deserialized after {@link ThirstProvider}). Saves an empty compound, which later loads skip.
     */
    private static final class LegacyThirstReader implements ICapabilitySerializable<CompoundTag>
    {
        private final ThirstProvider target;

        private LegacyThirstReader(ThirstProvider target)
        {
            this.target = target;
        }

        @Override
        public @NotNull <T> LazyOptional<T> getCapability(@NotNull Capability<T> cap, @Nullable Direction side)
        {
            return LazyOptional.empty();
        }

        @Override
        public CompoundTag serializeNBT()
        {
            return new CompoundTag();
        }

        @Override
        public void deserializeNBT(CompoundTag nbt)
        {
            if (!target.loaded && !nbt.isEmpty())
                target.deserializeNBT(nbt);
        }
    }
}

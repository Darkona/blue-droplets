package com.darkona.droplets.gametest;

import com.darkona.droplets.content.thirst.PlayerThirst;
import com.darkona.droplets.foundation.common.capability.ModAttachment;
import com.mojang.authlib.GameProfile;
import net.minecraft.gametest.framework.GameTestAssertException;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.phys.Vec3;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraftforge.common.ForgeConfigSpec;
import net.minecraftforge.fluids.capability.CapabilityFluidHandler;
import net.minecraftforge.fluids.capability.IFluidHandler;
import org.jetbrains.annotations.Nullable;
import net.minecraftforge.common.util.FakePlayerFactory;

import java.lang.reflect.Field;
import java.util.Objects;
import java.util.UUID;

/**
 * Players for thirst tests. Vanilla's mock server player is creative, and creative players neither lose thirst nor
 * overhydrate, so these are Forge fake players in survival. The server does not tick them: tests call what they need.
 */
final class TestSupport
{
    private TestSupport() {}

    static ServerPlayer player(GameTestHelper helper)
    {
        ServerPlayer player = FakePlayerFactory.get(helper.getLevel(), new GameProfile(UUID.randomUUID(), "droplets-test"));
        player.moveTo(helper.absoluteVec(Vec3.ZERO));
        return player;
    }

    /**
     * {@code GameTestHelper#assertValueEqual} of later versions, which 1.18.2 does not have.
     */
    static <T> void assertValueEqual(GameTestHelper helper, T actual, T expected, String name)
    {
        if (!Objects.equals(actual, expected))
            throw new GameTestAssertException("Expected " + name + " to be " + expected + ", but was " + actual);
    }

    /**
     * {@code GameTestHelper#assertTrue} of later versions, which 1.18.2 does not have.
     */
    static void assertTrue(GameTestHelper helper, boolean condition, String message)
    {
        if (!condition)
            throw new GameTestAssertException(message);
    }

    /**
     * {@code GameTestHelper#assertFalse} of later versions, which 1.18.2 does not have.
     */
    static void assertFalse(GameTestHelper helper, boolean condition, String message)
    {
        if (condition)
            throw new GameTestAssertException(message);
    }

    /**
     * Changes a config value for the running test without writing the file. {@code ConfigValue#set} of Forge 1.18.2
     * saves the file, and its file watcher then reloads it on another thread while later tests run, so the value a
     * test set can come back while another test reads it. Tests restore the old value the same way.
     */
    static <T> void set(ForgeConfigSpec.ConfigValue<T> value, T newValue)
    {
        try
        {
            if (CACHED_VALUE == null)
            {
                Field field = ForgeConfigSpec.ConfigValue.class.getDeclaredField("cachedValue");
                field.setAccessible(true);
                CACHED_VALUE = field;
            }
            CACHED_VALUE.set(value, newValue);
        }
        catch (ReflectiveOperationException e)
        {
            throw new IllegalStateException("Cannot override config value " + value.getPath(), e);
        }
    }

    private static Field CACHED_VALUE;

    /**
     * The fluid handler of the block entity at {@code pos} on {@code side}, or null.
     */
    static @Nullable IFluidHandler fluidHandler(GameTestHelper helper, BlockPos pos, @Nullable Direction side)
    {
        BlockEntity entity = helper.getLevel().getBlockEntity(pos);
        return entity == null ? null : entity.getCapability(CapabilityFluidHandler.FLUID_HANDLER_CAPABILITY, side).orElse(null);
    }

    static PlayerThirst thirst(ServerPlayer player)
    {
        return ModAttachment.thirst(player);
    }
}

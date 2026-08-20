package com.darkona.droplets.foundation.network.message;

import com.darkona.droplets.foundation.config.GameplayConfig;
import com.darkona.droplets.content.purity.WaterPurity;
import com.darkona.droplets.content.thirst.PlayerThirst;
import com.darkona.droplets.content.thirst.VampireThirst;
import com.darkona.droplets.core.ThirstConstants;
import com.darkona.droplets.foundation.common.capability.ModAttachment;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.FluidTags;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

/**
 * "I want to drink by hand": carries no data. The server checks config, hands, cooldown and its own raycast.
 */
public record DrinkByHandMessage()
{
    public static final DrinkByHandMessage INSTANCE = new DrinkByHandMessage();

    public void encode(FriendlyByteBuf buffer) {}

    public static DrinkByHandMessage decode(FriendlyByteBuf buffer) {
        return INSTANCE;
    }

    /**
     * Main thread (the channel's {@code consumerMainThread}).
     */
    public static void serverHandle(final DrinkByHandMessage data, final Supplier<NetworkEvent.Context> context) {
        ServerPlayer player = context.get().getSender();
        if (player == null || !GameplayConfig.HAND_DRINKING.get() || !player.isShiftKeyDown())
            return;

        PlayerThirst thirst = ModAttachment.thirst(player);
        int tick = player.server.getTickCount();
        if (thirst.getThirst() >= ThirstConstants.MAX_THIRST || !thirst.canDrinkByHand(tick) || VampireThirst.isVampire(player))
            return;

        if (!player.getMainHandItem().isEmpty() || GameplayConfig.HAND_DRINKING_BOTH_HANDS.get() && !player.getOffhandItem().isEmpty())
            return;

        ServerLevel level = player.serverLevel();
        BlockHitResult hit = WaterPurity.pickFluid(player, ClipContext.Fluid.ANY);
        BlockPos pos = hit.getBlockPos();
        if (hit.getType() != HitResult.Type.BLOCK || !level.getFluidState(pos).is(FluidTags.WATER) || !level.mayInteract(player, pos))
            return;

        thirst.startHandDrinkCooldown(tick, GameplayConfig.HAND_DRINKING_COOLDOWN.get());
        level.playSound(null, player.getX(), player.getY(), player.getZ(), SoundEvents.GENERIC_DRINK, SoundSource.PLAYERS, 1.0F, 1.0F);
        if (GameplayConfig.HAND_DRINKING_EFFECTS.get())
        {
            player.swing(InteractionHand.MAIN_HAND, true);
            level.sendParticles(ParticleTypes.SPLASH, hit.getLocation().x, hit.getLocation().y, hit.getLocation().z, 6, 0.15, 0.05, 0.15, 0.1);
        }
        PlayerThirst.drink(player, ItemStack.EMPTY, GameplayConfig.HAND_DRINKING_THIRST.get(), GameplayConfig.HAND_DRINKING_QUENCHED.get(), WaterPurity.getBlockPurity(level, pos));
    }
}

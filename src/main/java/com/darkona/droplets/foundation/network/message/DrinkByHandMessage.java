package com.darkona.droplets.foundation.network.message;

import com.darkona.droplets.foundation.config.GameplayConfig;
import com.darkona.droplets.BlueDroplets;
import com.darkona.droplets.compat.supernatural.SupernaturalCompat;
import com.darkona.droplets.content.purity.WaterPurity;
import com.darkona.droplets.content.thirst.PlayerThirst;
import com.darkona.droplets.core.ThirstConstants;
import com.darkona.droplets.foundation.common.capability.ModAttachment;
import io.netty.buffer.ByteBuf;
import net.minecraft.core.BlockPos;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.FluidTags;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import org.jetbrains.annotations.NotNull;

/**
 * "I want to drink by hand": carries no data. The server checks config, hands, cooldown and its own raycast.
 */
public record DrinkByHandMessage() implements CustomPacketPayload
{
    public static final DrinkByHandMessage INSTANCE = new DrinkByHandMessage();
    public static final CustomPacketPayload.Type<DrinkByHandMessage> TYPE = new Type<>(BlueDroplets.asResource("drinkbyhand"));
    public static final StreamCodec<ByteBuf, DrinkByHandMessage> STREAM_CODEC = StreamCodec.unit(INSTANCE);

    public static void serverHandle(final DrinkByHandMessage data, final IPayloadContext context) {
        context.enqueueWork(() ->
        {
            if (!(context.player() instanceof ServerPlayer player) || !GameplayConfig.HAND_DRINKING.get() || !player.isShiftKeyDown())
                return;

            PlayerThirst thirst = player.getData(ModAttachment.PLAYER_THIRST);
            int tick = player.server.getTickCount();
            if (thirst.getThirst() >= ThirstConstants.MAX_THIRST || !thirst.canDrinkByHand(tick) || SupernaturalCompat.isVampire(player))
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
            if (WaterPurity.givePurityEffects(player, WaterPurity.getBlockPurity(level, pos)))
                thirst.drink(GameplayConfig.HAND_DRINKING_THIRST.get(), GameplayConfig.HAND_DRINKING_QUENCHED.get());
        });
    }

    @Override
    public @NotNull Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}

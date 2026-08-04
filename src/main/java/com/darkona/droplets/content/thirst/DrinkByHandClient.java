package com.darkona.droplets.content.thirst;

import com.darkona.droplets.content.purity.WaterPurity;
import com.darkona.droplets.foundation.common.capability.ModAttachment;
import com.darkona.droplets.foundation.network.message.DrinkByHandMessage;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.tags.FluidTags;
import net.minecraft.world.level.ClipContext;
import net.neoforged.neoforge.client.network.ClientPacketDistributor;

/**
 * Client only: loaded from the right-click handlers only on the client side.
 */
public class DrinkByHandClient
{
    /**
     * Only a hint to avoid useless packets; the server repeats every check and plays the sound.
     */
    public static void drinkByHand()
    {
        LocalPlayer player = Minecraft.getInstance().player;
        if (player == null || !player.isCrouching() || !player.getMainHandItem().isEmpty()
                || player.getData(ModAttachment.PLAYER_THIRST).needsBothHandsToDrink() && !player.getOffhandItem().isEmpty())
            return;

        if (player.level().getFluidState(WaterPurity.pickFluid(player, ClipContext.Fluid.ANY).getBlockPos()).is(FluidTags.WATER))
            ClientPacketDistributor.sendToServer(DrinkByHandMessage.INSTANCE);
    }
}

package com.darkona.dropletsofthirst.content.thirst;

import com.darkona.dropletsofthirst.content.purity.WaterPurity;
import com.darkona.dropletsofthirst.foundation.common.capability.ModAttachment;
import com.darkona.dropletsofthirst.foundation.network.message.DrinkByHandMessage;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.tags.FluidTags;
import net.minecraft.world.level.ClipContext;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import com.darkona.dropletsofthirst.foundation.network.ThirstModPacketHandler;

@OnlyIn(Dist.CLIENT)
public class DrinkByHandClient
{
    /**
     * Only a hint to avoid useless packets; the server repeats every check and plays the sound.
     */
    public static void drinkByHand()
    {
        LocalPlayer player = Minecraft.getInstance().player;
        if (player == null || !player.isCrouching() || !player.getMainHandItem().isEmpty()
                || ModAttachment.thirst(player).needsBothHandsToDrink() && !player.getOffhandItem().isEmpty())
            return;

        if (player.level().getFluidState(WaterPurity.pickFluid(player, ClipContext.Fluid.ANY).getBlockPos()).is(FluidTags.WATER))
            ThirstModPacketHandler.sendToServer(DrinkByHandMessage.INSTANCE);
    }
}

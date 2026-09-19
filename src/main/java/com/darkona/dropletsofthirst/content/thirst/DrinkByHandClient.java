package com.darkona.dropletsofthirst.content.thirst;

import com.darkona.dropletsofthirst.content.purity.WaterPurity;
import com.darkona.dropletsofthirst.foundation.common.capability.ModAttachment;
import com.darkona.dropletsofthirst.foundation.network.message.DrinkByHandMessage;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.tags.FluidTags;
import net.minecraft.world.level.ClipContext;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import net.neoforged.neoforge.network.PacketDistributor;

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
                || player.getData(ModAttachment.PLAYER_THIRST).needsBothHandsToDrink() && !player.getOffhandItem().isEmpty())
            return;

        if (player.level().getFluidState(WaterPurity.pickFluid(player, ClipContext.Fluid.ANY).getBlockPos()).is(FluidTags.WATER))
            PacketDistributor.sendToServer(DrinkByHandMessage.INSTANCE);
    }
}

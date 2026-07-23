package com.darkona.droplets.foundation.mixin.cold_sweat;

import com.darkona.droplets.content.purity.WaterPurity;
import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.sugar.Local;
import com.momosoftworks.coldsweat.common.item.WaterskinItem;
import net.minecraft.core.BlockPos;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.AbstractCauldronBlock;
import net.minecraftforge.fluids.FluidStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;

/**
 * A filled waterskin gets the purity of the water it was filled with. Cold Sweat builds every filled waterskin in
 * {@code getFilledItem(empty, level, pos)}: {@code pos} is the water source or the cauldron (already lowered, maybe
 * empty); for a tank the water is drained before, so its purity is handed over from {@code useOn}'s tank lambda.
 */
@Mixin(value = WaterskinItem.class, remap = false)
public abstract class MixinWaterskinItem
{
    /** Purity of the water just drained from a tank, -1 outside that fill. Server thread only. */
    @Unique
    private static int blue_droplets$tankPurity = -1;

    @WrapOperation(method = "lambda$useOn$0", at = @At(value = "INVOKE", target = "Lcom/momosoftworks/coldsweat/common/item/WaterskinItem;handleFillWaterskin(Lnet/minecraft/world/entity/player/Player;Lnet/minecraft/world/item/ItemStack;Lnet/minecraft/world/InteractionHand;Lnet/minecraft/core/BlockPos;)V"))
    private static void blue_droplets$fromTank(Player player, ItemStack empty, InteractionHand hand, BlockPos pos, Operation<Void> original, @Local(ordinal = 1) FluidStack drained)
    {
        if (player.level.isClientSide())
        {
            original.call(player, empty, hand, pos);
            return;
        }
        blue_droplets$tankPurity = WaterPurity.getPurity(drained);
        try
        {
            original.call(player, empty, hand, pos);
        }
        finally
        {
            blue_droplets$tankPurity = -1;
        }
    }

    @ModifyReturnValue(method = "getFilledItem", at = @At("RETURN"))
    private static ItemStack blue_droplets$purity(ItemStack filled, @Local(argsOnly = true) Level level, @Local(argsOnly = true) BlockPos pos)
    {
        if (level.isClientSide())
            return filled;
        int purity = blue_droplets$tankPurity >= 0 ? blue_droplets$tankPurity
                : level.getBlockState(pos).getBlock() instanceof AbstractCauldronBlock ? WaterPurity.cauldronPurity(level, pos)
                : WaterPurity.getBlockPurity(level, pos);
        return WaterPurity.addPurity(filled, purity);
    }
}

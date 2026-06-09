package com.darkona.droplets.foundation.mixin;

import com.darkona.droplets.content.purity.WaterPurity;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import net.minecraft.core.BlockPos;
import net.minecraft.core.cauldron.CauldronInteraction;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.ItemInteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.AbstractCauldronBlock;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

/**
 * Every item interaction with a water cauldron (vanilla bottles and buckets, the terracotta bowl, other mods' entries
 * in {@code CauldronInteraction.WATER}) runs inside {@link WaterPurity#takingFromCauldron}, so the water it takes out
 * gets the cauldron purity; see {@link MixinItemUtils}.
 */
@Mixin(AbstractCauldronBlock.class)
public abstract class MixinAbstractCauldronBlock
{
    @WrapOperation(method = "useItemOn", at = @At(value = "INVOKE", target = "Lnet/minecraft/core/cauldron/CauldronInteraction;interact(Lnet/minecraft/world/level/block/state/BlockState;Lnet/minecraft/world/level/Level;Lnet/minecraft/core/BlockPos;Lnet/minecraft/world/entity/player/Player;Lnet/minecraft/world/InteractionHand;Lnet/minecraft/world/item/ItemStack;)Lnet/minecraft/world/ItemInteractionResult;"))
    private ItemInteractionResult bluedroplets$cauldronPurity(CauldronInteraction interaction, BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand, ItemStack stack, Operation<ItemInteractionResult> original)
    {
        if (!WaterPurity.takingFromCauldron(state, level, pos))
            return original.call(interaction, state, level, pos, player, hand, stack);
        try
        {
            return original.call(interaction, state, level, pos, player, hand, stack);
        }
        finally
        {
            WaterPurity.doneTakingFromCauldron();
        }
    }
}

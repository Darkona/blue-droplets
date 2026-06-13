package com.darkona.droplets.foundation.common.item;

import com.darkona.droplets.content.purity.WaterPurity;
import com.darkona.droplets.content.registry.ItemInit;
import com.darkona.droplets.foundation.config.SyncedValues;
import net.minecraft.core.BlockPos;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.stats.Stats;
import net.minecraft.tags.FluidTags;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ItemUtils;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.gameevent.GameEvent;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import org.jetbrains.annotations.NotNull;

/**
 * Empty terracotta bowl: fills from water in the world like a glass bottle, with the purity of that water (decided on
 * the server). Filling from a water cauldron is a cauldron interaction.
 */
public class TerracottaBowlItem extends Item
{
    public TerracottaBowlItem(Properties properties)
    {
        super(properties);
    }

    @Override
    public @NotNull InteractionResultHolder<ItemStack> use(@NotNull Level level, @NotNull Player player, @NotNull InteractionHand hand)
    {
        ItemStack bowl = player.getItemInHand(hand);
        BlockHitResult hit = getPlayerPOVHitResult(level, player, SyncedValues.canFillFromFlowingWater() ? ClipContext.Fluid.ANY : ClipContext.Fluid.SOURCE_ONLY);
        if (hit.getType() != HitResult.Type.BLOCK)
            return InteractionResultHolder.pass(bowl);
        BlockPos pos = hit.getBlockPos();
        if (!level.mayInteract(player, pos) || !level.getFluidState(pos).is(FluidTags.WATER))
            return InteractionResultHolder.pass(bowl);

        level.playSound(player, player.getX(), player.getY(), player.getZ(), SoundEvents.BUCKET_FILL, SoundSource.NEUTRAL, 1.0F, 1.0F);
        level.gameEvent(player, GameEvent.FLUID_PICKUP, pos);
        player.awardStat(Stats.ITEM_USED.get(this));
        ItemStack water = new ItemStack(ItemInit.TERRACOTTA_WATER_BOWL.get());
        if (!level.isClientSide() && WaterPurity.enabled())
            WaterPurity.addPurity(water, WaterPurity.takenWaterPurity(level, pos));
        return InteractionResultHolder.sidedSuccess(ItemUtils.createFilledResult(bowl, player, water), level.isClientSide());
    }
}

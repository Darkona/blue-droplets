package com.darkona.dropletsofthirst.foundation.common.item;

import net.minecraft.advancements.CriteriaTriggers;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.stats.Stats;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ItemUtils;
import net.minecraft.world.item.UseAnim;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.gameevent.GameEvent;
import org.jetbrains.annotations.NotNull;

public class DrinkableItem extends Item
{
    private Item container;

    public DrinkableItem()
    {
        super(new Properties().stacksTo(64));
    }

    public DrinkableItem(Properties p_41383_)
    {
        super(p_41383_);
    }

    public DrinkableItem setContainer(Item item)
    {
        this.container = item;
        return this;
    }

    /**
     * Uses up one drink and hands back its empty container, like vanilla bottles: into the inventory, or dropped when
     * it is full.
     */
    public @NotNull ItemStack finishUsingItem(@NotNull ItemStack item, @NotNull Level level, @NotNull LivingEntity entity)
    {
        level.gameEvent(entity, GameEvent.ITEM_INTERACT_FINISH, entity.getEyePosition());
        if (!(entity instanceof Player player))
        {
            item.consume(1, entity);
            return item.isEmpty() ? new ItemStack(container) : item;
        }
        if (player instanceof ServerPlayer serverPlayer)
            CriteriaTriggers.CONSUME_ITEM.trigger(serverPlayer, item);
        player.awardStat(Stats.ITEM_USED.get(this));
        return ItemUtils.createFilledResult(item, player, new ItemStack(container));
    }

    @Override
    public int getUseDuration(@NotNull ItemStack stack, @NotNull LivingEntity user) {
        return 32;
    }

    public @NotNull UseAnim getUseAnimation(@NotNull ItemStack p_42997_) {
        return UseAnim.DRINK;
    }

    public @NotNull InteractionResultHolder<ItemStack> use(@NotNull Level p_42993_, @NotNull Player p_42994_, @NotNull InteractionHand p_42995_)
    {
        return ItemUtils.startUsingInstantly(p_42993_, p_42994_, p_42995_);
    }
}

package com.darkona.droplets.content.thirst;

import com.darkona.droplets.api.ThirstHelper;
import com.darkona.droplets.content.purity.WaterPurity;
import com.darkona.droplets.core.ThirstConstants;
import com.darkona.droplets.foundation.common.capability.ModAttachment;
import com.darkona.droplets.foundation.config.CommonConfig;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.TagsUpdatedEvent;
import net.neoforged.neoforge.event.entity.living.LivingDeathEvent;
import net.neoforged.neoforge.event.entity.living.LivingEntityUseItemEvent;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;

@EventBusSubscriber
public class PlayerThirstManager {

    @SubscribeEvent
    public static void drinkByHand(PlayerInteractEvent.RightClickBlock event) {
        if (CommonConfig.CAN_DRINK_BY_HAND.get() && event.getHand() == InteractionHand.MAIN_HAND && event.getEntity().level().isClientSide)
            DrinkByHandClient.drinkByHand();
    }

    @SubscribeEvent
    public static void drinkByHand(PlayerInteractEvent.RightClickEmpty event) {
        if (CommonConfig.CAN_DRINK_BY_HAND.get() && event.getHand() == InteractionHand.MAIN_HAND && event.getEntity().level().isClientSide)
            DrinkByHandClient.drinkByHand();
    }

    /**
     * Single place for purity effects: water containers, and drinks that are not food, hydrate here, once per use,
     * on the server. Food hydrates in {@code Player#eat} (MixinPlayer).
     */
    @SubscribeEvent
    public static void drink(LivingEntityUseItemEvent.Finish event) {
        if (!(event.getEntity() instanceof ServerPlayer player))
            return;
        ItemStack item = event.getItem();
        if (!ThirstHelper.itemRestoresThirst(item))
            return;
        if ((WaterPurity.isWaterFilledContainer(item) || item.getFoodProperties(player) == null) && WaterPurity.givePurityEffects(player, item))
            PlayerThirst.drink(item, player);
    }

    @SubscribeEvent
    public static void onPlayerTick(PlayerTickEvent.Pre event) {
        if (event.getEntity() instanceof ServerPlayer serverPlayer) {
            serverPlayer.getData(ModAttachment.PLAYER_THIRST).tick(serverPlayer);
        }
    }

    @SubscribeEvent
    public static void onPlayerDeath(LivingDeathEvent event){
        if(event.getEntity() instanceof ServerPlayer player){
            player.getData(ModAttachment.PLAYER_THIRST).setThirst(ThirstConstants.RESPAWN_THIRST);
            player.getData(ModAttachment.PLAYER_THIRST).setQuenched(ThirstConstants.RESPAWN_QUENCHED);
        }
    }

    @SubscribeEvent
    public static void rebuildDrinks(TagsUpdatedEvent event){
        if (event.shouldUpdateStaticData())
            ThirstHelper.rebuild();
    }
}



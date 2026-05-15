package com.darkona.droplets.content.thirst;

import com.darkona.droplets.api.ThirstHelper;
import com.darkona.droplets.content.purity.WaterPurity;
import com.darkona.droplets.core.ThirstConstants;
import com.darkona.droplets.foundation.common.capability.ModAttachment;
import com.darkona.droplets.foundation.config.CommonConfig;
import com.darkona.droplets.foundation.network.message.ThirstValuesSyncMessage;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.ItemStack;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.OnDatapackSyncEvent;
import net.neoforged.neoforge.event.TagsUpdatedEvent;
import net.neoforged.neoforge.event.entity.living.LivingDeathEvent;
import net.neoforged.neoforge.event.entity.living.LivingEquipmentChangeEvent;
import net.neoforged.neoforge.event.entity.living.MobEffectEvent;
import net.neoforged.neoforge.event.entity.living.LivingEntityUseItemEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;
import net.neoforged.neoforge.network.PacketDistributor;

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
        if (event.getEntity() instanceof ServerPlayer serverPlayer && serverPlayer.isAlive()) {
            serverPlayer.getData(ModAttachment.PLAYER_THIRST).tick(serverPlayer);
        }
    }

    /**
     * The only place that sends thirst data: at most one packet per player and tick, and only when a synced value changed.
     */
    @SubscribeEvent
    public static void syncThirst(PlayerTickEvent.Post event) {
        if (event.getEntity() instanceof ServerPlayer serverPlayer) {
            PlayerThirst thirst = serverPlayer.getData(ModAttachment.PLAYER_THIRST);
            if ((serverPlayer.tickCount + serverPlayer.getId()) % ThirstConstants.SAFETY_RESYNC_TICKS == 0)
                thirst.updateThirstData(serverPlayer);
            thirst.syncIfChanged(serverPlayer);
        }
    }

    /**
     * The client creates a new player (with default thirst data) on respawn and on every dimension change.
     */
    @SubscribeEvent
    public static void onChangedDimension(PlayerEvent.PlayerChangedDimensionEvent event) {
        PlayerThirst thirst = event.getEntity().getData(ModAttachment.PLAYER_THIRST);
        thirst.updateThirstData(event.getEntity());
        thirst.invalidateModifier();
    }

    @SubscribeEvent
    public static void onRespawn(PlayerEvent.PlayerRespawnEvent event) {
        event.getEntity().getData(ModAttachment.PLAYER_THIRST).updateThirstData(event.getEntity());
    }

    @SubscribeEvent
    public static void onEquipmentChange(LivingEquipmentChangeEvent event) {
        if (event.getEntity() instanceof ServerPlayer player && event.getSlot().getType() == EquipmentSlot.Type.HUMANOID_ARMOR)
            player.getData(ModAttachment.PLAYER_THIRST).invalidateModifier();
    }

    @SubscribeEvent
    public static void onEffectAdded(MobEffectEvent.Added event) {
        if (event.getEntity() instanceof ServerPlayer player)
            player.getData(ModAttachment.PLAYER_THIRST).invalidateModifier();
    }

    @SubscribeEvent
    public static void onEffectRemoved(MobEffectEvent.Remove event) {
        if (event.getEntity() instanceof ServerPlayer player)
            player.getData(ModAttachment.PLAYER_THIRST).invalidateModifier();
    }

    @SubscribeEvent
    public static void onEffectExpired(MobEffectEvent.Expired event) {
        if (event.getEntity() instanceof ServerPlayer player)
            player.getData(ModAttachment.PLAYER_THIRST).invalidateModifier();
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
        if (event.shouldUpdateStaticData() && !(event.getUpdateCause() == TagsUpdatedEvent.UpdateCause.CLIENT_PACKET_RECEIVED && ThirstHelper.hasServerTables()))
            ThirstHelper.rebuild();
    }

    /**
     * Sends the resolved tables to remote clients on join and after {@code /reload} (after the rebuild above).
     * Not over the in-memory connection of singleplayer: that client shares the tables with the server.
     */
    @SubscribeEvent
    public static void syncValues(OnDatapackSyncEvent event){
        ThirstValuesSyncMessage message = ThirstValuesSyncMessage.fromTables();
        event.getRelevantPlayers()
                .filter(player -> !player.connection.getConnection().isMemoryConnection())
                .forEach(player -> PacketDistributor.sendToPlayer(player, message));
    }
}



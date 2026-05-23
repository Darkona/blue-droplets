package com.darkona.droplets.content.thirst;

import com.darkona.droplets.api.ThirstHelper;
import com.darkona.droplets.content.purity.WaterPurity;
import com.darkona.droplets.core.ThirstConstants;
import com.darkona.droplets.foundation.common.capability.ModAttachment;
import com.darkona.droplets.foundation.config.GameplayConfig;
import com.darkona.droplets.foundation.network.message.ThirstValuesSyncMessage;
import com.darkona.droplets.BlueDroplets;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.RecipeManager;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.AddReloadListenerEvent;
import net.neoforged.neoforge.event.OnDatapackSyncEvent;
import net.neoforged.neoforge.event.TagsUpdatedEvent;
import net.neoforged.neoforge.common.ModConfigSpec;
import net.neoforged.neoforge.event.entity.living.LivingDamageEvent;
import net.neoforged.neoforge.event.entity.living.LivingDeathEvent;
import net.neoforged.neoforge.event.entity.living.LivingEvent;
import net.neoforged.neoforge.event.entity.player.AttackEntityEvent;
import net.neoforged.neoforge.event.level.BlockEvent;
import net.neoforged.neoforge.event.entity.living.LivingEquipmentChangeEvent;
import net.neoforged.neoforge.event.entity.living.MobEffectEvent;
import net.neoforged.neoforge.event.entity.living.LivingEntityUseItemEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.entity.player.ItemTooltipEvent;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;
import net.neoforged.fml.config.ModConfig;
import net.neoforged.fml.event.config.ModConfigEvent;
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.server.ServerLifecycleHooks;

import java.util.List;
import java.util.stream.Stream;

@EventBusSubscriber
public class PlayerThirstManager {
    private static volatile RecipeManager reloadingRecipes;

    @SubscribeEvent
    public static void drinkByHand(PlayerInteractEvent.RightClickBlock event) {
        if (event.getHand() == InteractionHand.MAIN_HAND && event.getEntity().level().isClientSide && event.getEntity().getData(ModAttachment.PLAYER_THIRST).handDrinkingAllowed())
            DrinkByHandClient.drinkByHand();
    }

    @SubscribeEvent
    public static void drinkByHand(PlayerInteractEvent.RightClickEmpty event) {
        if (event.getHand() == InteractionHand.MAIN_HAND && event.getEntity().level().isClientSide && event.getEntity().getData(ModAttachment.PLAYER_THIRST).handDrinkingAllowed())
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
    public static void onJump(LivingEvent.LivingJumpEvent event) {
        if (event.getEntity() instanceof ServerPlayer player)
            activity(player, player.isSprinting() ? GameplayConfig.SPRINT_JUMP : GameplayConfig.JUMP, 1.0F);
    }

    @SubscribeEvent
    public static void onAttack(AttackEntityEvent event) {
        if (event.getEntity() instanceof ServerPlayer player)
            activity(player, GameplayConfig.ATTACK, 1.0F);
    }

    @SubscribeEvent
    public static void onBlockBreak(BlockEvent.BreakEvent event) {
        if (event.getPlayer() instanceof ServerPlayer player)
            activity(player, GameplayConfig.BLOCK_BREAK, 1.0F);
    }

    @SubscribeEvent
    public static void onDamaged(LivingDamageEvent.Post event) {
        if (event.getEntity() instanceof ServerPlayer player && event.getNewDamage() > 0)
            activity(player, GameplayConfig.DAMAGE_MULTIPLIER, event.getSource().getFoodExhaustion());
    }

    /**
     * OWN mode only: the same activities vanilla charges to hunger.
     */
    private static void activity(ServerPlayer player, ModConfigSpec.DoubleValue value, float factor) {
        if (GameplayConfig.MODE.get() == GameplayConfig.Mode.OWN)
            player.getData(ModAttachment.PLAYER_THIRST).addActivity(player, value.get().floatValue() * factor);
    }

    @SubscribeEvent
    public static void onPlayerDeath(LivingDeathEvent event){
        if(event.getEntity() instanceof ServerPlayer player){
            PlayerThirst thirst = player.getData(ModAttachment.PLAYER_THIRST);
            int respawnThirst = GameplayConfig.RESPAWN_THIRST.get();
            int respawnQuenched = GameplayConfig.RESPAWN_QUENCHED.get();
            if (respawnThirst >= 0)
                thirst.setThirst(respawnThirst);
            if (respawnQuenched >= 0)
                thirst.setQuenched(respawnQuenched);
        }
    }

    /**
     * The recipes of the data reload in progress: its {@code TagsUpdatedEvent} comes before the server exists on world load.
     */
    @SubscribeEvent
    public static void captureRecipes(AddReloadListenerEvent event){
        reloadingRecipes = event.getServerResources().getRecipeManager();
    }

    /**
     * Lowest priority: NeoForge applies the reloaded data maps in its own {@code TagsUpdatedEvent} listener.
     */
    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void rebuildDrinks(TagsUpdatedEvent event){
        if (event.getUpdateCause() == TagsUpdatedEvent.UpdateCause.SERVER_DATA_LOAD) {
            RecipeManager recipes = reloadingRecipes;
            reloadingRecipes = null;
            ThirstHelper.rebuild(recipes, event.getRegistryAccess());
        }
        else if (event.shouldUpdateStaticData() && !ThirstHelper.hasServerTables())
            ThirstHelper.rebuild(null, null);
    }

    @SubscribeEvent
    public static void estimatedTooltip(ItemTooltipEvent event){
        ItemStack stack = event.getItemStack();
        if (ThirstHelper.isEstimated(stack))
            event.getToolTip().add(Component.translatable("bluedroplets.tooltip.estimated", ThirstHelper.getThirst(stack), ThirstHelper.getQuenched(stack)).withStyle(ChatFormatting.GRAY));
    }

    /**
     * Sends the resolved tables to remote clients on join and after {@code /reload} (after the rebuild above).
     * Not over the in-memory connection of singleplayer: that client shares the tables with the server.
     */
    @SubscribeEvent
    public static void syncValues(OnDatapackSyncEvent event){
        sendValues(event.getRelevantPlayers());
    }

    private static void sendValues(Stream<ServerPlayer> players){
        ThirstValuesSyncMessage message = ThirstValuesSyncMessage.fromTables();
        players.filter(player -> !player.connection.getConnection().isMemoryConnection())
                .forEach(player -> PacketDistributor.sendToPlayer(player, message));
    }

    /**
     * Mod bus. A common config file of this mod changed while a server runs: rebuild the tables, resend them and the
     * player rules, and recompute every player's exhaustion modifier. The event may come from the file watcher thread.
     */
    public static void onConfigReloaded(ModConfigEvent.Reloading event){
        MinecraftServer server = ServerLifecycleHooks.getCurrentServer();
        if (server == null || event.getConfig().getType() == ModConfig.Type.CLIENT || !BlueDroplets.ID.equals(event.getConfig().getModId()))
            return;
        server.execute(() -> {
            ThirstHelper.rebuild(server.getRecipeManager(), server.registryAccess());
            List<ServerPlayer> players = server.getPlayerList().getPlayers();
            for (ServerPlayer player : players) {
                PlayerThirst thirst = player.getData(ModAttachment.PLAYER_THIRST);
                thirst.invalidateModifier();
                thirst.updateThirstData(player);
            }
            sendValues(players.stream());
        });
    }
}



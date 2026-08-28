package com.darkona.droplets.content.thirst;

import org.jetbrains.annotations.Nullable;
import com.darkona.droplets.api.event.ThirstChangeEvent;
import com.darkona.droplets.content.data.DrinkValues;
import com.darkona.droplets.content.data.DropletsDataMaps;
import com.darkona.droplets.content.purity.WaterPurity;
import com.darkona.droplets.core.ThirstConstants;
import com.darkona.droplets.foundation.common.capability.ModAttachment;
import com.darkona.droplets.foundation.config.ClientConfig;
import com.darkona.droplets.foundation.config.GameplayConfig;
import com.darkona.droplets.foundation.network.message.ThirstValuesSyncMessage;
import com.darkona.droplets.BlueDroplets;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.TickTask;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.core.component.DataComponents;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.RecipeManager;
import net.minecraft.core.RegistryAccess;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.DefaultDataComponentsBoundEvent;
import net.neoforged.neoforge.event.OnDatapackSyncEvent;
import net.neoforged.neoforge.event.TagsUpdatedEvent;
import net.neoforged.neoforge.common.ModConfigSpec;
import net.neoforged.neoforge.event.entity.living.LivingDamageEvent;
import net.neoforged.neoforge.event.entity.living.LivingDeathEvent;
import net.neoforged.neoforge.event.entity.living.LivingEvent;
import net.neoforged.neoforge.event.entity.player.AttackEntityEvent;
import net.neoforged.neoforge.event.level.block.BreakBlockEvent;
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
    /** The data reload in progress, between its {@code TagsUpdatedEvent} and the binding of item components. */
    private static RecipeManager reloadingRecipes;
    private static RegistryAccess reloadingRegistries;
    /**
     * Block foods of {@code blue_droplets:hydrating_blocks} (a cake slice): vanilla feeds the player straight from the
     * block, so a bite is detected after the interaction as a higher food level.
     */
    @SubscribeEvent
    public static void eatBlock(PlayerInteractEvent.RightClickBlock event) {
        if (event.getHand() != InteractionHand.MAIN_HAND || !(event.getEntity() instanceof ServerPlayer player))
            return;
        DrinkValues values = event.getLevel().getBlockState(event.getPos()).getBlock().builtInRegistryHolder().getData(DropletsDataMaps.HYDRATING_BLOCKS);
        if (values == null)
            return;
        int food = player.getFoodData().getFoodLevel();
        MinecraftServer server = player.level().getServer();
        server.schedule(new TickTask(server.getTickCount(), () -> {
            if (player.getFoodData().getFoodLevel() > food)
                PlayerThirst.eat(player, ItemStack.EMPTY, values.thirst(), values.quenched());
        }));
    }

    @SubscribeEvent
    public static void drinkByHand(PlayerInteractEvent.RightClickBlock event) {
        drinkByHandIfAllowed(event);
    }

    @SubscribeEvent
    public static void drinkByHand(PlayerInteractEvent.RightClickEmpty event) {
        drinkByHandIfAllowed(event);
    }

    private static void drinkByHandIfAllowed(PlayerInteractEvent event) {
        if (event.getHand() == InteractionHand.MAIN_HAND && event.getEntity().level().isClientSide() && event.getEntity().getData(ModAttachment.PLAYER_THIRST).handDrinkingAllowed())
            DrinkByHandClient.drinkByHand();
    }

    /**
     * Single place for purity effects: water containers, and drinks that are not food, hydrate here, once per use,
     * on the server. Food hydrates in {@code FoodProperties#onConsume} (MixinFoodProperties).
     */
    @SubscribeEvent
    public static void drink(LivingEntityUseItemEvent.Finish event) {
        if (!(event.getEntity() instanceof ServerPlayer player))
            return;
        ItemStack item = event.getItem();
        if (WaterPurity.isWaterFilledContainer(item) || !item.has(DataComponents.FOOD))
            PlayerThirst.consume(item, player);
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
        effectsChanged(event);
    }

    @SubscribeEvent
    public static void onEffectRemoved(MobEffectEvent.Remove event) {
        effectsChanged(event);
    }

    @SubscribeEvent
    public static void onEffectExpired(MobEffectEvent.Expired event) {
        effectsChanged(event);
    }

    private static void effectsChanged(MobEffectEvent event) {
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
    public static void onBlockBreak(BreakBlockEvent event) {
        if (event.getPlayer() instanceof ServerPlayer player)
            activity(player, GameplayConfig.BLOCK_BREAK, 1.0F);
    }

    @SubscribeEvent
    public static void onDamaged(LivingDamageEvent.Post event) {
        if (event.getEntity() instanceof ServerPlayer player && event.getHealthDamage() > 0)
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
            thirst.change(player, respawnThirst >= 0 ? respawnThirst : thirst.getThirst(), respawnQuenched >= 0 ? respawnQuenched : thirst.getQuenched(), ThirstChangeEvent.Cause.DEATH);
        }
    }

    /**
     * Lowest priority: NeoForge applies the reloaded data maps in its own {@code TagsUpdatedEvent} listener. On a
     * server data load, items have no components yet at this point (Minecraft 26.1 binds them right after the tags), so
     * the rebuild waits for {@link #rebuildDrinksWithComponents}, posted next in the same call.
     */
    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void rebuildDrinks(TagsUpdatedEvent event){
        if (event instanceof TagsUpdatedEvent.ServerDataLoad load) {
            reloadingRecipes = load.getServerResources().getRecipeManager();
            reloadingRegistries = event.getRegistries();
        }
        else if (event.shouldUpdateStaticData() && !ThirstHelper.hasServerTables())
            ThirstHelper.rebuild(null, null);
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void rebuildDrinksWithComponents(DefaultDataComponentsBoundEvent event){
        if (event.getUpdateCause() != DefaultDataComponentsBoundEvent.UpdateCause.SERVER_DATA_LOAD || reloadingRecipes == null)
            return;
        ThirstHelper.rebuild(reloadingRecipes, reloadingRegistries);
        reloadingRecipes = null;
        reloadingRegistries = null;
    }

    /**
     * Client only (registered in {@code BlueDroplets}): it reads the client config, which a dedicated server does not
     * load, and some mods build tooltips there.
     */
    public static void estimatedTooltip(ItemTooltipEvent event){
        ItemStack stack = event.getItemStack();
        if (!ClientConfig.SHOW_TOOLTIP_ICONS.get() && ThirstHelper.isEstimated(stack))
            event.getToolTip().add(estimatedLine(ThirstHelper.getThirst(stack), ThirstHelper.getQuenched(stack)));
    }

    private record EstimatedLine(int thirst, int quenched, Component line) {}

    private static volatile @Nullable EstimatedLine lastEstimated;

    /**
     * The tooltip line of an item with estimated values. The tooltip is rebuilt every frame, so the last line is kept
     * while the values stay the same; it is translated when drawn, so a language change needs no new line.
     */
    public static Component estimatedLine(int thirst, int quenched){
        EstimatedLine last = lastEstimated;
        if (last == null || last.thirst() != thirst || last.quenched() != quenched)
            lastEstimated = last = new EstimatedLine(thirst, quenched, Component.translatable("blue_droplets.tooltip.estimated", thirst, quenched).withStyle(ChatFormatting.GRAY));
        return last.line();
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



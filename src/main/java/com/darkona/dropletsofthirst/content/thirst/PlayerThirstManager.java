package com.darkona.dropletsofthirst.content.thirst;

import com.darkona.dropletsofthirst.api.event.ThirstChangeEvent;
import com.darkona.dropletsofthirst.content.data.DrinkValues;
import com.darkona.dropletsofthirst.content.data.DropletsDataMaps;
import com.darkona.dropletsofthirst.content.purity.WaterPurity;
import com.darkona.dropletsofthirst.core.ThirstConstants;
import com.darkona.dropletsofthirst.foundation.common.capability.ModAttachment;
import com.darkona.dropletsofthirst.foundation.config.ClientConfig;
import com.darkona.dropletsofthirst.foundation.config.GameplayConfig;
import com.darkona.dropletsofthirst.foundation.network.message.ThirstValuesSyncMessage;
import com.darkona.dropletsofthirst.DropletsOfThirst;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import org.jetbrains.annotations.Nullable;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.TickTask;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.RecipeManager;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod.EventBusSubscriber;
import net.minecraftforge.event.AddReloadListenerEvent;
import net.minecraftforge.event.OnDatapackSyncEvent;
import net.minecraftforge.event.TagsUpdatedEvent;
import net.minecraftforge.common.ForgeConfigSpec;
import net.minecraftforge.event.entity.living.LivingDamageEvent;
import net.minecraftforge.event.entity.living.LivingEvent;
import net.minecraftforge.event.level.BlockEvent;
import net.minecraftforge.event.entity.living.LivingEquipmentChangeEvent;
import net.minecraftforge.event.entity.living.MobEffectEvent;
import net.minecraftforge.event.entity.living.LivingEntityUseItemEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.event.entity.player.ItemTooltipEvent;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.fml.LogicalSide;
import net.minecraftforge.fml.config.ModConfig;
import net.minecraftforge.fml.event.config.ModConfigEvent;
import com.darkona.dropletsofthirst.foundation.network.ThirstModPacketHandler;
import net.minecraftforge.server.ServerLifecycleHooks;

import java.util.List;
import java.util.stream.Stream;

@EventBusSubscriber
public class PlayerThirstManager {
    private static volatile RecipeManager reloadingRecipes;

    /**
     * Block foods of {@code droplets_of_thirst:hydrating_blocks} (a cake slice): vanilla feeds the player straight from the
     * block, so a bite is detected after the interaction as a higher food level.
     */
    @SubscribeEvent
    public static void eatBlock(PlayerInteractEvent.RightClickBlock event) {
        if (event.getHand() != InteractionHand.MAIN_HAND || !(event.getEntity() instanceof ServerPlayer player))
            return;
        DrinkValues values = DropletsDataMaps.HYDRATING_BLOCKS.get(event.getLevel().getBlockState(event.getPos()).getBlock().builtInRegistryHolder());
        if (values == null)
            return;
        int food = player.getFoodData().getFoodLevel();
        MinecraftServer server = player.serverLevel().getServer();
        server.tell(new TickTask(server.getTickCount(), () -> {
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
        if (event.getHand() == InteractionHand.MAIN_HAND && event.getEntity().level().isClientSide && ModAttachment.thirst(event.getEntity()).handDrinkingAllowed())
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
        if (WaterPurity.isWaterFilledContainer(item) || item.getFoodProperties(player) == null)
            PlayerThirst.consume(item, player);
    }

    /**
     * Thirst ticks at the start of the player's tick; the only place that sends thirst data is its end: at most one
     * packet per player and tick, and only when a synced value changed.
     */
    @SubscribeEvent
    public static void onPlayerTick(TickEvent.PlayerTickEvent event) {
        if (event.side != LogicalSide.SERVER || !(event.player instanceof ServerPlayer serverPlayer))
            return;
        PlayerThirst thirst = ModAttachment.thirst(serverPlayer);
        if (event.phase == TickEvent.Phase.START) {
            if (serverPlayer.isAlive())
                thirst.tick(serverPlayer);
            return;
        }
        if ((serverPlayer.tickCount + serverPlayer.getId()) % ThirstConstants.SAFETY_RESYNC_TICKS == 0)
            thirst.updateThirstData(serverPlayer);
        thirst.syncIfChanged(serverPlayer);
    }

    /**
     * The client creates a new player (with default thirst data) on respawn and on every dimension change.
     */
    @SubscribeEvent
    public static void onChangedDimension(PlayerEvent.PlayerChangedDimensionEvent event) {
        PlayerThirst thirst = ModAttachment.thirst(event.getEntity());
        thirst.updateThirstData(event.getEntity());
        thirst.invalidateModifier();
    }

    /**
     * The new player after a death gets {@code death.respawnThirst} and {@code respawnQuenched} (-1 keeps what was
     * copied from the dead player). Done here and not at the death, which another mod may cancel; coming back from
     * the End is not a death. Then a full sync: the client player is new too.
     */
    @SubscribeEvent
    public static void onRespawn(PlayerEvent.PlayerRespawnEvent event) {
        PlayerThirst thirst = ModAttachment.thirst(event.getEntity());
        if (!event.isEndConquered()) {
            int respawnThirst = GameplayConfig.RESPAWN_THIRST.get();
            int respawnQuenched = GameplayConfig.RESPAWN_QUENCHED.get();
            thirst.change(event.getEntity(), respawnThirst >= 0 ? respawnThirst : thirst.getThirst(), respawnQuenched >= 0 ? respawnQuenched : thirst.getQuenched(), ThirstChangeEvent.Cause.DEATH);
        }
        thirst.updateThirstData(event.getEntity());
    }

    @SubscribeEvent
    public static void onEquipmentChange(LivingEquipmentChangeEvent event) {
        if (event.getEntity() instanceof ServerPlayer player && event.getSlot().getType() == EquipmentSlot.Type.ARMOR)
            ModAttachment.thirst(player).invalidateModifier();
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
            ModAttachment.thirst(player).invalidateModifier();
    }

    @SubscribeEvent
    public static void onJump(LivingEvent.LivingJumpEvent event) {
        if (event.getEntity() instanceof ServerPlayer player)
            activity(player, player.isSprinting() ? GameplayConfig.SPRINT_JUMP : GameplayConfig.JUMP, 1.0F);
    }

    /**
     * An attack that landed, from {@code MixinPlayer}, where vanilla charges it hunger.
     */
    public static void attackLanded(ServerPlayer player) {
        activity(player, GameplayConfig.ATTACK, 1.0F);
    }

    /**
     * Lowest priority: a block break that another mod cancels (claims, spawn protection) costs nothing, as it costs no
     * hunger in vanilla.
     */
    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void onBlockBreak(BlockEvent.BreakEvent event) {
        if (event.getPlayer() instanceof ServerPlayer player)
            activity(player, GameplayConfig.BLOCK_BREAK, 1.0F);
    }

    /**
     * Lowest priority and not when canceled: the damage that goes through, like vanilla's exhaustion in {@code actuallyHurt}.
     */
    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void onDamaged(LivingDamageEvent event) {
        if (event.getEntity() instanceof ServerPlayer player && event.getAmount() > 0)
            activity(player, GameplayConfig.DAMAGE_MULTIPLIER, event.getSource().getFoodExhaustion());
    }

    /**
     * OWN mode only: the same activities vanilla charges to hunger.
     */
    private static void activity(ServerPlayer player, ForgeConfigSpec.DoubleValue value, float factor) {
        if (GameplayConfig.MODE.get() == GameplayConfig.Mode.OWN)
            ModAttachment.thirst(player).addActivity(player, value.get().floatValue() * factor);
    }

    /**
     * The recipes of the data reload in progress: its {@code TagsUpdatedEvent} comes before the server exists on world load.
     */
    @SubscribeEvent
    public static void captureRecipes(AddReloadListenerEvent event){
        reloadingRecipes = event.getServerResources().getRecipeManager();
    }

    /**
     * Lowest priority: the data maps are resolved in a {@code TagsUpdatedEvent} listener of higher priority.
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

    /**
     * Client only (registered in {@code DropletsOfThirst}): it reads the client config, which a dedicated server does not
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
            lastEstimated = last = new EstimatedLine(thirst, quenched, Component.translatable("droplets_of_thirst.tooltip.estimated", thirst, quenched).withStyle(ChatFormatting.GRAY));
        return last.line();
    }

    /**
     * Sends the resolved tables to remote clients on join and after {@code /reload} (after the rebuild above).
     * Not over the in-memory connection of singleplayer: that client shares the tables with the server.
     */
    @SubscribeEvent
    public static void syncValues(OnDatapackSyncEvent event){
        ServerPlayer player = event.getPlayer();
        sendValues(player != null ? Stream.of(player) : event.getPlayerList().getPlayers().stream());
    }

    private static void sendValues(Stream<ServerPlayer> players){
        ThirstValuesSyncMessage message = ThirstValuesSyncMessage.fromTables();
        players.filter(player -> !player.connection.connection.isMemoryConnection())
                .forEach(player -> ThirstModPacketHandler.sendToPlayer(player, message));
    }

    /**
     * Mod bus. A common config file of this mod changed while a server runs: rebuild the tables, resend them and the
     * player rules, and recompute every player's exhaustion modifier. The event may come from the file watcher thread.
     */
    public static void onConfigReloaded(ModConfigEvent.Reloading event){
        MinecraftServer server = ServerLifecycleHooks.getCurrentServer();
        if (server == null || event.getConfig().getType() == ModConfig.Type.CLIENT || !DropletsOfThirst.ID.equals(event.getConfig().getModId()))
            return;
        server.execute(() -> {
            ThirstHelper.rebuild(server.getRecipeManager(), server.registryAccess());
            List<ServerPlayer> players = server.getPlayerList().getPlayers();
            for (ServerPlayer player : players) {
                PlayerThirst thirst = ModAttachment.thirst(player);
                thirst.invalidateModifier();
                thirst.updateThirstData(player);
            }
            sendValues(players.stream());
        });
    }
}



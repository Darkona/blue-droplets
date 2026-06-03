package com.darkona.droplets.content.thirst;

import com.darkona.droplets.api.DropletsView;
import com.darkona.droplets.api.ThirstValues;
import com.darkona.droplets.api.event.DehydrationDamageEvent;
import com.darkona.droplets.api.event.DrinkEvent;
import com.darkona.droplets.api.event.ThirstChangeEvent;
import com.darkona.droplets.content.purity.WaterPurity;
import com.darkona.droplets.foundation.config.GameplayConfig;
import com.darkona.droplets.compat.vampirism.VampirismCompat;
import com.darkona.droplets.content.registry.AttributeInit;
import com.darkona.droplets.content.registry.EffectInit;
import com.darkona.droplets.foundation.common.capability.IThirst;
import com.darkona.droplets.foundation.common.capability.ModAttachment;
import com.darkona.droplets.foundation.common.damagesource.ModDamageSource;
import com.darkona.droplets.foundation.network.message.PlayerThirstSyncMessage;
import net.minecraft.core.Holder;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.Mth;
import net.minecraft.world.Difficulty;
import net.minecraft.tags.FluidTags;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.common.util.FakePlayer;
import net.neoforged.neoforge.common.util.INBTSerializable;
import net.neoforged.neoforge.network.PacketDistributor;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.annotations.UnknownNullability;

import static com.darkona.droplets.core.ThirstConstants.*;

public class PlayerThirst implements IThirst, DropletsView, INBTSerializable<CompoundTag>
{
    public static final int SYNC_ENABLED = 1;
    public static final int SYNC_SPRINT_BLOCKED = 2;
    public static final int SYNC_BOTH_HANDS = 4;
    public static final int SYNC_HAND_DRINKING = 8;
    /** Bits 8-15 of the synced rules: {@code sprint.minThirst}. */
    public static final int SYNC_SPRINT_MIN_SHIFT = 8;
    private static final double MAX_STEP = 10.0;

    private static @Nullable Holder<MobEffect> ghostlyShape;
    private static @Nullable Holder<MobEffect> nourishment;
    private static @Nullable Holder<MobEffect> stuffed;
    private static @Nullable Holder<MobEffect> saturated;

    int thirst = MAX_THIRST;
    int quenched = RESPAWN_QUENCHED;
    float exhaustion = 0;
    int damageTimer = 0;
    float prevTickExhaustion = 0.0F;
    boolean justHealed = false;
    boolean shouldTickThirst = true;
    boolean exhaustionRecalculate = false;
    float pendingActivity;
    float pendingRegen;
    double lastX;
    double lastY;
    double lastZ;
    boolean hasLastPosition;
    float exhaustionModifier = 1.0F;
    boolean modifierDirty = true;
    boolean forceSync = true;
    int sentThirst;
    int sentQuenched;
    int sentExhaustionStep;
    int sentFlags;
    int syncedRules = SYNC_SPRINT_BLOCKED | SYNC_BOTH_HANDS | SPRINT_MIN_THIRST << SYNC_SPRINT_MIN_SHIFT;
    int handDrinkReadyTick = 0;
    int fullHydrationTicks;

    public PlayerThirst() {}

    public static void resolveCompatEffects()
    {
        ghostlyShape = compatEffect("tombstone", "ghostly_shape");
        nourishment = compatEffect("farmersdelight", "nourishment");
        stuffed = compatEffect("bakery", "stuffed");
        saturated = compatEffect("brewery", "saturated");
    }

    private static @Nullable Holder<MobEffect> compatEffect(String namespace, String path)
    {
        return BuiltInRegistries.MOB_EFFECT.getHolder(ResourceLocation.fromNamespaceAndPath(namespace, path)).orElse(null);
    }

    private static boolean has(Player player, @Nullable Holder<MobEffect> effect)
    {
        return effect != null && player.hasEffect(effect);
    }

    public int getThirst()
    {
        return thirst;
    }

    public void setThirst(int value)
    {
        thirst = Mth.clamp(value, 0, MAX_THIRST);
        quenched = Math.min(quenched, thirst);
    }

    public int getQuenched()
    {
        return quenched;
    }

    public void setQuenched(int value)
    {
        quenched = Mth.clamp(value, 0, thirst);
    }

    public float getExhaustion()
    {
        return exhaustion;
    }

    public void setExhaustion(float value)
    {
        exhaustion = value;
    }

    @Override
    public int thirst()
    {
        return thirst;
    }

    @Override
    public int quenched()
    {
        return quenched;
    }

    @Override
    public float exhaustion()
    {
        return exhaustion;
    }

    @Override
    public boolean isEnabled()
    {
        return shouldTickThirst;
    }

    @Override
    public float lastModifier()
    {
        return exhaustionModifier;
    }

    /**
     * The way thirst changes on the server: clamps, keeps quenched at or below thirst and, only when something
     * changes, posts {@link ThirstChangeEvent.Pre} (which may cancel or change it) and {@link ThirstChangeEvent.Post}.
     *
     * @return whether anything changed
     */
    public boolean change(Player player, int newThirst, int newQuenched, ThirstChangeEvent.Cause cause)
    {
        newThirst = Mth.clamp(newThirst, 0, MAX_THIRST);
        newQuenched = Mth.clamp(newQuenched, 0, newThirst);
        if (newThirst == thirst && newQuenched == quenched)
            return false;
        ThirstChangeEvent.Pre pre = NeoForge.EVENT_BUS.post(new ThirstChangeEvent.Pre(player, cause, thirst, quenched, newThirst, newQuenched));
        if (pre.isCanceled())
            return false;
        newThirst = Mth.clamp(pre.getNewThirst(), 0, MAX_THIRST);
        newQuenched = Mth.clamp(pre.getNewQuenched(), 0, newThirst);
        if (newThirst == thirst && newQuenched == quenched)
            return false;
        int oldThirst = thirst;
        int oldQuenched = quenched;
        thirst = newThirst;
        quenched = newQuenched;
        NeoForge.EVENT_BUS.post(new ThirstChangeEvent.Post(player, cause, oldThirst, oldQuenched, thirst, quenched));
        return true;
    }

    @Override
    public void setShouldTickThirst(boolean value){shouldTickThirst = value;}
    @Override
    public boolean getShouldTickThirst(){return shouldTickThirst;}

    /**
     * Drinking or eating an item with thirst values; nothing for other items.
     */
    public static void drink(ItemStack item, Player player)
    {
        ThirstValues values = ThirstHelper.valuesOf(item);
        if (values != null)
        {
            boolean hydrates = ThirstHelper.playerRestoresThirst(item, player);
            drink(player, item, hydrates ? values.thirst() : 0, hydrates ? values.quenched() : 0, WaterPurity.drinkPurity(item));
        }
    }

    /**
     * Every drink (items, hand drinking, the API), server side only: {@link DrinkEvent.Pre}, purity effects, which may
     * prevent hydration, hydration, {@link DrinkEvent.Post}. {@code item} is empty for hand drinking and the API.
     *
     * @return whether thirst or quenched changed
     */
    public static boolean drink(Player player, ItemStack item, int thirst, int quenched, int purity)
    {
        if (player.level().isClientSide)
            return false;
        DrinkEvent.Pre pre = NeoForge.EVENT_BUS.post(new DrinkEvent.Pre(player, item, thirst, quenched, purity));
        if (pre.isCanceled())
            return false;
        boolean hydrated = WaterPurity.givePurityEffects(player, pre.getPurity())
                && player.getData(ModAttachment.PLAYER_THIRST).drink(player, pre.getThirst(), pre.getQuenched());
        NeoForge.EVENT_BUS.post(new DrinkEvent.Post(player, item, pre.getThirst(), pre.getQuenched(), pre.getPurity(), hydrated));
        return hydrated;
    }

    /**
     * Hydration of a drink: with {@code extraThirstToQuenched}, thirst above the maximum becomes quenched.
     */
    public boolean drink(Player player, int thirst, int quenched)
    {
        int extra = GameplayConfig.EXTRA_THIRST_TO_QUENCHED.get() ? Math.max(this.thirst + thirst - MAX_THIRST, 0) : 0;
        return change(player, this.thirst + thirst, this.quenched + quenched + extra, ThirstChangeEvent.Cause.DRINK);
    }

    public void drink(int thirst, int quenched)
    {
        int extra_quenched = Math.max(this.thirst + thirst - MAX_THIRST, 0);
        if(!GameplayConfig.EXTRA_THIRST_TO_QUENCHED.get())
            extra_quenched = 0;
        setThirst(this.thirst + thirst);
        setQuenched(this.quenched + quenched + extra_quenched);
    }

    /**
    * Method adapted from minecraft's Food Data class equivalent for hunger.
    */
    public void tick(Player player)
    {
        if(player.getAbilities().invulnerable || !shouldTickThirst)
            return;

        if(has(player, ghostlyShape))
            return;

        if(VampirismCompat.isVampire(player))
            return;

        if((player.tickCount + player.getId()) % MODIFIER_INTERVAL_TICKS == 0)
        {
            modifierDirty = true;
            if(GameplayConfig.FULL_HYDRATION_BONUS.get())
                fullHydrationBonus(player);
        }

        Difficulty difficulty = player.level().getDifficulty();
        boolean paused = has(player, nourishment) || has(player, stuffed) || has(player, saturated);

        if(GameplayConfig.DEPLETES_WHEN_NAUSEOUS.get() && player.hasEffect(MobEffects.CONFUSION))
            exhaustion += GameplayConfig.NAUSEA_PER_TICK.get().floatValue() * exhaustionModifier(player);

        float activity;
        if(GameplayConfig.MODE.get() == GameplayConfig.Mode.OWN)
        {
            activity = pendingActivity + movementExhaustion(player);
            pendingActivity = 0;
        }
        else
        {
            activity = mirroredFoodExhaustion(player);
            MobEffectInstance hunger = player.getEffect(MobEffects.HUNGER);
            if(hunger != null)
                activity -= HUNGER_EXHAUSTION_PER_LEVEL * (float)(hunger.getAmplifier() + 1);
        }
        activity += GameplayConfig.BASAL_PER_TICK.get().floatValue();

        float scale = paused ? 0.0F : 1.0F;
        if(player.isPassenger())
            scale *= GameplayConfig.RIDING_MULTIPLIER.get().floatValue();
        if(player.isSleeping())
            scale *= GameplayConfig.SLEEPING_MULTIPLIER.get().floatValue();
        addExhaustion(player, activity * scale);

        if(pendingRegen > 0)
        {
            if(GameplayConfig.REGEN_DEPLETES_THIRST.get())
                exhaustion += pendingRegen * scale * (GameplayConfig.REGEN_CLIMATE_DEPENDENT.get() ? exhaustionModifier(player) : 1.0F);
            pendingRegen = 0;
        }

        float perPoint = GameplayConfig.EXHAUSTION_PER_POINT.get().floatValue();
        if (exhaustion > perPoint)
        {
            exhaustion -= perPoint;
            if (quenched > 0)
                change(player, thirst, quenched - 1, ThirstChangeEvent.Cause.DEPLETION);
            else if (difficulty != Difficulty.PEACEFUL || GameplayConfig.DEPLETES_IN_PEACEFUL.get())
                change(player, thirst - 1, quenched, ThirstChangeEvent.Cause.DEPLETION);
        }

        if(difficulty == Difficulty.PEACEFUL && !GameplayConfig.DEPLETES_IN_PEACEFUL.get() && player.tickCount % GameplayConfig.PEACEFUL_REGEN_INTERVAL_TICKS.get() == 0)
            change(player, thirst + GameplayConfig.PEACEFUL_REGEN_AMOUNT.get(), quenched, ThirstChangeEvent.Cause.PEACEFUL);

        if(GameplayConfig.RAIN_DRINKING.get() && player.tickCount % GameplayConfig.RAIN_INTERVAL_TICKS.get() == 0
                && Mth.wrapDegrees(player.getXRot()) <= GameplayConfig.RAIN_MAX_PITCH.get() && player.level().isRainingAt(player.blockPosition().above()))
            change(player, thirst + GameplayConfig.RAIN_THIRST.get(), quenched + GameplayConfig.RAIN_QUENCHED.get(), ThirstChangeEvent.Cause.RAIN);

        if (thirst <= 0)
        {
            ++damageTimer;
            if (damageTimer >= GameplayConfig.DAMAGE_INTERVAL_TICKS.get())
            {
                float damage = GameplayConfig.DAMAGE_AMOUNT.get().floatValue();
                float health = player.getHealth();
                if (health > minHealth(difficulty) && (GameplayConfig.DAMAGE_CAN_KILL.get() || health > damage))
                {
                    DehydrationDamageEvent event = NeoForge.EVENT_BUS.post(new DehydrationDamageEvent(player, damage));
                    if (!event.isCanceled() && event.getAmount() > 0)
                        player.hurt(ModDamageSource.getDamageSource(player.level(),ModDamageSource.DIE_OF_THIRST_KEY), event.getAmount());
                }

                damageTimer = 0;
            }
        }
        else
            damageTimer = 0;
    }

    /**
     * {@code hydration.fullBonus}: Hydrated I after {@code fullBonusSeconds} at full thirst and enough quenched,
     * refreshed when it is about to run out. Checked every {@code MODIFIER_INTERVAL_TICKS}.
     */
    private void fullHydrationBonus(Player player)
    {
        if(thirst < MAX_THIRST || quenched < GameplayConfig.FULL_HYDRATION_MIN_QUENCHED.get())
        {
            fullHydrationTicks = 0;
            return;
        }
        if(fullHydrationTicks < GameplayConfig.FULL_HYDRATION_SECONDS.get() * 20)
        {
            fullHydrationTicks += MODIFIER_INTERVAL_TICKS;
            return;
        }
        MobEffectInstance current = player.getEffect(EffectInit.HYDRATED);
        if(current == null || current.endsWithin(MODIFIER_INTERVAL_TICKS))
            player.addEffect(new MobEffectInstance(EffectInit.HYDRATED, GameplayConfig.FULL_HYDRATION_DURATION_TICKS.get(), 0, true, true));
    }

    private static float minHealth(Difficulty difficulty)
    {
        return (switch (difficulty)
        {
            case PEACEFUL, EASY -> GameplayConfig.DAMAGE_MIN_HEALTH_EASY;
            case NORMAL -> GameplayConfig.DAMAGE_MIN_HEALTH_NORMAL;
            case HARD -> GameplayConfig.DAMAGE_MIN_HEALTH_HARD;
        }).get().floatValue();
    }

    /**
     * MIRROR_FOOD: what vanilla added to the hunger exhaustion since last tick.
     */
    private float mirroredFoodExhaustion(Player player)
    {
        float hungerExhaustion = player.getFoodData().getExhaustionLevel();
        float normalizedHungerExhaustion = hungerExhaustion < this.prevTickExhaustion ? (exhaustionRecalculate ? hungerExhaustion + 4.0F : hungerExhaustion) : hungerExhaustion;
        exhaustionRecalculate = false;
        float deltaExhaustion = normalizedHungerExhaustion - this.prevTickExhaustion;
        this.prevTickExhaustion = hungerExhaustion;
        return deltaExhaustion;
    }

    /**
     * OWN: vanilla's movement exhaustion (swimming or walking in water, sprinting on the ground) from the position
     * change since last tick; jumps over {@code MAX_STEP} blocks (teleports) and riding count nothing.
     */
    private float movementExhaustion(Player player)
    {
        double dx = player.getX() - lastX;
        double dy = player.getY() - lastY;
        double dz = player.getZ() - lastZ;
        boolean first = !hasLastPosition;
        lastX = player.getX();
        lastY = player.getY();
        lastZ = player.getZ();
        hasLastPosition = true;

        double horizontal = dx * dx + dz * dz;
        double total = horizontal + dy * dy;
        if(first || total > MAX_STEP * MAX_STEP || player.isPassenger())
            return 0.0F;
        if(player.isSwimming() || player.isEyeInFluid(FluidTags.WATER))
            return (float) (Math.sqrt(total) * GameplayConfig.SWIM_PER_METER.get());
        if(player.isInWater())
            return (float) (Math.sqrt(horizontal) * GameplayConfig.SWIM_PER_METER.get());
        if(player.onGround() && player.isSprinting())
            return (float) (Math.sqrt(horizontal) * GameplayConfig.SPRINT_PER_METER.get());
        return 0.0F;
    }

    /**
     * OWN: exhaustion of a jump, attack, broken block or damage, added on the next tick.
     */
    public void addActivity(Player player, float amount)
    {
        if(!player.getAbilities().invulnerable && shouldTickThirst)
            pendingActivity += amount;
    }

    /**
     * Health regenerated from food: in MIRROR_FOOD the next exhaustion delta is treated as regeneration; in OWN it
     * costs {@code healPerHealth} per point.
     */
    public void onFoodHeal(float amount)
    {
        if(GameplayConfig.MODE.get() == GameplayConfig.Mode.OWN)
            pendingRegen += amount * GameplayConfig.HEAL_PER_HEALTH.get().floatValue();
        else
            justHealed = true;
    }

    /**
     * Sends the thirst data with the next {@link #syncIfChanged}, even if nothing changed.
     */
    public void updateThirstData(Player player)
    {
        forceSync = true;
    }

    /**
     * Sends thirst, quenched, exhaustion (in steps of 1/{@code EXHAUSTION_SYNC_STEPS}) and the synced rules
     * when one of them differs from the last packet, or when a sync was forced. Called once per player tick.
     */
    public void syncIfChanged(ServerPlayer player)
    {
        int flags = (shouldTickThirst ? SYNC_ENABLED : 0)
                | (GameplayConfig.SPRINT_BLOCKED_WHEN_THIRSTY.get() ? SYNC_SPRINT_BLOCKED : 0)
                | (GameplayConfig.HAND_DRINKING_BOTH_HANDS.get() ? SYNC_BOTH_HANDS : 0)
                | (GameplayConfig.HAND_DRINKING.get() ? SYNC_HAND_DRINKING : 0)
                | GameplayConfig.SPRINT_MIN_THIRST.get() << SYNC_SPRINT_MIN_SHIFT;
        int exhaustionStep = (int) (exhaustion * EXHAUSTION_SYNC_STEPS);
        if(!forceSync && thirst == sentThirst && quenched == sentQuenched && exhaustionStep == sentExhaustionStep && flags == sentFlags)
            return;
        if(player instanceof FakePlayer || player.connection == null)
            return;

        forceSync = false;
        sentThirst = thirst;
        sentQuenched = quenched;
        sentExhaustionStep = exhaustionStep;
        sentFlags = flags;
        PacketDistributor.sendToPlayer(player, new PlayerThirstSyncMessage(thirst, quenched, exhaustion, flags));
    }

    /**
     * Server rules received with the last sync ({@code SYNC_*} bits); only meaningful on the client.
     */
    public void setSyncedRules(int flags)
    {
        syncedRules = flags;
        shouldTickThirst = (flags & SYNC_ENABLED) != 0;
    }

    public boolean isSprintBlocked()
    {
        return (syncedRules & SYNC_SPRINT_BLOCKED) != 0;
    }

    public int sprintMinThirst()
    {
        return syncedRules >>> SYNC_SPRINT_MIN_SHIFT & 0xFF;
    }

    public boolean needsBothHandsToDrink()
    {
        return (syncedRules & SYNC_BOTH_HANDS) != 0;
    }

    public boolean handDrinkingAllowed()
    {
        return (syncedRules & SYNC_HAND_DRINKING) != 0;
    }

    public boolean canDrinkByHand(int serverTick)
    {
        return serverTick >= handDrinkReadyTick;
    }

    public void startHandDrinkCooldown(int serverTick, int cooldownTicks)
    {
        handDrinkReadyTick = serverTick + cooldownTicks;
    }

    @Override
    public void setJustHealed()
    {
        justHealed = true;
    }

    @Override
    public void ExhaustionRecalculate(){exhaustionRecalculate = true;}

    @Override
    public void copy(IThirst cap)
    {
        setThirst(cap.getThirst());
        setQuenched(cap.getQuenched());
        exhaustion = cap.getExhaustion();
        shouldTickThirst = cap.getShouldTickThirst();
    }

    public void addExhaustion(Player player, float amount)
    {
        if(!GameplayConfig.REGEN_DEPLETES_THIRST.get() && justHealed)
            amount = 0;

        if(!GameplayConfig.REGEN_CLIMATE_DEPENDENT.get() && justHealed)
            exhaustion += amount;
        else
            exhaustion += amount * exhaustionModifier(player);

        if(justHealed)
            justHealed = false;
    }

    /**
     * Exhaustion from another mod, multiplied like the mod's own activities.
     */
    public void addScaledExhaustion(Player player, float amount)
    {
        if(!player.getAbilities().invulnerable && shouldTickThirst)
            exhaustion = Math.max(0.0F, exhaustion + amount * exhaustionModifier(player));
    }

    /**
     * {@link ExhaustionFactors} cached (recomputed every {@code MODIFIER_INTERVAL_TICKS}, staggered per player, and
     * after {@link #invalidateModifier()}) times the {@code bluedroplets:thirst_drain} attribute (cached by vanilla).
     */
    public float exhaustionModifier(Player player)
    {
        if(modifierDirty)
        {
            modifierDirty = false;
            exhaustionModifier = ExhaustionFactors.compute(player, null);
        }
        return exhaustionModifier * (float) player.getAttributeValue(AttributeInit.THIRST_DRAIN);
    }

    /**
     * Recomputes the exhaustion modifier on next use: armor or effects changed, dimension change, respawn.
     */
    public void invalidateModifier()
    {
        modifierDirty = true;
    }


    @Override
    public @UnknownNullability CompoundTag serializeNBT(HolderLookup.@NotNull Provider provider) {
        CompoundTag nbt = new CompoundTag();
        nbt.putInt("thirst", thirst);
        nbt.putInt("quenched", quenched);
        nbt.putFloat("exhaustion", exhaustion);
        nbt.putBoolean("enable",shouldTickThirst);
        return nbt;
    }

    @Override
    public void deserializeNBT(HolderLookup.@NotNull Provider provider, CompoundTag nbt) {
        setThirst(nbt.getInt("thirst"));
        setQuenched(nbt.getInt("quenched"));
        exhaustion = nbt.getFloat("exhaustion");
        shouldTickThirst = !nbt.contains("enable") || nbt.getBoolean("enable");
    }
}

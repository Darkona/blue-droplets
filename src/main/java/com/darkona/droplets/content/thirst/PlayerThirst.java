package com.darkona.droplets.content.thirst;

import com.darkona.droplets.foundation.config.GameplayConfig;
import com.darkona.droplets.api.ThirstHelper;
import com.darkona.droplets.compat.vampirism.VampirismCompat;
import com.darkona.droplets.content.registry.AttributeInit;
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
import net.neoforged.neoforge.common.util.FakePlayer;
import net.neoforged.neoforge.common.util.INBTSerializable;
import net.neoforged.neoforge.network.PacketDistributor;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.annotations.UnknownNullability;

import static com.darkona.droplets.core.ThirstConstants.*;

public class PlayerThirst implements IThirst, INBTSerializable<CompoundTag>
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
    public void setShouldTickThirst(boolean value){shouldTickThirst = value;}
    @Override
    public boolean getShouldTickThirst(){return shouldTickThirst;}

    /**
     * Attempts to give hydration to player if item restores thirst.
     * @param item
     * @param player
     */
    public static void drink(ItemStack item, Player player)
    {
        if(ThirstHelper.itemRestoresThirst(item) && ThirstHelper.playerRestoresThirst(item, player))
        {
            player.getData(ModAttachment.PLAYER_THIRST).drink(ThirstHelper.getThirst(item),ThirstHelper.getQuenched(item));
        }
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
            modifierDirty = true;

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
            {
                quenched--;
            }
            else if (difficulty != Difficulty.PEACEFUL || GameplayConfig.DEPLETES_IN_PEACEFUL.get())
            {
                thirst = Math.max(thirst - 1, 0);
            }
        }

        if(difficulty == Difficulty.PEACEFUL && !GameplayConfig.DEPLETES_IN_PEACEFUL.get() && player.tickCount % GameplayConfig.PEACEFUL_REGEN_INTERVAL_TICKS.get() == 0)
            setThirst(thirst + GameplayConfig.PEACEFUL_REGEN_AMOUNT.get());

        if(GameplayConfig.RAIN_DRINKING.get() && player.tickCount % GameplayConfig.RAIN_INTERVAL_TICKS.get() == 0
                && Mth.wrapDegrees(player.getXRot()) <= GameplayConfig.RAIN_MAX_PITCH.get() && player.level().isRainingAt(player.blockPosition().above()))
        {
            setThirst(thirst + GameplayConfig.RAIN_THIRST.get());
            setQuenched(quenched + GameplayConfig.RAIN_QUENCHED.get());
        }

        if (thirst <= 0)
        {
            ++damageTimer;
            if (damageTimer >= GameplayConfig.DAMAGE_INTERVAL_TICKS.get())
            {
                float damage = GameplayConfig.DAMAGE_AMOUNT.get().floatValue();
                float health = player.getHealth();
                if (health > minHealth(difficulty) && (GameplayConfig.DAMAGE_CAN_KILL.get() || health > damage))
                {
                    player.hurt(ModDamageSource.getDamageSource(player.level(),ModDamageSource.DIE_OF_THIRST_KEY), damage);
                }

                damageTimer = 0;
            }
        }
        else
            damageTimer = 0;
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
     * {@link ExhaustionModifier} cached (recomputed every {@code MODIFIER_INTERVAL_TICKS}, staggered per player, and
     * after {@link #invalidateModifier()}) times the {@code bluedroplets:thirst_drain} attribute (cached by vanilla).
     */
    public float exhaustionModifier(Player player)
    {
        if(modifierDirty)
        {
            modifierDirty = false;
            exhaustionModifier = ExhaustionModifier.compute(player, null);
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

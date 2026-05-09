package com.darkona.droplets.content.thirst;

import com.darkona.droplets.api.ThirstHelper;
import com.darkona.droplets.compat.vampirism.VampirismCompat;
import com.darkona.droplets.foundation.common.capability.IThirst;
import com.darkona.droplets.foundation.common.capability.ModAttachment;
import com.darkona.droplets.foundation.common.damagesource.ModDamageSource;
import com.darkona.droplets.foundation.config.CommonConfig;
import com.darkona.droplets.foundation.network.message.PlayerThirstSyncMessage;
import net.minecraft.core.Holder;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.Mth;
import net.minecraft.world.Difficulty;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.common.util.FakePlayer;
import net.neoforged.neoforge.common.util.INBTSerializable;
import net.neoforged.neoforge.network.PacketDistributor;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.annotations.UnknownNullability;

public class PlayerThirst implements IThirst, INBTSerializable<CompoundTag>
{
    private static @Nullable Holder<MobEffect> ghostlyShape;
    private static @Nullable Holder<MobEffect> nourishment;
    private static @Nullable Holder<MobEffect> stuffed;
    private static @Nullable Holder<MobEffect> saturated;

    int thirst = 20;
    int quenched = 5;
    float exhaustion = 0;
    int damageTimer = 0;
    int syncTimer = 0;
    float prevTickExhaustion = 0.0F;
    boolean justHealed = false;
    boolean shouldTickThirst = true;
    boolean exhaustionRecalculate = false;
    boolean init = true;

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
        thirst = Mth.clamp(value, 0, 20);
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
        int extra_quenched = Math.max(this.thirst + thirst - 20, 0);
        if(!CommonConfig.EXTRA_HYDRATION_CONVERT_TO_QUENCHED.get())
            extra_quenched = 0;
        setThirst(this.thirst + thirst);
        setQuenched(this.quenched + quenched + extra_quenched);
    }

    /**
    * Method adapted from minecraft's Food Data class equivalent for hunger.
    */
    public void tick(Player player)
    {
        Difficulty difficulty = player.level().getDifficulty();

        if(player.getAbilities().invulnerable)
            return;

        if(!shouldTickThirst) {
            if (init) {
                init = false;
                updateThirstData(player);
            }
            return;
        }

        if(has(player, ghostlyShape))
            return;

        if(VampirismCompat.isVampire(player))
            return;

        boolean paused = has(player, nourishment) || has(player, stuffed) || has(player, saturated);

        if(CommonConfig.DEPLETES_WHEN_NAUSED.get() && player.hasEffect(MobEffects.CONFUSION))
            addExhaustion(player, 0.06F);

        boolean isHunger = player.hasEffect(MobEffects.HUNGER);
        boolean isSitting = player.isPassenger();

        if(isHunger){
            exhaustion -= 0.005F * (float)(player.getEffect(MobEffects.HUNGER).getAmplifier() + 1) *
                    ThirstHelper.getExhaustionBiomeModifier(player) *
                    ThirstHelper.getExhaustionFireProtModifier(player)*
                    ThirstHelper.getExhaustionFireResistanceModifier(player);
        }

        if (!isSitting && !paused)
        {
            updateExhaustion(player);
        }

        if (exhaustion > 4)
        {
            exhaustion -= 4;
            if (quenched > 0)
            {
                quenched--;
            }
            else if (difficulty != Difficulty.PEACEFUL || CommonConfig.THIRST_DEPLETION_IN_PEACEFUL.get())
            {
                thirst = Math.max(thirst - 1, 0);
            }
        }

        ++syncTimer;
        if(syncTimer > 10 && !player.level().isClientSide())
        {
            if(difficulty == Difficulty.PEACEFUL && !CommonConfig.THIRST_DEPLETION_IN_PEACEFUL.get()){
                setThirst(thirst + 1);
            }

            final float angle = Mth.wrapDegrees(player.getXRot());
            if (angle <= -80  && player.level().isRainingAt(player.blockPosition().above()) && CommonConfig.CAN_DRINK_RAIN_WATETR.get())
            {
                setThirst(thirst + 1);
                setQuenched(quenched + 1);
            }

            updateThirstData(player);
            syncTimer = 0;
        }

        if (thirst <= 0)
        {
            ++damageTimer;
            if (damageTimer >= 40)
            {
                if (player.getHealth() > 10.0F || difficulty == Difficulty.HARD || player.getHealth() > 0 && difficulty == Difficulty.NORMAL)
                {
                    player.hurt(ModDamageSource.getDamageSource(player.level(),ModDamageSource.DIE_OF_THIRST_KEY), 1.0F);
                }

                damageTimer = 0;
            }
        }
        else
            damageTimer = 0;
    }

    void updateExhaustion(Player player)
    {
        float hungerExhaustion = player.getFoodData().getExhaustionLevel();
        float normalizedHungerExhaustion = hungerExhaustion < this.prevTickExhaustion ? (exhaustionRecalculate ? hungerExhaustion + 4.0F : hungerExhaustion) : hungerExhaustion;
        if(exhaustionRecalculate){
            exhaustionRecalculate = false;
        }
        float deltaExhaustion = normalizedHungerExhaustion - this.prevTickExhaustion;
        this.addExhaustion(player, deltaExhaustion);
        this.prevTickExhaustion = hungerExhaustion;
    }

    public void updateThirstData(Player player)
    {
        if(player instanceof ServerPlayer serverPlayer && !(player instanceof FakePlayer) && serverPlayer.connection != null)
            PacketDistributor.sendToPlayer(serverPlayer, new PlayerThirstSyncMessage(thirst, quenched, exhaustion,shouldTickThirst));
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
        if(!CommonConfig.HEALTH_REGEN_DEPLETES_HYDRATION.get() && justHealed)
            amount = 0;

        if(!CommonConfig.HEALTH_REGEN_DEHYDRATION_IS_BIOME_DEPENDENT.get() && justHealed)
            exhaustion += amount;
        else
            exhaustion += (amount *
                    ThirstHelper.getExhaustionBiomeModifier(player) *
                    ThirstHelper.getExhaustionFireProtModifier(player)*
                    ThirstHelper.getExhaustionFireResistanceModifier(player)
            );

        if(justHealed)
            justHealed = false;

        updateThirstData(player);
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

package com.darkona.dropletsofthirst.api.event;

import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.player.Player;
import net.neoforged.bus.api.ICancellableEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;

import java.util.List;

/**
 * The effects of drinking water of some purity were rolled ({@code purity.toml}) and are about to be applied. Posted
 * on {@code NeoForge.EVENT_BUS}, on the server, once per drink with a purity while purity is enabled, also when the
 * roll gave no effects. Cancel to apply none and hydrate normally.
 */
public class PurityEffectEvent extends PlayerEvent implements ICancellableEvent
{
    private final int purity;
    private final List<MobEffectInstance> effects;
    private boolean hydrates;

    public PurityEffectEvent(Player player, int purity, List<MobEffectInstance> effects, boolean hydrates)
    {
        super(player);
        this.purity = purity;
        this.effects = effects;
        this.hydrates = hydrates;
    }

    public int getPurity()
    {
        return purity;
    }

    /**
     * The effects that will be applied; add, remove or replace entries.
     */
    public List<MobEffectInstance> getEffects()
    {
        return effects;
    }

    /**
     * Whether the drink still hydrates: false when a rolled effect blocks hydration and {@code quenchWhenDebuffed} is off.
     */
    public boolean hydrates()
    {
        return hydrates;
    }

    public void setHydrates(boolean hydrates)
    {
        this.hydrates = hydrates;
    }
}

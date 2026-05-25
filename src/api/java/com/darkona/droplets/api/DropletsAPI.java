package com.darkona.droplets.api;

import com.darkona.droplets.api.spi.DropletsService;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.neoforged.neoforge.fluids.FluidStack;
import org.jetbrains.annotations.Nullable;

import java.util.Objects;

/**
 * Blue Droplets: thirst, quenched and water purity for players.
 * <p>
 * <b>Sides.</b> Reading works on both sides (clients see their own synced values). Everything that changes thirst is
 * server side: on a client it does nothing and returns {@code false}.
 * <p>
 * <b>Soft dependency.</b> Compile against {@code bluedroplets-api} only and guard calls with
 * {@code ModList.get().isLoaded("bluedroplets")}.
 */
public final class DropletsAPI
{
    public static final String MOD_ID = "bluedroplets";
    /** Bumped when the API changes incompatibly. */
    public static final int API_VERSION = 1;

    public static final int MAX_THIRST = 20;
    public static final int NO_PURITY = -1;
    public static final int DIRTY = 0;
    public static final int SLIGHTLY_DIRTY = 1;
    public static final int ACCEPTABLE = 2;
    public static final int PURIFIED = 3;

    private static DropletsService service;

    private DropletsAPI() {}

    private static DropletsService service()
    {
        return Objects.requireNonNull(service, "Blue Droplets is not loaded");
    }

    /* Reading */

    /**
     * The player's thirst, live. Cheap: no copy is made.
     */
    public static DropletsView view(Player player)
    {
        return service().view(player);
    }

    /**
     * Resolved values of a stack (config, datapacks, registered drinks and providers, estimates), or null when it
     * does not restore thirst. Synced: the same on a remote client.
     */
    public static @Nullable ThirstValues getDrinkValues(ItemStack stack)
    {
        return service().getDrinkValues(stack);
    }

    /* Changing (server side) */

    /**
     * Sets thirst, clamped to 0..{@link #MAX_THIRST}; quenched is lowered to stay at or below it.
     *
     * @return whether it changed
     */
    public static boolean setThirst(Player player, int thirst)
    {
        return service().setThirst(player, thirst);
    }

    /**
     * Sets quenched, clamped to 0..thirst.
     *
     * @return whether it changed
     */
    public static boolean setQuenched(Player player, int quenched)
    {
        return service().setQuenched(player, quenched);
    }

    /**
     * Adds (or with negative amounts removes) thirst and quenched, clamped. Not a drink: no purity effects, no
     * drink events, and extra thirst is not turned into quenched.
     *
     * @return whether it changed
     */
    public static boolean addThirst(Player player, int thirst, int quenched)
    {
        return service().addThirst(player, thirst, quenched);
    }

    /**
     * Drinks without an item: same rules as drinking a bottle (extra thirst can become quenched).
     *
     * @return whether it hydrated
     */
    public static boolean drink(Player player, int thirst, int quenched)
    {
        return drink(player, thirst, quenched, NO_PURITY);
    }

    /**
     * Drinks water of this purity: rolls its purity effects first, which may prevent hydration.
     *
     * @param purity {@link #DIRTY} to {@link #PURIFIED}, or {@link #NO_PURITY} for no effects
     * @return whether it hydrated
     */
    public static boolean drink(Player player, int thirst, int quenched, int purity)
    {
        return service().drink(player, thirst, quenched, purity);
    }

    /**
     * Adds exhaustion, multiplied like Blue Droplets' own activities by the climate, armor, effects and the
     * {@code bluedroplets:thirst_drain} attribute. Every {@code exhaustionPerPoint} (4 by default) costs a point of
     * quenched, then of thirst. Ignored for creative players and players with thirst disabled.
     */
    public static void addExhaustion(Player player, float amount)
    {
        service().addExhaustion(player, amount);
    }

    /* Purity */

    /**
     * Whether water purity is on ({@code purity.enabled}, the server's value on remote clients). When off, nothing
     * stores, shows or rolls purity.
     */
    public static boolean isPurityEnabled()
    {
        return service().isPurityEnabled();
    }

    /**
     * Purity of a water stack, or of a drink with a fixed purity; missing or invalid purity reads as the server's
     * {@code defaultPurity}.
     */
    public static int getPurity(ItemStack stack)
    {
        return service().getPurity(stack);
    }

    public static int getPurity(FluidStack fluid)
    {
        return service().getPurity(fluid);
    }

    /**
     * A copy of the stack with this purity stored (invalid values become {@code defaultPurity}). Unchanged copy when
     * purity is off or the item is in {@code bluedroplets:purity_opt_out}.
     */
    public static ItemStack withPurity(ItemStack stack, int purity)
    {
        return service().withPurity(stack, purity);
    }

    public static FluidStack withPurity(FluidStack fluid, int purity)
    {
        return service().withPurity(fluid, purity);
    }

    /**
     * Purity of the water at this position (biome, dimension, altitude, running or still) or of a water cauldron.
     * Server side: biome and dimension data are not synced.
     */
    public static int getWaterPurity(Level level, BlockPos pos)
    {
        return service().getWaterPurity(level, pos);
    }

    /**
     * Internal: Blue Droplets installs its implementation here while it is constructed.
     */
    public static void setService(DropletsService implementation)
    {
        if (service != null)
            throw new IllegalStateException("The Blue Droplets service is already set");
        service = implementation;
    }
}

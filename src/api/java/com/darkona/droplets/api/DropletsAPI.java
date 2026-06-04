package com.darkona.droplets.api;

import com.darkona.droplets.api.spi.DropletsService;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ItemLike;
import net.minecraft.world.level.Level;
import net.neoforged.neoforge.fluids.FluidStack;
import org.jetbrains.annotations.Nullable;

import java.util.Objects;
import java.util.function.Predicate;

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
     * Drinks without an item: same rules as drinking a bottle (extra thirst can become quenched). Negative values
     * remove thirst and quenched, like salty food.
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
     * Eats without an item: like a food with these values ({@code EatEvent}, no purity effects). Negative values remove
     * thirst and quenched.
     *
     * @return whether thirst or quenched changed
     */
    public static boolean eat(Player player, int thirst, int quenched)
    {
        return service().eat(player, thirst, quenched);
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

    /* Registering */

    /**
     * Gives an item thirst values from code. Call any time from mod construction on; items are resolved when the
     * tables are built (world load, {@code /reload}), so deferred items are fine.
     * <p>
     * The config ({@code items.toml}), the {@code bluedroplets:no_thirst} tag and the {@code bluedroplets:drinks}
     * data map win over code, so players and modpacks can still change or remove it. Whether it counts as food or
     * drink follows the item: food if it can be eaten.
     * <p>
     * Values are points (2 points = 1 droplet on the HUD): thirst -20 to 20, quenched -20 or more. Negative values
     * make the item salty: eating or drinking it removes thirst and quenched.
     */
    public static void registerDrink(ItemLike item, int thirst, int quenched)
    {
        registerDrink(item, thirst, quenched, NO_PURITY);
    }

    /**
     * @param purity purity for the effects rolled when drinking it, or {@link #NO_PURITY}
     * @see #registerDrink(ItemLike, int, int)
     */
    public static void registerDrink(ItemLike item, int thirst, int quenched, int purity)
    {
        service().registerDrink(item, thirst, quenched, purity);
    }

    /**
     * Values that depend on the stack, for one item. Same precedence as {@link #registerDrink}, after it: the config,
     * the data map and registered drinks win; keywords and recipe estimates do not apply to the item.
     */
    public static void registerDrinkProvider(ItemLike item, DrinkValueProvider provider)
    {
        service().registerDrinkProvider(item, provider);
    }

    /**
     * An item that holds water with purity and is drunk, but is not filled from the world (e.g. a drink made from
     * water). Its stacks keep the purity they are given.
     */
    public static void registerContainer(ItemLike filled)
    {
        service().registerContainer(null, filled);
    }

    /**
     * An empty/filled pair that holds water with purity, like the glass bottle and the water bottle.
     */
    public static void registerContainer(ItemLike empty, ItemLike filled)
    {
        service().registerContainer(empty, filled);
    }

    /**
     * Adds a modifier to every player's thirst loss multiplier. Registering the same id again replaces it. Thread
     * safe; modifiers run in id order.
     */
    public static void registerExhaustionModifier(ResourceLocation id, ExhaustionModifier modifier)
    {
        service().registerExhaustionModifier(id, modifier);
    }

    /**
     * Recomputes the player's thirst loss multiplier on its next use, when something your {@link ExhaustionModifier}
     * reads changed (a season, a temperature); otherwise it refreshes within a second. Server side.
     */
    public static void refreshExhaustionModifier(Player player)
    {
        service().refreshExhaustionModifier(player);
    }

    /**
     * Tints the thirst bar's droplets with {@code rgb} ({@code 0xRRGGBB}) while {@code active} is true for the local
     * player. The active style with the highest priority wins; built-in priorities: vampire 400, Dehydration 300, Overhydrated 250,
     * Poison 200, Quenchness 100. Registering the same id again replaces it. Client side only (for example in
     * {@code FMLClientSetupEvent}); {@code active} is tested every frame, so keep it cheap and allocation free.
     */
    public static void registerBarStyle(ResourceLocation id, Predicate<Player> active, int rgb, int priority)
    {
        service().registerBarStyle(id, active, rgb, priority);
    }

    /**
     * Makes the thirst bar's droplets bounce one at a time, like hearts under Regeneration, while the local player
     * has {@code effect} (built in: Quenchness). Meant for positive thirst effects. Client side only (for example in
     * {@code FMLClientSetupEvent}).
     */
    public static void registerWaveEffect(Holder<MobEffect> effect)
    {
        service().registerWaveEffect(effect);
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

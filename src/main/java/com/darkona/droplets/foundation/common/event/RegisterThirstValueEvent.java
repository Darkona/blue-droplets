package com.darkona.droplets.foundation.common.event;

import com.darkona.droplets.content.purity.ContainerWithPurity;
import net.minecraft.world.item.Item;
import net.minecraftforge.eventbus.api.Event;

import java.util.List;
import java.util.Map;

/**
 * Posted on the game bus every time the thirst tables are rebuilt (world load and {@code /reload}).
 * Values added here can be overridden by the config, the {@code blue_droplets:drinks} data map and
 * {@code DropletsAPI.registerDrink}.
 *
 * @deprecated register once with {@link com.darkona.droplets.api.DropletsAPI#registerDrink},
 * {@link com.darkona.droplets.api.DropletsAPI#registerDrinkProvider} and
 * {@link com.darkona.droplets.api.DropletsAPI#registerContainer}; this event is not part of the API jar.
 */
@Deprecated
@SuppressWarnings("unused")
public class RegisterThirstValueEvent extends Event {
    private final Map<Item, int[]> drinks;
    private final Map<Item, int[]> foods;
    private final List<ContainerWithPurity> containers;

    public RegisterThirstValueEvent(Map<Item, int[]> drinks, Map<Item, int[]> foods, List<ContainerWithPurity> containers){
        this.drinks = drinks;
        this.foods = foods;
        this.containers = containers;
    }

    /**
     * Adds a hydration and "quenchness" value to an item via code, and treats it as food.
     * */
    public void addFood(Item item, int thirst, int quenched)
    {
        foods.putIfAbsent(item, new int[]{thirst, quenched, -1});
    }

    /**
     * Adds a hydration and "quenchness" value to an item via code, and treats it as a drink.
     * */
    public void addDrink(Item item, int thirst, int quenched)
    {
        drinks.putIfAbsent(item, new int[]{thirst, quenched, -1});
    }

    /**
     *Registers new custom water container
     *the container will be taken into consider of purity
     */
    public void addContainer(ContainerWithPurity container){
        containers.add(container);
    }

    /**
     * A simple version, If you don't need your item to harvest water like bucket.
     */
    public void addContainer(Item item){
        containers.add(new ContainerWithPurity(item));
    }
}

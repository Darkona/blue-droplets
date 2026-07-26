package com.darkona.droplets.compat.jei;

import com.darkona.droplets.BlueDroplets;
import com.darkona.droplets.compat.create.CreateCompat;
import mezz.jei.api.IModPlugin;
import mezz.jei.api.JeiPlugin;
import mezz.jei.api.recipe.RecipeType;
import mezz.jei.api.registration.IRecipeCatalystRegistration;
import mezz.jei.api.registration.IRecipeCategoryRegistration;
import mezz.jei.api.registration.IRecipeRegistration;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

import java.util.List;

/**
 * JEI (and EMI, which loads JEI plugins when both are installed): a purification page for the ways of purifying water
 * that are not recipes, and a hydration page listing what each item does to thirst. JEI finds and loads this class
 * itself, only when installed; nothing else imports JEI. JEI calls every step again each time it starts (joining a
 * world or server, {@code /reload}), so the pages follow the server's tables and {@code purity.enabled}.
 */
@JeiPlugin
public final class DropletsJeiPlugin implements IModPlugin
{
    static final RecipeType<PurificationEntry> PURIFICATION = RecipeType.create(BlueDroplets.ID, "purification", PurificationEntry.class);
    static final RecipeType<HydrationEntry> HYDRATION = RecipeType.create(BlueDroplets.ID, "hydration", HydrationEntry.class);

    /** Purification entries of this start; empty with purity off, and then the category is not registered. */
    private List<PurificationEntry> purification = List.of();

    @Override
    public ResourceLocation getPluginUid()
    {
        return BlueDroplets.asResource("jei");
    }

    @Override
    public void registerCategories(IRecipeCategoryRegistration registration)
    {
        purification = PurificationEntry.all();
        registration.addRecipeCategories(new HydrationCategory(registration.getJeiHelpers().getGuiHelper()));
        if (!purification.isEmpty())
            registration.addRecipeCategories(new PurificationCategory(registration.getJeiHelpers().getGuiHelper()));
    }

    @Override
    public void registerRecipes(IRecipeRegistration registration)
    {
        registration.addRecipes(HYDRATION, HydrationEntry.all());
        if (!purification.isEmpty())
            registration.addRecipes(PURIFICATION, purification);
    }

    @Override
    public void registerRecipeCatalysts(IRecipeCatalystRegistration registration)
    {
        if (purification.isEmpty())
            return;
        registration.addRecipeCatalyst(new ItemStack(Items.CAULDRON), PURIFICATION);
        Item filter = CreateCompat.sandFilter();
        if (filter != Items.AIR)
            registration.addRecipeCatalyst(new ItemStack(filter), PURIFICATION);
    }
}

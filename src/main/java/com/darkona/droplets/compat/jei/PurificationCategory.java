package com.darkona.droplets.compat.jei;

import com.darkona.droplets.BlueDroplets;
import mezz.jei.api.gui.builder.IRecipeLayoutBuilder;
import mezz.jei.api.gui.builder.IRecipeSlotBuilder;
import mezz.jei.api.gui.placement.HorizontalAlignment;
import mezz.jei.api.gui.widgets.IRecipeExtrasBuilder;
import mezz.jei.api.helpers.IGuiHelper;
import mezz.jei.api.recipe.IFocusGroup;
import mezz.jei.api.recipe.RecipeIngredientRole;
import mezz.jei.api.recipe.category.AbstractRecipeCategory;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Items;

/**
 * Water in, the block doing it (with what goes under it), water out, and one line saying how.
 */
final class PurificationCategory extends AbstractRecipeCategory<PurificationEntry>
{
    private static final int WIDTH = 150;
    private static final int TEXT_Y = 44;

    PurificationCategory(IGuiHelper gui)
    {
        super(DropletsJeiPlugin.PURIFICATION, Component.translatable(BlueDroplets.ID + ".jei.purification"), gui.createDrawableItemLike(Items.CAULDRON), WIDTH, TEXT_Y + 20);
    }

    @Override
    public void setRecipe(IRecipeLayoutBuilder builder, PurificationEntry entry, IFocusGroup focuses)
    {
        IRecipeSlotBuilder input = builder.addSlot(RecipeIngredientRole.INPUT, 12, 10).setStandardSlotBackground().addItemStacks(entry.inputs());
        IRecipeSlotBuilder output = builder.addSlot(RecipeIngredientRole.OUTPUT, WIDTH - 28, 10).setStandardSlotBackground().addItemStacks(entry.outputs());
        builder.createFocusLink(input, output);
        builder.addSlot(RecipeIngredientRole.CRAFTING_STATION, WIDTH / 2 - 8, 1).setStandardSlotBackground().add(entry.machine());
        if (!entry.below().isEmpty())
            builder.addSlot(RecipeIngredientRole.CRAFTING_STATION, WIDTH / 2 - 8, 19).setStandardSlotBackground().addItemStacks(entry.below());
    }

    @Override
    public void createRecipeExtras(IRecipeExtrasBuilder builder, PurificationEntry entry, IFocusGroup focuses)
    {
        builder.addRecipeArrowWidget().setPosition(36, 10);
        builder.addRecipeArrowWidget().setPosition(WIDTH - 60, 10);
        Component text = Component.translatable(BlueDroplets.ID + ".jei.purification." + entry.method());
        builder.addText(text, WIDTH, 20).setPosition(0, TEXT_Y).setColor(0xFF404040).setTextAlignment(HorizontalAlignment.CENTER);
    }
}

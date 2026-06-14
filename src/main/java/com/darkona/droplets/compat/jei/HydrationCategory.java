package com.darkona.droplets.compat.jei;

import com.darkona.droplets.BlueDroplets;
import com.darkona.droplets.api.ThirstValues;
import com.darkona.droplets.content.registry.ItemInit;
import com.darkona.droplets.foundation.gui.DrinkTooltip;
import mezz.jei.api.gui.builder.IRecipeLayoutBuilder;
import mezz.jei.api.gui.drawable.IDrawable;
import mezz.jei.api.gui.widgets.IRecipeExtrasBuilder;
import mezz.jei.api.helpers.IGuiHelper;
import mezz.jei.api.recipe.IFocusGroup;
import mezz.jei.api.recipe.RecipeIngredientRole;
import mezz.jei.api.recipe.category.AbstractRecipeCategory;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;

/**
 * One item that changes thirst: its thirst and quenched values as droplets (like the item tooltip) and in numbers.
 */
final class HydrationCategory extends AbstractRecipeCategory<HydrationEntry>
{
    private static final int WIDTH = 150;
    private static final int VALUES_X = 24;

    HydrationCategory(IGuiHelper gui)
    {
        super(DropletsJeiPlugin.HYDRATION, Component.translatable(BlueDroplets.ID + ".jei.hydration"), gui.createDrawableItemLike(ItemInit.TERRACOTTA_WATER_BOWL.get()), WIDTH, 32);
    }

    @Override
    public void setRecipe(IRecipeLayoutBuilder builder, HydrationEntry entry, IFocusGroup focuses)
    {
        builder.addSlot(RecipeIngredientRole.INPUT, 1, 8).setStandardSlotBackground().addItemStack(entry.stack());
    }

    @Override
    public void createRecipeExtras(IRecipeExtrasBuilder builder, HydrationEntry entry, IFocusGroup focuses)
    {
        ThirstValues values = entry.values();
        builder.addDrawable(new Droplets(DrinkTooltip.of(values)), VALUES_X, 1);
        String key = BlueDroplets.ID + (values.estimated() ? ".jei.hydration.values_estimated" : ".jei.hydration.values");
        builder.addText(Component.translatable(key, values.thirst(), values.quenched()), WIDTH - VALUES_X, 10).setPosition(VALUES_X, 22).setColor(0xFF404040);
    }

    /**
     * The tooltip row drawn in the recipe; built once per shown recipe, not per frame.
     */
    private record Droplets(DrinkTooltip row) implements IDrawable
    {
        @Override
        public int getWidth()
        {
            return row.getWidth(Minecraft.getInstance().font);
        }

        @Override
        public int getHeight()
        {
            return row.getHeight();
        }

        @Override
        public void draw(GuiGraphics guiGraphics, int xOffset, int yOffset)
        {
            row.renderImage(Minecraft.getInstance().font, xOffset, yOffset, guiGraphics);
        }
    }
}

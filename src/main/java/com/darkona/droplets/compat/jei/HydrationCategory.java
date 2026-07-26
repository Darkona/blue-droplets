package com.darkona.droplets.compat.jei;

import net.minecraft.network.chat.TranslatableComponent;
import com.darkona.droplets.BlueDroplets;
import com.darkona.droplets.api.ThirstValues;
import com.darkona.droplets.content.registry.ItemInit;
import com.darkona.droplets.foundation.gui.DrinkTooltip;
import mezz.jei.api.gui.builder.IRecipeLayoutBuilder;
import mezz.jei.api.gui.drawable.IDrawable;
import mezz.jei.api.gui.ingredient.IRecipeSlotsView;
import mezz.jei.api.helpers.IGuiHelper;
import mezz.jei.api.recipe.IFocusGroup;
import mezz.jei.api.recipe.RecipeIngredientRole;
import mezz.jei.api.recipe.RecipeType;
import mezz.jei.api.recipe.category.IRecipeCategory;
import net.minecraft.client.Minecraft;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import it.unimi.dsi.fastutil.objects.Reference2ObjectOpenHashMap;

/**
 * One item that changes thirst: its thirst and quenched values as droplets (like the item tooltip) and in numbers.
 */
final class HydrationCategory implements IRecipeCategory<HydrationEntry>
{
    private static final int WIDTH = 150;
    private static final int VALUES_X = 24;

    private final Component title = new TranslatableComponent(BlueDroplets.ID + ".jei.hydration");
    private final IDrawable background;
    private final IDrawable icon;
    private final IDrawable slotBackground;
    /** The droplets row of each shown entry, built once, not per frame. */
    private final Reference2ObjectOpenHashMap<HydrationEntry, Droplets> rows = new Reference2ObjectOpenHashMap<>();

    HydrationCategory(IGuiHelper gui)
    {
        background = gui.createBlankDrawable(WIDTH, 32);
        icon = gui.createDrawableItemStack(new ItemStack(ItemInit.TERRACOTTA_WATER_BOWL.get()));
        slotBackground = gui.getSlotDrawable();
    }

    @Override
    public RecipeType<HydrationEntry> getRecipeType()
    {
        return DropletsJeiPlugin.HYDRATION;
    }

    @SuppressWarnings("removal")
    @Override
    public ResourceLocation getUid()
    {
        return DropletsJeiPlugin.HYDRATION.getUid();
    }

    @SuppressWarnings("removal")
    @Override
    public Class<? extends HydrationEntry> getRecipeClass()
    {
        return HydrationEntry.class;
    }

    @Override
    public Component getTitle()
    {
        return title;
    }

    @Override
    public IDrawable getBackground()
    {
        return background;
    }

    @Override
    public IDrawable getIcon()
    {
        return icon;
    }

    @Override
    public void setRecipe(IRecipeLayoutBuilder builder, HydrationEntry entry, IFocusGroup focuses)
    {
        builder.addSlot(RecipeIngredientRole.INPUT, 1, 8).setBackground(slotBackground, -1, -1).addItemStack(entry.stack());
    }

    /**
     * JEI 10 has no recipe widgets: the droplets and the numbers are drawn here, the droplets from a row cached per entry.
     */
    @Override
    public void draw(HydrationEntry entry, IRecipeSlotsView slots, PoseStack poseStack, double mouseX, double mouseY)
    {
        ThirstValues values = entry.values();
        rows.computeIfAbsent(entry, e -> new Droplets(DrinkTooltip.of(((HydrationEntry) e).values()))).draw(poseStack, VALUES_X, 1);
        String key = BlueDroplets.ID + (values.estimated() ? ".jei.hydration.values_estimated" : ".jei.hydration.values");
        Minecraft.getInstance().font.draw(poseStack, new TranslatableComponent(key, values.thirst(), values.quenched()), VALUES_X, 22, 0xFF404040);
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
        public void draw(PoseStack poseStack, int xOffset, int yOffset)
        {
            row.renderImage(Minecraft.getInstance().font, xOffset, yOffset, poseStack, Minecraft.getInstance().getItemRenderer(), 0);
        }
    }
}

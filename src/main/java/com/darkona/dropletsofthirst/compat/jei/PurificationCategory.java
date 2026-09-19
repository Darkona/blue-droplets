package com.darkona.dropletsofthirst.compat.jei;

import com.darkona.dropletsofthirst.DropletsOfThirst;
import com.darkona.dropletsofthirst.content.purity.WaterPurity;
import com.darkona.dropletsofthirst.foundation.config.CompatConfig;
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
import net.neoforged.neoforge.fluids.FluidStack;

/**
 * Water in, the block doing it (with what goes under it), water out, and one line saying how.
 */
final class PurificationCategory extends AbstractRecipeCategory<PurificationEntry>
{
    private static final int WIDTH = 150;
    private static final int TEXT_Y = 44;

    PurificationCategory(IGuiHelper gui)
    {
        super(DropletsJeiPlugin.PURIFICATION, Component.translatable(DropletsOfThirst.ID + ".jei.purification"), gui.createDrawableItemLike(Items.CAULDRON), WIDTH, TEXT_Y + 20);
    }

    @Override
    public void setRecipe(IRecipeLayoutBuilder builder, PurificationEntry entry, IFocusGroup focuses)
    {
        IRecipeSlotBuilder input = slot(builder, RecipeIngredientRole.INPUT, 12, 10, entry.fluidIn());
        IRecipeSlotBuilder output = slot(builder, RecipeIngredientRole.OUTPUT, WIDTH - 28, 10, entry.fluidOut());
        if (entry.fluidIn().isEmpty())
        {
            input.addItemStacks(entry.inputs());
            output.addItemStacks(entry.outputs());
            builder.createFocusLink(input, output);
        }
        builder.addSlot(RecipeIngredientRole.CATALYST, WIDTH / 2 - 8, 1).setStandardSlotBackground().addItemStack(entry.machine());
        if (!entry.below().isEmpty())
            builder.addSlot(RecipeIngredientRole.CATALYST, WIDTH / 2 - 8, 19).setStandardSlotBackground().addItemStacks(entry.below());
    }

    private static IRecipeSlotBuilder slot(IRecipeLayoutBuilder builder, RecipeIngredientRole role, int x, int y, FluidStack fluid)
    {
        IRecipeSlotBuilder slot = builder.addSlot(role, x, y).setStandardSlotBackground();
        if (!fluid.isEmpty())
        {
            int purity = WaterPurity.getPurity(fluid);
            slot.setFluidRenderer(fluid.getAmount(), false, 16, 16)
                    .addFluidStack(fluid.getFluid(), fluid.getAmount(), fluid.getComponentsPatch())
                    .addRichTooltipCallback((view, tooltip) -> tooltip.add(Component.literal(WaterPurity.getPurityText(purity)).withColor(WaterPurity.getPurityColor(purity))));
        }
        return slot;
    }

    @Override
    public void createRecipeExtras(IRecipeExtrasBuilder builder, PurificationEntry entry, IFocusGroup focuses)
    {
        builder.addRecipeArrow().setPosition(36, 10);
        builder.addRecipeArrow().setPosition(WIDTH - 60, 10);
        Component text = entry.method().equals("sand_filter")
                ? Component.translatable(DropletsOfThirst.ID + ".jei.purification.sand_filter", CompatConfig.SAND_FILTER_FILTRATION_AMOUNT.get(), WaterPurity.getPurityText(CompatConfig.SAND_FILTER_MAX_PURITY.get()))
                : Component.translatable(DropletsOfThirst.ID + ".jei.purification." + entry.method());
        builder.addText(text, WIDTH, 20).setPosition(0, TEXT_Y).setColor(0xFF404040).setTextAlignment(HorizontalAlignment.CENTER);
    }
}

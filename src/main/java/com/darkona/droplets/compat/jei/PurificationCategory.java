package com.darkona.droplets.compat.jei;

import net.minecraft.network.chat.TranslatableComponent;
import net.minecraft.network.chat.TextComponent;
import com.darkona.droplets.BlueDroplets;
import com.darkona.droplets.content.purity.WaterPurity;
import com.darkona.droplets.foundation.config.CompatConfig;
import mezz.jei.api.gui.builder.IRecipeLayoutBuilder;
import mezz.jei.api.gui.builder.IRecipeSlotBuilder;
import mezz.jei.api.gui.drawable.IDrawable;
import mezz.jei.api.gui.ingredient.IRecipeSlotsView;
import mezz.jei.api.helpers.IGuiHelper;
import mezz.jei.api.recipe.IFocusGroup;
import mezz.jei.api.recipe.RecipeIngredientRole;
import mezz.jei.api.recipe.RecipeType;
import mezz.jei.api.recipe.category.IRecipeCategory;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraftforge.fluids.FluidStack;

/**
 * Water in, the block doing it (with what goes under it), water out, and one line saying how.
 */
final class PurificationCategory implements IRecipeCategory<PurificationEntry>
{
    private static final int WIDTH = 150;
    private static final int TEXT_Y = 44;
    /** The empty arrow of the vanilla furnace screen: JEI 10 has no recipe arrow widget. */
    private static final ResourceLocation FURNACE = new ResourceLocation("textures/gui/container/furnace.png");

    private final Component title = new TranslatableComponent(BlueDroplets.ID + ".jei.purification");
    private final IDrawable background;
    private final IDrawable icon;
    private final IDrawable slotBackground;
    private final IDrawable arrow;

    PurificationCategory(IGuiHelper gui)
    {
        background = gui.createBlankDrawable(WIDTH, TEXT_Y + 20);
        icon = gui.createDrawableItemStack(new ItemStack(Items.CAULDRON));
        slotBackground = gui.getSlotDrawable();
        arrow = gui.createDrawable(FURNACE, 79, 34, 24, 17);
    }

    @Override
    public RecipeType<PurificationEntry> getRecipeType()
    {
        return DropletsJeiPlugin.PURIFICATION;
    }

    @SuppressWarnings("removal")
    @Override
    public ResourceLocation getUid()
    {
        return DropletsJeiPlugin.PURIFICATION.getUid();
    }

    @SuppressWarnings("removal")
    @Override
    public Class<? extends PurificationEntry> getRecipeClass()
    {
        return PurificationEntry.class;
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
        builder.addSlot(RecipeIngredientRole.CATALYST, WIDTH / 2 - 8, 1).setBackground(slotBackground, -1, -1).addItemStack(entry.machine());
        if (!entry.below().isEmpty())
            builder.addSlot(RecipeIngredientRole.CATALYST, WIDTH / 2 - 8, 19).setBackground(slotBackground, -1, -1).addItemStacks(entry.below());
    }

    private IRecipeSlotBuilder slot(IRecipeLayoutBuilder builder, RecipeIngredientRole role, int x, int y, FluidStack fluid)
    {
        IRecipeSlotBuilder slot = builder.addSlot(role, x, y).setBackground(slotBackground, -1, -1);
        if (!fluid.isEmpty())
        {
            int purity = WaterPurity.getPurity(fluid);
            slot.setFluidRenderer(fluid.getAmount(), false, 16, 16)
                    .addFluidStack(fluid.getFluid(), fluid.getAmount(), fluid.getTag())
                    .addTooltipCallback((view, tooltip) -> tooltip.add(new TextComponent(WaterPurity.getPurityText(purity)).withStyle(style -> style.withColor(WaterPurity.getPurityColor(purity)))));
        }
        return slot;
    }

    /**
     * JEI 10 has no recipe widgets: the arrows and the line of text are drawn here.
     */
    @Override
    public void draw(PurificationEntry entry, IRecipeSlotsView slots, PoseStack poseStack, double mouseX, double mouseY)
    {
        arrow.draw(poseStack, 36, 10);
        arrow.draw(poseStack, WIDTH - 60, 10);
        Component text = entry.method().equals("sand_filter")
                ? new TranslatableComponent(BlueDroplets.ID + ".jei.purification.sand_filter", CompatConfig.SAND_FILTER_FILTRATION_AMOUNT.get(), WaterPurity.getPurityText(CompatConfig.SAND_FILTER_MAX_PURITY.get()))
                : new TranslatableComponent(BlueDroplets.ID + ".jei.purification." + entry.method());
        Font font = Minecraft.getInstance().font;
        int y = TEXT_Y;
        for (var line : font.split(text, WIDTH))
        {
            font.draw(poseStack, line, (WIDTH - font.width(line)) / 2f, y, 0xFF404040);
            y += font.lineHeight;
        }
    }
}

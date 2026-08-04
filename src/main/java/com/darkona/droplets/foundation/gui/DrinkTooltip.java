package com.darkona.droplets.foundation.gui;

import com.darkona.droplets.api.ThirstValues;
import com.darkona.droplets.compat.appleskin.AppleSkinCompat;
import com.darkona.droplets.content.thirst.ThirstHelper;
import com.darkona.droplets.foundation.config.ClientConfig;
import com.mojang.datafixers.util.Either;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.util.ARGB;
import net.minecraft.client.gui.screens.inventory.tooltip.ClientTooltipComponent;
import net.minecraft.client.resources.language.I18n;
import net.minecraft.network.chat.FormattedText;
import net.minecraft.world.inventory.tooltip.TooltipComponent;
import net.neoforged.neoforge.client.event.RegisterClientTooltipComponentFactoriesEvent;
import net.neoforged.neoforge.client.event.RenderTooltipEvent;
import org.jetbrains.annotations.Nullable;

/**
 * Thirst droplets and quenched icons in the tooltip of items that restore thirst (like AppleSkin's for food); red
 * when the item removes them (salty).
 */
public final class DrinkTooltip implements TooltipComponent, ClientTooltipComponent
{
    private static final int GAP = 2;
    private static final int TEXT_COLOR = 0xFFAAAAAA;
    private static final int SALTY_TEXT_COLOR = 0xFFE05050;
    private static final int SALTY = ARGB.colorFromFloat(1f, ThirstBarRenderer.SALTY_RED, ThirstBarRenderer.SALTY_GREEN, ThirstBarRenderer.SALTY_BLUE);
    private static @Nullable DrinkTooltip last;

    private final ThirstValues values;
    private final Either<FormattedText, TooltipComponent> element = Either.right(this);
    private final int thirstIcons;
    private final int quenchedIcons;
    private final @Nullable String thirstText;
    private final @Nullable String quenchedText;
    private final @Nullable String estimatedText;
    private int width = -1;
    private int thirstTextWidth;

    private DrinkTooltip(ThirstValues values)
    {
        this.values = values;
        int thirstBars = (Math.abs(values.thirst()) + 1) / 2;
        int quenchedBars = (Math.abs(values.quenched()) + 1) / 2;
        thirstIcons = thirstBars > 10 ? 1 : thirstBars;
        quenchedIcons = quenchedBars > 10 ? 1 : quenchedBars;
        thirstText = thirstBars > 10 ? "x" + thirstBars : null;
        quenchedText = quenchedBars > 10 ? "x" + quenchedBars : null;
        estimatedText = values.estimated() ? I18n.get("blue_droplets.tooltip.estimated_short") : null;
    }

    /**
     * The same droplets and quenched icons outside a tooltip (the recipe viewer's hydration page).
     */
    public static DrinkTooltip of(ThirstValues values)
    {
        return new DrinkTooltip(values);
    }

    public static void registerFactory(RegisterClientTooltipComponentFactoriesEvent event)
    {
        event.register(DrinkTooltip.class, tooltip -> tooltip);
    }

    /**
     * Runs every frame while a tooltip is open: reuses the last component while the values stay the same. Registered at
     * low priority, so with AppleSkin the droplets go right under its food row.
     */
    public static void gather(RenderTooltipEvent.GatherComponents event)
    {
        if (!ClientConfig.SHOW_TOOLTIP_ICONS.get() || !AppleSkinCompat.tooltip())
            return;
        ThirstValues values = ThirstHelper.drinkValuesOf(event.getItemStack());
        if (values == null || values.thirst() == 0 && values.quenched() == 0)
            return;
        DrinkTooltip tooltip = last;
        if (tooltip == null || !tooltip.values.equals(values))
            last = tooltip = new DrinkTooltip(values);
        event.getTooltipElements().add(tooltip.element);
    }

    @Override
    public int getHeight(Font font)
    {
        return quenchedIcons > 0 ? 20 : 10;
    }

    @Override
    public int getWidth(Font font)
    {
        if (width < 0)
        {
            thirstTextWidth = textWidth(font, thirstText);
            int thirstRow = thirstIcons * 9 + thirstTextWidth + textWidth(font, estimatedText);
            int quenchedRow = quenchedIcons * 7 + textWidth(font, quenchedText);
            width = Math.max(thirstRow, quenchedRow);
        }
        return width;
    }

    private static int textWidth(Font font, @Nullable String text)
    {
        return text == null ? 0 : GAP + font.width(text);
    }

    @Override
    public void extractImage(Font font, int x, int y, int w, int h, GuiGraphicsExtractor guiGraphics)
    {
        getWidth(font);
        boolean saltyThirst = values.thirst() < 0;
        int thirst = Math.abs(values.thirst());
        int offsetX = x + (thirstIcons - 1) * 9;
        for (int i = 0; i < thirstIcons; ++i)
        {
            int u = thirstText == null && thirst == i * 2 + 1 ? 8 : 16;
            if (saltyThirst)
            {
                guiGraphics.blit(RenderPipelines.GUI_TEXTURED, ThirstBarRenderer.THIRST_ICONS, offsetX, y, 0, 0, 9, 9, 25, 9);
                guiGraphics.blit(RenderPipelines.GUI_TEXTURED, ThirstBarRenderer.THIRST_MASK, offsetX, y, u, 0, 9, 9, 25, 9, SALTY);
            }
            else
                guiGraphics.blit(RenderPipelines.GUI_TEXTURED, ThirstBarRenderer.THIRST_ICONS, offsetX, y, u, 0, 9, 9, 25, 9);
            offsetX -= 9;
        }
        int textX = x + thirstIcons * 9 + GAP;
        if (thirstText != null)
            guiGraphics.text(font, thirstText, textX, y + 1, saltyThirst ? SALTY_TEXT_COLOR : TEXT_COLOR);
        if (estimatedText != null)
            guiGraphics.text(font, estimatedText, textX + thirstTextWidth, y + 1, TEXT_COLOR);

        if (quenchedIcons == 0)
            return;
        boolean saltyQuenched = values.quenched() < 0;
        float quenched = Math.abs(values.quenched());
        int color = saltyQuenched ? SALTY : -1;
        offsetX = x + (quenchedIcons - 1) * 7;
        for (int i = 0; i < quenchedIcons; ++i)
        {
            float bar = quenchedText == null ? quenched / 2f - i : 1f;
            int u = bar >= 1 ? 21 : bar > 0.5f ? 14 : bar > 0.25f ? 7 : 0;
            guiGraphics.blit(RenderPipelines.GUI_TEXTURED, ThirstBarRenderer.QUENCHED_ICONS, offsetX, y + 11, u, 27, 7, 7, 256, 256, color);
            offsetX -= 7;
        }
        if (quenchedText != null)
            guiGraphics.text(font, quenchedText, x + quenchedIcons * 7 + GAP, y + 10, saltyQuenched ? SALTY_TEXT_COLOR : TEXT_COLOR);
    }
}

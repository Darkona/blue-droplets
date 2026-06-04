package com.darkona.droplets.foundation.gui;

import com.mojang.blaze3d.systems.RenderSystem;
import com.darkona.droplets.BlueDroplets;
import com.darkona.droplets.api.ThirstValues;
import com.darkona.droplets.content.thirst.ThirstHelper;
import com.darkona.droplets.core.ThirstConstants;
import com.darkona.droplets.foundation.common.capability.IThirst;
import com.darkona.droplets.foundation.common.capability.ModAttachment;
import com.darkona.droplets.foundation.config.ClientConfig;
import com.darkona.droplets.foundation.config.GameplayConfig;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.food.FoodProperties;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.client.event.RegisterGuiLayersEvent;
import net.neoforged.neoforge.client.gui.VanillaGuiLayers;
import org.jetbrains.annotations.Nullable;

/**
 * The thirst bar with its quenched outline, drink preview and exhaustion underlay (like AppleSkin's for hunger).
 */
public final class ThirstBarRenderer
{
    public static final ResourceLocation LAYER = BlueDroplets.asResource("thirst_level");
    public static final ResourceLocation THIRST_ICONS = BlueDroplets.asResource("textures/gui/thirst_icons.png");
    /** Grayscale fill of {@link #THIRST_ICONS} (same UVs, no outline or background), tinted by {@link ThirstBarStyles}. */
    public static final ResourceLocation THIRST_MASK = BlueDroplets.asResource("textures/gui/thirst_icons_mask.png");
    /** Quenched outlines (row 0), exhaustion underlay (v 18) and tooltip quenched icons (7x7, v 27). */
    public static final ResourceLocation QUENCHED_ICONS = BlueDroplets.asResource("textures/gui/quenched_icons.png");
    private static final ResourceLocation QUENCHED_MASK = BlueDroplets.asResource("textures/gui/quenched_icons_mask.png");
    /** Tint of what a salty item would take away (preview) or takes away (tooltip). */
    public static final float SALTY_RED = 0.9f, SALTY_GREEN = 0.2f, SALTY_BLUE = 0.2f;
    private static final RandomSource random = RandomSource.create();
    private static int lastNotFullTick;

    private ThirstBarRenderer() {}

    public static void registerLayer(RegisterGuiLayersEvent event)
    {
        event.registerAbove(VanillaGuiLayers.FOOD_LEVEL, LAYER, ThirstBarRenderer::render);
    }

    private static void render(GuiGraphics guiGraphics, DeltaTracker deltaTracker)
    {
        Minecraft minecraft = Minecraft.getInstance();
        Player player = minecraft.player;
        if (player == null || minecraft.options.hideGui || !minecraft.gameMode.canHurtPlayer() || !(minecraft.getCameraEntity() instanceof Player)
                || player.getVehicle() instanceof LivingEntity vehicle && vehicle.showVehicleHealth())
            return;

        IThirst thirst = player.getData(ModAttachment.PLAYER_THIRST);
        if (player.isAlive() && !thirst.getShouldTickThirst() || shouldHideBar(minecraft, player, thirst))
            return;

        minecraft.getProfiler().push("thirst");
        int tint = ThirstBarStyles.resolve(player);
        float red = (tint >> 16 & 255) / 255f;
        float green = (tint >> 8 & 255) / 255f;
        float blue = (tint & 255) / 255f;
        ResourceLocation fill = tint < 0 ? THIRST_ICONS : THIRST_MASK;
        ResourceLocation outline = tint < 0 ? QUENCHED_ICONS : QUENCHED_MASK;
        int right = guiGraphics.guiWidth() / 2 + 91 + ClientConfig.THIRST_BAR_X_OFFSET.get();
        int top = guiGraphics.guiHeight() - minecraft.gui.rightHeight + ClientConfig.THIRST_BAR_Y_OFFSET.get();
        minecraft.gui.rightHeight += 10;

        int ticks = minecraft.gui.getGuiTicks();
        int level = thirst.getThirst();
        int quenched = thirst.getQuenched();
        boolean shake = quenched <= 0 && ticks % (level * 3 + 1) == 0;
        int wave = ClientConfig.BUFF_WAVE.get() && ThirstBarStyles.waves(player) ? ticks % (ThirstConstants.MAX_THIRST + 5) : -1;
        boolean showQuenched = ClientConfig.SHOW_QUENCHED_OVERLAY.get();

        ThirstValues gain = ClientConfig.SHOW_DRINK_PREVIEW.get() ? heldDrink(player) : null;
        int newLevel = level;
        int newQuenched = quenched;
        float flash = 0;
        if (gain != null)
        {
            newLevel = Mth.clamp(level + gain.thirst(), 0, ThirstConstants.MAX_THIRST);
            newQuenched = Mth.clamp(quenched + gain.quenched(), 0, newLevel);
            flash = flashAlpha(ticks);
        }

        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        if (ClientConfig.SHOW_EXHAUSTION_UNDERLAY.get())
        {
            int width = (int) (Mth.clamp(thirst.getExhaustion() / GameplayConfig.EXHAUSTION_PER_POINT.get().floatValue(), 0f, 1f) * 81);
            guiGraphics.setColor(1f, 1f, 1f, 0.75f);
            guiGraphics.blit(QUENCHED_ICONS, right - width, top, 81 - width, 18, width, 9);
        }

        for (int i = 0; i < 10; ++i)
        {
            int idx = i * 2 + 1;
            int x = right - i * 8 - 9;
            int y = shake ? top + random.nextInt(3) - 1 : top;
            if (i == wave)
                y -= 2;

            guiGraphics.setColor(1f, 1f, 1f, 1f);
            guiGraphics.blit(THIRST_ICONS, x, y, 0, 0, 9, 9, 25, 9);

            guiGraphics.setColor(red, green, blue, 1f);
            if (idx <= level)
                guiGraphics.blit(fill, x, y, idx < level ? 16 : 8, 0, 9, 9, 25, 9);
            if (showQuenched && quenched > i * 2)
                guiGraphics.blit(outline, x, y, outlineU(quenched, i), 0, 9, 9);

            if (gain == null)
                continue;
            guiGraphics.setColor(red, green, blue, flash);
            if (newLevel > level && idx >= level && idx <= newLevel)
                guiGraphics.blit(fill, x, y, idx < newLevel ? 16 : 8, 0, 9, 9, 25, 9);
            if (newQuenched > quenched && newQuenched > i * 2 && i >= quenched / 2)
                guiGraphics.blit(outline, x, y, outlineU(newQuenched, i), 0, 9, 9);

            if (newLevel < level && idx <= level && idx >= newLevel)
            {
                guiGraphics.setColor(SALTY_RED, SALTY_GREEN, SALTY_BLUE, flash);
                guiGraphics.blit(THIRST_MASK, x, y, idx < level ? 16 : 8, 0, 9, 9, 25, 9);
                if (idx == newLevel)
                {
                    guiGraphics.setColor(red, green, blue, 1f);
                    guiGraphics.blit(fill, x, y, 8, 0, 9, 9, 25, 9);
                }
            }
            if (newQuenched < quenched && quenched > i * 2 && newQuenched < i * 2 + 2)
            {
                guiGraphics.setColor(SALTY_RED, SALTY_GREEN, SALTY_BLUE, flash);
                guiGraphics.blit(QUENCHED_MASK, x, y, outlineU(quenched, i), 0, 9, 9);
            }
        }
        guiGraphics.setColor(1f, 1f, 1f, 1f);
        RenderSystem.disableBlend();
        minecraft.getProfiler().pop();
    }

    private static int outlineU(int quenched, int i)
    {
        float bar = quenched / 2f - i;
        return bar >= 1 ? 27 : bar > 0.5f ? 18 : bar > 0.25f ? 9 : 0;
    }

    /** AppleSkin's pulse: 0 to 0.65 and back every 32 ticks. */
    private static float flashAlpha(int ticks)
    {
        int phase = ticks % 32;
        float pulse = phase < 16 ? -0.5f + phase * 0.125f : 1.5f - (phase - 16) * 0.125f;
        return Mth.clamp(pulse, 0f, 1f) * 0.65f;
    }

    private static @Nullable ThirstValues heldDrink(Player player)
    {
        ThirstValues values = drinkValues(player, player.getMainHandItem());
        return values != null ? values : drinkValues(player, player.getOffhandItem());
    }

    private static @Nullable ThirstValues drinkValues(Player player, ItemStack stack)
    {
        if (stack.isEmpty() || !ThirstHelper.playerRestoresThirst(stack, player))
            return null;
        FoodProperties food = stack.getFoodProperties(player);
        if (food != null && !player.canEat(food.canAlwaysEat()))
            return null;
        return ThirstHelper.valuesOf(stack);
    }

    private static boolean shouldHideBar(Minecraft minecraft, Player player, IThirst thirst)
    {
        if (!ClientConfig.HIDE_BAR_WHEN_FULL.get())
            return false;

        int ticks = minecraft.gui.getGuiTicks();
        if (thirst.getThirst() < 20
                || ThirstHelper.itemRestoresThirst(player.getMainHandItem())
                || ThirstHelper.itemRestoresThirst(player.getOffhandItem())
                || ticks < lastNotFullTick)
        {
            lastNotFullTick = ticks;
            return false;
        }

        return ticks - lastNotFullTick >= ClientConfig.HIDE_BAR_DELAY_TICKS.get();
    }
}

package com.darkona.droplets.foundation.gui;

import com.mojang.blaze3d.systems.RenderSystem;
import com.darkona.droplets.BlueDroplets;
import com.darkona.droplets.content.thirst.ThirstHelper;
import com.darkona.droplets.foundation.common.capability.IThirst;
import com.darkona.droplets.foundation.common.capability.ModAttachment;
import com.darkona.droplets.foundation.config.ClientConfig;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.neoforged.neoforge.client.event.RegisterGuiLayersEvent;
import net.neoforged.neoforge.client.gui.VanillaGuiLayers;

public final class ThirstBarRenderer
{
    public static final ResourceLocation LAYER = BlueDroplets.asResource("thirst_level");
    public static final ResourceLocation THIRST_ICONS = BlueDroplets.asResource("textures/gui/thirst_icons.png");
    /** Grayscale fill of {@link #THIRST_ICONS} (same UVs, no outline or background), tinted by {@link ThirstBarStyles}. */
    public static final ResourceLocation THIRST_MASK = BlueDroplets.asResource("textures/gui/thirst_icons_mask.png");
    private static final RandomSource random = RandomSource.create();
    private static int lastNotFullTick;
    /** Whether the bar was drawn in the last frame, and where (without shake). */
    public static boolean drawn;
    public static int barRight;
    public static int barTop;
    /** {@code 0xRRGGBB} of the active {@link ThirstBarStyles} style in the last frame, or -1. */
    public static int tint = -1;

    private ThirstBarRenderer() {}

    public static void registerLayer(RegisterGuiLayersEvent event)
    {
        event.registerAbove(VanillaGuiLayers.FOOD_LEVEL, LAYER, ThirstBarRenderer::render);
    }

    private static void render(GuiGraphics guiGraphics, DeltaTracker deltaTracker)
    {
        drawn = false;
        Minecraft minecraft = Minecraft.getInstance();
        Player player = minecraft.player;
        if (player == null || minecraft.options.hideGui || !minecraft.gameMode.canHurtPlayer() || !(minecraft.getCameraEntity() instanceof Player)
                || player.getVehicle() instanceof LivingEntity vehicle && vehicle.showVehicleHealth())
            return;

        IThirst thirst = player.getData(ModAttachment.PLAYER_THIRST);
        if (player.isAlive() && !thirst.getShouldTickThirst() || shouldHideBar(minecraft, player, thirst))
            return;

        minecraft.getProfiler().push("thirst");
        tint = ThirstBarStyles.resolve(player);
        ResourceLocation fill = tint < 0 ? THIRST_ICONS : THIRST_MASK;
        float red = (tint >> 16 & 255) / 255f;
        float green = (tint >> 8 & 255) / 255f;
        float blue = (tint & 255) / 255f;
        int right = guiGraphics.guiWidth() / 2 + 91 + ClientConfig.THIRST_BAR_X_OFFSET.get();
        int top = guiGraphics.guiHeight() - minecraft.gui.rightHeight + ClientConfig.THIRST_BAR_Y_OFFSET.get();
        minecraft.gui.rightHeight += 10;

        int level = thirst.getThirst();
        boolean shake = thirst.getQuenched() <= 0 && minecraft.gui.getGuiTicks() % (level * 3 + 1) == 0;

        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        for (int i = 0; i < 10; ++i)
        {
            int idx = i * 2 + 1;
            int x = right - i * 8 - 9;
            int y = shake ? top + random.nextInt(3) - 1 : top;

            guiGraphics.blit(THIRST_ICONS, x, y, 0, 0, 9, 9, 25, 9);

            if (idx > level)
                continue;
            if (tint >= 0)
                guiGraphics.setColor(red, green, blue, 1f);
            guiGraphics.blit(fill, x, y, idx < level ? 16 : 8, 0, 9, 9, 25, 9);
            if (tint >= 0)
                guiGraphics.setColor(1f, 1f, 1f, 1f);
        }
        RenderSystem.disableBlend();

        drawn = true;
        barRight = right;
        barTop = top;
        minecraft.getProfiler().pop();
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

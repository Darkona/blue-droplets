package com.darkona.dropletsofthirst.foundation.gui;

import com.mojang.blaze3d.systems.RenderSystem;
import com.darkona.dropletsofthirst.DropletsOfThirst;
import com.darkona.dropletsofthirst.api.ThirstValues;
import com.darkona.dropletsofthirst.compat.appleskin.AppleSkinCompat;
import com.darkona.dropletsofthirst.content.thirst.ThirstHelper;
import com.darkona.dropletsofthirst.core.ThirstConstants;
import com.darkona.dropletsofthirst.foundation.common.capability.IThirst;
import com.darkona.dropletsofthirst.foundation.common.capability.ModAttachment;
import com.darkona.dropletsofthirst.foundation.config.ClientConfig;
import com.darkona.dropletsofthirst.foundation.config.GameplayConfig;
import net.minecraft.client.Minecraft;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.gui.GuiComponent;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.food.FoodProperties;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.client.event.RegisterGuiOverlaysEvent;
import net.minecraftforge.client.gui.overlay.ForgeGui;
import net.minecraftforge.client.gui.overlay.VanillaGuiOverlay;
import org.jetbrains.annotations.Nullable;

/**
 * The thirst bar with its quenched outline, drink preview and exhaustion underlay (like AppleSkin's for hunger).
 */
public final class ThirstBarRenderer
{
    public static final ResourceLocation LAYER = DropletsOfThirst.asResource("thirst_level");
    public static final ResourceLocation THIRST_ICONS = DropletsOfThirst.asResource("textures/gui/thirst_icons.png");
    /** Grayscale fill of {@link #THIRST_ICONS} (same UVs, no outline or background), tinted by {@link ThirstBarStyles}. */
    public static final ResourceLocation THIRST_MASK = DropletsOfThirst.asResource("textures/gui/thirst_icons_mask.png");
    /** Quenched outlines (row 0), exhaustion underlay (v 18) and tooltip quenched icons (7x7, v 27). */
    public static final ResourceLocation QUENCHED_ICONS = DropletsOfThirst.asResource("textures/gui/quenched_icons.png");
    private static final ResourceLocation QUENCHED_MASK = DropletsOfThirst.asResource("textures/gui/quenched_icons_mask.png");
    /** Tint of what a salty item would take away (preview) or takes away (tooltip). */
    public static final float SALTY_RED = 0.9f, SALTY_GREEN = 0.2f, SALTY_BLUE = 0.2f;
    private static final RandomSource random = RandomSource.create();
    private static int lastNotFullTick;

    private ThirstBarRenderer() {}

    /**
     * The overlay {@code droplets_of_thirst:thirst_level}, right above the hunger bar: the air bar and other mods' bars
     * stack above it, and other mods can cancel it with {@code RenderGuiOverlayEvent.Pre}.
     */
    public static void registerLayer(RegisterGuiOverlaysEvent event)
    {
        event.registerAbove(VanillaGuiOverlay.FOOD_LEVEL.id(), LAYER.getPath(), ThirstBarRenderer::render);
    }

    private static void render(ForgeGui gui, PoseStack poseStack, float partialTick, int screenWidth, int screenHeight)
    {
        Minecraft minecraft = Minecraft.getInstance();
        Player player = minecraft.player;
        if (player == null || minecraft.options.hideGui || !gui.shouldDrawSurvivalElements()
                || player.getVehicle() instanceof LivingEntity vehicle && vehicle.showVehicleHealth())
            return;

        IThirst thirst = ModAttachment.thirst(player);
        if (player.isAlive() && !thirst.getShouldTickThirst() || shouldHideBar(minecraft, player, thirst))
            return;

        minecraft.getProfiler().push("thirst");
        int tint = ThirstBarStyles.resolve(player);
        float red = (tint >> 16 & 255) / 255f;
        float green = (tint >> 8 & 255) / 255f;
        float blue = (tint & 255) / 255f;
        ResourceLocation fill = tint < 0 ? THIRST_ICONS : THIRST_MASK;
        ResourceLocation outline = tint < 0 ? QUENCHED_ICONS : QUENCHED_MASK;
        int right = screenWidth / 2 + 91 + ClientConfig.THIRST_BAR_X_OFFSET.get();
        int top = screenHeight - gui.rightHeight + ClientConfig.THIRST_BAR_Y_OFFSET.get();
        gui.rightHeight += 10;

        int ticks = gui.getGuiTicks();
        int level = thirst.getThirst();
        int quenched = thirst.getQuenched();
        boolean shake = quenched <= 0 && ticks % (level * 3 + 1) == 0;
        int wave = ClientConfig.BUFF_WAVE.get() && ThirstBarStyles.waves(player) ? ticks % (ThirstConstants.MAX_THIRST + 5) : -1;
        boolean showQuenched = ClientConfig.SHOW_QUENCHED_OVERLAY.get() && AppleSkinCompat.quenchedOverlay();

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
        if (ClientConfig.SHOW_EXHAUSTION_UNDERLAY.get() && AppleSkinCompat.exhaustionUnderlay())
        {
            int width = (int) (Mth.clamp(thirst.getExhaustion() / GameplayConfig.EXHAUSTION_PER_POINT.get().floatValue(), 0f, 1f) * 81);
            RenderSystem.setShaderColor(1f, 1f, 1f, 0.75f);
            blit(poseStack, QUENCHED_ICONS, right - width, top, 81 - width, 18, width, 9);
        }

        for (int i = 0; i < 10; ++i)
        {
            int idx = i * 2 + 1;
            int x = right - i * 8 - 9;
            int y = shake ? top + random.nextInt(3) - 1 : top;
            if (i == wave)
                y -= 2;

            RenderSystem.setShaderColor(1f, 1f, 1f, 1f);
            blit(poseStack, THIRST_ICONS, x, y, 0, 0, 9, 9, 25, 9);

            RenderSystem.setShaderColor(red, green, blue, 1f);
            if (idx <= level)
                blit(poseStack, fill, x, y, idx < level ? 16 : 8, 0, 9, 9, 25, 9);
            if (showQuenched && quenched > i * 2)
                blit(poseStack, outline, x, y, outlineU(quenched, i), 0, 9, 9);

            if (gain == null)
                continue;
            RenderSystem.setShaderColor(red, green, blue, flash);
            if (newLevel > level && idx >= level && idx <= newLevel)
                blit(poseStack, fill, x, y, idx < newLevel ? 16 : 8, 0, 9, 9, 25, 9);
            if (newQuenched > quenched && newQuenched > i * 2 && i >= quenched / 2)
                blit(poseStack, outline, x, y, outlineU(newQuenched, i), 0, 9, 9);

            if (newLevel < level && idx <= level && idx >= newLevel)
            {
                RenderSystem.setShaderColor(SALTY_RED, SALTY_GREEN, SALTY_BLUE, flash);
                blit(poseStack, THIRST_MASK, x, y, idx < level ? 16 : 8, 0, 9, 9, 25, 9);
                if (idx == newLevel)
                {
                    RenderSystem.setShaderColor(red, green, blue, 1f);
                    blit(poseStack, fill, x, y, 8, 0, 9, 9, 25, 9);
                }
            }
            if (newQuenched < quenched && quenched > i * 2 && newQuenched < i * 2 + 2)
            {
                RenderSystem.setShaderColor(SALTY_RED, SALTY_GREEN, SALTY_BLUE, flash);
                blit(poseStack, QUENCHED_MASK, x, y, outlineU(quenched, i), 0, 9, 9);
            }
        }
        RenderSystem.setShaderColor(1f, 1f, 1f, 1f);
        RenderSystem.disableBlend();
        minecraft.getProfiler().pop();
    }

    /** Draws part of a texture: 256x256 unless the texture size is given. */
    private static void blit(PoseStack poseStack, ResourceLocation texture, int x, int y, int u, int v, int width, int height)
    {
        blit(poseStack, texture, x, y, u, v, width, height, 256, 256);
    }

    static void blit(PoseStack poseStack, ResourceLocation texture, int x, int y, int u, int v, int width, int height, int textureWidth, int textureHeight)
    {
        RenderSystem.setShaderTexture(0, texture);
        GuiComponent.blit(poseStack, x, y, u, v, width, height, textureWidth, textureHeight);
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
        if (values != null)
            return AppleSkinCompat.drinkPreview(false) ? values : null;
        return AppleSkinCompat.drinkPreview(true) ? drinkValues(player, player.getOffhandItem()) : null;
    }

    private static @Nullable ThirstValues drinkValues(Player player, ItemStack stack)
    {
        if (stack.isEmpty() || !ThirstHelper.playerRestoresThirst(stack, player))
            return null;
        FoodProperties food = stack.getFoodProperties(player);
        if (food != null && !player.canEat(food.canAlwaysEat()))
            return null;
        return ThirstHelper.drinkValuesOf(stack);
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

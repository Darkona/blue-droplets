package com.darkona.droplets.foundation.gui;

import com.mojang.blaze3d.systems.RenderSystem;
import com.darkona.droplets.BlueDroplets;
import com.darkona.droplets.compat.supernatural.SupernaturalCompat;
import com.darkona.droplets.compat.vampirism.VampirismCompat;
import com.darkona.droplets.content.thirst.ThirstHelper;
import com.darkona.droplets.foundation.common.capability.IThirst;
import com.darkona.droplets.foundation.common.capability.ModAttachment;
import com.darkona.droplets.foundation.config.ClientConfig;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;

@EventBusSubscriber(value = Dist.CLIENT)
public class ThirstBarRenderer
{
    public static final ResourceLocation THIRST_ICONS = BlueDroplets.asResource("textures/gui/thirst_icons.png");
    public static boolean cancelRender = false;
    static Minecraft minecraft = Minecraft.getInstance();
    protected final static RandomSource random = RandomSource.create();
    private static int lastNotFullTick;

    @SubscribeEvent
    public static void onBeginRenderAir(RenderGuiEvent.Pre event)
    {
        if (event.getType() != RenderGuiEvent.Type.AIR)
            return;

        Entity vehicle = minecraft.player.getVehicle();
        boolean isMounted = vehicle != null && vehicle.showVehicleHealth();
        cancelRender =false;
        if (!isMounted && !minecraft.options.hideGui && shouldDrawSurvivalElements(minecraft))
        {
            if(VampirismCompat.isVampire(minecraft.player))
            {
                cancelRender =true;
                return;
            }

            if(minecraft.player.isAlive() && !minecraft.player.getData(ModAttachment.PLAYER_THIRST).getShouldTickThirst()){
                cancelRender = true;
                return;
            }

            if (shouldHideBar())
            {
                cancelRender = true;
                return;
            }

            setupOverlayRenderState(true, false);

            render(event.getScreenWidth(),event.getScreenHeight(),event.getGuiGraphics());
        }
    }

    private static boolean shouldHideBar()
    {
        if (!ClientConfig.HIDE_BAR_WHEN_FULL.get())
            return false;

        Player player = minecraft.player;
        int ticks = minecraft.gui.getGuiTicks();
        if (player.getData(ModAttachment.PLAYER_THIRST).getThirst() < 20
                || ThirstHelper.itemRestoresThirst(player.getMainHandItem())
                || ThirstHelper.itemRestoresThirst(player.getOffhandItem())
                || ticks < lastNotFullTick)
        {
            lastNotFullTick = ticks;
            return false;
        }

        return ticks - lastNotFullTick >= ClientConfig.HIDE_BAR_DELAY_TICKS.get();
    }

    public static boolean shouldDrawSurvivalElements(Minecraft minecraft)
    {
        return minecraft.gameMode.canHurtPlayer() && minecraft.getCameraEntity() instanceof Player;
    }

    public static void setupOverlayRenderState(boolean blend, boolean depthTest)
    {
        if (blend)
        {
            RenderSystem.enableBlend();
            RenderSystem.defaultBlendFunc();
        }
        else RenderSystem.disableBlend();

        if (depthTest)
            RenderSystem.enableDepthTest();
        else
            RenderSystem.disableDepthTest();

        RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
        RenderSystem.setShader(GameRenderer::getPositionTexShader);
    }

    public static void render(int width, int height, GuiGraphics guiGraphics)
    {
        minecraft.getProfiler().push("thirst");
        IThirst thirst = minecraft.player.getData(ModAttachment.PLAYER_THIRST);

        ResourceLocation thirst_icons = SupernaturalCompat.getVampireIcons(THIRST_ICONS, minecraft.player);

        RenderSystem.enableBlend();
        RenderSystem.setShaderTexture(0, thirst_icons);
        int left = width / 2 + 91 + ClientConfig.THIRST_BAR_X_OFFSET.get();
        int top = height - minecraft.gui.rightHeight + ClientConfig.THIRST_BAR_Y_OFFSET.get();
        minecraft.gui.rightHeight += 10;

        int level = thirst.getThirst();
        boolean shake = thirst.getQuenched() <= 0 && minecraft.gui.getGuiTicks() % (level * 3 + 1) == 0;

        for (int i = 0; i < 10; ++i)
        {
            int idx = i * 2 + 1;
            int x = left - i * 8 - 9;
            int y = top;

            if (shake)
            {
                y = top + (random.nextInt(3) - 1);
            }

            guiGraphics.blit(thirst_icons, x, y, 0, 0, 9, 9, 25, 9);

            if (idx < level)
                guiGraphics.blit(thirst_icons, x, y, 16, 0, 9, 9, 25, 9);
            else if (idx == level)
                guiGraphics.blit(thirst_icons, x, y, 8, 0, 9, 9, 25, 9);
        }
        RenderSystem.disableBlend();

        minecraft.getProfiler().pop();
    }
}

package com.darkona.droplets.foundation.mixin;

import com.darkona.droplets.BlueDroplets;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import net.minecraft.server.MinecraftServer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

import java.util.List;

/**
 * Minecraft 1.19.2 enables every data pack it has not seen before, and has no pack source that stays off (later
 * versions have {@code PackSource.FEATURE}). The mod's opt-in packs (presets, smoker purification) count as disabled
 * here until a player enables them, when creating the world or with {@code /datapack enable}.
 */
@Mixin(MinecraftServer.class)
public abstract class MixinMinecraftServer
{
    @WrapOperation(method = "configurePackRepository", at = @At(value = "INVOKE", target = "Ljava/util/List;contains(Ljava/lang/Object;)Z"))
    private static boolean blue_droplets$optInPacksStayOff(List<String> disabled, Object id, Operation<Boolean> original)
    {
        return original.call(disabled, id) || id instanceof String pack && BlueDroplets.isOptInPack(pack);
    }
}

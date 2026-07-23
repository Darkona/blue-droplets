package com.darkona.droplets.foundation.mixin.create;

import com.darkona.droplets.content.purity.WaterPurity;
import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import com.simibubi.create.foundation.utility.Lang;
import com.simibubi.create.foundation.utility.LangBuilder;
import net.minecraft.network.chat.Component;
import net.minecraft.tags.FluidTags;
import net.minecraftforge.fluids.FluidStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

/**
 * Create's goggles name fluids with {@code Lang.fluidName}: water and fluids that carry purity get their purity before
 * the name. In Create 0.5.1 its only callers are the goggle tooltips of tanks ({@code IHaveGoggleInformation}) and of
 * basins; Mixin 0.8.5 cannot inject into the interface's default method, as later versions do.
 */
@Mixin(value = Lang.class, remap = false)
public abstract class MixinLang
{
    @ModifyReturnValue(method = "fluidName", at = @At("RETURN"))
    private static LangBuilder blue_droplets$purityBeforeName(LangBuilder name, FluidStack fluid)
    {
        if (!WaterPurity.enabled() || !WaterPurity.hasPurity(fluid) && !fluid.getFluid().is(FluidTags.WATER))
            return name;
        int purity = WaterPurity.getPurity(fluid);
        return Lang.builder().add(Component.empty()
                .append(Component.literal(WaterPurity.getPurityText(purity) + " ").withStyle(style -> style.withColor(WaterPurity.getPurityColor(purity))))
                .append(name.component()));
    }
}

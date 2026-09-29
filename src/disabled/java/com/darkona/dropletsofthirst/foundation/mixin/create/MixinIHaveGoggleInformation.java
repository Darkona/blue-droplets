package com.darkona.dropletsofthirst.foundation.mixin.create;

import com.darkona.dropletsofthirst.content.purity.WaterPurity;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.simibubi.create.api.equipment.goggles.IHaveGoggleInformation;
import com.simibubi.create.foundation.utility.CreateLang;
import net.createmod.catnip.lang.LangBuilder;
import net.minecraft.network.chat.Component;
import net.minecraft.tags.FluidTags;
import net.neoforged.neoforge.fluids.FluidStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

/**
 * Create goggles on a fluid container: water and fluids that carry a purity show it, in its colour, before the fluid
 * name. Create styles the whole line gray afterwards; the purity keeps its own colour because it is a child component.
 */
@Mixin(value = IHaveGoggleInformation.class, remap = false)
public interface MixinIHaveGoggleInformation
{
    @WrapOperation(method = "containedFluidTooltip", at = @At(value = "INVOKE", target = "Lcom/simibubi/create/foundation/utility/CreateLang;fluidName(Lnet/neoforged/neoforge/fluids/FluidStack;)Lnet/createmod/catnip/lang/LangBuilder;"))
    private LangBuilder droplets_of_thirst$purityBeforeName(FluidStack fluid, Operation<LangBuilder> original)
    {
        LangBuilder name = original.call(fluid);
        if (!WaterPurity.enabled() || !WaterPurity.hasPurity(fluid) && !fluid.is(FluidTags.WATER))
            return name;
        int purity = WaterPurity.getPurity(fluid);
        return CreateLang.builder().add(Component.empty()
                .append(Component.literal(WaterPurity.getPurityText(purity) + " ").withColor(WaterPurity.getPurityColor(purity)))
                .append(name.component()));
    }
}

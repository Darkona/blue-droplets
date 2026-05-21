package com.darkona.droplets.foundation.mixin.create;


import com.simibubi.create.api.equipment.goggles.IHaveGoggleInformation;
import com.simibubi.create.foundation.utility.CreateLang;
import com.darkona.droplets.content.purity.WaterPurity;
import net.createmod.catnip.lang.LangBuilder;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.tags.FluidTags;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;

import java.util.List;

@Mixin(value = IHaveGoggleInformation.class,remap = false)
public interface MixinIHaveGoggleInformation {
    /**
     * @author mlus
     * @reason add purity information
     */
    @Overwrite
    default boolean containedFluidTooltip(List<Component> tooltip, boolean isPlayerSneaking,
                                          IFluidHandler handler) {
        if (handler == null)
            return false;

        if (handler.getTanks() == 0)
            return false;

        LangBuilder mb = CreateLang.translate("generic.unit.millibuckets");
        CreateLang.translate("gui.goggles.fluid_container")
                .forGoggles(tooltip);

        boolean isEmpty = true;
        for (int i = 0; i < handler.getTanks(); i++) {
            FluidStack fluidStack = handler.getFluidInTank(i);
            if (fluidStack.isEmpty())
                continue;

            if(WaterPurity.enabled() && (WaterPurity.hasPurity(fluidStack) || fluidStack.is(FluidTags.WATER))){
                int purity = WaterPurity.getPurity(fluidStack);
                CreateLang.builder()
                        .add(Component.literal(WaterPurity.getPurityText(purity) + " ").withColor(WaterPurity.getPurityColor(purity)))
                        .add(fluidStack.getHoverName().copy().withStyle(ChatFormatting.GRAY))
                        .forGoggles(tooltip, 1);
            }else {
                CreateLang.fluidName(fluidStack)
                        .style(ChatFormatting.GRAY)
                        .forGoggles(tooltip, 1);
            }



            CreateLang.builder()
                    .add(CreateLang.number(fluidStack.getAmount())
                            .add(mb)
                            .style(ChatFormatting.GOLD))
                    .text(ChatFormatting.GRAY, " / ")
                    .add(CreateLang.number(handler.getTankCapacity(i))
                            .add(mb)
                            .style(ChatFormatting.DARK_GRAY))
                    .forGoggles(tooltip, 1);

            isEmpty = false;
        }

        if (handler.getTanks() > 1) {
            if (isEmpty)
                tooltip.removeLast();
            return true;
        }

        if (!isEmpty)
            return true;

        CreateLang.translate("gui.goggles.fluid_container.capacity")
                .add(CreateLang.number(handler.getTankCapacity(0))
                        .add(mb)
                        .style(ChatFormatting.GOLD))
                .style(ChatFormatting.GRAY)
                .forGoggles(tooltip, 1);

        return true;
    }

}

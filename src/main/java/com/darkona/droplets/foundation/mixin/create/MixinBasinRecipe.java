package com.darkona.droplets.foundation.mixin.create;

import com.darkona.droplets.content.data.DropletsTags;
import com.darkona.droplets.content.purity.WaterPurity;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.sugar.Local;
import com.simibubi.create.content.processing.basin.BasinBlockEntity;
import com.simibubi.create.content.processing.basin.BasinRecipe;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.fluids.capability.IFluidHandler;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;

import java.util.List;

/**
 * Basin outputs in {@code blue_droplets:carries_purity} without their own purity take the purity of the input water.
 * Works on copies in the basin's output list, never on the recipe's shared stacks, and only in the simulated pass
 * that builds that list (the input is not drained yet); skipped when Create only tests the recipe.
 */
@Mixin(BasinRecipe.class)
public class MixinBasinRecipe {

    @WrapOperation(
            method = "apply(Lcom/simibubi/create/content/processing/basin/BasinBlockEntity;Lnet/minecraft/world/item/crafting/Recipe;Z)Z",
            at = @At(value = "INVOKE", target = "Lcom/simibubi/create/content/processing/basin/BasinBlockEntity;acceptOutputs(Ljava/util/List;Ljava/util/List;Z)Z"),
            remap = false)
    private static boolean blue_droplets$carryPurity(BasinBlockEntity basin, List<ItemStack> items, List<FluidStack> fluids, boolean simulate,
                                                    Operation<Boolean> original, @Local(argsOnly = true) boolean test)
    {
        if (simulate && !test && !fluids.isEmpty())
        {
            int purity = blue_droplets$inputPurity(basin);
            if (purity >= WaterPurity.MIN_PURITY)
                for (int i = 0; i < fluids.size(); i++)
                {
                    FluidStack fluid = fluids.get(i);
                    if (fluid.getFluid().is(DropletsTags.CARRIES_PURITY) && !WaterPurity.hasPurity(fluid))
                        fluids.set(i, WaterPurity.addPurity(fluid.copy(), purity));
                }
        }
        return original.call(basin, items, fluids, simulate);
    }

    @Unique
    private static int blue_droplets$inputPurity(BasinBlockEntity basin)
    {
        IFluidHandler input = basin.inputTank.getCapability().orElse(null);
        if (input == null)
            return -1;
        for (int tank = 0; tank < input.getTanks(); tank++)
        {
            FluidStack fluid = input.getFluidInTank(tank);
            if (WaterPurity.hasPurity(fluid))
                return WaterPurity.getPurity(fluid);
        }
        return -1;
    }
}

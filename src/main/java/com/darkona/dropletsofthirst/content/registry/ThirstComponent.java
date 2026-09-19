package com.darkona.dropletsofthirst.content.registry;

import com.mojang.serialization.Codec;
import com.darkona.dropletsofthirst.DropletsOfThirst;
import com.darkona.dropletsofthirst.api.PurityLevel;
import com.darkona.dropletsofthirst.content.purity.WaterPurity;
import com.darkona.dropletsofthirst.foundation.config.PurityConfig;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.codec.ByteBufCodecs;
import net.neoforged.neoforge.common.MutableDataComponentHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

import java.util.function.Consumer;
import java.util.function.Function;

public class ThirstComponent {
    public static final DeferredRegister<DataComponentType<?>> DR = DeferredRegister
            .create(Registries.DATA_COMPONENT_TYPE, DropletsOfThirst.ID);

    /** {@code purity.defaultPurity}'s own default, for values read before the config is loaded. */
    private static final int UNLOADED_DEFAULT_PURITY = PurityLevel.ACCEPTABLE.level();

    private ThirstComponent() {
    }

    /**
     * Water purity, {@link PurityLevel#MIN} to {@link PurityLevel#MAX}. Out-of-range values (old saves, other mods) read as the default purity instead of failing
     * the whole item or fluid, so they also stack with water of that purity.
     */
    public static final DataComponentType<Integer> PURITY = register("purity",
            builder -> builder.persistent(Codec.INT.xmap(ThirstComponent::bounded, Function.identity()))
                    .networkSynchronized(ByteBufCodecs.VAR_INT.map(ThirstComponent::bounded, Function.identity())));

    /**
     * {@code thirst:purity} of Thirst Was Taken stacks, on its scale of four levels. Never kept: stacks swap it for
     * {@link #PURITY} as they are built from saved or received data ({@link #migrateLegacyPurity}).
     */
    public static final DataComponentType<Integer> LEGACY_PURITY = register("legacy_purity",
            builder -> builder.persistent(Codec.INT).networkSynchronized(ByteBufCodecs.VAR_INT));

    /**
     * Replaces a {@link #LEGACY_PURITY} by the same purity on the new scale, unless the stack already has a
     * {@link #PURITY}. One map lookup when there is none.
     */
    public static void migrateLegacyPurity(MutableDataComponentHolder stack)
    {
        if (!stack.has(LEGACY_PURITY))
            return;
        Integer legacy = stack.remove(LEGACY_PURITY);
        if (legacy != null && !stack.has(PURITY))
            stack.set(PURITY, bounded(LegacyIds.purityFromLegacy(legacy)));
    }

    private static int bounded(int purity)
    {
        if (purity >= WaterPurity.MIN_PURITY && purity <= WaterPurity.MAX_PURITY)
            return purity;
        return PurityConfig.SPEC.isLoaded() ? WaterPurity.defaultPurity() : UNLOADED_DEFAULT_PURITY;
    }

    private static <T> DataComponentType<T> register(String name, Consumer<DataComponentType.Builder<T>> customizer) {
        var builder = DataComponentType.<T>builder();
        customizer.accept(builder);
        var componentType = builder.build();
        DR.register(name, () -> componentType);
        return componentType;
    }
}

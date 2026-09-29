package com.darkona.dropletsofthirst.content.purity;

import com.darkona.dropletsofthirst.DropletsOfThirst;
import com.darkona.dropletsofthirst.foundation.config.ClientConfig;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.client.color.item.ItemTintSource;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.util.ExtraCodecs;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RegisterColorHandlersEvent;
import org.jetbrains.annotations.Nullable;

@EventBusSubscriber(modid = DropletsOfThirst.ID, value = Dist.CLIENT)
public final class PurityTintClient
{
    /**
     * Item model tint {@code droplets_of_thirst:water_purity}: the purity color for a water container with a stored purity,
     * else {@code default}. The terracotta water bowl's item model uses it for its grey liquid layer; water bottles
     * keep vanilla's potion tint, changed in {@code MixinPotionTint}.
     */
    public record WaterPurityTint(int defaultColor) implements ItemTintSource
    {
        public static final MapCodec<WaterPurityTint> MAP_CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
                ExtraCodecs.RGB_COLOR_CODEC.optionalFieldOf("default", PurityTint.VANILLA_WATER).forGetter(WaterPurityTint::defaultColor)
        ).apply(instance, WaterPurityTint::new));

        @Override
        public int calculate(ItemStack stack, @Nullable ClientLevel level, @Nullable LivingEntity owner)
        {
            int color = 0xFF000000 | defaultColor;
            return ClientConfig.TINT_WATER_BY_PURITY.get() ? PurityTint.color(stack, 0, color) : color;
        }

        @Override
        public MapCodec<WaterPurityTint> type()
        {
            return MAP_CODEC;
        }
    }

    @SubscribeEvent
    public static void registerTintSources(RegisterColorHandlersEvent.ItemTintSources event)
    {
        event.register(DropletsOfThirst.asResource("water_purity"), WaterPurityTint.MAP_CODEC);
    }

    private PurityTintClient() {}
}

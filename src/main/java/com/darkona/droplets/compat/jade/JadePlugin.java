package com.darkona.droplets.compat.jade;

import net.minecraft.network.chat.TextComponent;
import com.darkona.droplets.BlueDroplets;
import com.darkona.droplets.content.purity.WaterPurity;
import it.unimi.dsi.fastutil.ints.IntArrayList;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.Level;
import net.minecraft.tags.FluidTags;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.fluids.capability.CapabilityFluidHandler;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.fluids.capability.IFluidHandler;
import mcp.mobius.waila.api.BlockAccessor;
import mcp.mobius.waila.api.IComponentProvider;
import mcp.mobius.waila.api.IServerDataProvider;
import mcp.mobius.waila.api.ITooltip;
import mcp.mobius.waila.api.IWailaClientRegistration;
import mcp.mobius.waila.api.IWailaCommonRegistration;
import mcp.mobius.waila.api.IWailaPlugin;
import mcp.mobius.waila.api.TooltipPosition;
import mcp.mobius.waila.api.WailaPlugin;
import mcp.mobius.waila.api.config.IPluginConfig;

/**
 * Jade: water purity of water cauldrons (murky, or clean on a heat source) and of the tanks of any block exposing a fluid handler
 * (sent by the server). Jade 5 keeps the Waila API ({@code mcp.mobius.waila.api}). Jade finds and loads this class itself, only when installed; nothing else imports Jade.
 */
@WailaPlugin(BlueDroplets.ID)
public class JadePlugin implements IWailaPlugin
{
    @Override
    public void register(IWailaCommonRegistration registration)
    {
        registration.registerBlockDataProvider(PurityProvider.INSTANCE, BlockEntity.class);
    }

    @Override
    public void registerClient(IWailaClientRegistration registration)
    {
        registration.registerComponentProvider(PurityProvider.INSTANCE, TooltipPosition.BODY, Block.class);
    }

    private enum PurityProvider implements IComponentProvider, IServerDataProvider<BlockEntity>
    {
        INSTANCE;

        private static final ResourceLocation UID = BlueDroplets.asResource("purity");
        private static final String KEY = UID.toString();

        @Override
        public void appendServerData(CompoundTag data, ServerPlayer player, Level level, BlockEntity blockEntity, boolean showDetails)
        {
            IFluidHandler handler = blockEntity.getCapability(CapabilityFluidHandler.FLUID_HANDLER_CAPABILITY).orElse(null);
            if (handler == null || !WaterPurity.enabled())
                return;

            IntArrayList purities = new IntArrayList();
            for (int tank = 0; tank < handler.getTanks(); tank++)
            {
                FluidStack fluid = handler.getFluidInTank(tank);
                if (!fluid.isEmpty() && (WaterPurity.hasPurity(fluid) || fluid.getFluid().is(FluidTags.WATER)))
                    purities.add(WaterPurity.getPurity(fluid));
            }
            if (!purities.isEmpty())
                data.putIntArray(KEY, purities.toIntArray());
        }

        @Override
        public void appendTooltip(ITooltip tooltip, BlockAccessor accessor, IPluginConfig config)
        {
            if (!WaterPurity.enabled())
                return;
            BlockState state = accessor.getBlockState();
            if (state.is(Blocks.WATER_CAULDRON))
                add(tooltip, WaterPurity.cauldronPurity(accessor.getLevel(), accessor.getPosition()));
            else
                for (int purity : accessor.getServerData().getIntArray(KEY))
                    add(tooltip, WaterPurity.sanitizePurity(purity));
        }

        private static void add(ITooltip tooltip, int purity)
        {
            tooltip.add(new TextComponent(WaterPurity.getPurityText(purity)).withStyle(style -> style.withColor(WaterPurity.getPurityColor(purity))));
        }
    }
}
